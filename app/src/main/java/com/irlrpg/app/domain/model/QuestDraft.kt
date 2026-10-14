package com.irlrpg.app.domain.model

import com.irlrpg.app.data.local.entity.QuestEntity

/** One step of a multi-step quest, as typed in the editor. */
data class QuestStepDraft(
    val title: String,
    val targetAmount: Long,
    val isRequired: Boolean = true,
    /** Existing row id when editing, 0 for a newly added step. */
    val id: Long = 0,
)

/** Validated input coming out of the quest editor, before it becomes a row. */
data class QuestDraft(
    val title: String,
    val emoji: String,
    val categoryId: Long?,
    val trackingKind: TrackingKind,
    val unit: UnitKind,
    val customUnit: String?,
    val targetAmount: Long,
    val scheduleKind: ScheduleKind,
    val scheduleDays: Set<Int>,
    val xpReward: Int,
    /** Attribute name — matches a built-in kind or a user-created skill. */
    val attributeName: String,
    val notes: String?,
    // ---- Step 2 additions (all defaulted: Step 1 call sites keep working) ----
    val description: String? = null,
    val difficulty: QuestDifficulty = QuestDifficulty.Medium,
    /** Optional deadline; null means the quest never goes overdue. */
    val deadlineEpochDay: Long? = null,
    val questKind: QuestKind = QuestKind.Standard,
    /** Long-term challenges: planned length in days (0 = open-ended). */
    val durationDays: Int = 0,
    /** Day the quest started — the reference point for long-term progress. */
    val startedEpochDay: Long? = null,
    val steps: List<QuestStepDraft> = emptyList(),
) {
    val unitLabel: String
        get() = if (unit == UnitKind.Custom) (customUnit?.takeIf { it.isNotBlank() } ?: "unit") else unit.label.lowercase()

    val unitShort: String
        get() = if (unit == UnitKind.Custom) (customUnit?.takeIf { it.isNotBlank() } ?: "unit") else unit.short

    fun toEntity(id: Long = 0L, createdAt: Long = System.currentTimeMillis()): QuestEntity = QuestEntity(
        id = id,
        title = title.trim(),
        emoji = emoji,
        categoryId = categoryId,
        trackingKind = trackingKind.name,
        unit = unit.name,
        customUnit = customUnit?.trim()?.takeIf { it.isNotBlank() },
        targetAmount = targetAmount,
        scheduleKind = scheduleKind.name,
        scheduleDays = scheduleDays.sorted().joinToString(","),
        xpReward = xpReward,
        attributeKind = attributeName,
        notes = notes?.trim()?.takeIf { it.isNotBlank() },
        description = description?.trim()?.takeIf { it.isNotBlank() },
        difficulty = difficulty.name,
        deadlineEpochDay = deadlineEpochDay,
        questKind = questKind.name,
        durationDays = durationDays.coerceAtLeast(0),
        startedEpochDay = startedEpochDay ?: (createdAt / 86_400_000L),
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis(),
    )

    companion object {
        fun fromEntity(entity: QuestEntity): QuestDraft = QuestDraft(
            title = entity.title,
            emoji = entity.emoji,
            categoryId = entity.categoryId,
            trackingKind = TrackingKind.fromName(entity.trackingKind),
            unit = UnitKind.fromName(entity.unit),
            customUnit = entity.customUnit,
            targetAmount = entity.targetAmount,
            scheduleKind = ScheduleKind.fromName(entity.scheduleKind),
            scheduleDays = entity.scheduleDays.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet(),
            xpReward = entity.xpReward,
            attributeName = entity.attributeKind,
            notes = entity.notes,
            description = entity.description,
            difficulty = QuestDifficulty.fromName(entity.difficulty),
            deadlineEpochDay = entity.deadlineEpochDay,
            questKind = QuestKind.fromName(entity.questKind),
            durationDays = entity.durationDays,
            startedEpochDay = entity.startedEpochDay.takeIf { it > 0L },
        )
    }
}
