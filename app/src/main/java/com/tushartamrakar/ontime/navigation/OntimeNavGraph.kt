package com.tushartamrakar.ontime.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tushartamrakar.ontime.alarm.presentation.AlarmsScreen
import com.tushartamrakar.ontime.alarm.presentation.CreateAlarmScreen
import com.tushartamrakar.ontime.core.ui.components.MainScaffold

// ─── Routes ───────────────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Alarms : Screen("alarms")
    object Calendar : Screen("calendar")
    object Focus : Screen("focus")
    object Profile : Screen("profile")
    object CreateAlarm : Screen("create_alarm")
}

// ─── Nav Graph ────────────────────────────────────────────────────────────────
@Composable
fun OntimeNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Alarms.route,
    ) {
        composable(Screen.Home.route) {
            MainScaffold(navController = navController) {
                PlaceholderScreen(title = "🏠 Home")
            }
        }
        composable(Screen.Alarms.route) {
            MainScaffold(navController = navController) { paddingValues ->
                AlarmsScreen(
                    navController = navController,
                    bottomPadding = paddingValues.calculateBottomPadding(),
                )
            }
        }
        composable(Screen.Calendar.route) {
            MainScaffold(navController = navController) {
                PlaceholderScreen(title = "📅 Calendar")
            }
        }
        composable(Screen.Focus.route) {
            MainScaffold(navController = navController) {
                PlaceholderScreen(title = "🎯 Focus")
            }
        }
        composable(Screen.Profile.route) {
            MainScaffold(navController = navController) {
                PlaceholderScreen(title = "👤 Profile")
            }
        }
        composable(Screen.CreateAlarm.route) {
            CreateAlarmScreen(navController = navController)
        }
    }
}