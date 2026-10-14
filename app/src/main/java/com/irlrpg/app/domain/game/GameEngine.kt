package com.irlrpg.app.domain.game

import androidx.room.withTransaction
import com.irlrpg.app.data.local.IrlRpgDatabase
import com.irlrpg.app.data.local.entity.PlayerStatsEntity
import com.irlrpg.app.data.local.entity.ProgressionEventEntity
import com.irlrpg.app.data.local.entity.QuestEntity
import com.irlrpg.app.data.local.entity.QuestLogEntity
import com.irlrpg.app.domain.model.ScheduleKind
import com.irlrpg.app.domain.model.TrackingKind
import com.irlrpg.app.domain.model.UnitKind
import java.time.LocalDate

/** Outcome of a check-in, used to drive the celebration UI. */
data class LogResult(
    val questTitle: String,
    val emoji: String,
    val xpGained: Int,
    val completedNow: Boolean,
    val levelUp: Boolean,
    val newLevel: Int,
    val totalXp: Int,
    val progressAfter: Long,
    val target: Long,
    val alreadyComplete: Boolean,
    val unitLabel: String,
) {
    val progressFraction: Float
        get() = if (target <= 0L) 0f else (progressAfter.toFloat() / target.toFloat()).coerceIn(0f, 1f)
}

/**
 * The only place XP is awarded.
 *
 * Every mutation runs inside a single Room transaction, so the log row, the
 * player's XP total, attribute totals and progression history can never drift
 * apart — and a process death mid-award cannot double-credit the player.
 */
class GameEngine(private val database: IrlRpgDatabase) {

    private val questDao = database.questDao()
    private val logDao = database.questLogDao()
    private val playerDao = database.playerDao()
    private val attributeDao = database.attributeDao()
    private val progressionDao = database.progressionDao()

    suspend fun logQuest(
        questId: Long,
        amount: Long,
        note: String? = null,
        today: LocalDate = LocalDate.now(),
    ): LogResult? {
        if (amount <= 0L) return null

        return database.withTransaction {
            val quest = questDao.byId(questId) ?: return@withTransaction null
            val kind = TrackingKind.fromName(quest.trackingKind)
            val schedule = ScheduleKind.fromName(quest.scheduleKind)
            val periodKey = PeriodKeys.forSchedule(schedule, today)
            val unitLabel = unitLabelOf(quest)

            val progressBefore = logDao.progressForQuestPeriod(questId, periodKey)?.totalAmount ?: 0L
            val alreadyComplete = isComplete(kind, quest.targetAmount, progressBefore)

            val progressXp = if (alreadyComplete) {
                0
            } else {
                XpEngine.xpForProgress(kind, quest.xpReward, quest.targetAmount, progressBefore, amount)
            }
            val completes = !alreadyComplete &&
                XpEngine.completesTarget(kind, quest.targetAmount, progressBefore, amount)
            val bonusXp = if (completes) XpEngine.completionBonus(quest.xpReward) else 0
            val xpGained = progressXp + bonusXp

            val logId = logDao.insert(
                QuestLogEntity(
                    questId = questId,
                    amount = amount,
                    unitLabel = unitLabel,
                    xpEarned = xpGained,
                    isCompletion = completes,
                    periodKey = periodKey,
                    epochDay = today.toEpochDay(),
                    note = note,
                )
            )

            val stats = playerDao.get()
                ?: PlayerStatsEntity(id = 1, createdAtEpochDay = today.toEpochDay()).also { playerDao.upsert(it) }
            val levelBefore = LevelCurve.levelForTotalXp(stats.totalXp)
            val newTotal = stats.totalXp + xpGained
            val levelAfter = LevelCurve.levelForTotalXp(newTotal)

            if (xpGained > 0) {
                playerDao.setXp(newTotal, System.currentTimeMillis())
                progressionDao.insert(
                    ProgressionEventEntity(
                        type = "xp",
                        title = quest.title,
                        detail = "$amount $unitLabel${if (completes) " · target reached" else ""}",
                        amount = xpGained,
                        level = levelAfter,
                        epochDay = today.toEpochDay(),
                        refLogId = logId,
                    )
                )
                attributeDao.byKindOrName(quest.attributeKind)?.let { attribute ->
                    attributeDao.addXp(attribute.id, xpGained)
                }
                if (levelAfter > levelBefore) {
                    progressionDao.insert(
                        ProgressionEventEntity(
                            type = "level_up",
                            title = "Level $levelAfter · ${LevelCurve.rankTitle(levelAfter)}",
                            detail = "Reached with ${quest.title}",
                            amount = 0,
                            level = levelAfter,
                            epochDay = today.toEpochDay(),
                            refLogId = logId,
                        )
                    )
                }
            }

            LogResult(
                questTitle = quest.title,
                emoji = quest.emoji,
                xpGained = xpGained,
                completedNow = completes,
                levelUp = levelAfter > levelBefore,
                newLevel = levelAfter,
                totalXp = newTotal,
                progressAfter = progressBefore + amount,
                target = quest.targetAmount,
                alreadyComplete = alreadyComplete,
                unitLabel = unitLabel,
            )
        }
    }

    /**
     * Removes one log entry and rebuilds the affected period from the surviving
     * rows, so XP that was already banked is recalculated — never guessed.
     */
    suspend fun undoLog(logId: Long): Boolean = database.withTransaction {
        val target = logDao.all().firstOrNull { it.id == logId } ?: return@withTransaction false
        val quest = questDao.byId(target.questId) ?: return@withTransaction false
        val kind = TrackingKind.fromName(quest.trackingKind)

        val periodBefore = logDao.rowsForPeriod(target.questId, target.periodKey)
        val xpBefore = periodBefore.sumOf { it.xpEarned }

        logDao.deleteById(logId)

        // Replay the surviving rows of this period to recompute exact XP.
        var progress = 0L
        val updated = logDao.rowsForPeriod(target.questId, target.periodKey).map { row ->
            val xp = XpEngine.xpForProgress(kind, quest.xpReward, quest.targetAmount, progress, row.amount)
            val completes = XpEngine.completesTarget(kind, quest.targetAmount, progress, row.amount)
            progress += row.amount
            row.copy(
                xpEarned = xp + if (completes) XpEngine.completionBonus(quest.xpReward) else 0,
                isCompletion = completes,
            )
        }
        if (updated.isNotEmpty()) logDao.updateRows(updated)
        val xpAfter = updated.sumOf { it.xpEarned }

        val delta = xpAfter - xpBefore
        if (delta != 0) {
            val stats = playerDao.get()
            val newTotal = ((stats?.totalXp ?: 0) + delta).coerceAtLeast(0)
            playerDao.setXp(newTotal, stats?.lastQuestAt)
            attributeDao.byKindOrName(quest.attributeKind)?.let { attribute ->
                attributeDao.adjustXp(attribute.id, delta)
            }
            progressionDao.insert(
                ProgressionEventEntity(
                    type = "undo",
                    title = "Undid ${target.amount} ${target.unitLabel} · ${quest.title}",
                    detail = "Progress corrected",
                    amount = delta,
                    level = LevelCurve.levelForTotalXp(newTotal),
                    epochDay = target.epochDay,
                )
            )
        }
        progressionDao.deleteByLogId(logId)
        true
    }

    /**
     * Clears every log entry of a quest and re-aligns the player's XP total and
     * attribute totals with it. Returns the number of rows removed.
     */
    suspend fun resetQuestProgress(questId: Long): Int = database.withTransaction {
        val rows = logDao.rowsForQuest(questId)
        if (rows.isEmpty()) return@withTransaction 0
        val xp = rows.sumOf { it.xpEarned }
        val quest = questDao.byId(questId)

        progressionDao.deleteByQuestLogs(questId)
        logDao.deleteForQuest(questId)

        if (xp != 0) {
            val stats = playerDao.get()
            val newTotal = ((stats?.totalXp ?: 0) - xp).coerceAtLeast(0)
            playerDao.setXp(newTotal, stats?.lastQuestAt)
            quest?.let { q ->
                attributeDao.byKindOrName(q.attributeKind)?.let { attributeDao.adjustXp(it.id, -xp) }
            }
        }
        rows.size
    }

    private fun unitLabelOf(quest: QuestEntity): String {
        val unit = UnitKind.fromName(quest.unit)
        return if (unit == UnitKind.Custom) quest.customUnit ?: "unit" else unit.label.lowercase()
    }

    private fun isComplete(kind: TrackingKind, target: Long, progress: Long): Boolean =
        if (kind == TrackingKind.Milestone) progress > 0L else target > 0L && progress >= target
}
