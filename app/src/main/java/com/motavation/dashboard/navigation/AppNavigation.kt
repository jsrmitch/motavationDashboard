package com.motavation.dashboard.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.motavation.dashboard.competition.CompetitionScreen
import com.motavation.dashboard.home.HomeScreen
import com.motavation.dashboard.rounds.RoundsScreen
import com.motavation.dashboard.settings.SettingsScreen

@Composable
fun AppNavigation(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen()
        }
        composable("rounds") {
            RoundsScreen()
        }
        composable("competition") {
            CompetitionScreen()
        }
        composable("settings") {
            SettingsScreen()
        }
    }
}