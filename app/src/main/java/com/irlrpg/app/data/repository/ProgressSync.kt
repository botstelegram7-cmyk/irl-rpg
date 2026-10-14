package com.irlrpg.app.data.repository

import com.irlrpg.app.data.local.dao.QuestDao
import com.irlrpg.app.data.local.dao.QuestInstanceDao
import com.irlrpg.app.data.local.dao.QuestLogDao
import com.irlrpg.app.data.local.dao.QuestStepDao
import com.irlrpg.app.data.local.entity.QuestInstanceEntity
import com.irlrpg.app.domain.game.AchievementCatalog
import com.irlrpg.app.domain.game.BossVictory
import com.irlrpg.app.domain.game.GameEngine
import com.irlrpg.app.domain.model.AchievementDef
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalDate

/** Everything the UI may want to celebrate after a sweep. */
data class ProgressEvent(
    val victories: List<BossVictory>,
    val achievements: List<AchievementDef>,
)

/**
 * One entry point that every screen calls after activity, and that the app calls
 * once at startup. It keeps the Step 2 systems in sync without scattering logic:
 *
 *  1. materialise recurring quest instances (idempotent),
 *  2. complete any boss challenge whose objectives are met (pays once),
 *  3. persist newly earned achievements and announce them.
 *
 * Deliberately *not* a background service: it runs on the main flow of app usage
 * and on resume, so the app keeps its no-permissions, no-wakeups promise.
 */
class ProgressSync(
    private val gameEngine: GameEngine,
    private val achievementRepository: AchievementRepository,
    private val bossRepository: BossRepository,
    private val instanceDao: QuestInstanceDao,
) {

    private val _events = MutableSharedFlow<ProgressEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ProgressEvent> = _events.asSharedFlow()

    /**
     * @param announce when false (first run after upgrading) achievements are
     *   still recorded honestly, but the banner only reports the total — nobody
     *   wants eleven pop-ups at once after installing an update.
     */
    suspend fun sweep(today: LocalDate = LocalDate.now(), announce: Boolean = true): ProgressEvent {
        gameEngine.ensureInstancesForToday(today)
        val victories = bossRepository.checkCompletions(today)
        val achievements = achievementRepository.sync(today)
        val event = ProgressEvent(victories = victories, achievements = achievements)
        if (announce && (victories.isNotEmpty() || achievements.isNotEmpty())) {
            _events.tryEmit(event)
        }
        return event
    }

    /** Catch-up pass: records everything earned from history, silently. */
    suspend fun prime(today: LocalDate = LocalDate.now()): List<AchievementDef> =
        achievementRepository.sync(today)

    suspend fun pendingAchievementCount(): Int = achievementRepository.views().count { it.earned }

    fun catalogue(): List<AchievementDef> = AchievementCatalog.all
}

/**
 * Small helper that keeps `quest_instances` an accurate mirror of the log rows.
 * Exposed separately so the quests screen can refresh instance state cheaply.
 */
class InstanceMaintenance(
    private val instanceDao: QuestInstanceDao,
    private val logDao: QuestLogDao,
    private val questDao: QuestDao,
    private val stepDao: QuestStepDao,
) {

    suspend fun syncQuest(questId: Long): Int {
        val quest = questDao.byId(questId) ?: return 0
        val rows = logDao.rowsForQuestOrdered(questId)
        val periods = rows.map { it.periodKey }.distinct()
        periods.forEach { periodKey ->
            val periodRows = rows.filter { it.periodKey == periodKey }
            val epochDay = periodRows.minOfOrNull { it.epochDay } ?: quest.createdAt / 86_400_000L
            instanceDao.insertIfAbsent(
                QuestInstanceEntity(
                    questId = questId,
                    periodKey = periodKey,
                    epochDay = epochDay,
                )
            )
            instanceDao.setOutcome(
                questId = questId,
                periodKey = periodKey,
                xp = periodRows.sumOf { it.xpEarned },
                status = if (periodRows.any { it.isCompletion }) {
                    QuestInstanceEntity.STATUS_COMPLETED
                } else {
                    QuestInstanceEntity.STATUS_OPEN
                },
            )
        }
        return periods.size
    }

    suspend fun stepsFor(questId: Long) = stepDao.forQuest(questId)
}
