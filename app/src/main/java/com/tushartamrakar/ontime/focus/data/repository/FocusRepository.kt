package com.tushartamrakar.ontime.focus.data.repository

import com.tushartamrakar.ontime.focus.data.local.BlockedAppEntity
import com.tushartamrakar.ontime.focus.data.local.FocusDao
import com.tushartamrakar.ontime.focus.data.local.FocusSettingsEntity
import com.tushartamrakar.ontime.focus.data.local.FocusStreakEntity
import com.tushartamrakar.ontime.focus.data.local.PlannerTaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusRepository @Inject constructor(
    internal val dao: FocusDao,
) {

    // ─── Settings ─────────────────────────────────────────────────────────────

    fun getSettings(): Flow<FocusSettingsEntity> =
        dao.getSettings().map { it ?: FocusSettingsEntity() }

    suspend fun getSettingsOnce(): FocusSettingsEntity =
        dao.getSettingsOnce() ?: FocusSettingsEntity()

    suspend fun saveSettings(settings: FocusSettingsEntity) =
        dao.upsertSettings(settings)

    // ─── Planner tasks ────────────────────────────────────────────────────────

    fun getTasksForToday(): Flow<List<PlannerTaskEntity>> =
        dao.getTasksForDate(today())

    fun getTasksForDate(date: String): Flow<List<PlannerTaskEntity>> =
        dao.getTasksForDate(date)

    suspend fun addTask(title: String, estimatedPomodoros: Int = 1): Long =
        dao.insertTask(
            PlannerTaskEntity(
                title              = title,
                date               = today(),
                estimatedPomodoros = estimatedPomodoros,
                sortOrder          = 0,
            )
        )

    suspend fun updateTask(task: PlannerTaskEntity)              = dao.updateTask(task)
    suspend fun deleteTask(id: Int)                              = dao.deleteTask(id)
    suspend fun setTaskCompleted(id: Int, completed: Boolean)    = dao.setTaskCompleted(id, completed)
    suspend fun incrementTaskPomodoro(label: String)             = dao.incrementTaskPomodoro(label, today())
    fun getTodayCompletedTaskCount(): Flow<Int>                  = dao.getCompletedTaskCountForDate(today())
    fun getTodayTotalTaskCount(): Flow<Int>                      = dao.getTaskCountForDate(today())

    // ─── Blocked apps ─────────────────────────────────────────────────────────

    fun getAllBlockedApps(): Flow<List<BlockedAppEntity>>        = dao.getAllBlockedApps()
    fun getEnabledBlockedAppCount(): Flow<Int>                  = dao.getEnabledBlockedAppCount()
    suspend fun upsertBlockedApp(app: BlockedAppEntity)         = dao.upsertBlockedApp(app)
    suspend fun upsertBlockedApps(apps: List<BlockedAppEntity>) = dao.upsertBlockedApps(apps)
    suspend fun setAppEnabled(packageName: String, enabled: Boolean) =
        dao.setAppEnabled(packageName, enabled)
    suspend fun deleteBlockedApp(packageName: String)           = dao.deleteBlockedApp(packageName)
    suspend fun getEnabledBlockedAppsOnce(): List<BlockedAppEntity> =
        dao.getEnabledBlockedAppsOnce()

    // ─── Live stats (for FocusScreen header row) ──────────────────────────────

    fun getTodayFocusSeconds(): Flow<Int>        = dao.getTodayFocusSeconds()
    fun getTodaySessionCount(): Flow<Int>        = dao.getTodayCompletedSessionCount()
    fun getTotalDistractionsBlocked(): Flow<Int> = dao.getTotalDistractionsBlocked()
    fun getRecentStreaks(): Flow<List<FocusStreakEntity>> = dao.getRecentStreaks()

    // ─── Streak calculation ───────────────────────────────────────────────────

    /**
     * Current streak = consecutive days going back from TODAY with
     * sessionsCompleted > 0. A day with zero sessions breaks the chain.
     * If today already has sessions → it counts. If not → we look from yesterday.
     */
    suspend fun getCurrentStreak(): Int = withContext(Dispatchers.Default) {
        val streaks = dao.getAllStreaks()          // DESC order from FocusDao
        if (streaks.isEmpty()) return@withContext 0

        val fmt       = DateTimeFormatter.ISO_LOCAL_DATE
        val today     = LocalDate.now()
        val yesterday = today.minusDays(1)

        // Start from the most recent row — must be today or yesterday to count
        val first = LocalDate.parse(streaks.first().date, fmt)
        var expected = when {
            first == today     -> today
            first == yesterday -> yesterday
            else               -> return@withContext 0
        }

        var count = 0
        for (s in streaks) {
            val d = LocalDate.parse(s.date, fmt)
            if (d == expected && s.sessionsCompleted > 0) {
                count++
                expected = expected.minusDays(1)
            } else break
        }
        count
    }

    /**
     * Longest streak = max consecutive-day run ever where sessionsCompleted > 0.
     */
    suspend fun getLongestStreak(): Int = withContext(Dispatchers.Default) {
        val streaks = dao.getAllStreaks().sortedBy { it.date }  // ASC for this pass
        if (streaks.isEmpty()) return@withContext 0

        val fmt      = DateTimeFormatter.ISO_LOCAL_DATE
        var longest  = 0
        var current  = 0
        var prevDate: LocalDate? = null

        for (s in streaks) {
            if (s.sessionsCompleted == 0) { current = 0; prevDate = null; continue }
            val d             = LocalDate.parse(s.date, fmt)
            val consecutive   = prevDate != null && d == prevDate!!.plusDays(1)
            current           = if (consecutive) current + 1 else 1
            longest           = maxOf(longest, current)
            prevDate          = d
        }
        longest
    }

    // ─── Full stats snapshot (for FocusStatsScreen) ───────────────────────────

    suspend fun computeFullStats(): FocusStats = withContext(Dispatchers.IO) {
        val settings   = getSettingsOnce()
        val todayStr   = today()
        val nowMillis  = System.currentTimeMillis()

        // ── Week start (Monday) ────────────────────────────────────────────
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val weekStart = cal.clone() as java.util.Calendar
        weekStart.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        if (weekStart.timeInMillis > cal.timeInMillis)
            weekStart.add(java.util.Calendar.WEEK_OF_YEAR, -1)

        // ── Month start ────────────────────────────────────────────────────
        val monthStart = cal.clone() as java.util.Calendar
        monthStart.set(java.util.Calendar.DAY_OF_MONTH, 1)

        val sevenDaysAgo = nowMillis - 7L * 24 * 60 * 60 * 1000

        // ── Queries ────────────────────────────────────────────────────────
        val allTimeSecs      = dao.getAllTimeFocusSeconds()
        val weekSecs         = dao.getWeekFocusSeconds(weekStart.timeInMillis)
        val monthSecs        = dao.getMonthFocusSeconds(monthStart.timeInMillis)
        val completionStats  = dao.getCompletionStats()
        val weeklyChart      = dao.getWeeklyDailyStats(sevenDaysAgo)
        val heatmap          = dao.getHourlyHeatmap()
        val currentStreak    = getCurrentStreak()
        val longestStreak    = getLongestStreak()
        val allStreaks        = dao.getAllStreaks()
        val todayStreak      = allStreaks.firstOrNull { it.date == todayStr }
        val allSessions      = dao.getWorkSessionHistory()

        // ── Derived ────────────────────────────────────────────────────────
        val goalMetThisMonth = allStreaks.count { s ->
            s.goalMet && s.date.startsWith(todayStr.take(7))   // same YYYY-MM
        }
        val completionPct  = (completionStats.rate * 100).toInt().coerceIn(0, 100)
        val completedSess  = allSessions.filter { it.wasCompleted }
        val avgMins        = if (completedSess.isEmpty()) 0
                             else completedSess.sumOf { it.actualDurationSeconds } / completedSess.size / 60
        val bestHour       = heatmap.maxByOrNull { it.totalSeconds }?.hour ?: -1
        val todayCount     = weeklyChart.lastOrNull { it.day == todayStr }?.sessionCount ?: 0

        // ── Focus score ────────────────────────────────────────────────────
        val scoreCompletion = (completionPct * 0.40f).toInt()
        val scoreGoal       = (minOf(todayCount, settings.dailyGoalSessions).toFloat()
                               / settings.dailyGoalSessions.coerceAtLeast(1) * 100 * 0.30f).toInt()
        val scoreStreak     = (currentStreak.coerceAtMost(30) / 30f * 100 * 0.30f).toInt()
        val focusScore      = (scoreCompletion + scoreGoal + scoreStreak).coerceIn(0, 100)

        FocusStats(
            todayFocusSeconds          = todayStreak?.totalFocusSeconds ?: 0,
            todaySessionsCompleted     = todayStreak?.sessionsCompleted ?: 0,
            todaySessionsAbandoned     = todayStreak?.sessionsAbandoned ?: 0,
            todayDistractionsBlocked   = todayStreak?.totalDistractionsBlocked ?: 0,
            todayGoalSessions          = settings.dailyGoalSessions,
            weekFocusSeconds           = weekSecs,
            weekSessionsCompleted      = weeklyChart.sumOf { it.sessionCount },
            monthFocusSeconds          = monthSecs,
            monthSessionsCompleted     = 0,
            allTimeFocusSeconds        = allTimeSecs,
            allTimeSessionsCompleted   = completionStats.completed,
            allTimeDistractionsBlocked = allSessions.sumOf { it.distractionsBlocked },
            currentStreakDays          = currentStreak,
            longestStreakDays          = longestStreak,
            goalMetDaysThisMonth       = goalMetThisMonth,
            completionRatePct          = completionPct,
            avgSessionMinutes          = avgMins,
            bestFocusHour              = bestHour,
            weeklyDailyStats           = weeklyChart,
            hourlyHeatmap              = heatmap,
            focusScore                 = focusScore,
        )
    }

    private fun today(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
}
