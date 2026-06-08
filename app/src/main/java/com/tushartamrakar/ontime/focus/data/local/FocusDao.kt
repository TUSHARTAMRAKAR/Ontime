package com.tushartamrakar.ontime.focus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {

    // ─── Sessions ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Update
    suspend fun updateSession(session: FocusSessionEntity)

    /** Live stream — used by FocusScreen to show most recent session state. */
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC LIMIT 50")
    fun getRecentSessions(): Flow<List<FocusSessionEntity>>

    /** One-shot — for stats screen history list. */
    @Query("SELECT * FROM focus_sessions WHERE type = 'WORK' ORDER BY startTime DESC LIMIT 100")
    suspend fun getWorkSessionHistory(): List<FocusSessionEntity>

    /** Today's completed WORK sessions count — for stats row in FocusScreen. */
    @Query("""
        SELECT COUNT(*) FROM focus_sessions
        WHERE type = 'WORK'
          AND wasCompleted = 1
          AND date(startTime / 1000, 'unixepoch', 'localtime') = date('now', 'localtime')
    """)
    fun getTodayCompletedSessionCount(): Flow<Int>

    /** Total focus seconds TODAY — for stats row display. */
    @Query("""
        SELECT COALESCE(SUM(actualDurationSeconds), 0) FROM focus_sessions
        WHERE type = 'WORK'
          AND date(startTime / 1000, 'unixepoch', 'localtime') = date('now', 'localtime')
    """)
    fun getTodayFocusSeconds(): Flow<Int>

    /** Total focus seconds this week (Mon–today). */
    @Query("""
        SELECT COALESCE(SUM(actualDurationSeconds), 0) FROM focus_sessions
        WHERE type = 'WORK'
          AND startTime >= :weekStartMillis
    """)
    suspend fun getWeekFocusSeconds(weekStartMillis: Long): Int

    /** Total focus seconds this month. */
    @Query("""
        SELECT COALESCE(SUM(actualDurationSeconds), 0) FROM focus_sessions
        WHERE type = 'WORK'
          AND startTime >= :monthStartMillis
    """)
    suspend fun getMonthFocusSeconds(monthStartMillis: Long): Int

    /** All-time total focus seconds. */
    @Query("SELECT COALESCE(SUM(actualDurationSeconds), 0) FROM focus_sessions WHERE type = 'WORK'")
    suspend fun getAllTimeFocusSeconds(): Int

    /** Sessions per day for the last 7 days — drives the weekly bar chart. */
    @Query("""
        SELECT date(startTime / 1000, 'unixepoch', 'localtime') as day,
               SUM(actualDurationSeconds) as totalSeconds,
               COUNT(*) as sessionCount
        FROM focus_sessions
        WHERE type = 'WORK' AND wasCompleted = 1
          AND startTime >= :sevenDaysAgoMillis
        GROUP BY day
        ORDER BY day ASC
    """)
    suspend fun getWeeklyDailyStats(sevenDaysAgoMillis: Long): List<DailyStatRow>

    /** Hour-of-day distribution — drives the heat map on stats screen. */
    @Query("""
        SELECT (startTime / 3600000 % 24) as hour,
               COUNT(*) as sessionCount,
               COALESCE(SUM(actualDurationSeconds), 0) as totalSeconds
        FROM focus_sessions
        WHERE type = 'WORK' AND wasCompleted = 1
        GROUP BY hour
        ORDER BY hour ASC
    """)
    suspend fun getHourlyHeatmap(): List<HourlyStatRow>

    /** Total distractions blocked all time. */
    @Query("SELECT COALESCE(SUM(distractionsBlocked), 0) FROM focus_sessions")
    fun getTotalDistractionsBlocked(): Flow<Int>

    /** Completion rate — completed / total WORK sessions. */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN wasCompleted = 1 THEN 1 ELSE 0 END) as completed
        FROM focus_sessions WHERE type = 'WORK'
    """)
    suspend fun getCompletionStats(): CompletionStatRow

    // ─── Streaks ──────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStreak(streak: FocusStreakEntity)

    @Query("SELECT * FROM focus_streaks WHERE date = :date")
    suspend fun getStreakForDate(date: String): FocusStreakEntity?

    /** All streak rows ordered newest first — for current streak calculation. */
    @Query("SELECT * FROM focus_streaks ORDER BY date DESC")
    suspend fun getAllStreaks(): List<FocusStreakEntity>

    /** Live — so FocusScreen streak banner updates in real time. */
    @Query("SELECT * FROM focus_streaks ORDER BY date DESC LIMIT 30")
    fun getRecentStreaks(): Flow<List<FocusStreakEntity>>

    // ─── Blocked apps ─────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockedApp(app: BlockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockedApps(apps: List<BlockedAppEntity>)

    @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
    fun getAllBlockedApps(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE isEnabled = 1")
    suspend fun getEnabledBlockedAppsOnce(): List<BlockedAppEntity>

    @Query("UPDATE blocked_apps SET isEnabled = :enabled WHERE packageName = :packageName")
    suspend fun setAppEnabled(packageName: String, enabled: Boolean)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun deleteBlockedApp(packageName: String)

    @Query("SELECT COUNT(*) FROM blocked_apps WHERE isEnabled = 1")
    fun getEnabledBlockedAppCount(): Flow<Int>

    // ─── Planner tasks ────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: PlannerTaskEntity): Long

    @Update
    suspend fun updateTask(task: PlannerTaskEntity)

    @Query("DELETE FROM planner_tasks WHERE id = :id")
    suspend fun deleteTask(id: Int)

    @Query("SELECT * FROM planner_tasks WHERE date = :date ORDER BY sortOrder ASC, createdAt ASC")
    fun getTasksForDate(date: String): Flow<List<PlannerTaskEntity>>

    @Query("UPDATE planner_tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setTaskCompleted(id: Int, completed: Boolean)

    @Query("UPDATE planner_tasks SET completedPomodoros = completedPomodoros + 1 WHERE title = :label AND date = :date")
    suspend fun incrementTaskPomodoro(label: String, date: String)

    @Query("SELECT COUNT(*) FROM planner_tasks WHERE date = :date")
    fun getTaskCountForDate(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM planner_tasks WHERE date = :date AND isCompleted = 1")
    fun getCompletedTaskCountForDate(date: String): Flow<Int>

    // ─── Settings ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: FocusSettingsEntity)

    @Query("SELECT * FROM focus_settings WHERE id = 1")
    fun getSettings(): Flow<FocusSettingsEntity?>

    @Query("SELECT * FROM focus_settings WHERE id = 1")
    suspend fun getSettingsOnce(): FocusSettingsEntity?
}

// ─── Lightweight result classes for aggregate queries ─────────────────────────

data class DailyStatRow(
    val day: String,                   // "YYYY-MM-DD"
    val totalSeconds: Int,
    val sessionCount: Int,
)

data class HourlyStatRow(
    val hour: Int,                     // 0-23
    val sessionCount: Int,
    val totalSeconds: Int,
)

data class CompletionStatRow(
    val total: Int,
    val completed: Int,
) {
    val rate: Float get() = if (total == 0) 0f else completed.toFloat() / total
}
