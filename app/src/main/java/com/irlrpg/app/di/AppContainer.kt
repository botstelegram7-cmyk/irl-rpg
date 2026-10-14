package com.irlrpg.app.di

import android.content.Context
import com.irlrpg.app.core.TodaySource
import com.irlrpg.app.data.local.DatabaseSeeder
import com.irlrpg.app.data.local.IrlRpgDatabase
import com.irlrpg.app.data.prefs.SettingsStore
import com.irlrpg.app.data.repository.AnalyticsRepository
import com.irlrpg.app.data.repository.DashboardRepository
import com.irlrpg.app.data.repository.PlayerRepository
import com.irlrpg.app.data.repository.QuestRepository
import com.irlrpg.app.domain.game.GameEngine

/**
 * Hand-rolled dependency container: one place where the object graph is built,
 * no annotation processing, no reflection — cheap to start on budget devices.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val database: IrlRpgDatabase = IrlRpgDatabase.get(appContext)

    val settings: SettingsStore = SettingsStore(appContext)

    val todaySource: TodaySource = TodaySource()

    val questRepository: QuestRepository = QuestRepository(
        questDao = database.questDao(),
        categoryDao = database.categoryDao(),
        logDao = database.questLogDao(),
    )

    val playerRepository: PlayerRepository = PlayerRepository(
        playerDao = database.playerDao(),
        attributeDao = database.attributeDao(),
        progressionDao = database.progressionDao(),
    )

    val dashboardRepository: DashboardRepository = DashboardRepository(
        questDao = database.questDao(),
        logDao = database.questLogDao(),
        categoryDao = database.categoryDao(),
        playerDao = database.playerDao(),
        todaySource = todaySource,
    )

    val analyticsRepository: AnalyticsRepository = AnalyticsRepository(
        logDao = database.questLogDao(),
        questDao = database.questDao(),
        categoryDao = database.categoryDao(),
        todaySource = todaySource,
    )

    val gameEngine: GameEngine = GameEngine(database)

    val seeder: DatabaseSeeder = DatabaseSeeder(database, settings)
}
