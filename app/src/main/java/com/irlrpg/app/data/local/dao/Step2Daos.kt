package com.irlrpg.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.irlrpg.app.data.local.entity.AchievementEarnedEntity
import com.irlrpg.app.data.local.entity.BossChallengeEntity
import com.irlrpg.app.data.local.entity.BossObjectiveEntity
import com.irlrpg.app.data.local.entity.QuestInstanceEntity
import com.irlrpg.app.data.local.entity.QuestStepEntity
import com.irlrpg.app.data.local.entity.SkillNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestStepDao {

    @Query("SELECT * FROM quest_steps WHERE questId = :questId ORDER BY sortOrder ASC, id ASC")
    fun observeForQuest(questId: Long): Flow<List<QuestStepEntity>>

    @Query("SELECT * FROM quest_steps WHERE questId = :questId ORDER BY sortOrder ASC, id ASC")
    suspend fun forQuest(questId: Long): List<QuestStepEntity>

    @Query("SELECT * FROM quest_steps ORDER BY questId ASC, sortOrder ASC")
    fun observeAll(): Flow<List<QuestStepEntity>>

    @Query("SELECT * FROM quest_steps ORDER BY questId ASC, sortOrder ASC")
    suspend fun all(): List<QuestStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(steps: List<QuestStepEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: QuestStepEntity): Long

    @Update
    suspend fun update(step: QuestStepEntity)

    @Query("DELETE FROM quest_steps WHERE questId = :questId")
    suspend fun deleteForQuest(questId: Long)

    @Query("DELETE FROM quest_steps WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM quest_steps")
    suspend fun count(): Int
}

@Dao
interface QuestInstanceDao {

    /**
     * Idempotent creation: the unique `(questId, periodKey)` index means a second
     * attempt for the same period is silently ignored, so a recurring quest can
     * never generate duplicate instances (and therefore never double-award XP).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(instance: QuestInstanceEntity): Long

    @Query("SELECT * FROM quest_instances WHERE questId = :questId AND periodKey = :periodKey")
    suspend fun find(questId: Long, periodKey: String): QuestInstanceEntity?

    @Query("SELECT * FROM quest_instances WHERE questId = :questId ORDER BY epochDay DESC, id DESC")
    fun observeForQuest(questId: Long): Flow<List<QuestInstanceEntity>>

    @Query("SELECT * FROM quest_instances ORDER BY epochDay DESC, id DESC")
    fun observeAll(): Flow<List<QuestInstanceEntity>>

    @Query("SELECT * FROM quest_instances WHERE questId = :questId ORDER BY epochDay DESC")
    suspend fun forQuest(questId: Long): List<QuestInstanceEntity>

    @Query(
        "UPDATE quest_instances SET xpEarned = :xp, status = :status " +
            "WHERE questId = :questId AND periodKey = :periodKey"
    )
    suspend fun setOutcome(questId: Long, periodKey: String, xp: Int, status: String)

    @Query("SELECT COUNT(*) FROM quest_instances")
    suspend fun count(): Int
}

@Dao
interface SkillNodeDao {

    @Query("SELECT * FROM skill_nodes ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<SkillNodeEntity>>

    @Query("SELECT * FROM skill_nodes ORDER BY sortOrder ASC, id ASC")
    suspend fun all(): List<SkillNodeEntity>

    @Query("SELECT * FROM skill_nodes WHERE id = :id")
    suspend fun byId(id: Long): SkillNodeEntity?

    @Query("SELECT COUNT(*) FROM skill_nodes")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM skill_nodes WHERE parentId = :parentId")
    suspend fun childCount(parentId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(node: SkillNodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(nodes: List<SkillNodeEntity>)

    @Update
    suspend fun update(node: SkillNodeEntity)

    /** Detaches children instead of deleting them — an edit never loses a branch. */
    @Query("UPDATE skill_nodes SET parentId = NULL WHERE parentId = :parentId")
    suspend fun detachChildren(parentId: Long)

    @Query("DELETE FROM skill_nodes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements_earned ORDER BY epochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<AchievementEarnedEntity>>

    @Query("SELECT * FROM achievements_earned ORDER BY epochDay DESC, createdAt DESC")
    suspend fun all(): List<AchievementEarnedEntity>

    @Query("SELECT * FROM achievements_earned WHERE code = :code")
    suspend fun byCode(code: String): AchievementEarnedEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(earned: AchievementEarnedEntity): Long

    @Query("SELECT COUNT(*) FROM achievements_earned")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM achievements_earned WHERE category = :category")
    suspend fun countInCategory(category: String): Int
}

@Dao
interface BossDao {

    @Query("SELECT * FROM boss_challenges ORDER BY status ASC, createdAt DESC")
    fun observeChallenges(): Flow<List<BossChallengeEntity>>

    @Query("SELECT * FROM boss_challenges ORDER BY status ASC, createdAt DESC")
    suspend fun challenges(): List<BossChallengeEntity>

    @Query("SELECT * FROM boss_challenges WHERE id = :id")
    fun observeChallenge(id: Long): Flow<BossChallengeEntity?>

    @Query("SELECT * FROM boss_challenges WHERE id = :id")
    suspend fun challenge(id: Long): BossChallengeEntity?

    @Query("SELECT COUNT(*) FROM boss_challenges")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM boss_challenges WHERE status = 'COMPLETED'")
    suspend fun completedCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: BossChallengeEntity): Long

    @Update
    suspend fun updateChallenge(challenge: BossChallengeEntity)

    @Query("DELETE FROM boss_challenges WHERE id = :id")
    suspend fun deleteChallenge(id: Long)

    @Query("SELECT * FROM boss_objectives WHERE bossId = :bossId ORDER BY sortOrder ASC, id ASC")
    fun observeObjectives(bossId: Long): Flow<List<BossObjectiveEntity>>

    @Query("SELECT * FROM boss_objectives WHERE bossId = :bossId ORDER BY sortOrder ASC, id ASC")
    suspend fun objectives(bossId: Long): List<BossObjectiveEntity>

    @Query("SELECT * FROM boss_objectives ORDER BY bossId ASC, sortOrder ASC")
    fun observeAllObjectives(): Flow<List<BossObjectiveEntity>>

    @Query("SELECT * FROM boss_objectives ORDER BY bossId ASC, sortOrder ASC")
    suspend fun allObjectives(): List<BossObjectiveEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObjectives(objectives: List<BossObjectiveEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObjective(objective: BossObjectiveEntity): Long

    @Update
    suspend fun updateObjective(objective: BossObjectiveEntity)

    @Query("DELETE FROM boss_objectives WHERE id = :id")
    suspend fun deleteObjective(id: Long)
}
