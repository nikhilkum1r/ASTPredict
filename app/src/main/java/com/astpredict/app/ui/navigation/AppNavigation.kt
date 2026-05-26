package com.astpredict.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.astpredict.app.data.ml.ColonyDetector
import com.astpredict.app.data.db.AppDatabase
import com.astpredict.app.ui.home.HomeScreen
import com.astpredict.app.ui.camera.CameraScreen
import com.astpredict.app.ui.results.ResultsScreen
import com.astpredict.app.ui.history.HistoryScreen
import com.astpredict.app.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Camera : Screen("camera")
    data object Results : Screen("results/{analysisId}") {
        fun createRoute(analysisId: Long) = "results/$analysisId"
    }
    data object History : Screen("history")
    data object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    detector: ColonyDetector,
    database: AppDatabase
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                initialOffsetX = { 100 },
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                initialOffsetX = { -100 },
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                targetOffsetX = { 100 },
                animationSpec = tween(300)
            )
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                database = database
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(
                navController = navController,
                detector = detector,
                database = database
            )
        }

        composable(
            route = Screen.Results.route,
            arguments = listOf(navArgument("analysisId") { type = NavType.LongType })
        ) { backStackEntry ->
            val analysisId = backStackEntry.arguments?.getLong("analysisId") ?: 0L
            ResultsScreen(
                analysisId = analysisId,
                navController = navController,
                database = database
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                navController = navController,
                database = database
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                detector = detector
            )
        }
    }
}
