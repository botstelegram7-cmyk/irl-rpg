package com.irlrpg.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-owned grouping for activities. The six seeded categories are only a
 * starting point — everything can be renamed, re-coloured or deleted.
 */
@Entity(
    tableName = "categories",
    indices = [Index("name")],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val accentId: String = "lavender",
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * An activity / quest definition. Universal by design: nothing about studying
 * is hardcoded — the tracking fields describe any measurable real-life goal.
 */
@Entity(
    tableName = "quests",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId"), Index("isActive")],
)
data class QuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val emoji: String = "⭐",
    val categoryId: Long? = null,
    /** [com.irlrpg.app.domain.model.TrackingKind] name. */
    val trackingKind: String,
    /** [com.irlrpg.app.domain.model.UnitKind] name or a free-form label. */
    val unit: String,
    val customUnit: String? = null,
    val targetAmount: Long,
    /** [com.irlrpg.app.domain.model.ScheduleKind] name. */
    val scheduleKind: String,
    /** ISO day numbers 1..7 joined for [com.irlrpg.app.domain.model.ScheduleKind.SelectedDays]. */
    val scheduleDays: String = "",
    val xpReward: Int,
    /** [com.irlrpg.app.domain.model.AttributeKind] name, or a custom attribute name. */
    val attributeKind: String,
    val isActive: Boolean = true,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * An immutable record of something the user actually did. XP is stored on the
 * row so history stays auditable and totals never have to be recomputed from
 * mutable rules.
 */
@Entity(
    tableName = "quest_logs",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("questId"), Index("epochDay"), Index("periodKey"), Index("questId", "periodKey")],
)
data class QuestLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questId: Long,
    val amount: Long,
    val unitLabel: String,
    val xpEarned: Int,
    /** True when this row is the entry that finished the period's target. */
    val isCompletion: Boolean = false,
    /** Day / week / month key depending on the quest schedule. */
    val periodKey: String,
    val epochDay: Long,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Single-row table holding the player's persistent progression totals. */
@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "Adventurer",
    val totalXp: Int = 0,
    val createdAtEpochDay: Long,
    val lastQuestAt: Long? = null,
)

/** Skill attribute — six built-in kinds are seeded, custom ones can be added. */
@Entity(
    tableName = "attributes",
    indices = [Index("attributeKind")],
)
data class AttributeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    /** [com.irlrpg.app.domain.model.AttributeKind] name for built-ins, null for custom. */
    val attributeKind: String? = null,
    val accentId: String = "lavender",
    val xpContributed: Int = 0,
    val isBuiltIn: Boolean = false,
    val sortOrder: Int = 0,
)

/** Append-only log of progression milestones and XP awards. */
@Entity(
    tableName = "progression_history",
    indices = [Index("epochDay")],
)
data class ProgressionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** "xp" | "level_up" | "attribute" */
    val type: String,
    val title: String,
    val detail: String? = null,
    val amount: Int = 0,
    val level: Int? = null,
    val epochDay: Long,
    /** Log row that produced this event, when applicable (enables exact undo). */
    val refLogId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
