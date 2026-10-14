package com.irlrpg.app.domain.game

import com.irlrpg.app.data.local.entity.AttributeEntity
import com.irlrpg.app.data.local.entity.SkillNodeEntity
import com.irlrpg.app.domain.model.SkillNodeState
import com.irlrpg.app.domain.model.SkillUnlockRule
import kotlin.math.roundToInt

/**
 * Derived view of one skill tree node. Nothing here is persisted: XP comes from
 * the linked attribute (maintained transactionally since Step 1), and the state
 * is computed from that XP plus the parent chain.
 */
data class SkillNodeView(
    val node: SkillNodeEntity,
    val xp: Int,
    val masteryXp: Int,
    val attributeName: String?,
    val attributeEmoji: String?,
    val children: List<Long>,
    val depth: Int,
    val state: SkillNodeState,
    /** Plain-language reason when the node is locked. */
    val lockReason: String?,
    val requirementProgress: Float,
    val requirementText: String,
) {
    val fraction: Float
        get() = if (masteryXp <= 0) 0f else (xp.toFloat() / masteryXp.toFloat()).coerceIn(0f, 1f)
    val isUnlocked: Boolean get() = state != SkillNodeState.Locked
}

/**
 * Computes the skill tree.
 *
 * Unlocking is *derived*, which is what makes Step 2 safe to add on top of Step 1:
 * a player who already earned 600 XP in "Reading" finds the Comprehension node
 * available immediately, and no existing progress can be lost by introducing the
 * tree. Locked nodes always explain what exactly is missing.
 */
object SkillTreeEngine {

    fun evaluate(
        nodes: List<SkillNodeEntity>,
        attributes: List<AttributeEntity>,
        totalXp: Int,
    ): List<SkillNodeView> {
        if (nodes.isEmpty()) return emptyList()
        val attributesById = attributes.associateBy { it.id }
        val childrenByParent = nodes.groupBy { it.parentId }
        val byId = nodes.associateBy { it.id }

        return nodes.map { node ->
            val attribute = node.attributeId?.let { attributesById[it] }
            val xp = attribute?.xpContributed ?: 0
            val parent = node.parentId?.let { byId[it] }

            val (state, lockReason, requirementProgress, requirementText) = resolve(
                node = node,
                parent = parent,
                parentXp = parent?.let { p -> p.attributeId?.let { attributesById[it]?.xpContributed ?: 0 } } ?: 0,
                totalXp = totalXp,
                xp = xp,
            )

            SkillNodeView(
                node = node,
                xp = xp,
                masteryXp = node.masteryXp,
                attributeName = attribute?.name,
                attributeEmoji = attribute?.emoji,
                children = childrenByParent[node.id].orEmpty().map { it.id },
                depth = depthOf(node, byId),
                state = state,
                lockReason = lockReason,
                requirementProgress = requirementProgress,
                requirementText = requirementText,
            )
        }
    }

    private fun depthOf(node: SkillNodeEntity, byId: Map<Long, SkillNodeEntity>): Int {
        var depth = 0
        var current: SkillNodeEntity? = node
        val guard = mutableSetOf<Long>()
        while (current?.parentId != null && guard.add(current.id)) {
            current = byId[current.parentId]
            depth++
            if (depth > 32) break
        }
        return depth
    }

    private data class Resolution(
        val state: SkillNodeState,
        val lockReason: String?,
        val requirementProgress: Float,
        val requirementText: String,
    )

    private fun resolve(
        node: SkillNodeEntity,
        parent: SkillNodeEntity?,
        parentXp: Int,
        totalXp: Int,
        xp: Int,
    ): Resolution {
        val rule = SkillUnlockRule.fromName(node.unlockRule)
        val parentName = parent?.name ?: "its parent skill"

        val unlocked = when (rule) {
            SkillUnlockRule.Root -> true
            SkillUnlockRule.ParentMastery -> parentXp >= node.unlockParentMastery
            SkillUnlockRule.ParentProgress -> parentXp >= (parent?.masteryXp ?: 0) / 2
            SkillUnlockRule.TotalXp -> totalXp >= node.requiredAnyXp
        }

        val requirementText = when (rule) {
            SkillUnlockRule.Root -> "Open from the start"
            SkillUnlockRule.ParentMastery ->
                "${node.unlockParentMastery} XP in $parentName (currently $parentXp)"
            SkillUnlockRule.ParentProgress ->
                "half-mastered $parentName (${(parent?.masteryXp ?: 0) / 2} XP, currently $parentXp)"
            SkillUnlockRule.TotalXp -> "${node.requiredAnyXp} XP lifetime (currently $totalXp)"
        }

        val requirementProgress = when (rule) {
            SkillUnlockRule.Root -> 1f
            SkillUnlockRule.ParentMastery ->
                if (node.unlockParentMastery <= 0) 1f
                else (parentXp.toFloat() / node.unlockParentMastery.toFloat()).coerceIn(0f, 1f)
            SkillUnlockRule.ParentProgress -> {
                val need = ((parent?.masteryXp ?: 0) / 2).coerceAtLeast(1)
                (parentXp.toFloat() / need.toFloat()).coerceIn(0f, 1f)
            }
            SkillUnlockRule.TotalXp ->
                if (node.requiredAnyXp <= 0) 1f
                else (totalXp.toFloat() / node.requiredAnyXp.toFloat()).coerceIn(0f, 1f)
        }

        if (!unlocked) {
            val reason = when (rule) {
                SkillUnlockRule.Root -> null
                SkillUnlockRule.ParentMastery ->
                    "Unlocks at ${node.unlockParentMastery} XP in $parentName · you have $parentXp XP"
                SkillUnlockRule.ParentProgress ->
                    "Unlocks when $parentName is half mastered (${(parent?.masteryXp ?: 0) / 2} XP) · you have $parentXp XP"
                SkillUnlockRule.TotalXp ->
                    "Unlocks at ${node.requiredAnyXp} lifetime XP · you have $totalXp XP"
            }
            return Resolution(SkillNodeState.Locked, reason, requirementProgress, requirementText)
        }

        val state = when {
            node.masteryXp > 0 && xp >= node.masteryXp -> SkillNodeState.Mastered
            xp > 0 -> SkillNodeState.InProgress
            else -> SkillNodeState.Available
        }
        return Resolution(state, null, requirementProgress, requirementText)
    }

    /** Overall tree completion, weighted by each node's mastery requirement. */
    fun masteryFraction(views: List<SkillNodeView>): Float {
        val masters = views.filter { it.state == SkillNodeState.Mastered }.size
        return if (views.isEmpty()) 0f else masters.toFloat() / views.size.toFloat()
    }

    /** XP still needed on a node, and the XP-remaining label used in the UI. */
    fun remainingLabel(view: SkillNodeView): String {
        if (view.state == SkillNodeState.Mastered) return "Mastered"
        val remaining = (view.masteryXp - view.xp).coerceAtLeast(0)
        val percent = (view.fraction * 100).roundToInt()
        return "$remaining XP to master · $percent%"
    }
}
