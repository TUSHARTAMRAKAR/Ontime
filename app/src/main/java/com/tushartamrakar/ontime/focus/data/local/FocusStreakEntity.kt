package com.tushartamrakar.ontime.focus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per day — upserted at the end of every completed WORK session.
 *
 * date is the primary key so there's always exactly one row per calendar day.
 * The streak (how many consecutive days) is computed at query time by
 * counting how many consecutive dates going back from today have goalMet = true.
 */
@Entity(tableName = "focus_streaks")
data class FocusStreakEntity(
    @PrimaryKey
    val date: String,                      // "YYYY-MM-DD" — primary key

    // ─── Daily totals ─────────────────────────────────────────────────────────
    val sessionsCompleted: Int = 0,        // completed WORK sessions this day
    val sessionsAbandoned: Int = 0,        // abandoned sessions (for completion rate)
    val totalFocusSeconds: Int = 0,        // total seconds of actual focus
    val totalDistractionsBlocked: Int = 0, // total blocked attempts this day

    // ─── Goal tracking ────────────────────────────────────────────────────────
    val dailyGoalSessions: Int = 4,        // snapshot of goal at time of last update
    val goalMet: Boolean = false,          // true when sessionsCompleted >= dailyGoalSessions
)

/** Convenience: focus minutes (rounded) for display. */
val FocusStreakEntity.focusMinutes: Int get() = totalFocusSeconds / 60
