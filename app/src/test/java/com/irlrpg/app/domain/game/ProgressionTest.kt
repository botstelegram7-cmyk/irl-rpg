package com.irlrpg.app.domain.game

import com.irlrpg.app.domain.model.ScheduleKind
import com.irlrpg.app.domain.model.TrackingKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Pure progression maths — no Android dependencies. */
class LevelCurveTest {

    @Test
    fun `level one starts at zero xp`() {
        assertEquals(1, LevelCurve.levelForTotalXp(0))
        assertEquals(0, LevelCurve.xpIntoLevel(0))
    }

    @Test
    fun `advance cost grows by 60 per level`() {
        assertEquals(150, LevelCurve.xpToAdvance(1))
        assertEquals(210, LevelCurve.xpToAdvance(2))
        assertEquals(690, LevelCurve.xpToAdvance(10))
    }

    @Test
    fun `cumulative requirement matches sum of steps`() {
        var sum = 0
        for (level in 1..9) sum += LevelCurve.xpToAdvance(level)
        assertEquals(sum, LevelCurve.totalXpForLevel(10))
    }

    @Test
    fun `exact boundary xp levels the player up`() {
        val needed = LevelCurve.totalXpForLevel(12)
        assertEquals(11, LevelCurve.levelForTotalXp(needed - 1))
        assertEquals(12, LevelCurve.levelForTotalXp(needed))
    }

    @Test
    fun `progress fraction stays inside zero and one`() {
        assertEquals(0f, LevelCurve.progressFraction(0), 0.001f)
        assertTrue(LevelCurve.progressFraction(75) > 0.49f)
        assertTrue(LevelCurve.progressFraction(149) < 1f)
    }

    @Test
    fun `ranks change with level`() {
        assertEquals("Wanderer", LevelCurve.rankTitle(1))
        assertEquals("Adept", LevelCurve.rankTitle(8))
        assertEquals("Legend", LevelCurve.rankTitle(41))
    }
}

class XpEngineTest {

    @Test
    fun `partial progress earns proportional xp`() {
        val xp = XpEngine.xpForProgress(TrackingKind.Time, baseXp = 60, target = 30, progressBefore = 0, amount = 15)
        assertEquals(30, xp)
    }

    @Test
    fun `logging beyond the target in one go is capped at the target`() {
        val xp = XpEngine.xpForProgress(TrackingKind.Count, baseXp = 60, target = 25, progressBefore = 0, amount = 100)
        assertEquals(60, xp)
    }

    @Test
    fun `completed periods never award more xp`() {
        val xp = XpEngine.xpForProgress(TrackingKind.Count, baseXp = 60, target = 25, progressBefore = 25, amount = 10)
        assertEquals(0, xp)
    }

    @Test
    fun `milestone awards once only`() {
        assertEquals(200, XpEngine.xpForProgress(TrackingKind.Milestone, 200, 1, 0, 1))
        assertEquals(0, XpEngine.xpForProgress(TrackingKind.Milestone, 200, 1, 1, 1))
    }

    @Test
    fun `completion bonus is granted exactly on the crossing entry`() {
        assertTrue(XpEngine.completesTarget(TrackingKind.Count, target = 25, progressBefore = 20, amount = 5))
        assertFalse(XpEngine.completesTarget(TrackingKind.Count, target = 25, progressBefore = 25, amount = 5))
        assertEquals(30, XpEngine.completionBonus(60))
        assertEquals(10, XpEngine.completionBonus(4))
    }

    @Test
    fun `many small logs earn the same as one big log`() {
        var progress = 0L
        var total = 0
        repeat(5) {
            total += XpEngine.xpForProgress(TrackingKind.Count, 50, 25, progress, 5)
            progress += 5
        }
        val single = XpEngine.xpForProgress(TrackingKind.Count, 50, 25, 0, 25)
        assertEquals(single, total)
    }
}

class StreakCalculatorTest {

    private val today = LocalDate.of(2026, 10, 14)

    @Test
    fun `counts consecutive days ending today`() {
        val days = listOf(today, today.minusDays(1), today.minusDays(2))
        assertEquals(3, StreakCalculator.current(days, today))
    }

    @Test
    fun `yesterday still counts so a fresh day is not punished`() {
        val days = listOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, StreakCalculator.current(days, today))
    }

    @Test
    fun `a two day old streak is over`() {
        val days = listOf(today.minusDays(2), today.minusDays(3))
        assertEquals(0, StreakCalculator.current(days, today))
    }

    @Test
    fun `longest streak scans the whole history`() {
        val days = listOf(
            today.minusDays(10), today.minusDays(9), today.minusDays(8), today.minusDays(7),
            today.minusDays(1),
        )
        assertEquals(4, StreakCalculator.longest(days))
    }

    @Test
    fun `consistency rate reflects proportion of active days`() {
        val first = today.minusDays(9)
        val days = listOf(first, today.minusDays(8), today)
        val rate = StreakCalculator.consistencyRate(days, first, today)
        assertEquals(0.3f, rate, 0.001f)
    }
}

class PeriodKeysTest {

    private val date = LocalDate.of(2026, 10, 14) // Wednesday

    @Test
    fun `daily quests bucket by day`() {
        assertEquals("2026-10-14", PeriodKeys.forSchedule(ScheduleKind.Daily, date))
    }

    @Test
    fun `weekly quests share one bucket for the whole iso week`() {
        val monday = PeriodKeys.forSchedule(ScheduleKind.Weekly, LocalDate.of(2026, 10, 12))
        val sunday = PeriodKeys.forSchedule(ScheduleKind.Weekly, LocalDate.of(2026, 10, 18))
        assertEquals(monday, sunday)
    }

    @Test
    fun `flexible quests accumulate in one bucket`() {
        assertEquals(PeriodKeys.FLEX_KEY, PeriodKeys.forSchedule(ScheduleKind.Flexible, date))
    }

    @Test
    fun `scheduled days are respected`() {
        assertTrue(PeriodKeys.isScheduledFor(ScheduleKind.Weekdays, "", date))
        assertFalse(PeriodKeys.isScheduledFor(ScheduleKind.Weekdays, "", LocalDate.of(2026, 10, 17)))
        assertTrue(PeriodKeys.isScheduledFor(ScheduleKind.SelectedDays, "3,7", date))
        assertFalse(PeriodKeys.isScheduledFor(ScheduleKind.SelectedDays, "1,2", date))
    }
}
