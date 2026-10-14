package com.irlrpg.app.data.repository

import com.irlrpg.app.data.local.dao.AchievementDao
import com.irlrpg.app.data.local.dao.AttributeDao
import com.irlrpg.app.data.local.dao.BossDao
import com.irlrpg.app.data.local.dao.CategoryDao
import com.irlrpg.app.data.local.dao.PlayerDao
import com.irlrpg.app.data.local.dao.ProgressionDao
import com.irlrpg.app.data.local.dao.QuestDao
import com.irlrpg.app.data.local.dao.QuestLogDao
import com.irlrpg.app.data.local.dao.SkillNodeDao
import com.irlrpg.app.data.local.entity.AchievementEarnedEntity
import com.irlrpg.app.data.local.entity.AttributeEntity
import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.local.entity.ProgressionEventEntity
import com.irlrpg.app.data.local.entity.QuestEntity
import com.irlrpg.app.data.local.entity.QuestLogEntity
import com.irlrpg.app.data.local.entity.SkillNodeEntity
import com.irlrpg.app.domain.game.AchievementCatalog
import com.irlrpg.app.domain.game.AchievementEngine
import com.irlrpg.app.domain.game.ActivitySnapshot
import com.irlrpg.app.domain.game.LevelProgression
import com.irlrpg.app.domain.game.LevelRules
import com.irlrpg.app.domain.game.SkillTreeEngine
import com.irlrpg.app.domain.game.StreakCalculator
import com.irlrpg.app.domain.model.AchievementCategory
import com.irlrpg.app.domain.model.AchievementDef
import com.irlrpg.app.domain.model.AchievementView
import com.irlrpg.app.domain.model.QuestKind
import com.irlrpg.app.domain.model.SkillNodeState
import com.irlrpg.app.domain.model.TrackingKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate

/**
 * Achievement progress + earning.
 *
 * Reading is pure (`AchievementEngine`); earning is a single `insertIfAbsent` per
 * newly satisfied definition, so re-running the sweep can never double-earn or
 * revoke anything, and the recorded date is the first day the condition truly held.
 * There is no path that marks an achievement earned without data behind it.
 */
class AchievementRepository(
    private val achievementDao: AchievementDao,
    private val logDao: QuestLogDao,
    private val questDao: QuestDao,
    private val categoryDao: CategoryDao,
    private val attributeDao: AttributeDao,
    private val skillNodeDao: SkillNodeDao,
    private val progressionDao: ProgressionDao,
    private val bossDao: BossDao,
    private val levelRules: () -> LevelRules,
) {

    /** Newly earned achievements, pushed to the in-app banner. */
    private val _unlocked = MutableSharedFlow<List<AchievementDef>>(extraBufferCapacity = 8)
    val unlocked: SharedFlow<List<AchievementDef>> = _unlocked.asSharedFlow()

    fun observeEarned(): Flow<List<AchievementEarnedEntity>> = achievementDao.observeAll()

    // ------------------------------------------------------------- snapshots

    /** Everything the catalogue may look at — all derived from recorded rows. */
    suspend fun snapshot(): ActivitySnapshot = buildSnapshot(
        quests = questDao.all(),
        categories = categoryDao.all(),
        attributes = attributeDao.all(),
        nodes = skillNodeDao.all(),
        logs = logDao.all(),
        bossesCompleted = bossDao.completedCount(),
        customBossCreated = bossDao.challenges().any { it.templateCode == null },
    )

    private fun buildSnapshot(
        quests: List<QuestEntity>,
        categories: List<CategoryEntity>,
        attributes: List<AttributeEntity>,
        nodes: List<SkillNodeEntity>,
        logs: List<QuestLogEntity>,
        bossesCompleted: Int,
        customBossCreated: Boolean,
    ): ActivitySnapshot {
        val questsById = quests.associateBy { it.id }
        val totalXp = logs.sumOf { it.xpEarned }
        val progression = LevelProgression(levelRules())
        val activeDays = MetricsAggregator.activeDays(logs)

        val nodeViews = SkillTreeEngine.evaluate(nodes, attributes, totalXp)
        val masteredIds = nodeViews.filter { it.state == SkillNodeState.Mastered }.map { it.node.id }.toSet()
        val branchComplete = nodeViews.any { view ->
            masteredIds.contains(view.node.id) &&
                view.children.isNotEmpty() &&
                view.children.all { masteredIds.contains(it) }
        }

        val readingCategoryIds = categories.filter { it.isReadingLike() }.map { it.id }.toSet()

        return ActivitySnapshot(
            totalXp = totalXp,
            level = progression.levelForTotalXp(totalXp),
            completions = logs.count { it.isCompletion },
            activeDays = activeDays,
            longestStreak = StreakCalculator.longest(activeDays),
            fullWeekAchieved = AchievementEngine.hasFullWeek(activeDays),
            masteredNodes = masteredIds.size,
            branchComplete = branchComplete,
            pagesLogged = MetricsAggregator.pagesRead(logs, questsById),
            minutesLogged = logs.sumOf { MetricsAggregator.minutesOf(it, questsById[it.questId]) },
            bookFinished = logs.any { log ->
                val quest = questsById[log.questId] ?: return@any false
                log.isCompletion &&
                    TrackingKind.fromName(quest.trackingKind) == TrackingKind.Milestone &&
                    readingCategoryIds.contains(quest.categoryId)
            },
            bossesCompleted = bossesCompleted,
            multiStepDone = logs.any { log ->
                log.isCompletion && QuestKind.fromName(
                    questsById[log.questId]?.questKind ?: "STANDARD"
                ) == QuestKind.MultiStep
            },
            longTermDone = logs.any { log ->
                log.isCompletion && QuestKind.fromName(
                    questsById[log.questId]?.questKind ?: "STANDARD"
                ) == QuestKind.LongTerm
            },
            customSkillNodes = nodes.count { !it.isBuiltIn },
            customCategories = categories.count { !it.isBuiltIn },
            questCount = quests.size,
            customBossCreated = customBossCreated,
            skillNodesMasteredNames = nodeViews.filter { it.state == SkillNodeState.Mastered }
                .map { it.node.name },
        )
    }

    private fun CategoryEntity.isReadingLike(): Boolean {
        val n = name.lowercase()
        return n.contains("read") || n.contains("book") || n.contains("novel") || n.contains("literature")
    }

    // ----------------------------------------------------------------- views

    /** Live views for the achievements screen (recomputes whenever any input changes). */
    fun observeViews(): Flow<List<AchievementView>> = combine(
        achievementDao.observeAll(),
        questDao.observeAll(),
        categoryDao.observeAll(),
        attributeDao.observeAll(),
        skillNodeDao.observeAll(),
        logDao.observeAll(),
        bossDao.observeChallenges(),
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val earned = (array[0] as List<AchievementEarnedEntity>).associate { it.code to it.epochDay }
        val bosses = array[6] as List<com.irlrpg.app.data.local.entity.BossChallengeEntity>
        AchievementEngine.views(
            snapshot = buildSnapshot(
                quests = array[1] as List<QuestEntity>,
                categories = array[2] as List<CategoryEntity>,
                attributes = array[3] as List<AttributeEntity>,
                nodes = array[4] as List<SkillNodeEntity>,
                logs = array[5] as List<QuestLogEntity>,
                bossesCompleted = bosses.count {
                    it.status == com.irlrpg.app.data.local.entity.BossChallengeEntity.STATUS_COMPLETED
                },
                customBossCreated = bosses.any { it.templateCode == null },
            ),
            earnedOn = earned,
        )
    }.flowOn(Dispatchers.Default)

    suspend fun views(): List<AchievementView> {
        return AchievementEngine.views(
            snapshot = snapshot(),
            earnedOn = achievementDao.all().associate { it.code to it.epochDay },
        )
    }

    // --------------------------------------------------------------- earning

    /**
     * Persists every newly satisfied achievement and returns them so the UI can
     * celebrate. Idempotent: safe to call after every activity and at startup.
     */
    suspend fun sync(today: LocalDate = LocalDate.now()): List<AchievementDef> {
        val snapshot = snapshot()
        val earnedOn = achievementDao.all().associate { it.code to it.epochDay }
        val fresh = AchievementEngine.newlyEarned(snapshot, earnedOn)
        if (fresh.isEmpty()) return emptyList()
        fresh.forEach { def ->
            achievementDao.insertIfAbsent(
                AchievementEarnedEntity(
                    code = def.code,
                    category = def.category.name,
                    epochDay = today.toEpochDay(),
                )
            )
            progressionDao.insert(
                ProgressionEventEntity(
                    type = "achievement",
                    title = "Achievement · ${def.name}",
                    detail = def.description,
                    amount = 0,
                    level = null,
                    epochDay = today.toEpochDay(),
                )
            )
        }
        _unlocked.tryEmit(fresh)
        return fresh
    }

    fun groupedViews(views: List<AchievementView>): Map<AchievementCategory, List<AchievementView>> =
        AchievementEngine.grouped(views)

    fun catalog(): List<AchievementDef> = AchievementCatalog.all

    fun earnDates(earned: List<AchievementEarnedEntity>): Map<String, Long> =
        earned.associate { it.code to it.epochDay }

    fun earnedCodes(earned: List<AchievementEarnedEntity>): Set<String> = earned.map { it.code }.toSet()
}
