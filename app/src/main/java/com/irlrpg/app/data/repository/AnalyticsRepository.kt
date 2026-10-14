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
import com.irlrpg.app.domain.model.AnalyticsRange
import com.irlrpg.app.domain.model.AnalyticsState
import com.irlrpg.app.domain.model.RangeSummary
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

        val todayMetrics = MetricsAggregator.dayMetrics(logs, today, questsById)
        val yesterdayMetrics = MetricsAggregator.dayMetrics(logs, today.minusDays(1), questsById)

        // ---- per-range summaries (all arithmetic on recorded rows) ----------
        val weekEnd = weekStart.plusDays(6)
        val prevWeekStart = weekStart.minusDays(7)
        val prevWeekEnd = weekStart.minusDays(1)
        val monthEnd = monthStart.plusMonths(1).minusDays(1)
        val prevMonthStart = monthStart.minusMonths(1)
        val prevMonthEnd = monthStart.minusDays(1)
        val yearEnd = yearStart.plusYears(1).minusDays(1)
        val prevYearStart = yearStart.minusYears(1)
        val prevYearEnd = yearStart.minusDays(1)

        val monthBuckets = MetricsAggregator.buckets(
            logs = logs,
            questsById = questsById,
            from = monthStart,
            toInclusive = today,
            stepDays = 7,
            label = { start, end -> "${start.dayOfMonth}–${end.dayOfMonth}" },
        )
        val yearBuckets = MetricsAggregator.monthsOfYear(logs, questsById, yearStart, today)

        val ranges = mapOf(
            AnalyticsRange.Today to RangeSummary(
                xp = todayMetrics.xp,
                completions = todayMetrics.completions,
                entries = MetricsAggregator.entriesBetween(logs, today, today),
                minutes = todayMetrics.minutes,
                activeDays = if (todayMetrics.completions > 0) 1 else 0,
                previousXp = yesterdayMetrics.xp,
                previousCompletions = yesterdayMetrics.completions,
                previousLabel = "yesterday",
                bucketCaption = "Entries are stored per day, so there is no hourly split to show",
                buckets = emptyList(),
                categories = MetricsAggregator.breakdown(logs, questsById, categories, today, today),
            ),
            AnalyticsRange.Week to RangeSummary(
                xp = week.sumOf { it.xp },
                completions = week.sumOf { it.completions },
                entries = MetricsAggregator.entriesBetween(logs, weekStart, weekEnd),
                minutes = week.sumOf { it.minutes },
                activeDays = MetricsAggregator.activeDaysBetween(logs, weekStart, weekEnd),
                previousXp = MetricsAggregator.xpBetween(logs, prevWeekStart, prevWeekEnd),
                previousCompletions = MetricsAggregator.completionsBetween(logs, prevWeekStart, prevWeekEnd),
                previousLabel = "last week",
                bucketCaption = "XP per day, Monday to Sunday",
                buckets = week,
                categories = MetricsAggregator.breakdown(logs, questsById, categories, weekStart, weekEnd),
            ),
            AnalyticsRange.Month to RangeSummary(
                xp = MetricsAggregator.xpBetween(logs, monthStart, today),
                completions = MetricsAggregator.completionsBetween(logs, monthStart, today),
                entries = MetricsAggregator.entriesBetween(logs, monthStart, today),
                minutes = MetricsAggregator.minutesBetween(logs, questsById, monthStart, today),
                activeDays = MetricsAggregator.activeDaysBetween(logs, monthStart, today),
                previousXp = MetricsAggregator.xpBetween(logs, prevMonthStart, prevMonthEnd),
                previousCompletions = MetricsAggregator.completionsBetween(logs, prevMonthStart, prevMonthEnd),
                previousLabel = "last month",
                bucketCaption = "XP per week of this month",
                buckets = monthBuckets,
                categories = MetricsAggregator.breakdown(logs, questsById, categories, monthStart, today),
            ),
            AnalyticsRange.Year to RangeSummary(
                xp = MetricsAggregator.xpBetween(logs, yearStart, today),
                completions = MetricsAggregator.completionsBetween(logs, yearStart, today),
                entries = MetricsAggregator.entriesBetween(logs, yearStart, today),
                minutes = MetricsAggregator.minutesBetween(logs, questsById, yearStart, today),
                activeDays = MetricsAggregator.activeDaysBetween(logs, yearStart, today),
                previousXp = MetricsAggregator.xpBetween(logs, prevYearStart, prevYearEnd),
                previousCompletions = MetricsAggregator.completionsBetween(logs, prevYearStart, prevYearEnd),
                previousLabel = "last year",
                bucketCaption = "XP per month of this year",
                buckets = yearBuckets,
                categories = MetricsAggregator.breakdown(logs, questsById, categories, yearStart, today),
            ),
        )

        return AnalyticsState(
            today = todayMetrics,
            yesterday = yesterdayMetrics,
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
            ranges = ranges,
        )
    }
}
