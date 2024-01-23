package com.jianzheng.studyzero.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


object AppRoutes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val TUTORIAL = "tutorial"
    const val TUTORIAL2 = "tutorial2"
    const val EXPORT = "EXPORT"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()  // Create a NavController

    NavHost(navController = navController, startDestination = AppRoutes.HOME) {
        composable(AppRoutes.HOME) { HomeScreen(navController) }
        composable(AppRoutes.SETTINGS) { SettingsScreen(navController) }
        composable(AppRoutes.TUTORIAL) { TutorialScreen(navController) }
        composable(AppRoutes.TUTORIAL2) { TutorialScreen2(navController) }
        composable(AppRoutes.EXPORT) { ExportScreen(navController) }
    }
}
