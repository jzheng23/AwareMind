package com.jianzheng.studyzero.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

//@Composable
//fun HomeScreen(navController: NavController) {
//    Column(
//        verticalArrangement = Arrangement.SpaceBetween,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ){
//        Text("Home")
//        Button(onClick = { navController.navigate(AppRoutes.SETTINGS) }){
//            Text("Setting")
//        }
//    }
//
//}
//
//@Composable
//fun SettingsScreen(navController: NavController) {
//    Column(
//        verticalArrangement = Arrangement.SpaceBetween,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ){
//        Text("Setting")
//        Button(onClick = { navController.navigate(AppRoutes.HOME) }){
//            Text("Home")
//        }
//    }
//}
//
//@Composable
//fun HelpScreen(navController: NavController) {
//    Text("Help")
//}

object AppRoutes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val TUTORIAL = "tutorial"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()  // Create a NavController

    NavHost(navController = navController, startDestination = AppRoutes.HOME) {
        composable(AppRoutes.HOME) { HomeScreen(navController) }
        composable(AppRoutes.SETTINGS) { SettingsScreen(navController) }
        composable(AppRoutes.TUTORIAL) { TutorialScreen(navController) }
    }
}
