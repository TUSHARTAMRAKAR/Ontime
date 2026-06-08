package com.tushartamrakar.ontime.focus.data.repository

import com.tushartamrakar.ontime.focus.data.local.DailyStatRow
import com.tushartamrakar.ontime.focus.data.local.HourlyStatRow

/**
 * Aggregated stats snapshot — passed to FocusStatsScreen as a
 * single StateFlow so the UI does zero calculation, just formats.
 */
data class FocusStats(
    // ── Today ────────────────────────────────────────────────────────────────
    val todayFocusSeconds: Int = 0,
    val todaySessionsCompleted: Int = 0,
    val todaySessionsAbandoned: Int = 0,
    val todayDistractionsBlocked: Int = 0,
    val todayGoalSessions: Int = 4,
    // ── Week ─────────────────────────────────────────────────────────────────
    val weekFocusSeconds: Int = 0,
    val weekSessionsCompleted: Int = 0,
    // ── Month ────────────────────────────────────────────────────────────────
    val monthFocusSeconds: Int = 0,
    val monthSessionsCompleted: Int = 0,
    // ── All time ─────────────────────────────────────────────────────────────
    val allTimeFocusSeconds: Int = 0,
    val allTimeSessionsCompleted: Int = 0,
    val allTimeDistractionsBlocked: Int = 0,
    // ── Streaks ───────────────────────────────────────────────────────────────
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val goalMetDaysThisMonth: Int = 0,
    // ── Quality ───────────────────────────────────────────────────────────────
    val completionRatePct: Int = 0,       // 0-100
    val avgSessionMinutes: Int = 0,
    val bestFocusHour: Int = -1,          // 0-23, -1 = not enough data
    // ── Charts ───────────────────────────────────────────────────────────────
    val weeklyDailyStats: List<DailyStatRow> = emptyList(),
    val hourlyHeatmap: List<HourlyStatRow> = emptyList(),
    // ── Composite score ───────────────────────────────────────────────────────
    // 40% completion rate + 30% today goal progress + 30% streak (capped 30 days)
    val focusScore: Int = 0,
) {
    val todayFocusMinutes: Int   get() = todayFocusSeconds / 60
    val weekFocusMinutes: Int    get() = weekFocusSeconds / 60
    val monthFocusMinutes: Int   get() = monthFocusSeconds / 60
    val allTimeFocusMinutes: Int get() = allTimeFocusSeconds / 60

    /** "2h 35m" formatted string. */
    val todayFocusFormatted: String get() {
        val h = todayFocusMinutes / 60
        val m = todayFocusMinutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    /** 0.0-1.0 progress toward today's session goal. */
    val todayGoalProgress: Float get() =
        if (todayGoalSessions == 0) 0f
        else (todaySessionsCompleted.toFloat() / todayGoalSessions).coerceIn(0f, 1f)

    val isTodayGoalMet: Boolean get() = todaySessionsCompleted >= todayGoalSessions
}

fun emptyFocusStats() = FocusStats()
