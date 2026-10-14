package com.irlrpg.app.data.repository

import com.irlrpg.app.core.DateX
import com.irlrpg.app.core.TodaySource
import com.irlrpg.app.data.local.dao.CategoryDao
import com.irlrpg.app.data.local.dao.PlayerDao
import com.irlrpg.app.data.local.dao.QuestDao
import com.irlrpg.app.data.local.dao.QuestLogDao
import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.local.entity.PlayerStatsEntity
import com.irlrpg.app.data.local.entity.QuestEntity
import com.irlrpg.app.data.local.entity.QuestLogEntity
import com.irlrpg.app.domain.game.LevelCurve
import com.irlrpg.app.domain.game.PeriodKeys
import com.irlrpg.app.domain.game.StreakCalculator
import com.irlrpg.app.domain.model.DashboardState
import com.irlrpg.app.domain.model.QuestWithProgress
import com.irlrpg.app.domain.model.RecentCompletion
import com.irlrpg.app.domain.model.ScheduleKind
import com.irlrpg.app.domain.model.StreakInfo
import com.irlrpg.app.domain.model.TrackingKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate

/**
 * Assembles everything the Home Dashboard shows, from the player row, quest
 * definitions, categories and the append-only activity log.
 */
class DashboardRepository(
    private val questDao: QuestDao,
    private val logDao: QuestLogDao,
    private val categoryDao: CategoryDao,
    private val playerDao: PlayerDao,
    private val todaySource: TodaySource,
) {

    fun observeDashboard(): Flow<DashboardState> = combine(
        playerDao.observe(),
        questDao.observeActive(),
        categoryDao.observeAll(),
        logDao.observeAll(),
        todaySource.flow,
    ) { stats, quests, categories, logs, today ->
        build(stats ?: PlayerRepository.placeholderStats(today), quests, categories, logs, today)
    }.flowOn(Dispatchers.Default)

    private fun build(
        stats: PlayerStatsEntity,
        quests: List<QuestEntity>,
        categories: List<CategoryEntity>,
        logs: List<QuestLogEntity>,
        today: LocalDate,
    ): DashboardState {
        val questsById = quests.associateBy { it.id }
        val categoriesById = categories.associateBy { it.id }

        val todayQuests = quests
            .filter { quest ->
                PeriodKeys.isScheduledFor(
                    ScheduleKind.fromName(quest.scheduleKind),
                    quest.scheduleDays,
                    today,
                )
            }
            .map { quest -> QuestProgress.withProgress(quest, logs, categoriesById[quest.categoryId], today) }
            .sortedWith(
                compareBy<QuestWithProgress> { it.isComplete }
                    .thenByDescending { it.category?.name ?: "" }
                    .thenBy { it.quest.createdAt }
            )

        val activeDays = MetricsAggregator.activeDays(logs)
        val weekStart = DateX.startOfWeek(today)
        val week = MetricsAggregator.weekDays(logs, weekStart, questsById)
        val todayMetrics = MetricsAggregator.dayMetrics(logs, today, questsById)

        val levelUps = levelUpLogIds(logs)

        val recent = logs
            .filter { it.isCompletion }
            .sortedWith(compareByDescending<QuestLogEntity> { it.epochDay }.thenByDescending { it.id })
            .take(6)
            .map { log ->
                RecentCompletion(
                    questTitle = questsById[log.questId]?.title ?: "Quest",
                    emoji = questsById[log.questId]?.emoji ?: "⭐",
                    xp = log.xpEarned,
                    amountLabel = "${log.amount} ${log.unitLabel}",
                    date = LocalDate.ofEpochDay(log.epochDay),
                    levelUp = levelUps.contains(log.id),
                )
            }

        return DashboardState(
            player = PlayerRepository.snapshotOf(stats),
            today = todayMetrics,
            week = week,
            weekXp = week.sumOf { it.xp },
            weekCompletions = week.sumOf { it.completions },
            weekMinutes = week.sumOf { it.minutes },
            streak = StreakInfo(
                current = StreakCalculator.current(activeDays, today),
                longest = StreakCalculator.longest(activeDays),
                activeToday = todayMetrics.hasActivity,
            ),
            todayQuests = todayQuests,
            recentCompletions = recent,
            lifetime = MetricsAggregator.lifetime(logs, questsById, quests.size),
        )
    }

    /** Log ids that pushed the player across a level boundary. */
    private fun levelUpLogIds(logs: List<QuestLogEntity>): Set<Long> {
        var running = 0
        val ids = mutableSetOf<Long>()
        logs.sortedWith(compareBy({ it.epochDay }, { it.id })).forEach { log ->
            val before = LevelCurve.levelForTotalXp(running)
            running += log.xpEarned
            val after = LevelCurve.levelForTotalXp(running)
            if (after > before) ids.add(log.id)
        }
        return ids
    }
}
