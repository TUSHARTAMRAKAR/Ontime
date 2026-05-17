package com.tushartamrakar.ontime.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tushartamrakar.ontime.alarm.presentation.AlarmsScreen
import com.tushartamrakar.ontime.alarm.presentation.CreateAlarmScreen
import com.tushartamrakar.ontime.auth.presentation.AuthViewModel
import com.tushartamrakar.ontime.auth.presentation.LoginScreen
import com.tushartamrakar.ontime.auth.presentation.RegisterScreen
import com.tushartamrakar.ontime.auth.presentation.WelcomeScreen
import com.tushartamrakar.ontime.core.ui.components.MainScaffold

// ─── Routes ───────────────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
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
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState(initial = null)

    // Determine start destination based on auth state
    val startDestination = if (authViewModel.currentUser.value != null) {
        Screen.Alarms.route
    } else {
        Screen.Welcome.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        // ─── Auth Screens ─────────────────────────────────────────────────────
        composable(Screen.Welcome.route) {
            WelcomeScreen(navController = navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        // ─── Main App Screens ─────────────────────────────────────────────────
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