package com.irlrpg.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.irlrpg.app.data.local.dao.AchievementDao
import com.irlrpg.app.data.local.dao.AttributeDao
import com.irlrpg.app.data.local.dao.BossDao
import com.irlrpg.app.data.local.dao.CategoryDao
import com.irlrpg.app.data.local.dao.PlayerDao
import com.irlrpg.app.data.local.dao.ProgressionDao
import com.irlrpg.app.data.local.dao.QuestDao
import com.irlrpg.app.data.local.dao.QuestInstanceDao
import com.irlrpg.app.data.local.dao.QuestLogDao
import com.irlrpg.app.data.local.dao.QuestStepDao
import com.irlrpg.app.data.local.dao.SkillNodeDao
import com.irlrpg.app.data.local.entity.AchievementEarnedEntity
import com.irlrpg.app.data.local.entity.AttributeEntity
import com.irlrpg.app.data.local.entity.BossChallengeEntity
import com.irlrpg.app.data.local.entity.BossObjectiveEntity
import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.local.entity.PlayerStatsEntity
import com.irlrpg.app.data.local.entity.ProgressionEventEntity
import com.irlrpg.app.data.local.entity.QuestEntity
import com.irlrpg.app.data.local.entity.QuestInstanceEntity
import com.irlrpg.app.data.local.entity.QuestLogEntity
import com.irlrpg.app.data.local.entity.QuestStepEntity
import com.irlrpg.app.data.local.entity.SkillNodeEntity

@Database(
    entities = [
        QuestEntity::class,
        CategoryEntity::class,
        QuestLogEntity::class,
        PlayerStatsEntity::class,
        AttributeEntity::class,
        ProgressionEventEntity::class,
        // ---- Step 2 tables ----
        QuestStepEntity::class,
        QuestInstanceEntity::class,
        SkillNodeEntity::class,
        AchievementEarnedEntity::class,
        BossChallengeEntity::class,
        BossObjectiveEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class IrlRpgDatabase : RoomDatabase() {

    abstract fun questDao(): QuestDao
    abstract fun categoryDao(): CategoryDao
    abstract fun questLogDao(): QuestLogDao
    abstract fun playerDao(): PlayerDao
    abstract fun attributeDao(): AttributeDao
    abstract fun progressionDao(): ProgressionDao

    // ---- Step 2 ----
    abstract fun questStepDao(): QuestStepDao
    abstract fun questInstanceDao(): QuestInstanceDao
    abstract fun skillNodeDao(): SkillNodeDao
    abstract fun achievementDao(): AchievementDao
    abstract fun bossDao(): BossDao

    companion object {
        private const val DB_NAME = "irl_rpg.db"

        @Volatile
        private var instance: IrlRpgDatabase? = null

        fun get(context: Context): IrlRpgDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    IrlRpgDatabase::class.java,
                    DB_NAME,
                )
                    // Explicit, additive migrations only. A user's XP, logs and
                    // attributes are never discarded: every step is an ALTER or a
                    // CREATE, verified by a migration test that boots a Step 1
                    // database and checks the data survived.
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                    .also { instance = it }
            }
    }
}
