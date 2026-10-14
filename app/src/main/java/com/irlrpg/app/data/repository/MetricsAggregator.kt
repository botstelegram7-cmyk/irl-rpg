package com.irlrpg.app.data.repository

import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.local.entity.QuestEntity
import com.irlrpg.app.data.local.entity.QuestLogEntity
import com.irlrpg.app.domain.model.CategoryBreakdown
import com.irlrpg.app.domain.model.DayMetrics
import com.irlrpg.app.domain.model.LifetimeTotals
import com.irlrpg.app.domain.model.UnitKind
import com.irlrpg.app.domain.model.WeekDay
import com.irlrpg.app.core.DateX
import java.time.LocalDate

/**
 * Pure aggregation over recorded log rows. No sampling, no synthetic numbers:
 * every value returned here is arithmetic on rows the user actually created.
 */
object MetricsAggregator {

    /**
     * Time attributed to a log entry, normalised to minutes. Count-based
     * activities contribute 0 minutes (the UI shows their unit instead).
     */
    fun minutesOf(log: QuestLogEntity, quest: QuestEntity?): Int {
        if (quest == null) return 0
        return when (UnitKind.fromName(quest.unit)) {
            UnitKind.Minutes -> log.amount.toInt()
            UnitKind.Hours -> (log.amount * 60).toInt()
            else -> if (quest.trackingKind == com.irlrpg.app.domain.model.TrackingKind.Time.name) {
                log.amount.toInt()
            } else {
                0
            }
        }
    }

    fun dayMetrics(logs: List<QuestLogEntity>, date: LocalDate, questsById: Map<Long, QuestEntity>): DayMetrics {
        val epochDay = date.toEpochDay()
        val dayLogs = logs.filter { it.epochDay == epochDay }
        return DayMetrics(
            date = date,
            xp = dayLogs.sumOf { it.xpEarned },
            completions = dayLogs.count { it.isCompletion },
            minutes = dayLogs.sumOf { minutesOf(it, questsById[it.questId]) },
            units = dayLogs.sumOf { it.amount },
        )
    }

    fun weekDays(
        logs: List<QuestLogEntity>,
        weekStart: LocalDate,
        questsById: Map<Long, QuestEntity>,
    ): List<WeekDay> = (0..6).map { offset ->
        val date = weekStart.plusDays(offset.toLong())
        val metrics = dayMetrics(logs, date, questsById)
        WeekDay(
            date = date,
            label = DateX.shortDayLabel(date),
            xp = metrics.xp,
            completions = metrics.completions,
            minutes = metrics.minutes,
        )
    }

    fun xpBetween(logs: List<QuestLogEntity>, from: LocalDate, to: LocalDate): Int {
        val start = from.toEpochDay()
        val end = to.toEpochDay()
        return logs.filter { it.epochDay in start..end }.sumOf { it.xpEarned }
    }

    fun completionsBetween(logs: List<QuestLogEntity>, from: LocalDate, to: LocalDate): Int {
        val start = from.toEpochDay()
        val end = to.toEpochDay()
        return logs.count { it.epochDay in start..end && it.isCompletion }
    }

    fun minutesBetween(
        logs: List<QuestLogEntity>,
        questsById: Map<Long, QuestEntity>,
        from: LocalDate,
        to: LocalDate,
    ): Int {
        val start = from.toEpochDay()
        val end = to.toEpochDay()
        return logs.filter { it.epochDay in start..end }.sumOf { minutesOf(it, questsById[it.questId]) }
    }

    fun breakdown(
        logs: List<QuestLogEntity>,
        questsById: Map<Long, QuestEntity>,
        categories: List<CategoryEntity>,
        from: LocalDate? = null,
        to: LocalDate? = null,
    ): List<CategoryBreakdown> {
        val start = from?.toEpochDay() ?: Long.MIN_VALUE
        val end = to?.toEpochDay() ?: Long.MAX_VALUE
        val scoped = logs.filter { it.epochDay in start..end }
        val categoriesById = categories.associateBy { it.id }

        return scoped
            .groupBy { questsById[it.questId]?.categoryId }
            .map { (categoryId, rows) ->
                val category = categoryId?.let { categoriesById[it] }
                CategoryBreakdown(
                    categoryId = categoryId,
                    name = category?.name ?: "Uncategorised",
                    emoji = category?.emoji ?: "🗂️",
                    accentId = category?.accentId ?: "lavender",
                    minutes = rows.sumOf { minutesOf(it, questsById[it.questId]) },
                    completions = rows.count { it.isCompletion },
                    xp = rows.sumOf { it.xpEarned },
                    units = rows.sumOf { it.amount },
                )
            }
            .sortedWith(compareByDescending<CategoryBreakdown> { it.minutes }.thenByDescending { it.xp })
    }

    fun activeDays(logs: List<QuestLogEntity>): Set<LocalDate> =
        logs.filter { it.isCompletion }.map { LocalDate.ofEpochDay(it.epochDay) }.toSet()

    fun activeDaysAnyLog(logs: List<QuestLogEntity>): Set<LocalDate> =
        logs.map { LocalDate.ofEpochDay(it.epochDay) }.toSet()

    fun lifetime(
        logs: List<QuestLogEntity>,
        questsById: Map<Long, QuestEntity>,
        questsCreated: Int,
    ): LifetimeTotals {
        val days = activeDaysAnyLog(logs)
        return LifetimeTotals(
            totalXp = logs.sumOf { it.xpEarned },
            totalCompletions = logs.count { it.isCompletion },
            totalMinutes = logs.sumOf { minutesOf(it, questsById[it.questId]) },
            activeDays = days.size,
            questsCreated = questsCreated,
            firstDay = days.minOrNull(),
        )
    }


    /** Number of distinct days with at least one completion inside a range. */
    fun activeDaysBetween(logs: List<QuestLogEntity>, from: LocalDate, to: LocalDate): Int {
        val start = from.toEpochDay()
        val end = to.toEpochDay()
        return logs.filter { it.isCompletion && it.epochDay in start..end }
            .map { it.epochDay }
            .toSet()
            .size
    }

    /** Number of log rows (partial entries included) inside a range. */
    fun entriesBetween(logs: List<QuestLogEntity>, from: LocalDate, to: LocalDate): Int {
        val start = from.toEpochDay()
        val end = to.toEpochDay()
        return logs.count { it.epochDay in start..end }
    }

    /**
     * Fixed-width buckets inside a range, used by the month and year charts.
     * Labels are day spans ("1–7") or month names ("Jan") depending on the step.
     */
    fun buckets(
        logs: List<QuestLogEntity>,
        questsById: Map<Long, QuestEntity>,
        from: LocalDate,
        toInclusive: LocalDate,
        stepDays: Int,
        label: (LocalDate, LocalDate) -> String,
    ): List<WeekDay> {
        val out = mutableListOf<WeekDay>()
        var cursor = from
        while (!cursor.isAfter(toInclusive)) {
            val rawEnd = cursor.plusDays(stepDays.toLong() - 1)
            val end = if (rawEnd.isAfter(toInclusive)) toInclusive else rawEnd
            val start = cursor.toEpochDay()
            val endDay = end.toEpochDay()
            val rows = logs.filter { it.epochDay in start..endDay }
            out += WeekDay(
                date = cursor,
                label = label(cursor, end),
                xp = rows.sumOf { it.xpEarned },
                completions = rows.count { it.isCompletion },
                minutes = rows.sumOf { minutesOf(it, questsById[it.questId]) },
            )
            cursor = end.plusDays(1)
        }
        return out
    }

    /**
     * One bucket per calendar month of [yearStart], up to and including the month
     * that contains [today] — no empty future months are invented.
     */
    fun monthsOfYear(
        logs: List<QuestLogEntity>,
        questsById: Map<Long, QuestEntity>,
        yearStart: LocalDate,
        today: LocalDate,
    ): List<WeekDay> {
        val out = mutableListOf<WeekDay>()
        var cursor: LocalDate = yearStart
        while (!cursor.isAfter(today) && out.size < 12) {
            val monthEnd = cursor.plusMonths(1).minusDays(1)
            val end = if (monthEnd.isAfter(today)) today else monthEnd
            val start = cursor.toEpochDay()
            val endDay = end.toEpochDay()
            val rows = logs.filter { it.epochDay in start..endDay }
            out += WeekDay(
                date = cursor,
                label = DateX.shortMonthLabel(cursor),
                xp = rows.sumOf { it.xpEarned },
                completions = rows.count { it.isCompletion },
                minutes = rows.sumOf { minutesOf(it, questsById[it.questId]) },
            )
            cursor = cursor.plusMonths(1)
        }
        return out
    }

    /** Pages/chapters read, derived from quests whose unit is Pages. */
    fun pagesRead(logs: List<QuestLogEntity>, questsById: Map<Long, QuestEntity>): Long =
        logs.filter { questsById[it.questId]?.let { q -> UnitKind.fromName(q.unit) == UnitKind.Pages } == true }
            .sumOf { it.amount }

    /**
     * Reading time is attributed to quests sitting in a reading-flavoured
     * category (the user's own naming decides this — nothing is assumed).
     */
    fun readingMinutes(
        logs: List<QuestLogEntity>,
        questsById: Map<Long, QuestEntity>,
        categories: List<CategoryEntity>,
    ): Int {
        val readingCategoryIds = categories
            .filter { category ->
                val n = category.name.lowercase()
                n.contains("read") || n.contains("book") || n.contains("novel") || n.contains("literature")
            }
            .map { it.id }
            .toSet()
        if (readingCategoryIds.isEmpty()) return 0
        return logs
            .filter { questsById[it.questId]?.categoryId in readingCategoryIds }
            .sumOf { minutesOf(it, questsById[it.questId]) }
    }
}
