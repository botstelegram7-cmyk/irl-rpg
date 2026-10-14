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
import com.irlrpg.app.domain.game.LevelProgression
import com.irlrpg.app.domain.game.LevelRules
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
    private val stepDao: com.irlrpg.app.data.local.dao.QuestStepDao,
    private val todaySource: TodaySource,
    private val levelRules: () -> LevelRules = { LevelRules.Default },
) {

    private data class DashboardInputs(
        val stats: PlayerStatsEntity?,
        val quests: List<QuestEntity>,
        val categories: List<CategoryEntity>,
        val logs: List<QuestLogEntity>,
        val steps: List<com.irlrpg.app.data.local.entity.QuestStepEntity>,
    )

    fun observeDashboard(): Flow<DashboardState> = combine(
        combine(
            playerDao.observe(),
            questDao.observeActive(),
            categoryDao.observeAll(),
            logDao.observeAll(),
        ) { stats, quests, categories, logs -> DashboardInputs(stats, quests, categories, logs, emptyList()) },
        stepDao.observeAll(),
        todaySource.flow,
    ) { inputs, steps, today ->
        build(
            stats = inputs.stats ?: PlayerRepository.placeholderStats(today),
            quests = inputs.quests,
            categories = inputs.categories,
            logs = inputs.logs,
            stepsByQuest = steps.groupBy { it.questId },
            today = today,
        )
    }.flowOn(Dispatchers.Default)

    private fun build(
        stats: PlayerStatsEntity,
        quests: List<QuestEntity>,
        categories: List<CategoryEntity>,
        logs: List<QuestLogEntity>,
        stepsByQuest: Map<Long, List<com.irlrpg.app.data.local.entity.QuestStepEntity>>,
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
            .map { quest ->
                QuestProgress.withProgress(
                    quest = quest,
                    logs = logs,
                    category = categoriesById[quest.categoryId],
                    today = today,
                    steps = stepsByQuest[quest.id].orEmpty(),
                )
            }
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
            player = PlayerRepository.companionSnapshot(stats, levelRules()),
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
            stepsByQuest = stepsByQuest,
        )
    }

    /** Log ids that pushed the player across a level boundary. */
    private fun levelUpLogIds(logs: List<QuestLogEntity>): Set<Long> {
        val progression = LevelProgression(levelRules())
        var running = 0
        val ids = mutableSetOf<Long>()
        logs.sortedWith(compareBy({ it.epochDay }, { it.id })).forEach { log ->
            val before = progression.levelForTotalXp(running)
            running += log.xpEarned
            val after = progression.levelForTotalXp(running)
            if (after > before) ids.add(log.id)
        }
        return ids
    }
}
