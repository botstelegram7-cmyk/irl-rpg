package com.irlrpg.app.data.repository

import com.irlrpg.app.data.local.dao.BossDao
import com.irlrpg.app.data.local.dao.QuestDao
import com.irlrpg.app.data.local.entity.BossChallengeEntity
import com.irlrpg.app.data.local.entity.BossObjectiveEntity
import com.irlrpg.app.domain.game.BossEngine
import com.irlrpg.app.domain.game.BossTemplates
import com.irlrpg.app.domain.game.BossVictory
import com.irlrpg.app.domain.model.BossProgress
import com.irlrpg.app.domain.model.BossTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate

data class BossBoardState(
    val loading: Boolean = true,
    val active: List<BossProgress> = emptyList(),
    val completed: List<BossProgress> = emptyList(),
    val archived: List<BossProgress> = emptyList(),
    val linkableQuests: List<Pair<Long, String>> = emptyList(),
    val templates: List<BossTemplate> = BossTemplates.all,
) {
    val hasAny: Boolean get() = active.isNotEmpty() || completed.isNotEmpty() || archived.isNotEmpty()
}

/** Read/edit access to boss challenges, with progress derived from recorded logs. */
class BossRepository(
    private val bossDao: BossDao,
    private val questDao: QuestDao,
    private val engine: BossEngine,
) {

    fun observeBoard(): Flow<BossBoardState> = combine(
        bossDao.observeChallenges(),
        bossDao.observeAllObjectives(),
        questDao.observeAll(),
    ) { challenges, objectives, quests ->
        val questsById = quests.associateBy { it.id }
        val logsByQuest = emptyMap<Long, Long>() // progress resolved by the engine on demand
        val progress = challenges.map { challenge ->
            BossProgress(
                boss = challenge,
                objectives = objectives
                    .filter { it.bossId == challenge.id }
                    .map { objective ->
                        com.irlrpg.app.domain.model.BossObjectiveView(
                            objective = objective,
                            progress = if (objective.metric == BossObjectiveEntity.METRIC_MANUAL) {
                                objective.manualProgress
                            } else {
                                logsByQuest[objective.questId] ?: 0L
                            },
                            linkedQuestTitle = objective.questId?.let { questsById[it]?.title },
                        )
                    },
            )
        }
        BossBoardState(
            loading = false,
            active = progress.filter { it.boss.status == BossChallengeEntity.STATUS_ACTIVE },
            completed = progress.filter { it.boss.status == BossChallengeEntity.STATUS_COMPLETED },
            archived = progress.filter { it.boss.status == BossChallengeEntity.STATUS_ARCHIVED },
            linkableQuests = quests.filter { it.isActive }.map { it.id to it.title },
        )
    }.flowOn(Dispatchers.Default)

    /** Accurate progress (queries logs) — used by detail screens and the sweep. */
    suspend fun progressAll(): List<BossProgress> = engine.progressAll()

    suspend fun progressOf(bossId: Long): BossProgress? = engine.progressOf(bossId)

    suspend fun checkCompletions(today: LocalDate = LocalDate.now()): List<BossVictory> =
        engine.checkCompletions(today)

    suspend fun logManualObjective(objectiveId: Long, amount: Long): Boolean =
        engine.logManualObjective(objectiveId, amount)

    suspend fun createFromTemplate(
        template: BossTemplate,
        deadline: LocalDate?,
        linkedQuests: Map<Int, Long?>,
        today: LocalDate = LocalDate.now(),
    ): Long = engine.createFromTemplate(template, today, deadline, linkedQuests)

    suspend fun createCustom(
        title: String,
        description: String?,
        emoji: String,
        accentId: String,
        deadline: LocalDate?,
        rewardXp: Int,
        objectives: List<Triple<String, Long, Long?>>,
        today: LocalDate = LocalDate.now(),
    ): Long = engine.createCustom(title, description, emoji, accentId, deadline, rewardXp, objectives, today)

    suspend fun archive(bossId: Long) = engine.setStatus(bossId, BossChallengeEntity.STATUS_ARCHIVED)

    suspend fun reactivate(bossId: Long) = engine.setStatus(bossId, BossChallengeEntity.STATUS_ACTIVE)

    suspend fun delete(bossId: Long) = engine.delete(bossId)

    suspend fun count(): Int = bossDao.count()

    suspend fun completedCount(): Int = bossDao.completedCount()

    fun templates(): List<BossTemplate> = BossTemplates.all
}
