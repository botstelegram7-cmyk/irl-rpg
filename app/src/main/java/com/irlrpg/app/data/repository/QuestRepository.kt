package com.irlrpg.app.data.repository

import com.irlrpg.app.data.local.dao.CategoryDao
import com.irlrpg.app.data.local.dao.QuestDao
import com.irlrpg.app.data.local.dao.QuestLogDao
import com.irlrpg.app.data.local.dao.QuestProgressRow
import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.local.entity.QuestEntity
import com.irlrpg.app.data.local.entity.QuestLogEntity
import com.irlrpg.app.domain.model.QuestDraft
import kotlinx.coroutines.flow.Flow

/**
 * Single access point for quest definitions, categories and their activity
 * logs. The UI layer never talks to DAOs directly.
 */
class QuestRepository(
    private val questDao: QuestDao,
    private val categoryDao: CategoryDao,
    private val logDao: QuestLogDao,
) {

    fun observeQuests(): Flow<List<QuestEntity>> = questDao.observeAll()

    fun observeActiveQuests(): Flow<List<QuestEntity>> = questDao.observeActive()

    fun observeQuest(id: Long): Flow<QuestEntity?> = questDao.observeById(id)

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun observeAllLogs(): Flow<List<QuestLogEntity>> = logDao.observeAll()

    fun observeRecentLogs(limit: Int = 40): Flow<List<QuestLogEntity>> = logDao.observeRecent(limit)

    fun observeQuestLogs(questId: Long): Flow<List<QuestLogEntity>> = logDao.observeForQuest(questId)

    fun observeProgressForPeriods(periodKeys: List<String>): Flow<List<QuestProgressRow>> =
        logDao.observeProgressForPeriods(periodKeys)

    suspend fun quest(id: Long): QuestEntity? = questDao.byId(id)

    suspend fun category(id: Long): CategoryEntity? = categoryDao.byId(id)

    suspend fun categoriesOnce(): List<CategoryEntity> = categoryDao.all()

    suspend fun questCount(): Int = questDao.count()

    suspend fun createQuest(draft: QuestDraft): Long = questDao.insert(draft.toEntity())

    suspend fun updateQuest(entity: QuestEntity) = questDao.update(entity.copy(updatedAt = System.currentTimeMillis()))

    suspend fun setQuestActive(id: Long, active: Boolean) {
        questDao.byId(id)?.let { questDao.update(it.copy(isActive = active, updatedAt = System.currentTimeMillis())) }
    }

    suspend fun deleteQuest(id: Long) = questDao.deleteById(id)

    suspend fun createCategory(name: String, emoji: String, accentId: String): Long =
        categoryDao.insert(CategoryEntity(name = name.trim(), emoji = emoji, accentId = accentId))

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.update(category)

    suspend fun deleteCategory(id: Long) = categoryDao.deleteById(id)

    suspend fun deleteLog(logId: Long) = logDao.deleteById(logId)

    suspend fun resetQuestProgress(questId: Long) = logDao.deleteForQuest(questId)
}
