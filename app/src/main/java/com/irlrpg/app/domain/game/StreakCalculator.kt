package com.irlrpg.app.domain.game

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Consistency maths over the set of days that actually recorded a completed
 * quest. The tone is deliberately forgiving: a streak simply counts back from
 * today (or yesterday, if today is still young) — missed days never subtract or
 * punish progress.
 */
object StreakCalculator {

    fun current(activeDays: Collection<LocalDate>, today: LocalDate): Int {
        if (activeDays.isEmpty()) return 0
        val days = activeDays.toHashSet()
        val yesterday = today.minusDays(1)
        val start = when {
            days.contains(today) -> today
            days.contains(yesterday) -> yesterday
            else -> return 0
        }
        var streak = 0
        var cursor = start
        while (days.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun longest(activeDays: Collection<LocalDate>): Int {
        if (activeDays.isEmpty()) return 0
        val sorted = activeDays.distinct().sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            val gap = ChronoUnit.DAYS.between(sorted[i - 1], sorted[i])
            run = if (gap == 1L) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    /**
     * Share of days with activity since [firstDay] — a gentle consistency
     * signal that never drops because of a single bad week.
     */
    fun consistencyRate(activeDays: Collection<LocalDate>, firstDay: LocalDate?, today: LocalDate): Float {
        if (firstDay == null) return 0f
        val total = (ChronoUnit.DAYS.between(firstDay, today) + 1).coerceAtLeast(1)
        val active = activeDays.distinct().size.coerceAtMost(total.toInt())
        return (active.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }
}
