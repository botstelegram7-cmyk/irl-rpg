package com.irlrpg.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.irlrpg.app.data.local.DatabaseSeeder
import com.irlrpg.app.data.local.IrlRpgDatabase
import com.irlrpg.app.data.local.entity.QuestEntity
import com.irlrpg.app.data.prefs.SettingsStore
import com.irlrpg.app.domain.game.GameEngine
import com.irlrpg.app.domain.game.LevelCurve
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * End-to-end checks of the persistence + progression core on a real SQLite
 * database (Robolectric): XP calculation, duplicate-reward protection, undo,
 * reset and survival across an app restart.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GameEngineTest {

    private lateinit var database: IrlRpgDatabase
    private lateinit var engine: GameEngine
    private val today = LocalDate.of(2026, 10, 14)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            IrlRpgDatabase::class.java,
        ).allowMainThreadQueries().build()
        engine = GameEngine(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun quest(
        title: String = "Read pages",
        kind: String = "Count",
        unit: String = "Pages",
        target: Long = 25,
        xp: Int = 60,
        schedule: String = "Daily",
        attribute: String = "Intellect",
    ) = QuestEntity(
        title = title,
        emoji = "📘",
        categoryId = null,
        trackingKind = kind,
        unit = unit,
        customUnit = null,
        targetAmount = target,
        scheduleKind = schedule,
        scheduleDays = "",
        xpReward = xp,
        attributeKind = attribute,
    )

    @Test
    fun `progress logs award proportional xp and a single completion bonus`() = runBlocking {
        val questId = database.questDao().insert(quest())

        val first = engine.logQuest(questId, 15L, today = today)!!
        assertEquals(36, first.xpGained) // 60 * 15/25
        assertFalse(first.completedNow)

        val second = engine.logQuest(questId, 10L, today = today)!!
        assertEquals(24 + 30, second.xpGained) // final 24 + completion bonus 30
        assertTrue(second.completedNow)

        val stats = database.playerDao().get()!!
        assertEquals(90, stats.totalXp)
        assertEquals(1, database.questLogDao().completionCount(questId, "2026-10-14"))
    }

    @Test
    fun `logging after completion records progress but grants no xp`() = runBlocking {
        val questId = database.questDao().insert(quest(target = 10, xp = 50))
        engine.logQuest(questId, 10L, today = today)
        val xpAfterComplete = database.playerDao().get()!!.totalXp

        val extra = engine.logQuest(questId, 5L, today = today)!!
        assertEquals(0, extra.xpGained)
        assertTrue(extra.alreadyComplete)
        assertEquals(xpAfterComplete, database.playerDao().get()!!.totalXp)
        // The entry is still stored — it is real progress history.
        assertEquals(2, database.questLogDao().rowsForPeriod(questId, "2026-10-14").size)
    }

    @Test
    fun `milestone quests complete exactly once`() = runBlocking {
        val questId = database.questDao().insert(
            quest(title = "Finish the novel", kind = "Milestone", unit = "Sessions", target = 1, xp = 200)
        )
        val first = engine.logQuest(questId, 1L, today = today)!!
        assertTrue(first.completedNow)
        assertEquals(200 + 100, first.xpGained)

        val second = engine.logQuest(questId, 1L, today = today)!!
        assertEquals(0, second.xpGained)
        assertTrue(second.alreadyComplete)
    }

    @Test
    fun `daily targets reset on a new day`() = runBlocking {
        val questId = database.questDao().insert(quest(target = 10, xp = 40))
        val dayOne = engine.logQuest(questId, 10L, today = today)!!
        assertTrue(dayOne.completedNow)

        val dayTwo = engine.logQuest(questId, 10L, today = today.plusDays(1))!!
        assertTrue(dayTwo.completedNow)
        // Each day is its own period: one completion each, and XP for both.
        assertEquals(1, database.questLogDao().completionCount(questId, "2026-10-14"))
        assertEquals(1, database.questLogDao().completionCount(questId, "2026-10-15"))
        assertEquals(dayOne.xpGained, dayTwo.xpGained)
    }

    @Test
    fun `level ups are detected and recorded in history`() = runBlocking {
        val questId = database.questDao().insert(
            quest(title = "Long session", kind = "Time", unit = "Minutes", target = 120, xp = 120)
        )
        // 120 XP progress + 60 bonus = 180 XP, which crosses level 2 (150 XP).
        val result = engine.logQuest(questId, 120L, today = today)!!
        assertTrue(result.levelUp)
        assertEquals(2, result.newLevel)
        assertEquals(180, result.totalXp)

        val history = database.progressionDao().recent(10)
        assertTrue(history.any { it.type == "level_up" })
    }

    @Test
    fun `undo removes an entry and corrects player xp exactly`() = runBlocking {
        val questId = database.questDao().insert(quest(target = 25, xp = 60))
        engine.logQuest(questId, 15L, today = today)
        val second = engine.logQuest(questId, 10L, today = today)!!
        assertEquals(90, database.playerDao().get()!!.totalXp)

        val rows = database.questLogDao().rowsForPeriod(questId, "2026-10-14")
        assertTrue(engine.undoLog(rows.last().id))

        assertEquals(36, database.playerDao().get()!!.totalXp) // back to only the first entry
        val remaining = database.questLogDao().rowsForPeriod(questId, "2026-10-14")
        assertEquals(1, remaining.size)
        assertFalse(remaining.first().isCompletion)
        assertEquals(0, database.questLogDao().completionCount(questId, "2026-10-14"))
        assertTrue(second.xpGained > 0)
    }

    @Test
    fun `reset clears a quest and re-aligns xp totals`() = runBlocking {
        val questId = database.questDao().insert(quest())
        engine.logQuest(questId, 25L, today = today)
        assertTrue(database.playerDao().get()!!.totalXp > 0)

        val removed = engine.resetQuestProgress(questId)
        assertEquals(1, removed)
        assertEquals(0, database.playerDao().get()!!.totalXp)
        assertTrue(database.questLogDao().rowsForQuest(questId).isEmpty())
    }

    @Test
    fun `attribute totals follow the awarded xp`() = runBlocking {
        database.attributeDao().insert(
            com.irlrpg.app.data.local.entity.AttributeEntity(
                name = "Intellect",
                emoji = "🧠",
                attributeKind = "Intellect",
                accentId = "sky",
                isBuiltIn = true,
            )
        )
        val questId = database.questDao().insert(quest())
        engine.logQuest(questId, 25L, today = today)

        val attribute = database.attributeDao().byKind("Intellect")!!
        assertEquals(90, attribute.xpContributed)
    }

    @Test
    fun `progress survives an app restart`() {
        runBlocking {
            val context: Context = ApplicationProvider.getApplicationContext()
            val dbName = "persistence-test.db"
            context.deleteDatabase(dbName)

            var db = Room.databaseBuilder(context, IrlRpgDatabase::class.java, dbName)
                .allowMainThreadQueries().build()
            val questId = db.questDao().insert(quest())
            GameEngine(db).logQuest(questId, 25L, today = today)
            db.close()

            // Simulate a fresh app launch against the same database file.
            db = Room.databaseBuilder(context, IrlRpgDatabase::class.java, dbName)
                .allowMainThreadQueries().build()
            val stats = db.playerDao().get()
            assertNotNull(stats)
            assertEquals(90, stats!!.totalXp)
            assertEquals(1, db.questLogDao().count())
            assertEquals(1, db.questDao().count())
            db.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `seeder creates starter categories and attributes once`() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        // Isolated preferences file so the flag cannot be touched concurrently
        // by the real application process that Robolectric also launches.
        val settings = SettingsStore(context, "irl_rpg_settings_test")
        settings.isSeeded = false
        val seeder = DatabaseSeeder(database, settings)

        seeder.seedIfNeeded(today)
        assertEquals(6, database.categoryDao().count())
        assertEquals(6, database.attributeDao().count())
        assertNotNull(database.playerDao().get())
        assertEquals(0, database.playerDao().get()!!.totalXp)

        // Nothing is duplicated on the second run.
        seeder.seedIfNeeded(today)
        assertEquals(6, database.categoryDao().count())
    }

    @Test
    fun `quests keep their category when a category is deleted`() = runBlocking {
        val categoryId = database.categoryDao().insert(
            com.irlrpg.app.data.local.entity.CategoryEntity(name = "Reading", emoji = "📚", accentId = "sky")
        )
        val questId = database.questDao().insert(quest().copy(categoryId = categoryId))
        database.categoryDao().deleteById(categoryId)

        val stored = database.questDao().byId(questId)
        assertNotNull(stored)
        assertEquals(null, stored!!.categoryId)
    }
}
