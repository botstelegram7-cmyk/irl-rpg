package com.irlrpg.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.irlrpg.app.data.local.entity.AttributeEntity
import com.irlrpg.app.data.local.entity.PlayerStatsEntity
import com.irlrpg.app.data.local.entity.ProgressionEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Query("SELECT * FROM player_stats WHERE id = 1")
    fun observe(): Flow<PlayerStatsEntity?>

    @Query("SELECT * FROM player_stats WHERE id = 1")
    suspend fun get(): PlayerStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: PlayerStatsEntity)

    @Query("UPDATE player_stats SET totalXp = :totalXp, lastQuestAt = :lastQuestAt WHERE id = 1")
    suspend fun setXp(totalXp: Int, lastQuestAt: Long?)

    @Query("UPDATE player_stats SET name = :name WHERE id = 1")
    suspend fun setName(name: String)

    @Query("UPDATE player_stats SET displayTitle = :code WHERE id = 1")
    suspend fun setDisplayTitle(code: String?)

    @Query("UPDATE player_stats SET cosmeticFrame = :code WHERE id = 1")
    suspend fun setCosmeticFrame(code: String)
}

@Dao
interface AttributeDao {

    @Query("SELECT * FROM attributes ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<AttributeEntity>>

    @Query("SELECT * FROM attributes ORDER BY sortOrder ASC, id ASC")
    suspend fun all(): List<AttributeEntity>

    @Query("SELECT * FROM attributes WHERE attributeKind = :kind LIMIT 1")
    suspend fun byKind(kind: String): AttributeEntity?

    /** Matches a quest's stored attribute value against built-in kinds and custom names. */
    @Query("SELECT * FROM attributes WHERE attributeKind = :value OR name = :value LIMIT 1")
    suspend fun byKindOrName(value: String): AttributeEntity?

    @Query("SELECT COUNT(*) FROM attributes")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attribute: AttributeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attributes: List<AttributeEntity>)

    @Update
    suspend fun update(attribute: AttributeEntity)

    @Query("UPDATE attributes SET xpContributed = xpContributed + :amount WHERE id = :id")
    suspend fun addXp(id: Long, amount: Int)

    @Query("UPDATE attributes SET xpContributed = MAX(0, xpContributed + :delta) WHERE id = :id")
    suspend fun adjustXp(id: Long, delta: Int)

    @Query("DELETE FROM attributes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ProgressionDao {

    @Query("SELECT * FROM progression_history ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ProgressionEventEntity>>

    @Query("SELECT * FROM progression_history ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ProgressionEventEntity>

    @Insert
    suspend fun insert(event: ProgressionEventEntity): Long

    @Query("DELETE FROM progression_history WHERE refLogId = :logId")
    suspend fun deleteByLogId(logId: Long)

    /** Must run *before* the referenced log rows are deleted. */
    @Query(
        "DELETE FROM progression_history WHERE refLogId IN (SELECT id FROM quest_logs WHERE questId = :questId)"
    )
    suspend fun deleteByQuestLogs(questId: Long)

    @Query("SELECT COUNT(*) FROM progression_history")
    suspend fun count(): Int
}
