package com.tushartamrakar.ontime.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tushartamrakar.ontime.alarm.presentation.AlarmSoundScreen
import com.tushartamrakar.ontime.alarm.presentation.AlarmsScreen
import com.tushartamrakar.ontime.alarm.presentation.CreateAlarmScreen
import com.tushartamrakar.ontime.alarm.presentation.ExtraLoudInfoScreen
import com.tushartamrakar.ontime.alarm.presentation.GentleWakeUpScreen
import com.tushartamrakar.ontime.alarm.presentation.HabitAlarmScreen
import com.tushartamrakar.ontime.alarm.presentation.SnoozeSettingsScreen
import com.tushartamrakar.ontime.auth.presentation.AuthViewModel
import com.tushartamrakar.ontime.auth.presentation.LoginScreen
import com.tushartamrakar.ontime.auth.presentation.RegisterScreen
import com.tushartamrakar.ontime.auth.presentation.WelcomeScreen
import com.tushartamrakar.ontime.calendar.data.repository.CalendarRepository
import com.tushartamrakar.ontime.calendar.presentation.AddPeopleScreen
import com.tushartamrakar.ontime.calendar.presentation.CalendarScreen
import com.tushartamrakar.ontime.calendar.presentation.CreateEventScreen
import com.tushartamrakar.ontime.calendar.presentation.EventDetailScreen
import com.tushartamrakar.ontime.calendar.presentation.SearchScreen
import com.tushartamrakar.ontime.calendar.sync.CalendarSyncScreen
import com.tushartamrakar.ontime.core.ui.components.MainScaffold
import com.tushartamrakar.ontime.focus.presentation.BlockerScreen
import com.tushartamrakar.ontime.focus.presentation.FocusScreen
import com.tushartamrakar.ontime.focus.presentation.FocusSettingsScreen
import com.tushartamrakar.ontime.focus.presentation.FocusStatsScreen
import com.tushartamrakar.ontime.focus.presentation.PlannerScreen
import com.tushartamrakar.ontime.onboarding.presentation.OnboardingScreen
import com.tushartamrakar.ontime.onboarding.presentation.isOnboardingCompleted
import com.tushartamrakar.ontime.period.presentation.PeriodOnboardingScreen
import com.tushartamrakar.ontime.period.presentation.PeriodTrackerScreen
import com.tushartamrakar.ontime.settings.presentation.AppSettingsScreen
import com.tushartamrakar.ontime.tasks.presentation.TasksScreen

// ─── Animation constants ──────────────────────────────────────────────────────

private const val TAB_ANIM_MS   = 220   // fast crossfade between bottom nav tabs
private const val SLIDE_ANIM_MS = 300   // slide for sub-screen drill-downs

// Tab screens: simple fade in/out — feels native and smooth
private val tabEnter  = fadeIn(tween(TAB_ANIM_MS))
private val tabExit   = fadeOut(tween(TAB_ANIM_MS))

// Sub-screens: slide from right (forward) / slide to right (back)
private val subEnter       = slideInHorizontally(tween(SLIDE_ANIM_MS))  { it } +
                             fadeIn(tween(SLIDE_ANIM_MS))
private val subExit        = slideOutHorizontally(tween(SLIDE_ANIM_MS)) { -it / 3 } +
                             fadeOut(tween(SLIDE_ANIM_MS))
private val subPopEnter    = slideInHorizontally(tween(SLIDE_ANIM_MS))  { -it / 3 } +
                             fadeIn(tween(SLIDE_ANIM_MS))
private val subPopExit     = slideOutHorizontally(tween(SLIDE_ANIM_MS)) { it } +
                             fadeOut(tween(SLIDE_ANIM_MS))

// ─── Screen routes ────────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    // Auth
    object Welcome    : Screen("welcome")
    object Login      : Screen("login")
    object Register   : Screen("register")
    object Onboarding : Screen("onboarding")

    // Bottom nav (4 tabs)
    object Alarms   : Screen("alarms")
    object Calendar : Screen("calendar")
    object Focus    : Screen("focus")
    object Settings : Screen("settings")

    // Calendar sub-screens
    object CalendarSync : Screen("calendar_sync")
    object Tasks        : Screen("tasks")
    object Search       : Screen("search")
    object AddPeople    : Screen("add_people")
    object CreateEvent  : Screen("create_event/{date}") {
        fun createRoute(date: String) = "create_event/$date"
    }
    object EditEvent    : Screen("edit_event/{eventId}") {
        fun createRoute(eventId: Int) = "edit_event/$eventId"
    }
    object EventDetail  : Screen("event_detail/{eventId}") {
        fun createRoute(eventId: Int) = "event_detail/$eventId"
    }

    // Period Tracker
    object PeriodTracker    : Screen("period_tracker")
    object PeriodOnboarding : Screen("period_onboarding")

    // Focus sub-screens
    object Planner      : Screen("planner")
    object Blocker      : Screen("blocker")
    object FocusStats   : Screen("focus_stats")
    object FocusSettings: Screen("focus_settings")

    // Alarm sub-screens
    object CreateAlarm   : Screen("create_alarm")
    object HabitAlarm    : Screen("habit_alarm")
    object EditAlarm     : Screen("edit_alarm/{alarmId}") {
        fun createRoute(alarmId: Int) = "edit_alarm/$alarmId"
    }
    object AlarmSound    : Screen("alarm_sound/{currentSound}") {
        fun createRoute(currentSound: String) = "alarm_sound/$currentSound"
    }
    object GentleWakeUp  : Screen("gentle_wake_up/{currentSeconds}") {
        fun createRoute(currentSeconds: Int) = "gentle_wake_up/$currentSeconds"
    }
    object ExtraLoudInfo : Screen("extra_loud_info/{currentSound}") {
        fun createRoute(currentSound: String) = "extra_loud_info/$currentSound"
    }
    object SnoozeSettings: Screen("snooze_settings/{enabled}/{interval}/{limit}/{progressive}") {
        fun createRoute(enabled: Boolean, interval: Int, limit: Int, progressive: Boolean) =
            "snooze_settings/${if (enabled) 1 else 0}/$interval/$limit/${if (progressive) 1 else 0}"
    }
}

// Helper — is this route one of the 4 bottom nav tabs?
private fun String?.isTabRoute() = this in setOf(
    Screen.Alarms.route,
    Screen.Calendar.route,
    Screen.Focus.route,
    Screen.Settings.route,
)

// ─── Nav graph ────────────────────────────────────────────────────────────────

@Composable
fun OntimeNavGraph(
    navController: NavHostController,
    repository: CalendarRepository,
) {
    val context          = androidx.compose.ui.platform.LocalContext.current
    val authViewModel: AuthViewModel = hiltViewModel()
    val startDestination = when {
        authViewModel.currentUser.value == null          -> Screen.Welcome.route
        !isOnboardingCompleted(context)                  -> Screen.Onboarding.route
        else                                             -> Screen.Alarms.route
    }

    NavHost(
        navController    = navController,
        startDestination = startDestination,
        // Default transitions (overridden per composable where needed)
        enterTransition  = { fadeIn(tween(TAB_ANIM_MS)) },
        exitTransition   = { fadeOut(tween(TAB_ANIM_MS)) },
    ) {

        // ── Auth (no animation needed) ────────────────────────────────────────
        composable(Screen.Welcome.route)  { WelcomeScreen(navController = navController) }
        composable(Screen.Login.route)    { LoginScreen(navController = navController) }
        composable(Screen.Register.route) { RegisterScreen(navController = navController) }

        // ── Onboarding ────────────────────────────────────────────────────────
        composable(
            route           = Screen.Onboarding.route,
            enterTransition = { tabEnter },
            exitTransition  = { tabExit },
        ) {
            OnboardingScreen(navController = navController)
        }

        // ── Tab: Alarms ───────────────────────────────────────────────────────
        composable(
            route           = Screen.Alarms.route,
            enterTransition = { tabEnter },
            exitTransition  = { tabExit },
        ) {
            MainScaffold(navController = navController) { paddingValues ->
                AlarmsScreen(
                    navController = navController,
                    bottomPadding = paddingValues.calculateBottomPadding(),
                )
            }
        }

        // ── Tab: Calendar ─────────────────────────────────────────────────────
        composable(
            route           = Screen.Calendar.route,
            enterTransition = { tabEnter },
            exitTransition  = { tabExit },
        ) {
            MainScaffold(navController = navController) { paddingValues ->
                CalendarScreen(
                    navController = navController,
                    bottomPadding = paddingValues.calculateBottomPadding(),
                )
            }
        }

        // ── Tab: Focus ────────────────────────────────────────────────────────
        composable(
            route           = Screen.Focus.route,
            enterTransition = { tabEnter },
            exitTransition  = { tabExit },
        ) {
            MainScaffold(navController = navController) { paddingValues ->
                FocusScreen(
                    navController = navController,
                    bottomPadding = paddingValues.calculateBottomPadding(),
                )
            }
        }

        // ── Tab: Settings ─────────────────────────────────────────────────────
        composable(
            route           = Screen.Settings.route,
            enterTransition = { tabEnter },
            exitTransition  = { tabExit },
        ) {
            MainScaffold(navController = navController) { paddingValues ->
                AppSettingsScreen(
                    navController = navController,
                    bottomPadding = paddingValues.calculateBottomPadding(),
                )
            }
        }

        // ── Sub: Calendar ─────────────────────────────────────────────────────
        composable(
            route            = Screen.Search.route,
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { SearchScreen(navController = navController) }

        composable(
            route            = Screen.Tasks.route,
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { TasksScreen(navController = navController) }

        composable(
            route            = Screen.CalendarSync.route,
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { CalendarSyncScreen(navController = navController, repository = repository) }

        composable(
            route            = Screen.AddPeople.route,
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { AddPeopleScreen(navController = navController) }

        composable(
            route          = Screen.CreateEvent.route,
            arguments      = listOf(navArgument("date") { type = NavType.StringType }),
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { backStackEntry ->
            CreateEventScreen(
                navController = navController,
                initialDate   = backStackEntry.arguments?.getString("date") ?: "",
            )
        }

        composable(
            route          = Screen.EditEvent.route,
            arguments      = listOf(navArgument("eventId") { type = NavType.IntType }),
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { backStackEntry ->
            CreateEventScreen(
                navController = navController,
                eventId       = backStackEntry.arguments?.getInt("eventId") ?: -1,
            )
        }

        composable(
            route          = Screen.EventDetail.route,
            arguments      = listOf(navArgument("eventId") { type = NavType.IntType }),
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { backStackEntry ->
            EventDetailScreen(
                navController = navController,
                eventId       = backStackEntry.arguments?.getInt("eventId") ?: -1,
            )
        }

        // ── Sub: Period Tracker ───────────────────────────────────────────────
        composable(
            route            = Screen.PeriodTracker.route,
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { PeriodTrackerScreen(navController = navController) }

        composable(
            route            = Screen.PeriodOnboarding.route,
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { PeriodOnboardingScreen(navController = navController) }

        // ── Sub: Focus ────────────────────────────────────────────────────────
        composable(
            route            = Screen.Planner.route,
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { PlannerScreen(navController = navController) }

        composable(
            route            = Screen.Blocker.route,
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { BlockerScreen(navController = navController) }

        composable(
            route            = Screen.FocusStats.route,
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { FocusStatsScreen(navController = navController) }

        composable(
            route            = Screen.FocusSettings.route,
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { FocusSettingsScreen(navController = navController) }

        // ── Sub: Alarm ────────────────────────────────────────────────────────
        composable(
            route            = Screen.CreateAlarm.route,
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { CreateAlarmScreen(navController = navController) }

        composable(
            route            = Screen.HabitAlarm.route,
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { HabitAlarmScreen(navController = navController) }

        composable(
            route          = Screen.EditAlarm.route,
            arguments      = listOf(navArgument("alarmId") { type = NavType.IntType }),
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { backStackEntry ->
            CreateAlarmScreen(
                navController = navController,
                alarmId       = backStackEntry.arguments?.getInt("alarmId") ?: -1,
            )
        }

        composable(
            route          = Screen.AlarmSound.route,
            arguments      = listOf(navArgument("currentSound") { type = NavType.StringType }),
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { backStackEntry ->
            AlarmSoundScreen(
                navController   = navController,
                currentSound    = backStackEntry.arguments?.getString("currentSound") ?: "alarm_digital_alarm",
                onSoundSelected = { sound ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("selected_sound", sound)
                },
            )
        }

        composable(
            route          = Screen.GentleWakeUp.route,
            arguments      = listOf(navArgument("currentSeconds") { type = NavType.IntType }),
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { backStackEntry ->
            GentleWakeUpScreen(
                navController    = navController,
                currentSeconds   = backStackEntry.arguments?.getInt("currentSeconds") ?: 0,
                onOptionSelected = { seconds ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("gentle_wake_up_seconds", seconds)
                },
            )
        }

        composable(
            route          = Screen.ExtraLoudInfo.route,
            arguments      = listOf(navArgument("currentSound") { type = NavType.StringType }),
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { backStackEntry ->
            ExtraLoudInfoScreen(
                navController = navController,
                currentSound  = backStackEntry.arguments?.getString("currentSound") ?: "alarm_digital_alarm",
                onEnable      = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("extra_loud_enabled", true)
                },
            )
        }

        composable(
            route      = Screen.SnoozeSettings.route,
            arguments  = listOf(
                navArgument("enabled")     { type = NavType.IntType },
                navArgument("interval")    { type = NavType.IntType },
                navArgument("limit")       { type = NavType.IntType },
                navArgument("progressive") { type = NavType.IntType },
            ),
            enterTransition  = { subEnter },
            exitTransition   = { subExit },
            popEnterTransition = { subPopEnter },
            popExitTransition  = { subPopExit },
        ) { backStackEntry ->
            SnoozeSettingsScreen(
                navController      = navController,
                initialEnabled     = (backStackEntry.arguments?.getInt("enabled") ?: 1) == 1,
                initialInterval    = backStackEntry.arguments?.getInt("interval") ?: 5,
                initialLimit       = backStackEntry.arguments?.getInt("limit") ?: 3,
                initialProgressive = (backStackEntry.arguments?.getInt("progressive") ?: 0) == 1,
            )
        }
    }
}
