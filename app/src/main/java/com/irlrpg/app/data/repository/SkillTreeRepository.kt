package com.irlrpg.app.data.repository

import com.irlrpg.app.data.local.dao.AttributeDao
import com.irlrpg.app.data.local.dao.PlayerDao
import com.irlrpg.app.data.local.dao.QuestDao
import com.irlrpg.app.data.local.dao.SkillNodeDao
import com.irlrpg.app.data.local.entity.SkillNodeEntity
import com.irlrpg.app.domain.game.LevelProgression
import com.irlrpg.app.domain.game.LevelRules
import com.irlrpg.app.domain.game.SkillNodeView
import com.irlrpg.app.domain.game.SkillTreeEngine
import com.irlrpg.app.domain.model.AttributeKind
import com.irlrpg.app.domain.model.SkillUnlockRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/** A quest that feeds a particular skill node (shown in the node detail sheet). */
data class NodeQuestLink(
    val questId: Long,
    val title: String,
    val emoji: String,
    val isActive: Boolean,
)

data class SkillTreeState(
    val loading: Boolean = true,
    val nodes: List<SkillNodeView> = emptyList(),
    val mastery: Float = 0f,
    val mastered: Int = 0,
    val total: Int = 0,
    val inProgress: Int = 0,
    val locked: Int = 0,
    val available: Int = 0,
    val totalTreeXp: Int = 0,
    val questsByAttribute: Map<Long, List<NodeQuestLink>> = emptyMap(),
)

/**
 * Skill tree reads + edits. Node state is always derived (XP from the linked
 * attribute), so nothing has to be migrated or recomputed when the tree changes:
 * add a node and existing XP immediately counts towards it.
 */
class SkillTreeRepository(
    private val skillNodeDao: SkillNodeDao,
    private val attributeDao: AttributeDao,
    private val questDao: QuestDao,
    private val playerDao: PlayerDao,
    private val levelRules: () -> LevelRules,
) {

    fun observeTree(): Flow<SkillTreeState> = combine(
        skillNodeDao.observeAll(),
        attributeDao.observeAll(),
        questDao.observeAll(),
        playerDao.observe(),
    ) { nodes, attributes, quests, stats ->
        val totalXp = stats?.totalXp ?: 0
        val views = SkillTreeEngine.evaluate(nodes, attributes, totalXp)
        SkillTreeState(
            loading = false,
            nodes = views,
            mastery = SkillTreeEngine.masteryFraction(views),
            mastered = views.count { it.state == com.irlrpg.app.domain.model.SkillNodeState.Mastered },
            total = views.size,
            inProgress = views.count { it.state == com.irlrpg.app.domain.model.SkillNodeState.InProgress },
            locked = views.count { it.state == com.irlrpg.app.domain.model.SkillNodeState.Locked },
            available = views.count { it.state == com.irlrpg.app.domain.model.SkillNodeState.Available },
            totalTreeXp = views.sumOf { it.xp },
            questsByAttribute = attributes.associate { attribute ->
                attribute.id to quests
                    .filter { quest -> quest.attributeKind.equals(attribute.name, true) || quest.attributeKind == attribute.attributeKind }
                    .map { quest ->
                        NodeQuestLink(
                            questId = quest.id,
                            title = quest.title,
                            emoji = quest.emoji,
                            isActive = quest.isActive,
                        )
                    }
            },
        )
    }.flowOn(Dispatchers.Default)

    fun observeNodes(): Flow<List<SkillNodeEntity>> = skillNodeDao.observeAll()

    suspend fun nodes(): List<SkillNodeEntity> = skillNodeDao.all()

    suspend fun node(id: Long): SkillNodeEntity? = skillNodeDao.byId(id)

    suspend fun nodeCount(): Int = skillNodeDao.count()

    /** Attribute list used by the node editor (built-in + custom skills). */
    fun observeAttributes() = attributeDao.observeAll()

    suspend fun attributes() = attributeDao.all()

    suspend fun createNode(
        name: String,
        emoji: String,
        description: String?,
        accentId: String,
        parentId: Long?,
        attributeId: Long?,
        masteryXp: Int,
        unlockRule: SkillUnlockRule,
        unlockParentMastery: Int,
        requiredAnyXp: Int,
    ): Long = skillNodeDao.insert(
        SkillNodeEntity(
            name = name.trim(),
            emoji = emoji,
            description = description?.trim()?.takeIf { it.isNotBlank() },
            accentId = accentId,
            parentId = parentId,
            attributeId = attributeId,
            masteryXp = masteryXp.coerceIn(30, 20_000),
            unlockRule = unlockRule.name,
            unlockParentMastery = unlockParentMastery.coerceAtLeast(0),
            requiredAnyXp = requiredAnyXp.coerceAtLeast(0),
            isBuiltIn = false,
            sortOrder = 100 + (skillNodeDao.count()),
        )
    )

    suspend fun updateNode(node: SkillNodeEntity) = skillNodeDao.update(node)

    /**
     * Deleting a node never deletes a branch: children are detached and become
     * roots, so a user can restructure the tree without losing skills or their XP.
     */
    suspend fun deleteNode(id: Long): Int {
        val children = skillNodeDao.childCount(id)
        skillNodeDao.detachChildren(id)
        skillNodeDao.deleteById(id)
        return children
    }

    suspend fun rescanAttributes() = attributeDao.all()

    /** Attributes that have XP and no node yet — offered as suggested nodes. */
    suspend fun unlinkedAttributes(): List<com.irlrpg.app.data.local.entity.AttributeEntity> {
        val linked = skillNodeDao.all().mapNotNull { it.attributeId }.toSet()
        return attributeDao.all().filter { it.id !in linked }
    }

    fun levelFor(totalXp: Int): Int = LevelProgression(levelRules()).levelForTotalXp(totalXp)

    fun defaultMasteryFor(kind: AttributeKind): Int = when (kind) {
        AttributeKind.Strength -> 300
        AttributeKind.Intellect -> 320
        AttributeKind.Focus -> 280
        AttributeKind.Vitality -> 260
        AttributeKind.Creativity -> 300
        AttributeKind.Discipline -> 340
    }
}
