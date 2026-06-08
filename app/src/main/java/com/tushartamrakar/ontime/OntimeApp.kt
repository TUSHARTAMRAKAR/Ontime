package com.tushartamrakar.ontime

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.tushartamrakar.ontime.calendar.presentation.CalendarViewModel
import com.tushartamrakar.ontime.core.navigation.DeepLinkHandler
import com.tushartamrakar.ontime.core.security.AppLockManager
import com.tushartamrakar.ontime.core.security.AppLockScreen
import com.tushartamrakar.ontime.focus.foreground.FocusTimerService
import com.tushartamrakar.ontime.focus.presentation.CelebrationOverlay
import com.tushartamrakar.ontime.navigation.OntimeNavGraph
import kotlinx.coroutines.delay

@Composable
fun OntimeApp(launchIntent: Intent? = null) {
    val context           = LocalContext.current
    val navController     = rememberNavController()
    val calendarViewModel = hiltViewModel<CalendarViewModel>()
    val appLockManager    = remember { AppLockManager(context) }

    // ── App lock state ────────────────────────────────────────────────────────
    var isLocked by remember {
        mutableStateOf(appLockManager.shouldLockOnStart())
    }

    // ── Celebration event ─────────────────────────────────────────────────────
    val celebrationData by FocusTimerService.celebrationEvent.collectAsState()

    // ── Lifecycle observer — lock on background → foreground ──────────────────
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP  -> appLockManager.onAppBackground()
                Lifecycle.Event.ON_START -> {
                    if (appLockManager.shouldLockOnStart()) isLocked = true
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Deep link: cold start ─────────────────────────────────────────────────
    // When the app is launched from a notification tap (fresh start), the route
    // is in the Intent extra. We wait for the nav graph to initialize (300ms)
    // before navigating so the graph is ready to accept navigation commands.
    LaunchedEffect(launchIntent) {
        val route = launchIntent?.getStringExtra(DeepLinkHandler.EXTRA_ROUTE) ?: return@LaunchedEffect
        delay(300)   // let NavHost + start destination compose first
        navigateTo(navController, route)
    }

    // ── Deep link: warm start (app already running) ───────────────────────────
    // When a notification is tapped while the app is open, MainActivity.onNewIntent
    // calls DeepLinkHandler.emit() → we collect here and navigate immediately.
    LaunchedEffect(Unit) {
        DeepLinkHandler.newRoute.collect { route ->
            navigateTo(navController, route)
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        OntimeNavGraph(
            navController = navController,
            repository    = calendarViewModel.repository,
        )

        // Lock screen overlays everything
        if (isLocked) {
            AppLockScreen(
                appLockManager = appLockManager,
                onUnlocked     = { isLocked = false },
            )
        }

        // Celebration overlay — shows when daily goal is hit
        celebrationData?.let { data ->
            CelebrationOverlay(
                data      = data,
                onDismiss = { FocusTimerService.celebrationEvent.value = null },
            )
        }
    }
}

// ─── Navigation helper ────────────────────────────────────────────────────────

/**
 * Navigates to [route] with a clean back stack.
 * Validates the route before navigating to avoid crashes from stale intents.
 * Falls back to Alarms if the route is unrecognized.
 */
private fun navigateTo(
    navController: androidx.navigation.NavController,
    route:         String,
) {
    val safeRoute = when {
        // Exact tab routes
        route in listOf("alarms", "focus", "calendar", "settings") -> route
        // Sub-screen routes with parameters (e.g. "event_detail/42")
        route.startsWith("event_detail/") -> route
        route.startsWith("period")        -> route
        route.startsWith("focus_")        -> route
        // Unknown — fallback
        else -> "alarms"
    }

    try {
        navController.navigate(safeRoute) {
            // Keep Alarms as the root so back button works naturally
            popUpTo("alarms") { saveState = true }
            launchSingleTop = true
            restoreState    = true
        }
    } catch (e: Exception) {
        // Silently ignore stale intents pointing to non-existent routes
    }
}
