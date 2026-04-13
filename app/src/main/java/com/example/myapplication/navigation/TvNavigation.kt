package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvNavigation(
    navController: NavHostController
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf("Home", "Rounds", "Competition")
    val routes = listOf("home", "rounds", "competition")

    TabRow(
        selectedTabIndex = selectedTabIndex
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onFocus = {
                    selectedTabIndex = index
                    navController.navigate(routes[index])
                },
                onClick = {
                    selectedTabIndex = index
                    navController.navigate(routes[index])
                }
            ) {
                Text(title)
            }
        }
    }
}