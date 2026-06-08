package com.tushartamrakar.ontime.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.tushartamrakar.ontime.MainActivity
import com.tushartamrakar.ontime.R

/**
 * FocusWidgetProvider
 *
 * AppWidgetProvider is a BroadcastReceiver. Android calls onUpdate() when:
 *  1. The user places the widget on the home screen
 *  2. The updatePeriodMillis interval elapses (every 30 minutes)
 *  3. The app explicitly broadcasts ACTION_APPWIDGET_UPDATE (via FocusWidgetDataStore.notifyWidgetUpdate)
 *
 * IMPORTANT: Widgets use RemoteViews, not Compose.
 * All UI updates go through views.setTextViewText(), views.setProgressBar(), etc.
 */
class FocusWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context:         Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds:    IntArray,
    ) {
        val data = FocusWidgetDataStore.load(context)
        appWidgetIds.forEach { widgetId ->
            renderWidget(context, appWidgetManager, widgetId, data)
        }
    }

    // ─── Render ───────────────────────────────────────────────────────────────

    private fun renderWidget(
        context:  Context,
        manager:  AppWidgetManager,
        widgetId: Int,
        data:     WidgetData,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_focus_medium)

        // ── Focus time ────────────────────────────────────────────────────────
        views.setTextViewText(
            R.id.widget_time,
            data.todayFormatted.ifBlank { "0m" },
        )

        // ── Streak ────────────────────────────────────────────────────────────
        views.setTextViewText(
            R.id.widget_streak,
            if (data.streakDays > 0) "🔥 ${data.streakDays} day streak"
            else "Start your streak",
        )

        // ── Session count ─────────────────────────────────────────────────────
        views.setTextViewText(
            R.id.widget_sessions_text,
            "${data.todaySessions} / ${data.goalSessions} sessions",
        )

        // ── Progress bar ──────────────────────────────────────────────────────
        views.setProgressBar(R.id.widget_progress, 100, data.progressPercent, false)

        // ── CTA button label ──────────────────────────────────────────────────
        views.setTextViewText(
            R.id.widget_start_btn,
            if (data.isGoalMet) "🎯 Daily goal achieved!" else "▶  Start Focus Session",
        )

        // ── Tap anywhere → open app to Focus tab ─────────────────────────────
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "focus")   // MainActivity can handle this extra
        }
        val pendingIntent = PendingIntent.getActivity(
            context, widgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        manager.updateAppWidget(widgetId, views)
    }
}
