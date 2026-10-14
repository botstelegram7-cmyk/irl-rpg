package com.irlrpg.app.di

import android.content.Context
import com.irlrpg.app.core.TodaySource
import com.irlrpg.app.data.local.DatabaseSeeder
import com.irlrpg.app.data.local.IrlRpgDatabase
import com.irlrpg.app.data.prefs.SettingsStore
import com.irlrpg.app.data.repository.AchievementRepository
import com.irlrpg.app.data.repository.AnalyticsRepository
import com.irlrpg.app.data.repository.BossRepository
import com.irlrpg.app.data.repository.DashboardRepository
import com.irlrpg.app.data.repository.HistoryRepository
import com.irlrpg.app.data.repository.InstanceMaintenance
import com.irlrpg.app.data.repository.PlayerRepository
import com.irlrpg.app.data.repository.ProgressSync
import com.irlrpg.app.data.repository.QuestRepository
import com.irlrpg.app.data.repository.SkillTreeRepository
import com.irlrpg.app.domain.game.BossEngine
import com.irlrpg.app.domain.game.GameEngine
import com.irlrpg.app.domain.game.LevelRules

/**
 * Hand-rolled dependency container: one place where the object graph is built.
 * No annotation processing, no reflection — cheap to start on budget devices.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val database: IrlRpgDatabase = IrlRpgDatabase.get(appContext)

    val settings: SettingsStore = SettingsStore(appContext)

    val todaySource: TodaySource = TodaySource()

    /** Level thresholds live in settings; every engine reads them reactively. */
    private val levelRules: () -> LevelRules = { settings.levelRules() }

    val questRepository: QuestRepository = QuestRepository(
        database = database,
        questDao = database.questDao(),
        categoryDao = database.categoryDao(),
        logDao = database.questLogDao(),
        stepDao = database.questStepDao(),
        instanceDao = database.questInstanceDao(),
    )

    val playerRepository: PlayerRepository = PlayerRepository(
        playerDao = database.playerDao(),
        attributeDao = database.attributeDao(),
        progressionDao = database.progressionDao(),
        levelRules = levelRules,
    )

    val dashboardRepository: DashboardRepository = DashboardRepository(
        questDao = database.questDao(),
        logDao = database.questLogDao(),
        categoryDao = database.categoryDao(),
        playerDao = database.playerDao(),
        stepDao = database.questStepDao(),
        todaySource = todaySource,
        levelRules = levelRules,
    )

    val analyticsRepository: AnalyticsRepository = AnalyticsRepository(
        logDao = database.questLogDao(),
        questDao = database.questDao(),
        categoryDao = database.categoryDao(),
        todaySource = todaySource,
    )

    val historyRepository: HistoryRepository = HistoryRepository(
        logDao = database.questLogDao(),
        questDao = database.questDao(),
        categoryDao = database.categoryDao(),
        levelRules = levelRules,
    )

    val skillTreeRepository: SkillTreeRepository = SkillTreeRepository(
        skillNodeDao = database.skillNodeDao(),
        attributeDao = database.attributeDao(),
        questDao = database.questDao(),
        playerDao = database.playerDao(),
        levelRules = levelRules,
    )

    val achievementRepository: AchievementRepository = AchievementRepository(
        achievementDao = database.achievementDao(),
        logDao = database.questLogDao(),
        questDao = database.questDao(),
        categoryDao = database.categoryDao(),
        attributeDao = database.attributeDao(),
        skillNodeDao = database.skillNodeDao(),
        progressionDao = database.progressionDao(),
        bossDao = database.bossDao(),
        levelRules = levelRules,
    )

    val gameEngine: GameEngine = GameEngine(database, levelRules)

    val bossEngine: BossEngine = BossEngine(database)

    val bossRepository: BossRepository = BossRepository(
        bossDao = database.bossDao(),
        questDao = database.questDao(),
        engine = bossEngine,
    )

    val progressSync: ProgressSync = ProgressSync(
        gameEngine = gameEngine,
        achievementRepository = achievementRepository,
        bossRepository = bossRepository,
        instanceDao = database.questInstanceDao(),
    )

    val instanceMaintenance: InstanceMaintenance = InstanceMaintenance(
        instanceDao = database.questInstanceDao(),
        logDao = database.questLogDao(),
        questDao = database.questDao(),
        stepDao = database.questStepDao(),
    )

    val seeder: DatabaseSeeder = DatabaseSeeder(database, settings)
}
