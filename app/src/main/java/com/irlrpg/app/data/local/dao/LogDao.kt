package com.irlrpg.app.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.irlrpg.app.data.local.entity.QuestLogEntity
import kotlinx.coroutines.flow.Flow

/** Aggregated progress for one quest inside one period. */
data class QuestProgressRow(
    @ColumnInfo(name = "questId") val questId: Long,
    @ColumnInfo(name = "totalAmount") val totalAmount: Long,
    @ColumnInfo(name = "entries") val entries: Int,
    @ColumnInfo(name = "completions") val completions: Int,
    @ColumnInfo(name = "xpEarned") val xpEarned: Int,
)

/**
 * Append-only activity log. All analytics are derived from these rows, so the
 * dashboard can never show a number that was not actually recorded.
 */
@Dao
interface QuestLogDao {

    @Insert
    suspend fun insert(log: QuestLogEntity): Long

    @Query("SELECT * FROM quest_logs ORDER BY epochDay DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<QuestLogEntity>>

    @Query("SELECT * FROM quest_logs ORDER BY epochDay ASC, id ASC")
    fun observeAll(): Flow<List<QuestLogEntity>>

    @Query("SELECT * FROM quest_logs ORDER BY epochDay ASC, id ASC")
    suspend fun all(): List<QuestLogEntity>

    @Query("SELECT * FROM quest_logs WHERE questId = :questId ORDER BY epochDay DESC, id DESC")
    fun observeForQuest(questId: Long): Flow<List<QuestLogEntity>>

    @Query("SELECT * FROM quest_logs WHERE questId = :questId ORDER BY epochDay DESC, id DESC")
    suspend fun rowsForQuestOrdered(questId: Long): List<QuestLogEntity>

    @Query("SELECT * FROM quest_logs WHERE questId = :questId AND periodKey = :periodKey AND stepId = :stepId")
    suspend fun rowsForStep(questId: Long, periodKey: String, stepId: Long): List<QuestLogEntity>

    /** Newest completions across every quest — powers the global history screen. */
    @Query("SELECT * FROM quest_logs WHERE isCompletion = 1 ORDER BY epochDay DESC, id DESC LIMIT :limit")
    fun observeCompletions(limit: Int): Flow<List<QuestLogEntity>>

    @Query("SELECT * FROM quest_logs WHERE isCompletion = 1 ORDER BY epochDay DESC, id DESC LIMIT :limit")
    suspend fun completions(limit: Int): List<QuestLogEntity>

    @Query("SELECT COUNT(*) FROM quest_logs WHERE questId = :questId AND isCompletion = 1")
    suspend fun completionCountForQuest(questId: Long): Int

    @Query(
        """
        SELECT questId AS questId,
               SUM(amount) AS totalAmount,
               COUNT(*) AS entries,
               SUM(CASE WHEN isCompletion = 1 THEN 1 ELSE 0 END) AS completions,
               SUM(xpEarned) AS xpEarned
        FROM quest_logs
        WHERE periodKey IN (:periodKeys)
        GROUP BY questId
        """
    )
    fun observeProgressForPeriods(periodKeys: List<String>): Flow<List<QuestProgressRow>>

    @Query(
        """
        SELECT questId AS questId,
               SUM(amount) AS totalAmount,
               COUNT(*) AS entries,
               SUM(CASE WHEN isCompletion = 1 THEN 1 ELSE 0 END) AS completions,
               SUM(xpEarned) AS xpEarned
        FROM quest_logs
        WHERE questId = :questId AND periodKey = :periodKey
        GROUP BY questId
        """
    )
    suspend fun progressForQuestPeriod(questId: Long, periodKey: String): QuestProgressRow?

    @Query("SELECT COUNT(*) FROM quest_logs WHERE questId = :questId AND periodKey = :periodKey AND isCompletion = 1")
    suspend fun completionCount(questId: Long, periodKey: String): Int

    @Query("SELECT * FROM quest_logs WHERE questId = :questId AND periodKey = :periodKey ORDER BY id ASC")
    suspend fun rowsForPeriod(questId: Long, periodKey: String): List<QuestLogEntity>

    @Query("SELECT * FROM quest_logs WHERE questId = :questId ORDER BY id ASC")
    suspend fun rowsForQuest(questId: Long): List<QuestLogEntity>

    @Update
    suspend fun updateRows(rows: List<QuestLogEntity>)

    @Query("SELECT COUNT(*) FROM quest_logs")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM quest_logs")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM quest_logs WHERE id = :logId")
    suspend fun deleteById(logId: Long)

    /** Used by the editor's "reset progress" action. */
    @Query("DELETE FROM quest_logs WHERE questId = :questId")
    suspend fun deleteForQuest(questId: Long)
}
