package com.irlrpg.app.data.repository

import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.local.entity.QuestEntity
import com.irlrpg.app.data.local.entity.QuestLogEntity
import com.irlrpg.app.domain.game.PeriodKeys
import com.irlrpg.app.domain.model.QuestWithProgress
import com.irlrpg.app.domain.model.ScheduleKind
import com.irlrpg.app.domain.model.TrackingKind
import java.time.LocalDate

/**
 * Resolves a quest's progress for the period that is currently relevant
 * (today / this ISO week / all time for flexible goals).
 */
object QuestProgress {

    fun withProgress(
        quest: QuestEntity,
        logs: List<QuestLogEntity>,
        category: CategoryEntity?,
        today: LocalDate,
    ): QuestWithProgress {
        val periodKey = PeriodKeys.forSchedule(ScheduleKind.fromName(quest.scheduleKind), today)
        val rows = logs.filter { it.questId == quest.id && it.periodKey == periodKey }
        val kind = TrackingKind.fromName(quest.trackingKind)
        val progress = if (kind == TrackingKind.Milestone) {
            if (rows.isEmpty()) 0L else 1L
        } else {
            rows.sumOf { it.amount }
        }
        return QuestWithProgress(
            quest = quest,
            progress = progress,
            category = category,
            completions = rows.count { it.isCompletion },
        )
    }

    fun all(
        quests: List<QuestEntity>,
        logs: List<QuestLogEntity>,
        categories: List<CategoryEntity>,
        today: LocalDate,
    ): List<QuestWithProgress> {
        val categoriesById = categories.associateBy { it.id }
        val logsByQuest = logs.groupBy { it.questId }
        return quests.map { quest ->
            withProgress(quest, logsByQuest[quest.id].orEmpty(), categoriesById[quest.categoryId], today)
        }
    }

    fun periodLabel(quest: QuestEntity): String = when (ScheduleKind.fromName(quest.scheduleKind)) {
        ScheduleKind.Daily, ScheduleKind.Weekdays, ScheduleKind.SelectedDays -> "today"
        ScheduleKind.Weekly -> "this week"
        ScheduleKind.Flexible -> "all time"
    }
}
