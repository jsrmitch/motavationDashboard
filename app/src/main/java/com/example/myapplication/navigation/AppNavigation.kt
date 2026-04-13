package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.home.HomeScreen
import com.example.myapplication.rounds.RoundsScreen
import com.example.myapplication.competition.CompetitionScreen

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
    }
}