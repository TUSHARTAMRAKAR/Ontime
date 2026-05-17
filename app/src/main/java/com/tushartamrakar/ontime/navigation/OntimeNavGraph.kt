package com.tushartamrakar.ontime.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tushartamrakar.ontime.alarm.presentation.AlarmsScreen
import com.tushartamrakar.ontime.core.ui.components.MainScaffold

// ─── Routes ───────────────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Alarms : Screen("alarms")
    object Calendar : Screen("calendar")
    object Focus : Screen("focus")
    object Profile : Screen("profile")
    object CreateAlarm : Screen("create_alarm?alarmId={alarmId}") {
        fun createRoute(alarmId: String? = null) =
            if (alarmId != null) "create_alarm?alarmId=$alarmId"
            else "create_alarm"
    }
}

// ─── Nav Graph ────────────────────────────────────────────────────────────────
@Composable
fun OntimeNavGraph(navController: NavHostController) {
    MainScaffold(navController = navController) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Alarms.route,
        ) {
            composable(Screen.Home.route) {
                PlaceholderScreen(title = "🏠 Home")
            }
            composable(Screen.Alarms.route) {
                AlarmsScreen(
                    navController = navController,
                )
            }
            composable(Screen.Calendar.route) {
                PlaceholderScreen(title = "📅 Calendar")
            }
            composable(Screen.Focus.route) {
                PlaceholderScreen(title = "🎯 Focus")
            }
            composable(Screen.Profile.route) {
                PlaceholderScreen(title = "👤 Profile")
            }
        }
    }
}