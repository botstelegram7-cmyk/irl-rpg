package com.irlrpg.app.domain.game

import androidx.room.withTransaction
import com.irlrpg.app.data.local.IrlRpgDatabase
import com.irlrpg.app.data.local.entity.BossChallengeEntity
import com.irlrpg.app.data.local.entity.BossObjectiveEntity
import com.irlrpg.app.data.local.entity.ProgressionEventEntity
import com.irlrpg.app.domain.model.BossObjectiveView
import com.irlrpg.app.domain.model.BossProgress
import com.irlrpg.app.domain.model.BossTemplate
import java.time.LocalDate

/** Outcome of a completion sweep, used to trigger the victory sequence. */
data class BossVictory(
    val bossId: Long,
    val title: String,
    val emoji: String,
    val xpAwarded: Int,
    val objectivesCompleted: Int,
)

/**
 * Boss challenges: multi-objective real-life goals.
 *
 * Progress is read from what the player actually recorded — a linked quest's log
 * rows since the challenge started, or a manually tracked objective. Required
 * objectives gate completion, and the victory reward is paid exactly once because
 * `xpAwarded`/`status` are written inside the same transaction that pays it.
 *
 * There is no combat system anywhere in this class: a boss is simply a goal that
 * takes several weeks and is worth celebrating.
 */
class BossEngine(private val database: IrlRpgDatabase) {

    private val bossDao = database.bossDao()
    private val logDao = database.questLogDao()
    private val playerDao = database.playerDao()
    private val progressionDao = database.progressionDao()

    /** Resolves every challenge with live objective progress. */
    suspend fun progressAll(): List<BossProgress> {
        val challenges = bossDao.challenges()
        val objectives = bossDao.allObjectives()
        val questsById = database.questDao().all().associateBy { it.id }
        val logs = logDao.all()
        return challenges.map { challenge ->
            val views = objectives
                .filter { it.bossId == challenge.id }
                .map { objective -> view(objective, challenge, questsById, logs) }
            BossProgress(boss = challenge, objectives = views)
        }
    }

    suspend fun progressOf(bossId: Long): BossProgress? {
        val challenge = bossDao.challenge(bossId) ?: return null
        val objectives = bossDao.objectives(bossId)
        val questsById = database.questDao().all().associateBy { it.id }
        val logs = logDao.all()
        return BossProgress(
            boss = challenge,
            objectives = objectives.map { view(it, challenge, questsById, logs) },
        )
    }

    private fun view(
        objective: BossObjectiveEntity,
        challenge: BossChallengeEntity,
        questsById: Map<Long, com.irlrpg.app.data.local.entity.QuestEntity>,
        logs: List<com.irlrpg.app.data.local.entity.QuestLogEntity>,
    ): BossObjectiveView {
        val linkedTitle = objective.questId?.let { questsById[it]?.title }
        val progress = when (objective.metric) {
            BossObjectiveEntity.METRIC_LINKED_QUEST -> {
                val questId = objective.questId
                if (questId == null) {
                    0L
                } else {
                    logs.filter { log ->
                        log.questId == questId && log.epochDay >= challenge.startedEpochDay
                    }.sumOf { it.amount }
                }
            }
            else -> objective.manualProgress
        }
        return BossObjectiveView(
            objective = objective,
            progress = progress,
            linkedQuestTitle = linkedTitle,
        )
    }

    /**
     * Checks every active challenge; completes those whose required objectives are
     * done, paying the reward once. Returns the victories that happened now so the
     * UI can run the celebration.
     */
    suspend fun checkCompletions(today: LocalDate = LocalDate.now()): List<BossVictory> {
        val progress = progressAll().filter { !it.isCompleted && it.boss.status == BossChallengeEntity.STATUS_ACTIVE }
        val ready = progress.filter { it.allRequiredComplete }
        if (ready.isEmpty()) return emptyList()

        return database.withTransaction {
            val victories = mutableListOf<BossVictory>()
            ready.forEach { item ->
                // Re-read inside the transaction so two sweeps can never both pay.
                val fresh = bossDao.challenge(item.boss.id) ?: return@forEach
                if (fresh.status == BossChallengeEntity.STATUS_COMPLETED || fresh.xpAwarded > 0) {
                    return@forEach
                }
                val reward = fresh.rewardXp
                bossDao.updateChallenge(
                    fresh.copy(
                        status = BossChallengeEntity.STATUS_COMPLETED,
                        completedAtEpochDay = today.toEpochDay(),
                        xpAwarded = reward,
                    )
                )
                val stats = playerDao.get()
                val newTotal = ((stats?.totalXp ?: 0) + reward).coerceAtLeast(0)
                playerDao.setXp(newTotal, stats?.lastQuestAt)
                progressionDao.insert(
                    ProgressionEventEntity(
                        type = "boss_completed",
                        title = "Boss defeated · ${fresh.title}",
                        detail = "${item.completedRequired} of ${item.required.size} objectives cleared",
                        amount = reward,
                        level = LevelCurve.levelForTotalXp(newTotal),
                        epochDay = today.toEpochDay(),
                    )
                )
                victories += BossVictory(
                    bossId = fresh.id,
                    title = fresh.title,
                    emoji = fresh.emoji,
                    xpAwarded = reward,
                    objectivesCompleted = item.completedRequired,
                )
            }
            victories
        }
    }

    /** Manual objective tracking (objectives not tied to a quest). */
    suspend fun logManualObjective(objectiveId: Long, amount: Long): Boolean {
        if (amount <= 0L) return false
        val objective = bossDao.allObjectives().firstOrNull { it.id == objectiveId } ?: return false
        bossDao.updateObjective(
            objective.copy(manualProgress = (objective.manualProgress + amount).coerceAtLeast(0))
        )
        return true
    }

    suspend fun setStatus(bossId: Long, status: String) {
        bossDao.challenge(bossId)?.let { bossDao.updateChallenge(it.copy(status = status)) }
    }

    suspend fun delete(bossId: Long) = bossDao.deleteChallenge(bossId)

    /** Creates a challenge from a template, as an ordinary editable row. */
    suspend fun createFromTemplate(
        template: BossTemplate,
        today: LocalDate = LocalDate.now(),
        deadlineOverride: LocalDate? = null,
        linkedQuests: Map<Int, Long?> = emptyMap(),
    ): Long = database.withTransaction {
        val deadline = deadlineOverride ?: template.durationDays.let { today.plusDays(it.toLong()) }
        val bossId = bossDao.insertChallenge(
            BossChallengeEntity(
                title = template.title,
                description = template.description,
                emoji = template.emoji,
                accentId = template.accentId,
                deadlineEpochDay = deadline.toEpochDay(),
                rewardXp = template.rewardXp,
                templateCode = template.code,
                startedEpochDay = today.toEpochDay(),
            )
        )
        bossDao.insertObjectives(
            template.objectiveTitles.mapIndexed { index, (title, target) ->
                BossObjectiveEntity(
                    bossId = bossId,
                    title = title,
                    metric = linkedQuests[index]?.let { BossObjectiveEntity.METRIC_LINKED_QUEST }
                        ?: BossObjectiveEntity.METRIC_MANUAL,
                    questId = linkedQuests[index],
                    unitName = "units",
                    target = target,
                    isRequired = true,
                    sortOrder = index,
                )
            }
        )
        bossId
    }

    /** Fully custom challenge builder. */
    suspend fun createCustom(
        title: String,
        description: String?,
        emoji: String,
        accentId: String,
        deadline: LocalDate?,
        rewardXp: Int,
        objectives: List<Triple<String, Long, Long?>>,
        today: LocalDate = LocalDate.now(),
    ): Long = database.withTransaction {
        val bossId = bossDao.insertChallenge(
            BossChallengeEntity(
                title = title.trim(),
                description = description?.trim()?.takeIf { it.isNotBlank() },
                emoji = emoji,
                accentId = accentId,
                deadlineEpochDay = deadline?.toEpochDay(),
                rewardXp = rewardXp.coerceIn(50, 5000),
                startedEpochDay = today.toEpochDay(),
                templateCode = null,
            )
        )
        bossDao.insertObjectives(
            objectives.mapIndexed { index, (objectiveTitle, target, questId) ->
                BossObjectiveEntity(
                    bossId = bossId,
                    title = objectiveTitle,
                    metric = if (questId != null) {
                        BossObjectiveEntity.METRIC_LINKED_QUEST
                    } else {
                        BossObjectiveEntity.METRIC_MANUAL
                    },
                    questId = questId,
                    target = target.coerceAtLeast(1L),
                    isRequired = true,
                    sortOrder = index,
                )
            }
        )
        bossId
    }
}
