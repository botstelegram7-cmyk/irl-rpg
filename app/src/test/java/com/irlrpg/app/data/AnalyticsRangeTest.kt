package com.irlrpg.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.irlrpg.app.core.TodaySource
import com.irlrpg.app.data.local.IrlRpgDatabase
import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.local.entity.QuestEntity
import com.irlrpg.app.data.local.entity.QuestLogEntity
import com.irlrpg.app.data.repository.AnalyticsRepository
import com.irlrpg.app.data.repository.MetricsAggregator
import com.irlrpg.app.domain.model.AnalyticsRange
import com.irlrpg.app.domain.model.AnalyticsState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Range analytics for the Step 3 UI.
 *
 * The screen promises four ranges (Today / Week / Month / Year) whose numbers are
 * arithmetic over recorded rows only. These tests write a small, hand-computed log
 * into a real in-memory Room database and assert that every summary matches the
 * numbers a human would derive from the same rows — including the "previous
 * period" comparisons and the empty-range case, which must stay zero.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AnalyticsRangeTest {

    private lateinit var db: IrlRpgDatabase
    private lateinit var repository: AnalyticsRepository
    private val today: LocalDate = LocalDate.of(2026, 3, 18) // a Wednesday

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, IrlRpgDatabase::class.java).build()
        repository = AnalyticsRepository(
            logDao = db.questLogDao(),
            questDao = db.questDao(),
            categoryDao = db.categoryDao(),
            todaySource = TodaySource { today },
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ------------------------------------------------------------------ helpers

    private suspend fun seedWorld(): Pair<Long, Long> {
        val reading = db.categoryDao().insert(
            CategoryEntity(name = "Reading", emoji = "📚", accentId = "butter")
        )
        val gym = db.categoryDao().insert(
            CategoryEntity(name = "Fitness", emoji = "💪", accentId = "mint")
        )
        val pages = db.questDao().insert(
            QuestEntity(
                title = "Read pages",
                emoji = "📖",
                categoryId = reading,
                trackingKind = "COUNT",
                unit = "Pages",
                targetAmount = 30,
                scheduleKind = "DAILY",
                xpReward = 12,
                attributeKind = "INTELLECT",
            )
        )
        val workout = db.questDao().insert(
            QuestEntity(
                title = "Workout",
                emoji = "🏋️",
                categoryId = gym,
                trackingKind = "TIME",
                unit = "Minutes",
                targetAmount = 45,
                scheduleKind = "DAILY",
                xpReward = 20,
                attributeKind = "STRENGTH",
            )
        )
        return pages to workout
    }

    private suspend fun log(
        questId: Long,
        amount: Long,
        xp: Int,
        epochDay: Long,
        isCompletion: Boolean = true,
        unitLabel: String = "pages",
    ) {
        db.questLogDao().insert(
            QuestLogEntity(
                questId = questId,
                amount = amount,
                unitLabel = unitLabel,
                xpEarned = xp,
                isCompletion = isCompletion,
                periodKey = LocalDate.ofEpochDay(epochDay).toString(),
                epochDay = epochDay,
            )
        )
    }

    private fun analytics(): AnalyticsState = runBlocking { repository.observeAnalytics().first() }

    private fun day(offset: Long): Long = today.plusDays(offset).toEpochDay()

    // -------------------------------------------------------------------- tests

    @Test
    fun weekRange_sumsOnlyRecordedRowsOfThisWeek() {
        runBlocking {
            val (pages, workout) = seedWorld()
            val weekStart = today.minusDays(2) // Monday of the current week
            log(pages, 30, 12, weekStart.toEpochDay())
            log(pages, 15, 6, day(-1), isCompletion = false)
            log(workout, 45, 20, day(0), unitLabel = "minutes")
            // A row from last week must not leak into this week's totals.
            log(pages, 30, 12, day(-8))
        }

        val state = analytics()
        val week = state.ranges.getValue(AnalyticsRange.Week)

        assertEquals(38, week.xp) // 12 + 6 + 20
        assertEquals(2, week.completions)
        assertEquals(3, week.entries)
        assertEquals(45, week.minutes)
        assertEquals(2, week.activeDays) // two days carry completions
        assertEquals(12, week.previousXp) // only the day(-8) row
        assertEquals(1, week.previousCompletions)
        assertEquals("last week", week.previousLabel)
        assertEquals(7, week.buckets.size)
        assertEquals(38, week.buckets.sumOf { it.xp })
    }

    @Test
    fun monthRange_bucketsAreWeeksOfThisMonthAndMatchTheMonthTotal() {
        runBlocking {
            val (pages, workout) = seedWorld()
            log(pages, 30, 12, today.withDayOfMonth(1).toEpochDay())
            log(pages, 30, 12, today.withDayOfMonth(8).toEpochDay())
            log(workout, 60, 24, day(0), unitLabel = "minutes")
            // Previous month, same quest: only the comparison number may see it.
            log(pages, 30, 12, today.minusMonths(1).withDayOfMonth(10).toEpochDay())
        }

        val state = analytics()
        val month = state.ranges.getValue(AnalyticsRange.Month)

        assertEquals(48, month.xp)
        assertEquals(3, month.completions)
        assertEquals(60, month.minutes)
        assertEquals(3, month.activeDays)
        assertEquals(12, month.previousXp)
        assertEquals("last month", month.previousLabel)
        // One bucket per week slice elapsed in the month (1–7, 8–14, 15–21, …)
        // and their sum is the month total — no future days are invented.
        assertEquals(48, month.buckets.sumOf { it.xp })
        assertEquals((today.dayOfMonth + 6) / 7, month.buckets.size)
        assertEquals("1–7", month.buckets.first().label)
    }

    @Test
    fun yearRange_hasOneBucketPerElapsedMonthAndRealYearOverYearComparison() {
        runBlocking {
            val (pages, _) = seedWorld()
            val yearStart = today.withDayOfYear(1)
            log(pages, 30, 12, yearStart.toEpochDay())
            log(pages, 30, 12, yearStart.plusMonths(1).toEpochDay())
            log(pages, 30, 12, today.minusYears(1).withDayOfYear(200).toEpochDay())
        }

        val state = analytics()
        val year = state.ranges.getValue(AnalyticsRange.Year)

        assertEquals(24, year.xp)
        assertEquals(12, year.previousXp)
        assertEquals("last year", year.previousLabel)
        // March is the current month, so Jan/Feb/Mar have elapsed: exactly 3 buckets.
        assertEquals(today.monthValue, year.buckets.size)
        assertEquals(24, year.buckets.sumOf { it.xp })
        assertEquals("Jan", year.buckets.first().label)
    }

    @Test
    fun todayRange_comparesAgainstYesterdayAndKeepsCategoriesScopedToToday() {
        runBlocking {
            val (pages, workout) = seedWorld()
            log(pages, 30, 12, day(-1))
            log(workout, 45, 20, day(0), unitLabel = "minutes")
            log(pages, 10, 4, day(0), isCompletion = false)
        }

        val state = analytics()
        val todayRange = state.ranges.getValue(AnalyticsRange.Today)

        assertEquals(24, todayRange.xp) // 20 + 4
        assertEquals(1, todayRange.completions)
        assertEquals(45, todayRange.minutes)
        assertEquals(12, todayRange.previousXp)
        assertEquals("yesterday", todayRange.previousLabel)
        // Categories are scoped to today only: the workout completion and the
        // partial reading entry, and nothing from yesterday.
        assertEquals(setOf("Fitness", "Reading"), todayRange.categories.map { it.name }.toSet())
        assertEquals(45, todayRange.categories.first { it.name == "Fitness" }.minutes)
        assertEquals(4, todayRange.categories.first { it.name == "Reading" }.xp)
        // No hourly series is offered because none is recorded.
        assertTrue(todayRange.buckets.isEmpty())
    }

    @Test
    fun emptyDatabase_producesZeroedRangesWithoutInventedSeries() {
        val state = analytics()

        AnalyticsRange.entries.forEach { range ->
            val summary = state.ranges.getValue(range)
            assertEquals("$range xp", 0, summary.xp)
            assertEquals("$range completions", 0, summary.completions)
            assertEquals("$range entries", 0, summary.entries)
            assertEquals("$range minutes", 0, summary.minutes)
            assertEquals("$range activeDays", 0, summary.activeDays)
            assertEquals("$range previousXp", 0, summary.previousXp)
            assertTrue("$range categories", summary.categories.isEmpty())
            assertTrue("$range buckets", summary.buckets.all { it.xp == 0 })
        }
        assertEquals(0, state.lifetime.totalXp)
        assertEquals(0f, state.completionRate, 0.0001f)
    }

    @Test
    fun weekBuckets_areLabelledMonToSunAndKeepDayValues() {
        runBlocking {
            val (pages, _) = seedWorld()
            log(pages, 30, 12, today.minusDays(2).toEpochDay())
        }

        val state = analytics()
        val buckets = state.week

        assertEquals(7, buckets.size)
        assertEquals(12, buckets.map { it.xp }.sum())
        assertEquals(1, buckets.count { it.xp > 0 })
    }

    @Test
    fun monthBuckets_doNotIncludeDaysAfterToday() {
        runBlocking {
            val (pages, _) = seedWorld()
            log(pages, 30, 12, day(0))
        }

        val state = analytics()
        val month = state.ranges.getValue(AnalyticsRange.Month)
        val lastBucket = month.buckets.last()

        assertTrue(lastBucket.date.toEpochDay() <= today.toEpochDay())
        assertEquals(12, month.buckets.sumOf { it.xp })
        // The final bucket starts no later than today and stops at today.
        assertTrue(lastBucket.date <= today)
    }

    @Test
    fun aggregator_countsActiveDaysAndEntriesExactly() {
        val rows = listOf(
            QuestLogEntity(questId = 1, amount = 1, unitLabel = "x", xpEarned = 1, isCompletion = true, periodKey = "a", epochDay = 10),
            QuestLogEntity(questId = 1, amount = 1, unitLabel = "x", xpEarned = 1, isCompletion = false, periodKey = "a", epochDay = 10),
            QuestLogEntity(questId = 1, amount = 1, unitLabel = "x", xpEarned = 1, isCompletion = true, periodKey = "b", epochDay = 11),
            QuestLogEntity(questId = 1, amount = 1, unitLabel = "x", xpEarned = 1, isCompletion = true, periodKey = "c", epochDay = 40),
        )
        val from = LocalDate.ofEpochDay(10)
        val to = LocalDate.ofEpochDay(20)

        assertEquals(2, MetricsAggregator.activeDaysBetween(rows, from, to))
        assertEquals(3, MetricsAggregator.entriesBetween(rows, from, to))
        assertEquals(3, MetricsAggregator.xpBetween(rows, from, to))
    }
}
