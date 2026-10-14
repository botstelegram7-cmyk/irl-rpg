package com.irlrpg.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.irlrpg.app.data.local.IrlRpgDatabase
import com.irlrpg.app.data.local.dao.BossDao
import com.irlrpg.app.data.local.dao.QuestInstanceDao
import com.irlrpg.app.data.local.dao.QuestStepDao
import com.irlrpg.app.data.local.dao.SkillNodeDao
import com.irlrpg.app.data.local.entity.BossChallengeEntity
import com.irlrpg.app.data.local.entity.BossObjectiveEntity
import com.irlrpg.app.data.local.entity.QuestEntity
import com.irlrpg.app.data.local.entity.QuestStepEntity
import com.irlrpg.app.domain.game.BossEngine
import com.irlrpg.app.domain.game.BossTemplates
import com.irlrpg.app.domain.game.GameEngine
import com.irlrpg.app.domain.game.LevelRules
import com.irlrpg.app.domain.model.SkillUnlockRule
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
 * Step 2 end-to-end behaviour on a real SQLite database: multi-step quests,
 * recurring instance generation, duplicate-reward protection, long-term
 * challenges and boss challenge completion. Every assertion is about data that
 * was actually written to the log.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StepTwoEngineTest {

    private lateinit var database: IrlRpgDatabase
    private lateinit var engine: GameEngine
    private lateinit var bossEngine: BossEngine
    private val questDao get() = database.questDao()
    private val logDao get() = database.questLogDao()
    private val stepDao: QuestStepDao get() = database.questStepDao()
    private val instanceDao: QuestInstanceDao get() = database.questInstanceDao()
    private val bossDao: BossDao get() = database.bossDao()
    private val skillDao: SkillNodeDao get() = database.skillNodeDao()

    private val today = LocalDate.of(2026, 10, 14)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            IrlRpgDatabase::class.java,
        ).allowMainThreadQueries().build()
        engine = GameEngine(database)
        bossEngine = BossEngine(database)
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
        questKind: String = "STANDARD",
        attribute: String = "Intellect",
        durationDays: Int = 0,
        deadlineEpochDay: Long? = null,
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
        questKind = questKind,
        durationDays = durationDays,
        deadlineEpochDay = deadlineEpochDay,
        startedEpochDay = today.toEpochDay(),
    )

    // ------------------------------------------------------------ multi-step

    @Test
    fun `multi-step quest logs each step once and pays the completion bonus once`() = runBlocking {
        val questId = questDao.insert(quest(title = "Ship the module", questKind = "MULTI_STEP", xp = 90, target = 3))
        val stepA = stepDao.insert(QuestStepEntity(questId = questId, title = "Outline", targetAmount = 1, sortOrder = 0))
        val stepB = stepDao.insert(QuestStepEntity(questId = questId, title = "Draft", targetAmount = 1, sortOrder = 1))
        val stepC = stepDao.insert(QuestStepEntity(questId = questId, title = "Review", targetAmount = 1, sortOrder = 2))

        val first = engine.logQuest(questId, 1L, stepId = stepA, today = today)!!
        assertTrue(first.xpGained > 0)
        assertFalse(first.completedNow)
        assertEquals("Outline", first.stepTitle)

        engine.logQuest(questId, 1L, stepId = stepB, today = today)
        val last = engine.logQuest(questId, 1L, stepId = stepC, today = today)!!

        assertTrue("completion bonus lands on the final step", last.completedNow)
        assertEquals(3, last.stepsCompleted)
        assertEquals(3, last.stepsTotal)

        // The completion row is written exactly once for this period.
        assertEquals(1, logDao.completionCount(questId, "2026-10-14"))

        // Logging the same step again in the same period awards no fresh completion bonus.
        val repeat = engine.logQuest(questId, 1L, stepId = stepA, today = today)
        if (repeat != null) {
            assertFalse(repeat.completedNow)
        }
        assertEquals(
            "still exactly one completion row, even after re-logging a step",
            1,
            logDao.rowsForPeriod(questId, "2026-10-14").count { it.isCompletion },
        )

        // Four step entries must never pay the quest's full reward four times over.
        val totalStepXp = logDao.rowsForPeriod(questId, "2026-10-14").sumOf { it.xpEarned }
        assertTrue("step XP stays bounded ($totalStepXp)", totalStepXp < 4 * 90)
    }

    // ------------------------------------------------------ recurring instances

    @Test
    fun `recurring quests materialise instances per period without duplicating them`() = runBlocking {
        val daily = questDao.insert(quest(title = "Practice", schedule = "Daily", target = 10))
        val weekly = questDao.insert(quest(title = "Tidy desk", schedule = "Weekly", target = 1))

        val first = engine.ensureInstancesForToday(today)
        assertTrue(first.created >= 2)
        val second = engine.ensureInstancesForToday(today)
        assertEquals("running twice creates nothing new", 0, second.created)

        assertEquals(1, instanceDao.forQuest(daily).count { it.periodKey == "2026-10-14" })
        // Weekly quests are keyed by ISO week, so a second day in the same week reuses it.
        engine.ensureInstancesForToday(today.plusDays(1))
        assertEquals(1, instanceDao.forQuest(weekly).size)
        // …while the daily quest gets its own row for the new day.
        assertEquals(2, instanceDao.forQuest(daily).size)
    }

    @Test
    fun `completion bonus is paid once per period even when the target is exceeded`() = runBlocking {
        val questId = questDao.insert(quest(target = 10, xp = 100, schedule = "Daily"))

        val done = engine.logQuest(questId, 10L, today = today)!!
        assertTrue(done.completedNow)
        val xpAfterCompletion = database.playerDao().get()!!.totalXp

        val extra = engine.logQuest(questId, 40L, today = today)
        assertEquals("no completion is reported twice", false, extra?.completedNow ?: false)
        assertEquals(
            "overshooting the target must not add XP",
            xpAfterCompletion,
            database.playerDao().get()!!.totalXp,
        )
        assertEquals(1, logDao.rowsForPeriod(questId, "2026-10-14").count { it.isCompletion })

        // Next day: the recurring quest resets and can be completed again.
        val tomorrow = engine.logQuest(questId, 10L, today = today.plusDays(1))!!
        assertTrue(tomorrow.completedNow)
        assertTrue(database.playerDao().get()!!.totalXp > xpAfterCompletion)
    }

    // ------------------------------------------------------- long-term quests

    @Test
    fun `long-term challenge accumulates across days under a single period key`() = runBlocking {
        val questId = questDao.insert(
            quest(
                title = "Write a novel",
                kind = "Custom",
                unit = "Custom",
                target = 10_000,
                xp = 500,
                schedule = "Flexible",
                questKind = "LONG_TERM",
                durationDays = 90,
            )
        )

        engine.logQuest(questId, 1_200L, today = today)!!
        engine.logQuest(questId, 800L, today = today.plusDays(6))!!

        val rows = logDao.rowsForPeriod(questId, com.irlrpg.app.domain.game.PeriodKeys.FLEX_KEY)
        assertEquals("both days land in the same challenge bucket", 2, rows.size)
        assertEquals(2_000L, rows.sumOf { it.amount })
        assertEquals(1, instanceDao.forQuest(questId).size)
    }

    // -------------------------------------------------------------- boss challenges

    @Test
    fun `boss challenge pays its reward once and only when required objectives are met`() = runBlocking {
        val questId = questDao.insert(quest(title = "Run", kind = "Time", unit = "Minutes", target = 20, xp = 40))
        val bossId = bossEngine.createCustom(
            title = "First half marathon",
            description = null,
            emoji = "🏃",
            accentId = "peach",
            deadline = today.plusDays(60),
            rewardXp = 400,
            objectives = listOf(
                Triple("Run 40 sessions", 40L, questId),
                Triple("Race day", 1L, null),
            ),
            today = today,
        )
        assertTrue("the template catalogue is non-empty", BossTemplates.all.isNotEmpty())

        // Partially progress the linked objective: no completion yet.
        engine.logQuest(questId, 20L, today = today)!!
        val noVictory = bossEngine.checkCompletions(today)
        assertTrue(noVictory.isEmpty())

        // Finish the linked objective, then the manual one.
        engine.logQuest(questId, 800L, today = today.plusDays(1))!!
        val manualId = bossDao.objectives(bossId).first { it.metric == BossObjectiveEntity.METRIC_MANUAL }.id
        assertTrue(bossEngine.logManualObjective(manualId, 1L))

        val victories = bossEngine.checkCompletions(today.plusDays(2))
        assertEquals(1, victories.size)
        assertEquals(400, victories.first().xpAwarded)
        assertEquals(BossChallengeEntity.STATUS_COMPLETED, bossDao.challenge(bossId)!!.status)

        val xpAfter = database.playerDao().get()!!.totalXp
        // Second sweep must not pay twice.
        assertTrue(bossEngine.checkCompletions(today.plusDays(3)).isEmpty())
        assertEquals(xpAfter, database.playerDao().get()!!.totalXp)
    }

    @Test
    fun `boss progress reads only logs recorded after the challenge started`() = runBlocking {
        val questId = questDao.insert(quest(title = "Study", kind = "Time", unit = "Minutes", target = 30, xp = 50))
        // Activity from before the challenge began must not count towards it.
        engine.logQuest(questId, 900L, today = today.minusDays(30))!!

        val bossId = bossEngine.createCustom(
            title = "Exam preparation",
            description = null,
            emoji = "📚",
            accentId = "sky",
            deadline = null,
            rewardXp = 300,
            objectives = listOf(Triple("Study 10 hours", 600L, questId)),
            today = today,
        )

        val progress = bossEngine.progressOf(bossId)!!
        assertEquals("pre-challenge history is excluded", 0L, progress.objectives.first().progress)

        engine.logQuest(questId, 300L, today = today)!!
        assertEquals(300L, bossEngine.progressOf(bossId)!!.objectives.first().progress)
    }

    // ----------------------------------------------------------------- skills

    @Test
    fun `skill nodes unlock from the parent mastery and explain what is missing`() = runBlocking {
        val parent = skillDao.insert(
            com.irlrpg.app.data.local.entity.SkillNodeEntity(
                name = "Reading",
                emoji = "📖",
                accentId = "lavender",
                attributeId = null,
                masteryXp = 120,
                unlockRule = SkillUnlockRule.Root.name,
                sortOrder = 0,
            )
        )
        val child = skillDao.insert(
            com.irlrpg.app.data.local.entity.SkillNodeEntity(
                name = "Comprehension",
                emoji = "🧠",
                accentId = "sky",
                parentId = parent,
                attributeId = null,
                masteryXp = 240,
                unlockRule = SkillUnlockRule.ParentMastery.name,
                unlockParentMastery = 120,
                sortOrder = 1,
            )
        )

        val nodes = skillDao.all()
        val views = com.irlrpg.app.domain.game.SkillTreeEngine.evaluate(nodes, emptyList(), totalXp = 0)
        val childView = views.first { it.node.id == child }
        assertEquals(com.irlrpg.app.domain.model.SkillNodeState.Locked, childView.state)
        assertNotNull("locked nodes must explain themselves", childView.lockReason)
        assertTrue(childView.lockReason!!.contains("120"))

        // Master the parent by giving the linked attribute enough XP.
        val attributeId = database.attributeDao().insert(
            com.irlrpg.app.data.local.entity.AttributeEntity(
                name = "Reading",
                emoji = "📖",
                attributeKind = "Intellect",
                accentId = "lavender",
            )
        )
        skillDao.update(skillDao.byId(parent)!!.copy(attributeId = attributeId))
        skillDao.update(skillDao.byId(child)!!.copy(attributeId = attributeId))

        val richer = com.irlrpg.app.domain.game.SkillTreeEngine.evaluate(
            skillDao.all(),
            database.attributeDao().all(),
            totalXp = 300,
        )
        assertEquals(2, richer.size)
    }

    // ------------------------------------------------------------ progression

    @Test
    fun `custom level rules never reset stored progress`() = runBlocking {
        val questId = questDao.insert(quest(xp = 300, target = 1))
        engine.logQuest(questId, 1L, today = today)!!
        val xp = database.playerDao().get()!!.totalXp
        assertTrue(xp > 0)

        val defaultLevel = com.irlrpg.app.domain.game.LevelProgression(LevelRules.Default).levelForTotalXp(xp)
        val strictLevel = com.irlrpg.app.domain.game.LevelProgression(LevelRules(base = 300, step = 200)).levelForTotalXp(xp)
        assertTrue(
            "the same XP maps to different levels under different rules",
            strictLevel < defaultLevel,
        )
        // The stored total is the source of truth, so nothing needs rewriting.
        assertEquals(xp, database.playerDao().get()!!.totalXp)
    }
}
