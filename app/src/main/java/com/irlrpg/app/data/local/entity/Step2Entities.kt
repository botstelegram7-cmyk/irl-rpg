package com.irlrpg.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One objective inside a multi-step quest. Steps are optional: a plain quest
 * simply has none.
 *
 * Progress for a step is derived from `quest_logs.stepId` inside the quest's
 * current period, so a daily multi-step quest resets every day while a long-term
 * challenge accumulates.
 */
@Entity(
    tableName = "quest_steps",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("questId")],
)
data class QuestStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questId: Long,
    val title: String,
    val targetAmount: Long,
    /** [com.irlrpg.app.domain.model.UnitKind] name, or null to inherit the quest unit. */
    val unit: String? = null,
    val customUnit: String? = null,
    val isRequired: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * A materialised occurrence of a recurring quest (today, this ISO week, or the
 * single lifetime bucket of a long-term challenge).
 *
 * The unique index on `(questId, periodKey)` is the database-level guarantee that
 * a recurring quest can never generate two instances for the same period — and
 * therefore can never award duplicate XP for the same completion.
 */
@Entity(
    tableName = "quest_instances",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("questId"),
        Index(value = ["questId", "periodKey"], unique = true),
        Index("epochDay"),
    ],
)
data class QuestInstanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questId: Long,
    val periodKey: String,
    val epochDay: Long,
    /** OPEN | COMPLETED */
    val status: String = STATUS_OPEN,
    val xpEarned: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val STATUS_OPEN = "OPEN"
        const val STATUS_COMPLETED = "COMPLETED"
    }
}

/**
 * A node in the universal skill tree.
 *
 * Nothing about the tree state is stored: a node's XP comes from its linked
 * attribute (which Step 1 already maintains transactionally), and its state
 * (locked / available / in-progress / mastered) is derived from that XP plus the
 * parent's mastery. That keeps upgrades non-destructive — adding the tree can
 * never reset or contradict existing progress.
 */
@Entity(
    tableName = "skill_nodes",
    foreignKeys = [
        ForeignKey(
            entity = SkillNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AttributeEntity::class,
            parentColumns = ["id"],
            childColumns = ["attributeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("parentId"), Index("attributeId")],
)
data class SkillNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "✨",
    val description: String? = null,
    val accentId: String = "lavender",
    val parentId: Long? = null,
    /** Attribute whose earned XP powers this node's mastery. */
    val attributeId: Long? = null,
    /** XP required to master this node. */
    val masteryXp: Int = 240,
    /** [com.irlrpg.app.domain.model.SkillUnlockRule] name. */
    val unlockRule: String,
    /** Parent mastery (XP) required when the rule is ParentMastery. */
    val unlockParentMastery: Int = 0,
    /** XP required when the rule is TotalXp. */
    val requiredAnyXp: Int = 0,
    val isBuiltIn: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * An achievement that has actually been earned. Definitions live in code
 * (`AchievementCatalog`); the database only stores what the player unlocked and
 * when — so no achievement can ever appear "hardcoded as completed".
 */
@Entity(tableName = "achievements_earned")
data class AchievementEarnedEntity(
    @PrimaryKey val code: String,
    val category: String,
    val epochDay: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * A boss challenge: a significant real-life goal expressed as a set of
 * objectives. Optional and fully user-configurable — no combat gameplay.
 */
@Entity(tableName = "boss_challenges")
data class BossChallengeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val emoji: String = "🐉",
    val accentId: String = "peach",
    val deadlineEpochDay: Long? = null,
    val rewardXp: Int = 400,
    /** ACTIVE | COMPLETED | ARCHIVED */
    val status: String = STATUS_ACTIVE,
    /** Preset code when the challenge came from a template. */
    val templateCode: String? = null,
    val startedEpochDay: Long,
    val completedAtEpochDay: Long? = null,
    /** XP already granted — guarantees the victory reward is paid once. */
    val xpAwarded: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_ARCHIVED = "ARCHIVED"
    }
}

/**
 * One objective of a boss challenge. `metric` decides how progress is read:
 * a linked quest's recorded logs, or a manually tracked amount.
 */
@Entity(
    tableName = "boss_objectives",
    foreignKeys = [
        ForeignKey(
            entity = BossChallengeEntity::class,
            parentColumns = ["id"],
            childColumns = ["bossId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("bossId"), Index("questId")],
)
data class BossObjectiveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bossId: Long,
    val title: String,
    /** LINKED_QUEST | MANUAL */
    val metric: String,
    val questId: Long? = null,
    val unitName: String = "units",
    val target: Long = 1,
    val isRequired: Boolean = true,
    /** Manual progress for objectives that are not tied to a quest. */
    val manualProgress: Long = 0,
    val sortOrder: Int = 0,
) {
    companion object {
        const val METRIC_LINKED_QUEST = "LINKED_QUEST"
        const val METRIC_MANUAL = "MANUAL"
    }
}
