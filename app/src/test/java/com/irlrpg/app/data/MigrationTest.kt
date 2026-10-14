package com.irlrpg.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.test.core.app.ApplicationProvider
import com.irlrpg.app.data.local.ALL_MIGRATIONS
import com.irlrpg.app.data.local.IrlRpgDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/**
 * Step 1 → Step 2 migration test.
 *
 * A real v1 database (Step 1 schema) is written to disk, populated with the kind
 * of rows a Step 1 user has — XP, logs, attributes, categories — and then opened
 * by the real v2 database with [ALL_MIGRATIONS]. The test asserts that every Step 1
 * value survives untouched and the new Step 2 columns get their documented
 * defaults, i.e. installing Step 2 cannot reset anyone's progress.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MigrationTest {

    // ------------------------------------------------------------- v1 schema

    @Entity(tableName = "player_stats")
    data class V1PlayerStats(
        @PrimaryKey val id: Int = 1,
        val name: String,
        val totalXp: Int,
        val createdAtEpochDay: Long,
        val lastQuestAt: Long? = null,
    )

    @Entity(tableName = "categories", indices = [Index("name")])
    data class V1Category(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val name: String,
        val emoji: String,
        val accentId: String = "lavender",
        val isBuiltIn: Boolean = false,
        val createdAt: Long = 0,
    )

    @Entity(
        tableName = "quests",
        foreignKeys = [
            ForeignKey(
                entity = V1Category::class,
                parentColumns = ["id"],
                childColumns = ["categoryId"],
                onDelete = ForeignKey.SET_NULL,
            ),
        ],
        indices = [Index("categoryId"), Index("isActive")],
    )
    data class V1Quest(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val title: String,
        val emoji: String = "⭐",
        val categoryId: Long? = null,
        val trackingKind: String,
        val unit: String,
        val customUnit: String? = null,
        val targetAmount: Long,
        val scheduleKind: String,
        val scheduleDays: String = "",
        val xpReward: Int,
        val attributeKind: String,
        val isActive: Boolean = true,
        val notes: String? = null,
        val createdAt: Long = 0,
        val updatedAt: Long = 0,
    )

    @Entity(
        tableName = "quest_logs",
        foreignKeys = [
            ForeignKey(
                entity = V1Quest::class,
                parentColumns = ["id"],
                childColumns = ["questId"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
        indices = [Index("questId"), Index("epochDay"), Index("periodKey"), Index("questId", "periodKey")],
    )
    data class V1Log(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val questId: Long,
        val amount: Long,
        val unitLabel: String,
        val xpEarned: Int,
        val isCompletion: Boolean = false,
        val periodKey: String,
        val epochDay: Long,
        val note: String? = null,
        val createdAt: Long = 0,
    )

    @Entity(tableName = "attributes", indices = [Index("attributeKind")])
    data class V1Attribute(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val name: String,
        val emoji: String,
        val attributeKind: String? = null,
        val accentId: String = "lavender",
        val xpContributed: Int = 0,
        val isBuiltIn: Boolean = false,
        val sortOrder: Int = 0,
    )

    @Entity(tableName = "progression_history", indices = [Index("epochDay")])
    data class V1Progression(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val type: String,
        val title: String,
        val detail: String? = null,
        val amount: Int = 0,
        val level: Int? = null,
        val epochDay: Long,
        val refLogId: Long? = null,
        val createdAt: Long = 0,
    )

    @Database(
        entities = [
            V1PlayerStats::class,
            V1Category::class,
            V1Quest::class,
            V1Log::class,
            V1Attribute::class,
            V1Progression::class,
        ],
        version = 1,
        exportSchema = false,
    )
    abstract class V1Database : RoomDatabase()

    private lateinit var context: Context
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = File(context.cacheDir, "migration-test-${System.nanoTime()}.db")
    }

    @After
    fun tearDown() {
        dbFile.delete()
    }

    @Test
    fun `step 1 data survives the migration to version 2 with step 2 defaults`() = runBlocking {
        val today = LocalDate.of(2026, 10, 14)

        // ---- seed a v1 database exactly as Step 1 would ----
        val legacy = Room.databaseBuilder(context, V1Database::class.java, dbFile.absolutePath).build()
        val sql = legacy.openHelper.writableDatabase
        sql.execSQL(
            "INSERT INTO player_stats (id, name, totalXp, createdAtEpochDay, lastQuestAt) VALUES (1, 'Rae', 1840, ?, NULL)",
            arrayOf(today.toEpochDay() - 60),
        )
        sql.execSQL("INSERT INTO categories (id, name, emoji, accentId, isBuiltIn, createdAt) VALUES (1, 'Study', '📚', 'sky', 1, 0)")
        sql.execSQL(
            "INSERT INTO quests (id, title, emoji, categoryId, trackingKind, unit, targetAmount, " +
                "scheduleKind, scheduleDays, xpReward, attributeKind, isActive, createdAt, updatedAt) " +
                "VALUES (1, 'Read pages', '📘', 1, 'Count', 'Pages', 25, 'Daily', '', 60, 'Intellect', 1, ?, 0)",
            arrayOf(today.toEpochDay() * 86_400_000L),
        )
        sql.execSQL(
            "INSERT INTO quest_logs (id, questId, amount, unitLabel, xpEarned, isCompletion, periodKey, epochDay, createdAt) " +
                "VALUES (1, 1, 25, 'pages', 90, 1, '2026-10-13', ?, 0)",
            arrayOf(today.toEpochDay() - 1),
        )
        sql.execSQL("INSERT INTO attributes (id, name, emoji, attributeKind, accentId, xpContributed, isBuiltIn, sortOrder) " +
            "VALUES (1, 'Intellect', '🧠', 'Intellect', 'sky', 1840, 1, 0)")
        sql.execSQL(
            "INSERT INTO progression_history (id, type, title, detail, amount, level, epochDay, refLogId, createdAt) " +
                "VALUES (1, 'level_up', 'Level 12', NULL, 0, 12, ?, NULL, 0)",
            arrayOf(today.toEpochDay() - 5),
        )
        legacy.close()

        // ---- open the very same file with the real v2 database ----
        val upgraded = Room.databaseBuilder(context, IrlRpgDatabase::class.java, dbFile.absolutePath)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        val player = upgraded.playerDao().get()
        assertNotNull(player)
        assertEquals("XP is carried over untouched", 1840, player!!.totalXp)
        assertEquals("Rae", player.name)
        assertEquals("new cosmetic column keeps its default", "frame_ivory", player.cosmeticFrame)
        assertEquals("no title is granted retroactively", null, player.displayTitle)

        val quest = upgraded.questDao().byId(1L)
        assertNotNull(quest)
        assertEquals("Read pages", quest!!.title)
        assertEquals("Step 1 quests keep Step 1 behaviour", "MEDIUM", quest.difficulty)
        assertEquals("STANDARD", quest.questKind)
        assertEquals("no deadline is invented", null, quest.deadlineEpochDay)
        assertEquals(0, quest.durationDays)
        assertTrue("the start day is backfilled from createdAt", quest.startedEpochDay > 0)

        val log = upgraded.questLogDao().all().first()
        assertEquals(1, upgraded.questLogDao().all().size)
        assertEquals(90, log.xpEarned)
        assertEquals("Step 1 logs belong to no step", null, log.stepId)

        assertEquals(1, upgraded.attributeDao().all().size)
        assertEquals(1, upgraded.categoryDao().all().size)
        assertEquals(1, upgraded.progressionDao().recent(10).size)

        // ---- Step 2 tables start empty: no fake achievements, no fake bosses ----
        assertEquals(0, upgraded.achievementDao().count())
        assertEquals(0, upgraded.bossDao().count())
        assertEquals(0, upgraded.skillNodeDao().count())
        assertEquals(0, upgraded.questStepDao().count())
        assertEquals(0, upgraded.questInstanceDao().count())

        upgraded.close()
    }
}
