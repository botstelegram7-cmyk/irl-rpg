package com.irlrpg.app.domain.game

import androidx.room.withTransaction
import com.irlrpg.app.data.local.IrlRpgDatabase
import com.irlrpg.app.data.local.entity.PlayerStatsEntity
import com.irlrpg.app.data.local.entity.ProgressionEventEntity
import com.irlrpg.app.data.local.entity.QuestEntity
import com.irlrpg.app.data.local.entity.QuestInstanceEntity
import com.irlrpg.app.data.local.entity.QuestLogEntity
import com.irlrpg.app.data.local.entity.QuestStepEntity
import com.irlrpg.app.domain.model.QuestKind
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
    /** Set when the entry belonged to a step of a multi-step quest. */
    val stepTitle: String? = null,
    /** Steps finished in this period after this entry (multi-step quests). */
    val stepsCompleted: Int = 0,
    val stepsTotal: Int = 0,
) {
    val progressFraction: Float
        get() = if (target <= 0L) 0f else (progressAfter.toFloat() / target.toFloat()).coerceIn(0f, 1f)
}

/** Result of materialising recurring quest instances for a day. */
data class InstanceSyncResult(
    val created: Int,
    val existing: Int,
)

/**
 * The only place XP is awarded.
 *
 * Step 2 additions, all inside the same single-transaction discipline as Step 1:
 *
 *  * **Recurring instances** — `quest_instances` gets one row per quest and period.
 *    A unique index makes a duplicate instance impossible, so a recurring quest
 *    cannot be counted (or paid) twice for the same day/week.
 *  * **Multi-step quests** — XP is earned per step against that step's own target,
 *    and the completion bonus is granted only on the entry that finishes the last
 *    required step, once per period (tracked on the instance row).
 *  * **Long-term challenges** — progress accumulates in a single lifetime bucket
 *    with an optional deadline, and the target can only ever be paid once.
 *
 * Standard Step 1 quests behave exactly as before: proportional progress XP, one
 * completion bonus per period, zero XP after the target is reached.
 */
class GameEngine(
    private val database: IrlRpgDatabase,
    /** Level rules are consulted (never cached) so config changes apply at once. */
    private val levelRules: () -> LevelRules = { LevelRules.Default },
) {

    private val questDao = database.questDao()
    private val logDao = database.questLogDao()
    private val stepDao = database.questStepDao()
    private val instanceDao = database.questInstanceDao()
    private val playerDao = database.playerDao()
    private val attributeDao = database.attributeDao()
    private val progressionDao = database.progressionDao()

    // ---------------------------------------------------------------- check-in

    suspend fun logQuest(
        questId: Long,
        amount: Long,
        stepId: Long? = null,
        note: String? = null,
        today: LocalDate = LocalDate.now(),
    ): LogResult? {
        if (amount <= 0L) return null

        return database.withTransaction {
            val quest = questDao.byId(questId) ?: return@withTransaction null
            val kind = TrackingKind.fromName(quest.trackingKind)
            val questKind = QuestKind.fromName(quest.questKind)
            val schedule = ScheduleKind.fromName(quest.scheduleKind)
            val periodKey = periodKeyFor(quest, schedule, today)
            val unitLabel = unitLabelOf(quest)

            // Always materialise the instance before touching XP: this is the row
            // that proves "this period already exists" and blocks double rewards.
            val instance = ensureInstance(quest, periodKey, today)

            val steps = stepDao.forQuest(questId)

            val outcome = when {
                questKind == QuestKind.MultiStep && steps.isNotEmpty() ->
                    logStepEntry(quest, steps, stepId, amount, periodKey, kind, unitLabel)

                questKind == QuestKind.LongTerm ->
                    logLongTermEntry(quest, amount, periodKey, kind, unitLabel)

                else ->
                    logStandardEntry(quest, amount, periodKey, kind, unitLabel)
            }

            val logId = logDao.insert(
                QuestLogEntity(
                    questId = questId,
                    amount = amount,
                    unitLabel = outcome.unitLabel,
                    xpEarned = outcome.xp,
                    isCompletion = outcome.completesQuest,
                    periodKey = periodKey,
                    epochDay = today.toEpochDay(),
                    note = note,
                    stepId = stepId,
                )
            )

            // Refresh the instance summary from the rows that now exist.
            val periodRows = logDao.rowsForPeriod(questId, periodKey)
            val instanceStatus = if (periodRows.any { it.isCompletion }) {
                QuestInstanceEntity.STATUS_COMPLETED
            } else {
                QuestInstanceEntity.STATUS_OPEN
            }
            instanceDao.setOutcome(
                questId = questId,
                periodKey = periodKey,
                xp = periodRows.sumOf { it.xpEarned },
                status = instanceStatus,
            )

            val stats = playerDao.get()
                ?: PlayerStatsEntity(id = 1, createdAtEpochDay = today.toEpochDay()).also { playerDao.upsert(it) }
            val progression = LevelProgression(currentRules())
            val levelBefore = progression.levelForTotalXp(stats.totalXp)
            val newTotal = stats.totalXp + outcome.xp
            val levelAfter = progression.levelForTotalXp(newTotal)

            if (outcome.xp > 0) {
                playerDao.setXp(newTotal, System.currentTimeMillis())
                progressionDao.insert(
                    ProgressionEventEntity(
                        type = "xp",
                        title = quest.title,
                        detail = buildString {
                            append(amount)
                            append(' ')
                            append(outcome.unitLabel)
                            outcome.stepTitle?.let { append(" · ").append(it) }
                            if (outcome.completesQuest) append(" · target reached")
                        },
                        amount = outcome.xp,
                        level = levelAfter,
                        epochDay = today.toEpochDay(),
                        refLogId = logId,
                    )
                )
                attributeDao.byKindOrName(quest.attributeKind)?.let { attribute ->
                    attributeDao.addXp(attribute.id, outcome.xp)
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
                xpGained = outcome.xp,
                completedNow = outcome.completesQuest,
                levelUp = levelAfter > levelBefore,
                newLevel = levelAfter,
                totalXp = newTotal,
                progressAfter = if (questKind == QuestKind.MultiStep) {
                    outcome.stepsCompleted.toLong()
                } else {
                    (if (kind == TrackingKind.Milestone) 1L else periodRows.sumOf { it.amount })
                },
                target = if (questKind == QuestKind.MultiStep) steps.count { it.isRequired }.toLong() else quest.targetAmount,
                alreadyComplete = instance?.status == QuestInstanceEntity.STATUS_COMPLETED && outcome.xp == 0,
                unitLabel = if (questKind == QuestKind.MultiStep) "steps" else outcome.unitLabel,
                stepTitle = outcome.stepTitle,
                stepsCompleted = outcome.stepsCompleted,
                stepsTotal = if (questKind == QuestKind.MultiStep) steps.count { it.isRequired } else 0,
            )
        }
    }

    private data class EntryOutcome(
        val xp: Int,
        val completesQuest: Boolean,
        val unitLabel: String,
        val stepTitle: String? = null,
        val stepsCompleted: Int = 0,
    )

    /** Step 1 behaviour, unchanged: one proportional award plus one bonus per period. */
    private suspend fun logStandardEntry(
        quest: QuestEntity,
        amount: Long,
        periodKey: String,
        kind: TrackingKind,
        unitLabel: String,
    ): EntryOutcome {
        val progressBefore = logDao.progressForQuestPeriod(quest.id, periodKey)?.totalAmount ?: 0L
        val alreadyComplete = isComplete(kind, quest.targetAmount, progressBefore)
        val xp = if (alreadyComplete) {
            0
        } else {
            XpEngine.xpForProgress(kind, quest.xpReward, quest.targetAmount, progressBefore, amount)
        }
        val completes = !alreadyComplete &&
            XpEngine.completesTarget(kind, quest.targetAmount, progressBefore, amount)
        return EntryOutcome(
            xp = xp + if (completes) XpEngine.completionBonus(quest.xpReward) else 0,
            completesQuest = completes,
            unitLabel = unitLabel,
        )
    }

    /** Long-term challenges accumulate in one lifetime bucket. */
    private suspend fun logLongTermEntry(
        quest: QuestEntity,
        amount: Long,
        periodKey: String,
        kind: TrackingKind,
        unitLabel: String,
    ): EntryOutcome {
        // Identical maths to a standard quest — the difference is the bucket
        // (never resets) and the deadline shown in the UI.
        val progressBefore = logDao.progressForQuestPeriod(quest.id, periodKey)?.totalAmount ?: 0L
        val alreadyComplete = quest.targetAmount > 0L && progressBefore >= quest.targetAmount
        val xp = if (alreadyComplete) {
            0
        } else {
            XpEngine.xpForProgress(kind, quest.xpReward, quest.targetAmount, progressBefore, amount)
        }
        val completes = !alreadyComplete &&
            XpEngine.completesTarget(kind, quest.targetAmount, progressBefore, amount)
        return EntryOutcome(
            xp = xp + if (completes) XpEngine.completionBonus(quest.xpReward) else 0,
            completesQuest = completes,
            unitLabel = unitLabel,
        )
    }

    /**
     * Multi-step quests: progress (and XP) is per step, and the quest-level bonus
     * is paid exactly once per period — enforced by the instance row's status.
     */
    private suspend fun logStepEntry(
        quest: QuestEntity,
        steps: List<QuestStepEntity>,
        stepId: Long?,
        amount: Long,
        periodKey: String,
        kind: TrackingKind,
        unitLabel: String,
    ): EntryOutcome {
        val step = stepId?.let { id -> steps.firstOrNull { it.id == id } }
            ?: steps.firstOrNull { step ->
                // Default to the first unfinished required step.
                val logged = logDao.rowsForStep(quest.id, periodKey, step.id).sumOf { it.amount }
                logged < step.targetAmount
            }
            ?: steps.first()

        val requiredSteps = steps.filter { it.isRequired }
        val stepXpBudget = (quest.xpReward / requiredSteps.size.coerceAtLeast(1)).coerceAtLeast(5)
        val stepProgressBefore = logDao.rowsForStep(quest.id, periodKey, step.id).sumOf { it.amount }
        val stepAlreadyDone = stepProgressBefore >= step.targetAmount
        val completingStep = !stepAlreadyDone && stepProgressBefore + amount >= step.targetAmount

        val stepXp = if (stepAlreadyDone) {
            0
        } else {
            XpEngine.xpForProgress(kind, stepXpBudget, step.targetAmount, stepProgressBefore, amount)
        }

        // Recompute which steps are complete after this entry.
        val stepsCompleted = steps.count { candidate ->
            val logged = if (candidate.id == step.id) stepProgressBefore + amount else {
                logDao.rowsForStep(quest.id, periodKey, candidate.id).sumOf { it.amount }
            }
            candidate.targetAmount > 0L && logged >= candidate.targetAmount
        }
        val requiredComplete = requiredSteps.all { candidate ->
            val logged = if (candidate.id == step.id) stepProgressBefore + amount else {
                logDao.rowsForStep(quest.id, periodKey, candidate.id).sumOf { it.amount }
            }
            candidate.targetAmount > 0L && logged >= candidate.targetAmount
        }
        val instance = instanceDao.find(quest.id, periodKey)
        val bonusAlreadyPaid = instance?.status == QuestInstanceEntity.STATUS_COMPLETED
        val paysBonus = requiredComplete && !bonusAlreadyPaid

        return EntryOutcome(
            xp = stepXp + if (paysBonus) XpEngine.completionBonus(quest.xpReward) else 0,
            completesQuest = paysBonus,
            unitLabel = stepUnitLabel(step, quest) ?: unitLabel,
            stepTitle = step.title,
            stepsCompleted = stepsCompleted,
        )
    }

    // ------------------------------------------------------------- instances

    /**
     * Creates the instance row for a quest's current period if it does not exist
     * yet. Idempotent by construction (ignored conflict on the unique index).
     */
    suspend fun ensureInstance(
        quest: QuestEntity,
        periodKey: String,
        today: LocalDate,
    ): QuestInstanceEntity? {
        val existing = instanceDao.find(quest.id, periodKey)
        if (existing != null) return existing
        instanceDao.insertIfAbsent(
            QuestInstanceEntity(
                questId = quest.id,
                periodKey = periodKey,
                epochDay = today.toEpochDay(),
                status = QuestInstanceEntity.STATUS_OPEN,
            )
        )
        return instanceDao.find(quest.id, periodKey)
    }

    /**
     * Materialises today's instances for every active quest that is scheduled for
     * today. Cheap, and safe to call on every app start / resume.
     */
    suspend fun ensureInstancesForToday(today: LocalDate = LocalDate.now()): InstanceSyncResult {
        var created = 0
        var existing = 0
        database.withTransaction {
            val quests = questDao.allActive()
            quests.forEach { quest ->
                if (!PeriodKeys.isScheduledFor(
                        ScheduleKind.fromName(quest.scheduleKind),
                        quest.scheduleDays,
                        today,
                    )
                ) {
                    return@forEach
                }
                val periodKey = periodKeyFor(quest, ScheduleKind.fromName(quest.scheduleKind), today)
                if (instanceDao.find(quest.id, periodKey) != null) {
                    existing++
                } else {
                    instanceDao.insertIfAbsent(
                        QuestInstanceEntity(
                            questId = quest.id,
                            periodKey = periodKey,
                            epochDay = today.toEpochDay(),
                        )
                    )
                    created++
                }
            }
        }
        return InstanceSyncResult(created = created, existing = existing)
    }

    // ------------------------------------------------------------------- undo

    /**
     * Removes one log entry and rebuilds the affected period from the surviving
     * rows, so XP that was already banked is recalculated — never guessed.
     */
    suspend fun undoLog(logId: Long): Boolean = database.withTransaction {
        val target = logDao.all().firstOrNull { it.id == logId } ?: return@withTransaction false
        val quest = questDao.byId(target.questId) ?: return@withTransaction false
        val kind = TrackingKind.fromName(quest.trackingKind)
        val questKind = QuestKind.fromName(quest.questKind)
        val steps = stepDao.forQuest(quest.id)

        val periodBefore = logDao.rowsForPeriod(target.questId, target.periodKey)
        val xpBefore = periodBefore.sumOf { it.xpEarned }

        logDao.deleteById(logId)

        var progress = 0L
        val stepProgress = mutableMapOf<Long, Long>()
        val updated = logDao.rowsForPeriod(target.questId, target.periodKey).map { row ->
            val (xp, completes) = when {
                questKind == QuestKind.MultiStep && steps.isNotEmpty() -> {
                    val step = row.stepId?.let { id -> steps.firstOrNull { it.id == id } } ?: steps.first()
                    val before = stepProgress[step.id] ?: 0L
                    val stepXpBudget = (quest.xpReward / steps.count { it.isRequired }.coerceAtLeast(1))
                        .coerceAtLeast(5)
                    val xp = XpEngine.xpForProgress(kind, stepXpBudget, step.targetAmount, before, row.amount)
                    stepProgress[step.id] = before + row.amount
                    xp to false // quest-level bonus resolved after the loop
                }

                else -> {
                    val xp = XpEngine.xpForProgress(kind, quest.xpReward, quest.targetAmount, progress, row.amount)
                    val completes = XpEngine.completesTarget(kind, quest.targetAmount, progress, row.amount)
                    progress += row.amount
                    (xp + if (completes) XpEngine.completionBonus(quest.xpReward) else 0) to completes
                }
            }
            row.copy(xpEarned = xp, isCompletion = completes)
        }

        val rebuilt = if (questKind == QuestKind.MultiStep && steps.isNotEmpty()) {
            // Re-derive the quest-level bonus from the rebuilt step state.
            val requiredSteps = steps.filter { it.isRequired }
            val allDone = requiredSteps.all { step ->
                (stepProgress[step.id] ?: 0L) >= step.targetAmount
            }
            if (allDone) {
                val bonusIndex = updated.indexOfLast { row ->
                    val step = row.stepId?.let { id -> steps.firstOrNull { it.id == id } } ?: steps.first()
                    val finalStep = requiredSteps.lastOrNull { required ->
                        (stepProgress[required.id] ?: 0L) >= required.targetAmount
                    }
                    finalStep != null && step.id == finalStep.id
                }
                updated.mapIndexed { index, row ->
                    if (index == bonusIndex) {
                        row.copy(
                            xpEarned = row.xpEarned + XpEngine.completionBonus(quest.xpReward),
                            isCompletion = true,
                        )
                    } else {
                        row
                    }
                }
            } else {
                updated
            }
        } else {
            updated
        }

        if (rebuilt.isNotEmpty()) logDao.updateRows(rebuilt)
        val xpAfter = rebuilt.sumOf { it.xpEarned }

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
        syncInstanceFromRows(quest.id, target.periodKey)
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
        // Instances are derived summaries — rebuild them from the surviving rows.
        rows.map { it.periodKey }.distinct().forEach { key -> syncInstanceFromRows(questId, key) }
        rows.size
    }

    /** Rebuilds a period's instance row from the log rows that survived. */
    private suspend fun syncInstanceFromRows(questId: Long, periodKey: String) {
        val rows = logDao.rowsForPeriod(questId, periodKey)
        if (rows.isEmpty()) return
        instanceDao.setOutcome(
            questId = questId,
            periodKey = periodKey,
            xp = rows.sumOf { it.xpEarned },
            status = if (rows.any { it.isCompletion }) {
                QuestInstanceEntity.STATUS_COMPLETED
            } else {
                QuestInstanceEntity.STATUS_OPEN
            },
        )
    }

    // ---------------------------------------------------------------- helpers

    private fun currentRules(): LevelRules = levelRules()

    private fun periodKeyFor(quest: QuestEntity, schedule: ScheduleKind, today: LocalDate): String =
        if (QuestKind.fromName(quest.questKind) == QuestKind.LongTerm) {
            PeriodKeys.FLEX_KEY
        } else {
            PeriodKeys.forSchedule(schedule, today)
        }

    private fun unitLabelOf(quest: QuestEntity): String {
        val unit = UnitKind.fromName(quest.unit)
        return if (unit == UnitKind.Custom) quest.customUnit ?: "unit" else unit.label.lowercase()
    }

    private fun stepUnitLabel(step: QuestStepEntity, quest: QuestEntity): String? {
        val unitName = step.unit ?: return null
        val unit = UnitKind.fromName(unitName)
        return if (unit == UnitKind.Custom) step.customUnit ?: "unit" else unit.label.lowercase()
    }

    private fun isComplete(kind: TrackingKind, target: Long, progress: Long): Boolean =
        if (kind == TrackingKind.Milestone) progress > 0L else target > 0L && progress >= target
}
