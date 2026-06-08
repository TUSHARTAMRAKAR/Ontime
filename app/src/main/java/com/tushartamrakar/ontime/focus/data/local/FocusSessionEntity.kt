package com.tushartamrakar.ontime.focus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per focus session attempt.
 *
 * Covers both completed and abandoned sessions so stats are honest —
 * a session that was stopped early has wasCompleted = false and
 * endTime = actual stop time, not the intended end.
 *
 * distractionsBlocked is incremented by BlockerAccessibilityService
 * every time a blocked app is detected during this session.
 */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // ─── Timing ───────────────────────────────────────────────────────────────
    val startTime: Long,                         // epoch millis — when session began
    val endTime: Long? = null,                   // null if still running
    val plannedDurationSeconds: Int,             // what the timer was set to
    val actualDurationSeconds: Int = 0,          // how long they actually focused

    // ─── Session metadata ─────────────────────────────────────────────────────
    val type: String = SessionType.WORK.name,    // WORK / SHORT_BREAK / LONG_BREAK
    val taskLabel: String = "",                  // what they planned to focus on
    val wasCompleted: Boolean = false,           // true only if timer reached 0
    val sessionIndexToday: Int = 1,             // which session of the day (1, 2, 3...)

    // ─── Distraction tracking ─────────────────────────────────────────────────
    val distractionsBlocked: Int = 0,           // app-open attempts blocked during this session

    // ─── Sound ────────────────────────────────────────────────────────────────
    val soundUsed: String = AmbientSound.SILENCE.name,
)

enum class SessionType { WORK, SHORT_BREAK, LONG_BREAK }

enum class AmbientSound {
    RAIN, WHITE_NOISE, BROWN_NOISE, FOREST, OCEAN, CAFE, LOFI, SILENCE
}

/** Hour-of-day bucket for heat map — extracted from startTime when saving. */
val FocusSessionEntity.startHour: Int
    get() = java.util.Calendar.getInstance()
        .apply { timeInMillis = startTime }
        .get(java.util.Calendar.HOUR_OF_DAY)

/** Date string "YYYY-MM-DD" — used to join with FocusStreakEntity. */
val FocusSessionEntity.dateString: String
    get() {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = startTime }
        return "%04d-%02d-%02d".format(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH),
        )
    }
