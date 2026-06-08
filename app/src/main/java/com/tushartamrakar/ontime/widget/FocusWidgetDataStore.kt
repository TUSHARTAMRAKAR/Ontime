package com.tushartamrakar.ontime.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * FocusWidgetDataStore
 *
 * Widgets run in a separate process from the app (the launcher's process).
 * They cannot access Room databases, Hilt injections, or Compose state directly.
 *
 * This object is the data bridge:
 *  - App side: FocusTimerService calls save() after every session completes
 *  - Widget side: FocusWidgetProvider calls load() in onUpdate to build RemoteViews
 *
 * SharedPreferences is the standard Android approach for this pattern.
 */
object FocusWidgetDataStore {

    private const val PREFS_NAME           = "ontime_widget_prefs"
    private const val KEY_TODAY_SECONDS    = "today_seconds"
    private const val KEY_TODAY_SESSIONS   = "today_sessions"
    private const val KEY_GOAL_SESSIONS    = "goal_sessions"
    private const val KEY_STREAK_DAYS      = "streak_days"

    // ── Write (called from app process) ───────────────────────────────────────

    fun save(
        context:       Context,
        todaySeconds:  Int,
        todaySessions: Int,
        goalSessions:  Int,
        streakDays:    Int,
    ) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_TODAY_SECONDS,  todaySeconds)
            .putInt(KEY_TODAY_SESSIONS, todaySessions)
            .putInt(KEY_GOAL_SESSIONS,  goalSessions)
            .putInt(KEY_STREAK_DAYS,    streakDays)
            .apply()
    }

    // ── Read (called from widget process) ─────────────────────────────────────

    fun load(context: Context): WidgetData {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return WidgetData(
            todaySeconds  = prefs.getInt(KEY_TODAY_SECONDS,  0),
            todaySessions = prefs.getInt(KEY_TODAY_SESSIONS, 0),
            goalSessions  = prefs.getInt(KEY_GOAL_SESSIONS,  4),
            streakDays    = prefs.getInt(KEY_STREAK_DAYS,    0),
        )
    }

    // ── Trigger widget redraw ─────────────────────────────────────────────────

    /**
     * Broadcasts an update request to all active Ontime widgets.
     * Call this after save() so the widget immediately reflects new data.
     */
    fun notifyWidgetUpdate(context: Context) {
        val manager = AppWidgetManager.getInstance(context.applicationContext)
        val ids     = manager.getAppWidgetIds(
            ComponentName(context.applicationContext, FocusWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return   // no widgets placed, nothing to do

        val intent = Intent(context, FocusWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }
}

// ─── Widget data model ────────────────────────────────────────────────────────

data class WidgetData(
    val todaySeconds:  Int,
    val todaySessions: Int,
    val goalSessions:  Int,
    val streakDays:    Int,
) {
    val todayMinutes: Int get() = todaySeconds / 60

    val todayFormatted: String get() {
        val h = todayMinutes / 60
        val m = todayMinutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0           -> "${h}h"
            else            -> "${m}m"
        }
    }

    val progressPercent: Int get() =
        if (goalSessions == 0) 0
        else ((todaySessions.toFloat() / goalSessions) * 100)
            .toInt().coerceIn(0, 100)

    val isGoalMet: Boolean get() = todaySessions >= goalSessions
}
