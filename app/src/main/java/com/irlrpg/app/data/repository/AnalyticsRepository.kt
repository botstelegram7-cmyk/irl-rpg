package com.irlrpg.app.data.repository

import com.irlrpg.app.core.DateX
import com.irlrpg.app.core.TodaySource
import com.irlrpg.app.data.local.dao.CategoryDao
import com.irlrpg.app.data.local.dao.QuestDao
import com.irlrpg.app.data.local.dao.QuestLogDao
import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.local.entity.QuestEntity
import com.irlrpg.app.data.local.entity.QuestLogEntity
import com.irlrpg.app.domain.game.PeriodKeys
import com.irlrpg.app.domain.game.StreakCalculator
import com.irlrpg.app.domain.model.AnalyticsState
import com.irlrpg.app.domain.model.ScheduleKind
import com.irlrpg.app.domain.model.StreakInfo
import com.irlrpg.app.domain.model.TrackingKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate

/**
 * Analytics are computed from the activity log only — day, week, month and year
 * summaries, per-category splits, consistency and progress towards targets.
 * Anything that cannot be derived from recorded rows is simply not shown.
 */
class AnalyticsRepository(
    private val logDao: QuestLogDao,
    private val questDao: QuestDao,
    private val categoryDao: CategoryDao,
    private val todaySource: TodaySource,
) {

    fun observeAnalytics(): Flow<AnalyticsState> = combine(
        logDao.observeAll(),
        questDao.observeActive(),
        categoryDao.observeAll(),
        todaySource.flow,
    ) { logs, quests, categories, today ->
        build(logs, quests, categories, today)
    }.flowOn(Dispatchers.Default)

    private fun build(
        logs: List<QuestLogEntity>,
        quests: List<QuestEntity>,
        categories: List<CategoryEntity>,
        today: LocalDate,
    ): AnalyticsState {
        val questsById = quests.associateBy { it.id }
        val weekStart = DateX.startOfWeek(today)
        val week = MetricsAggregator.weekDays(logs, weekStart, questsById)
        val lastWeekStart = weekStart.minusDays(7)

        val monthStart = DateX.startOfMonth(today)
        val yearStart = DateX.startOfYear(today)

        val activeDays = MetricsAggregator.activeDays(logs)
        val lifetime = MetricsAggregator.lifetime(logs, questsById, quests.size)

        // Completion rate: quests finished today versus quests scheduled today.
        val scheduledToday = quests.filter {
            PeriodKeys.isScheduledFor(ScheduleKind.fromName(it.scheduleKind), it.scheduleDays, today)
        }
        val completedToday = scheduledToday.count { quest ->
            val periodKey = PeriodKeys.forSchedule(ScheduleKind.fromName(quest.scheduleKind), today)
            val rows = logs.filter { it.questId == quest.id && it.periodKey == periodKey }
            val kind = TrackingKind.fromName(quest.trackingKind)
            if (kind == TrackingKind.Milestone) {
                rows.isNotEmpty()
            } else {
                quest.targetAmount > 0L && rows.sumOf { it.amount } >= quest.targetAmount
            }
        }

        return AnalyticsState(
            today = MetricsAggregator.dayMetrics(logs, today, questsById),
            yesterday = MetricsAggregator.dayMetrics(logs, today.minusDays(1), questsById),
            week = week,
            weekXp = week.sumOf { it.xp },
            weekCompletions = week.sumOf { it.completions },
            weekMinutes = week.sumOf { it.minutes },
            lastWeekXp = MetricsAggregator.xpBetween(logs, lastWeekStart, weekStart.minusDays(1)),
            monthXp = MetricsAggregator.xpBetween(logs, monthStart, today),
            monthCompletions = MetricsAggregator.completionsBetween(logs, monthStart, today),
            monthMinutes = MetricsAggregator.minutesBetween(logs, questsById, monthStart, today),
            yearXp = MetricsAggregator.xpBetween(logs, yearStart, today),
            categories = MetricsAggregator.breakdown(logs, questsById, categories),
            streak = StreakInfo(
                current = StreakCalculator.current(activeDays, today),
                longest = StreakCalculator.longest(activeDays),
                activeToday = MetricsAggregator.dayMetrics(logs, today, questsById).hasActivity,
            ),
            lifetime = lifetime,
            readingMinutes = MetricsAggregator.readingMinutes(logs, questsById, categories),
            readingPages = MetricsAggregator.pagesRead(logs, questsById),
            completionRate = if (scheduledToday.isEmpty()) 0f else {
                completedToday.toFloat() / scheduledToday.size.toFloat()
            },
            consistencyRate = StreakCalculator.consistencyRate(activeDays, lifetime.firstDay, today),
        )
    }
}
