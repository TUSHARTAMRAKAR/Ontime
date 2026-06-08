package com.tushartamrakar.ontime.core.navigation

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * DeepLinkHandler
 *
 * Central hub for all notification → in-app navigation.
 *
 * Every notification PendingIntent sets:
 *   intent.putExtra(EXTRA_ROUTE, "some/route")
 *
 * Two entry points:
 *   1. Cold start  — OntimeApp reads activity.intent on first launch
 *   2. Warm start  — MainActivity.onNewIntent emits to [newRoute] flow
 *
 * OntimeApp collects [newRoute] and calls navController.navigate(route).
 */
object DeepLinkHandler {

    // ── Intent extra key ──────────────────────────────────────────────────────
    const val EXTRA_ROUTE = "navigate_to"

    // ── Route constants ───────────────────────────────────────────────────────
    const val ROUTE_ALARMS         = "alarms"
    const val ROUTE_FOCUS          = "focus"
    const val ROUTE_CALENDAR       = "calendar"
    const val ROUTE_PERIOD_TRACKER = "period_tracker"
    const val ROUTE_SETTINGS       = "settings"

    /** Builds route for a specific calendar event detail screen. */
    fun routeEventDetail(eventId: Int) = "event_detail/$eventId"

    // ── New intent flow ───────────────────────────────────────────────────────

    /**
     * Emitted when a notification is tapped while the app is already running.
     * MainActivity.onNewIntent calls [emit] → OntimeApp collects and navigates.
     */
    private val _newRoute = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val newRoute = _newRoute.asSharedFlow()

    fun emit(intent: Intent?) {
        val route = intent?.getStringExtra(EXTRA_ROUTE) ?: return
        _newRoute.tryEmit(route)
    }

    // ── Helper: build a launch intent with deep link route ───────────────────

    /**
     * Creates an Intent that will open MainActivity and navigate to [route].
     * Use this when building notification PendingIntents.
     *
     *   FLAG_ACTIVITY_SINGLE_TOP — if MainActivity is already running, calls
     *   onNewIntent instead of creating a second instance.
     *
     *   FLAG_ACTIVITY_CLEAR_TOP  — clears any activities on top of MainActivity.
     */
    fun buildIntent(context: Context, route: String): Intent =
        Intent(context, com.tushartamrakar.ontime.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK     or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP    or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ROUTE, route)
        }
}
