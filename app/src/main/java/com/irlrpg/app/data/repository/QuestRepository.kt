package com.irlrpg.app.data.repository

import androidx.room.withTransaction
import com.irlrpg.app.data.local.IrlRpgDatabase
import com.irlrpg.app.data.local.dao.CategoryDao
import com.irlrpg.app.data.local.dao.QuestDao
import com.irlrpg.app.data.local.dao.QuestInstanceDao
import com.irlrpg.app.data.local.dao.QuestLogDao
import com.irlrpg.app.data.local.dao.QuestStepDao
import com.irlrpg.app.data.local.dao.QuestProgressRow
import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.local.entity.QuestEntity
import com.irlrpg.app.data.local.entity.QuestInstanceEntity
import com.irlrpg.app.data.local.entity.QuestLogEntity
import com.irlrpg.app.data.local.entity.QuestStepEntity
import com.irlrpg.app.domain.model.QuestDraft
import com.irlrpg.app.domain.model.QuestStepDraft
import com.irlrpg.app.domain.model.QuestKind
import kotlinx.coroutines.flow.Flow

/**
 * Single access point for quest definitions, their steps, recurring instances,
 * categories and activity logs. The UI layer never talks to DAOs directly.
 */
class QuestRepository(
    private val database: IrlRpgDatabase,
    private val questDao: QuestDao,
    private val categoryDao: CategoryDao,
    private val logDao: QuestLogDao,
    private val stepDao: QuestStepDao,
    private val instanceDao: QuestInstanceDao,
) {

    // -------------------------------------------------------------- read paths

    fun observeQuests(): Flow<List<QuestEntity>> = questDao.observeAll()

    fun observeActiveQuests(): Flow<List<QuestEntity>> = questDao.observeActive()

    fun observeQuest(id: Long): Flow<QuestEntity?> = questDao.observeById(id)

    fun observeSteps(questId: Long): Flow<List<QuestStepEntity>> = stepDao.observeForQuest(questId)

    fun observeAllSteps(): Flow<List<QuestStepEntity>> = stepDao.observeAll()

    fun observeInstances(questId: Long): Flow<List<QuestInstanceEntity>> = instanceDao.observeForQuest(questId)

    fun observeAllInstances(): Flow<List<QuestInstanceEntity>> = instanceDao.observeAll()

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun observeAllLogs(): Flow<List<QuestLogEntity>> = logDao.observeAll()

    fun observeRecentLogs(limit: Int = 40): Flow<List<QuestLogEntity>> = logDao.observeRecent(limit)

    fun observeQuestLogs(questId: Long): Flow<List<QuestLogEntity>> = logDao.observeForQuest(questId)

    /** Global completion history (newest first). */
    fun observeCompletionHistory(limit: Int = 200): Flow<List<QuestLogEntity>> =
        logDao.observeCompletions(limit)

    fun observeProgressForPeriods(periodKeys: List<String>): Flow<List<QuestProgressRow>> =
        logDao.observeProgressForPeriods(periodKeys)

    suspend fun quest(id: Long): QuestEntity? = questDao.byId(id)

    suspend fun questsOnce(): List<QuestEntity> = questDao.all()

    suspend fun stepsOnce(questId: Long): List<QuestStepEntity> = stepDao.forQuest(questId)

    suspend fun instancesOnce(questId: Long): List<QuestInstanceEntity> = instanceDao.forQuest(questId)

    suspend fun category(id: Long): CategoryEntity? = categoryDao.byId(id)

    suspend fun categoriesOnce(): List<CategoryEntity> = categoryDao.all()

    suspend fun questCount(): Int = questDao.count()

    suspend fun completionCount(questId: Long): Int = logDao.completionCountForQuest(questId)

    // ------------------------------------------------------------- write paths

    suspend fun createQuest(draft: QuestDraft, steps: List<QuestStepDraft> = draft.steps): Long =
        database.withTransaction {
            val id = questDao.insert(draft.toEntity())
            steps.filter { it.title.isNotBlank() }.forEachIndexed { index, step ->
                stepDao.insert(
                    QuestStepEntity(
                        questId = id,
                        title = step.title.trim(),
                        targetAmount = step.targetAmount.coerceAtLeast(1L),
                        isRequired = step.isRequired,
                        sortOrder = index,
                    )
                )
            }
            id
        }

    /** Replaces the step list of a quest (progress history stays untouched). */
    suspend fun replaceSteps(questId: Long, steps: List<QuestStepDraft>) = database.withTransaction {
        stepDao.deleteForQuest(questId)
        steps.filter { it.title.isNotBlank() }.forEachIndexed { index, step ->
            stepDao.insert(
                QuestStepEntity(
                    questId = questId,
                    title = step.title.trim(),
                    targetAmount = step.targetAmount.coerceAtLeast(1L),
                    isRequired = step.isRequired,
                    sortOrder = index,
                )
            )
        }
    }

    suspend fun updateQuest(entity: QuestEntity, steps: List<QuestStepDraft>? = null) = database.withTransaction {
        questDao.update(entity.copy(updatedAt = System.currentTimeMillis()))
        if (steps != null) replaceSteps(entity.id, steps)
    }

    suspend fun setQuestActive(id: Long, active: Boolean) {
        questDao.byId(id)?.let { questDao.update(it.copy(isActive = active, updatedAt = System.currentTimeMillis())) }
    }

    /**
     * Deleting a quest removes its logs (Step 1 behaviour) plus its steps and
     * instances via foreign key cascades.
     */
    suspend fun deleteQuest(id: Long) = database.withTransaction {
        instanceDao.setOutcome(id, "", 0, QuestInstanceEntity.STATUS_OPEN)
        questDao.deleteById(id)
    }

    suspend fun createCategory(name: String, emoji: String, accentId: String): Long =
        categoryDao.insert(CategoryEntity(name = name.trim(), emoji = emoji, accentId = accentId))

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.update(category)

    suspend fun deleteCategory(id: Long) = categoryDao.deleteById(id)

    suspend fun deleteLog(logId: Long) = logDao.deleteById(logId)

    suspend fun resetQuestProgress(questId: Long) = logDao.deleteForQuest(questId)

    /** Steps that still need work in the current period, for the dashboard card. */
    suspend fun openSteps(questId: Long, periodKey: String): List<QuestStepEntity> {
        val steps = stepDao.forQuest(questId)
        return steps.filter { step ->
            logDao.rowsForStep(questId, periodKey, step.id).sumOf { it.amount } < step.targetAmount
        }
    }

    fun isMultiStep(quest: QuestEntity): Boolean =
        QuestKind.fromName(quest.questKind) == QuestKind.MultiStep
}
