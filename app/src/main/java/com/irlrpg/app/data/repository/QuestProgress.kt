package com.irlrpg.app.data.repository

import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.local.entity.QuestEntity
import com.irlrpg.app.data.local.entity.QuestLogEntity
import com.irlrpg.app.data.local.entity.QuestStepEntity
import com.irlrpg.app.domain.game.PeriodKeys
import com.irlrpg.app.domain.model.QuestKind
import com.irlrpg.app.domain.model.QuestWithProgress
import com.irlrpg.app.domain.model.ScheduleKind
import com.irlrpg.app.domain.model.TrackingKind
import java.time.LocalDate

/**
 * Resolves a quest's progress for the period that is currently relevant
 * (today / this ISO week / all time for long-term challenges).
 *
 * Multi-step quest progress is expressed in *steps*, so the UI can show
 * "2 of 4 steps done" while each step keeps its own unit and target.
 */
object QuestProgress {

    fun withProgress(
        quest: QuestEntity,
        logs: List<QuestLogEntity>,
        category: CategoryEntity?,
        today: LocalDate,
        steps: List<QuestStepEntity> = emptyList(),
    ): QuestWithProgress {
        val periodKey = PeriodKeys.forSchedule(ScheduleKind.fromName(quest.scheduleKind), today)
        val questKind = QuestKind.fromName(quest.questKind)
        val effectiveKey = if (questKind == QuestKind.LongTerm) PeriodKeys.FLEX_KEY else periodKey

        val rows = logs.filter { it.questId == quest.id && it.periodKey == effectiveKey }
        val kind = TrackingKind.fromName(quest.trackingKind)

        if (questKind == QuestKind.MultiStep && steps.isNotEmpty()) {
            val required = steps.filter { it.isRequired }.ifEmpty { steps }
            val completedIds = required
                .filter { step -> rows.filter { it.stepId == step.id }.sumOf { it.amount } >= step.targetAmount }
                .map { it.id }
                .toSet()
            return QuestWithProgress(
                quest = quest,
                progress = completedIds.size.toLong(),
                category = category,
                completions = rows.count { it.isCompletion },
                stepsTotal = required.size,
                stepsCompleted = completedIds.size,
                periodKey = effectiveKey,
                completedStepIds = completedIds,
            )
        }

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
            stepsTotal = steps.count { it.isRequired },
            stepsCompleted = 0,
            periodKey = effectiveKey,
        )
    }

    fun all(
        quests: List<QuestEntity>,
        logs: List<QuestLogEntity>,
        categories: List<CategoryEntity>,
        today: LocalDate,
        stepsByQuest: Map<Long, List<QuestStepEntity>> = emptyMap(),
    ): List<QuestWithProgress> {
        val categoriesById = categories.associateBy { it.id }
        val logsByQuest = logs.groupBy { it.questId }
        return quests.map { quest ->
            withProgress(
                quest = quest,
                logs = logsByQuest[quest.id].orEmpty(),
                category = categoriesById[quest.categoryId],
                today = today,
                steps = stepsByQuest[quest.id].orEmpty(),
            )
        }
    }

    fun periodLabel(quest: QuestEntity): String =
        if (QuestKind.fromName(quest.questKind) == QuestKind.LongTerm) {
            "all time"
        } else {
            when (ScheduleKind.fromName(quest.scheduleKind)) {
                ScheduleKind.Daily, ScheduleKind.Weekdays, ScheduleKind.SelectedDays -> "today"
                ScheduleKind.Weekly -> "this week"
                ScheduleKind.Flexible -> "all time"
            }
        }
}
