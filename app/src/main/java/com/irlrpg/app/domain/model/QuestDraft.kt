package com.irlrpg.app.domain.model

import com.irlrpg.app.data.local.entity.QuestEntity

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
        )
    }
}
