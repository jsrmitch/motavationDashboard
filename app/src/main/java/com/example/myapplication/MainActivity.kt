package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.myapplication.navigation.AppNavigation
import com.example.myapplication.navigation.TvNavigation
import com.example.myapplication.ui.theme.MyApplicationTheme


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    Column {
                        TvNavigation(navController = navController)
                        AppNavigation(navController = navController)
                    }
                }
            }
        }
    }
}