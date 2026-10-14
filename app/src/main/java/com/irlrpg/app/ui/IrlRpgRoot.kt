package com.irlrpg.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.irlrpg.app.ui.analytics.AnalyticsScreen
import com.irlrpg.app.ui.analytics.AnalyticsViewModel
import com.irlrpg.app.ui.common.appViewModel
import com.irlrpg.app.ui.dashboard.DashboardScreen
import com.irlrpg.app.ui.dashboard.DashboardViewModel
import com.irlrpg.app.ui.design.ClayBottomBar
import com.irlrpg.app.ui.navigation.Routes
import com.irlrpg.app.ui.navigation.topLevelDestinations
import com.irlrpg.app.ui.profile.ProfileScreen
import com.irlrpg.app.ui.profile.ProfileViewModel
import com.irlrpg.app.ui.quests.QuestDetailScreen
import com.irlrpg.app.ui.quests.QuestDetailViewModel
import com.irlrpg.app.ui.quests.QuestEditorScreen
import com.irlrpg.app.ui.quests.QuestEditorViewModel
import com.irlrpg.app.ui.quests.QuestsScreen
import com.irlrpg.app.ui.quests.QuestsViewModel
import com.irlrpg.app.ui.skills.SkillsScreen
import com.irlrpg.app.ui.skills.SkillsViewModel

/**
 * App shell: five top-level destinations behind a floating clay bottom bar,
 * plus the quest editor and quest detail as full-screen routes.
 */
@Composable
fun IrlRpgRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val navigateTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val bottomBar: @Composable (String) -> Unit = { route ->
        ClayBottomBar(
            destinations = topLevelDestinations,
            selectedRoute = route,
            onSelect = { navigateTopLevel(it.route) },
        )
    }

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(Routes.DASHBOARD) {
            val viewModel: DashboardViewModel = appViewModel {
                DashboardViewModel(it.dashboardRepository, it.gameEngine, it.todaySource)
            }
            DashboardScreen(
                viewModel = viewModel,
                bottomBar = { bottomBar(Routes.DASHBOARD) },
                onNewQuest = { navController.navigate(Routes.questEditor()) },
                onOpenQuest = { questId -> navController.navigate(Routes.questDetail(questId)) },
                onOpenAnalytics = { navigateTopLevel(Routes.ANALYTICS) },
                onOpenQuests = { navigateTopLevel(Routes.QUESTS) },
            )
        }

        composable(Routes.QUESTS) {
            val viewModel: QuestsViewModel = appViewModel {
                QuestsViewModel(it.questRepository, it.gameEngine, it.todaySource)
            }
            QuestsScreen(
                viewModel = viewModel,
                bottomBar = { bottomBar(Routes.QUESTS) },
                onCreateQuest = { navController.navigate(Routes.questEditor()) },
                onOpenQuest = { questId -> navController.navigate(Routes.questDetail(questId)) },
                onEditQuest = { questId -> navController.navigate(Routes.questEditor(questId)) },
            )
        }

        composable(Routes.SKILLS) {
            val viewModel: SkillsViewModel = appViewModel {
                SkillsViewModel(it.playerRepository, it.questRepository)
            }
            SkillsScreen(viewModel = viewModel, bottomBar = { bottomBar(Routes.SKILLS) })
        }

        composable(Routes.ANALYTICS) {
            val viewModel: AnalyticsViewModel = appViewModel {
                AnalyticsViewModel(it.analyticsRepository, it.todaySource)
            }
            AnalyticsScreen(
                viewModel = viewModel,
                bottomBar = { bottomBar(Routes.ANALYTICS) },
                onCreateQuest = { navController.navigate(Routes.questEditor()) },
            )
        }

        composable(Routes.PROFILE) {
            val viewModel: ProfileViewModel = appViewModel {
                ProfileViewModel(it.playerRepository, it.questRepository)
            }
            ProfileScreen(
                viewModel = viewModel,
                bottomBar = { bottomBar(Routes.PROFILE) },
                onOpenSkills = { navigateTopLevel(Routes.SKILLS) },
            )
        }

        composable(
            route = Routes.QUEST_EDITOR,
            arguments = listOf(
                navArgument("questId") {
                    type = NavType.LongType
                    defaultValue = Routes.NEW_QUEST
                }
            ),
        ) { entry ->
            val questId = entry.arguments?.getLong("questId") ?: Routes.NEW_QUEST
            val viewModel: QuestEditorViewModel = appViewModel(key = "quest-editor-$questId") {
                QuestEditorViewModel(
                    questRepository = it.questRepository,
                    playerRepository = it.playerRepository,
                    questId = questId.takeIf { id -> id > 0L },
                )
            }
            QuestEditorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.QUEST_DETAIL,
            arguments = listOf(navArgument("questId") { type = NavType.LongType }),
        ) { entry ->
            val questId = entry.arguments?.getLong("questId") ?: 0L
            val viewModel: QuestDetailViewModel = appViewModel(key = "quest-detail-$questId") {
                QuestDetailViewModel(
                    questRepository = it.questRepository,
                    gameEngine = it.gameEngine,
                    todaySource = it.todaySource,
                    questId = questId,
                )
            }
            QuestDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.questEditor(id)) },
                onDeleted = { navController.popBackStack() },
            )
        }
    }
}
