package com.irlrpg.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.irlrpg.app.data.local.dao.AttributeDao
import com.irlrpg.app.data.local.dao.CategoryDao
import com.irlrpg.app.data.local.dao.PlayerDao
import com.irlrpg.app.data.local.dao.ProgressionDao
import com.irlrpg.app.data.local.dao.QuestDao
import com.irlrpg.app.data.local.dao.QuestLogDao
import com.irlrpg.app.data.local.entity.AttributeEntity
import com.irlrpg.app.data.local.entity.CategoryEntity
import com.irlrpg.app.data.local.entity.PlayerStatsEntity
import com.irlrpg.app.data.local.entity.ProgressionEventEntity
import com.irlrpg.app.data.local.entity.QuestEntity
import com.irlrpg.app.data.local.entity.QuestLogEntity

@Database(
    entities = [
        QuestEntity::class,
        CategoryEntity::class,
        QuestLogEntity::class,
        PlayerStatsEntity::class,
        AttributeEntity::class,
        ProgressionEventEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class IrlRpgDatabase : RoomDatabase() {

    abstract fun questDao(): QuestDao
    abstract fun categoryDao(): CategoryDao
    abstract fun questLogDao(): QuestLogDao
    abstract fun playerDao(): PlayerDao
    abstract fun attributeDao(): AttributeDao
    abstract fun progressionDao(): ProgressionDao

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
                    // Step 1 schema is v1; destructive fallback keeps developer
                    // builds from crashing if the schema evolves before release.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
