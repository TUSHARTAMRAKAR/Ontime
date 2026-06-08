package com.tushartamrakar.ontime.focus.presentation

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tushartamrakar.ontime.focus.blocker.AdultContentVpnService
import com.tushartamrakar.ontime.focus.blocker.BlockedAppsManager
import com.tushartamrakar.ontime.focus.data.UsageData
import com.tushartamrakar.ontime.focus.data.UsageStatsHelper
import com.tushartamrakar.ontime.focus.data.local.AmbientSound
import com.tushartamrakar.ontime.focus.data.local.BlockedAppEntity
import com.tushartamrakar.ontime.focus.data.local.CustomPreset
import com.tushartamrakar.ontime.focus.data.local.DailyStatRow
import com.tushartamrakar.ontime.focus.data.local.FocusSettingsEntity
import com.tushartamrakar.ontime.focus.data.local.FocusStreakEntity
import com.tushartamrakar.ontime.focus.data.local.PlannerTaskEntity
import com.tushartamrakar.ontime.focus.data.local.TechniqueType
import com.tushartamrakar.ontime.focus.data.repository.FocusRepository
import com.tushartamrakar.ontime.focus.data.repository.FocusStats
import com.tushartamrakar.ontime.focus.data.repository.emptyFocusStats
import com.tushartamrakar.ontime.focus.foreground.FocusTimerService
import com.tushartamrakar.ontime.focus.foreground.FocusTimerState
import com.tushartamrakar.ontime.focus.foreground.StopwatchService
import com.tushartamrakar.ontime.focus.foreground.StopwatchTimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val repository: FocusRepository,
    private val blockedAppsManager: BlockedAppsManager,
    private val installedAppsLoader: InstalledAppsLoader,
    private val usageStatsHelper: UsageStatsHelper,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // ─── Timer state — collected directly from service companion ─────────────
    val timerState: StateFlow<FocusTimerState> = FocusTimerService.timerState

    /** Stopwatch state — separate from Pomodoro, collected from StopwatchService companion. */
    val stopwatchState: StateFlow<StopwatchTimerState> = StopwatchService.stopwatchState

    // ─── Settings ─────────────────────────────────────────────────────────────
    val settings: StateFlow<FocusSettingsEntity> = repository.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusSettingsEntity())

    /** Current technique type — derived from settings so the UI reacts automatically. */
    val techniqueType: StateFlow<TechniqueType> = settings
        .map { s -> runCatching { TechniqueType.valueOf(s.techniqueType) }.getOrDefault(TechniqueType.POMODORO) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TechniqueType.POMODORO)

    // ─── Usage stats (top pills) ──────────────────────────────────────────────
    private val _usageData = MutableStateFlow(UsageData())
    val usageData: StateFlow<UsageData> = _usageData

    // ─── Live session stats ───────────────────────────────────────────────────
    val todayFocusSeconds: StateFlow<Int> = repository.getTodayFocusSeconds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todaySessionCount: StateFlow<Int> = repository.getTodaySessionCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalDistractionsBlocked: StateFlow<Int> = repository.getTotalDistractionsBlocked()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ─── Streak ───────────────────────────────────────────────────────────────
    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak

    private val _longestStreak = MutableStateFlow(0)
    val longestStreak: StateFlow<Int> = _longestStreak

    // ─── Planner tasks ────────────────────────────────────────────────────────
    val plannerTasksToday: StateFlow<List<PlannerTaskEntity>> =
        repository.getTasksForToday()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayCompletedTaskCount: StateFlow<Int> = repository.getTodayCompletedTaskCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayTotalTaskCount: StateFlow<Int> = repository.getTodayTotalTaskCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ─── Blocked apps ─────────────────────────────────────────────────────────
    val blockedApps: StateFlow<List<BlockedAppEntity>> = repository.getAllBlockedApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledBlockedAppCount: StateFlow<Int> = repository.getEnabledBlockedAppCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ─── Full stats ───────────────────────────────────────────────────────────
    private val _focusStats = MutableStateFlow(emptyFocusStats())
    val focusStats: StateFlow<FocusStats> = _focusStats

    private val _isLoadingStats = MutableStateFlow(false)
    val isLoadingStats: StateFlow<Boolean> = _isLoadingStats

    // ── Monthly data for streak calendar + efficiency trend ───────────────────
    private val _monthlyDailyStats = MutableStateFlow<List<DailyStatRow>>(emptyList())
    val monthlyDailyStats: StateFlow<List<DailyStatRow>> = _monthlyDailyStats

    private val _recentStreaks = MutableStateFlow<List<FocusStreakEntity>>(emptyList())
    val recentStreaks: StateFlow<List<FocusStreakEntity>> = _recentStreaks

    // ─── Installed apps ───────────────────────────────────────────────────────
    private val _installedApps = MutableStateFlow<List<InstalledAppsLoader.AppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppsLoader.AppInfo>> = _installedApps

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps

    // ─── Selected sound for session setup ─────────────────────────────────────
    private val _selectedSound = MutableStateFlow(AmbientSound.SILENCE)
    val selectedSound: StateFlow<AmbientSound> = _selectedSound

    private val _pendingTaskLabel = MutableStateFlow("")
    val pendingTaskLabel: StateFlow<String> = _pendingTaskLabel

    init {
        loadStreaks()
    }

    // ─── Usage stats ──────────────────────────────────────────────────────────

    /** Refresh usage pills — call from FocusScreen LaunchedEffect. */
    fun refreshUsageStats() {
        viewModelScope.launch {
            _usageData.value = usageStatsHelper.buildUsageData(todayFocusSeconds.value)
        }
    }

    fun hasUsagePermission(): Boolean = usageStatsHelper.hasPermission()

    fun openUsagePermissionSettings() = usageStatsHelper.openPermissionSettings()

    // ─── Pomodoro timer controls ──────────────────────────────────────────────

    fun startFocusSession(
        taskLabel: String = "",
        sound: AmbientSound = _selectedSound.value,
        workMinutesOverride: Int? = null,
        shortBreakOverride: Int? = null,
        sessionsBeforeLongBreakOverride: Int? = null,
    ) {
        _pendingTaskLabel.value = taskLabel
        context.startForegroundService(
            Intent(context, FocusTimerService::class.java).apply {
                action = FocusTimerService.ACTION_START
                putExtra(FocusTimerService.EXTRA_TASK_LABEL, taskLabel)
                putExtra(FocusTimerService.EXTRA_SOUND, sound.name)
                workMinutesOverride?.let           { putExtra(FocusTimerService.EXTRA_WORK_MINUTES, it) }
                shortBreakOverride?.let            { putExtra(FocusTimerService.EXTRA_SHORT_BREAK_MINUTES, it) }
                sessionsBeforeLongBreakOverride?.let { putExtra(FocusTimerService.EXTRA_SESSIONS_BEFORE_LONG, it) }
            }
        )
    }

    fun pauseSession() {
        context.startService(Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_PAUSE
        })
    }

    fun resumeSession() {
        context.startService(Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_RESUME
        })
    }

    fun stopSession() {
        context.startService(Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_STOP
        })
        loadStreaks()
    }

    fun forceStopSession() {
        context.startService(Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_FORCE_STOP
        })
        loadStreaks()
    }

    fun skipPhase() {
        context.startService(Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_SKIP
        })
    }

    // ─── Stopwatch controls ───────────────────────────────────────────────────

    fun startStopwatchSession(taskLabel: String = "", sound: AmbientSound = _selectedSound.value) {
        // Stop any active Pomodoro timer before switching to stopwatch
        if (timerState.value !is FocusTimerState.Idle) forceStopSession()
        context.startForegroundService(Intent(context, StopwatchService::class.java).apply {
            action = StopwatchService.ACTION_START
            putExtra(StopwatchService.EXTRA_TASK_LABEL, taskLabel)
            putExtra(StopwatchService.EXTRA_SOUND, sound.name)
        })
    }

    fun pauseStopwatch() {
        context.startService(Intent(context, StopwatchService::class.java).apply {
            action = StopwatchService.ACTION_PAUSE
        })
    }

    fun resumeStopwatch() {
        context.startService(Intent(context, StopwatchService::class.java).apply {
            action = StopwatchService.ACTION_RESUME
        })
    }

    fun stopStopwatch() {
        context.startService(Intent(context, StopwatchService::class.java).apply {
            action = StopwatchService.ACTION_STOP
        })
        loadStreaks()
    }

    // ─── Technique / settings helpers ────────────────────────────────────────

    fun setTechniqueType(type: TechniqueType) {
        viewModelScope.launch {
            repository.saveSettings(settings.value.copy(
                techniqueType    = type.name,
                lastUsedTechnique = type.name,
            ))
        }
    }

    fun applyCustomPreset(preset: CustomPreset) {
        viewModelScope.launch {
            repository.saveSettings(settings.value.copy(
                customPreset      = preset.name,
                workMinutes       = preset.workMin,
                shortBreakMinutes = preset.breakMin,
            ))
        }
    }

    fun setStrictMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSettings(settings.value.copy(strictMode = enabled))
        }
    }

    fun setBlockHomeScreen(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSettings(settings.value.copy(blockHomeScreen = enabled))
        }
    }

    fun adjustWorkTime(deltaMinutes: Int) {
        val current = settings.value
        val newMinutes = (current.workMinutes + deltaMinutes).coerceIn(1, 120)
        viewModelScope.launch {
            repository.saveSettings(current.copy(workMinutes = newMinutes))
        }
    }

    fun adjustShortBreak(deltaMinutes: Int) {
        val current = settings.value
        val newMinutes = (current.shortBreakMinutes + deltaMinutes).coerceIn(1, 60)
        viewModelScope.launch {
            repository.saveSettings(current.copy(shortBreakMinutes = newMinutes))
        }
    }

    fun selectSound(sound: AmbientSound) {
        _selectedSound.value = sound
        viewModelScope.launch {
            repository.saveSettings(settings.value.copy(selectedSound = sound.name))
        }
    }

    fun setPendingTaskLabel(label: String) { _pendingTaskLabel.value = label }

    fun saveSettings(updated: FocusSettingsEntity) {
        viewModelScope.launch {
            repository.saveSettings(updated)
            val wasEnabled = settings.value.adultFilterEnabled
            if (updated.adultFilterEnabled && !wasEnabled) startAdultFilter()
            else if (!updated.adultFilterEnabled && wasEnabled) stopAdultFilter()
        }
    }

    fun toggleAdultFilter(enabled: Boolean) {
        saveSettings(settings.value.copy(adultFilterEnabled = enabled))
    }

    private fun startAdultFilter() {
        context.startService(Intent(context, AdultContentVpnService::class.java).apply {
            action = AdultContentVpnService.ACTION_START
        })
    }

    private fun stopAdultFilter() {
        context.startService(Intent(context, AdultContentVpnService::class.java).apply {
            action = AdultContentVpnService.ACTION_STOP
        })
    }

    // ─── Planner tasks ────────────────────────────────────────────────────────

    fun addPlannerTask(title: String, estimatedPomodoros: Int = 1) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.addTask(title.trim(), estimatedPomodoros) }
    }

    fun toggleTaskComplete(taskId: Int, completed: Boolean) {
        viewModelScope.launch { repository.setTaskCompleted(taskId, completed) }
    }

    fun deletePlannerTask(taskId: Int) {
        viewModelScope.launch { repository.deleteTask(taskId) }
    }

    fun updateTask(task: PlannerTaskEntity) {
        viewModelScope.launch { repository.updateTask(task) }
    }

    // ─── Blocked apps ─────────────────────────────────────────────────────────

    fun toggleBlockedApp(app: BlockedAppEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.setAppEnabled(app.packageName, enabled)
            if (enabled) {
                if (app.blockOnlyDuringFocus) blockedAppsManager.addFocusBlock(app.packageName)
                else blockedAppsManager.addAlwaysBlock(app.packageName)
            } else {
                blockedAppsManager.removeFocusBlock(app.packageName)
                blockedAppsManager.removeAlwaysBlock(app.packageName)
            }
        }
    }

    fun addBlockedApp(app: BlockedAppEntity) {
        viewModelScope.launch {
            repository.upsertBlockedApp(app)
            // Use setFocusBlock/setAlwaysBlock which CLEAR the other set —
            // prevents cross-contamination when user changes ALWAYS ↔ FOCUS_ONLY
            if (app.isEnabled) {
                if (app.blockOnlyDuringFocus) blockedAppsManager.setFocusBlock(app.packageName)
                else                          blockedAppsManager.setAlwaysBlock(app.packageName)
            } else {
                blockedAppsManager.clearBlock(app.packageName)
            }
        }
    }

    fun removeBlockedApp(packageName: String) {
        viewModelScope.launch {
            repository.deleteBlockedApp(packageName)
            blockedAppsManager.clearBlock(packageName)
        }
    }

    // ── Always-On Mode ────────────────────────────────────────────────────────

    private val _alwaysOnMode = MutableStateFlow(blockedAppsManager.alwaysOnMode)
    val alwaysOnMode: StateFlow<Boolean> = _alwaysOnMode

    fun setAlwaysOnMode(enabled: Boolean) {
        blockedAppsManager.alwaysOnMode = enabled
        _alwaysOnMode.value = enabled
    }

    fun loadInstalledApps(forceRefresh: Boolean = false) {
        if (_isLoadingApps.value && !forceRefresh) return
        viewModelScope.launch {
            _isLoadingApps.value = true
            _installedApps.value = installedAppsLoader.getInstalledApps(forceRefresh)
            _isLoadingApps.value = false
        }
    }

    // ─── Full stats ───────────────────────────────────────────────────────────

    fun loadStats() {
        if (_isLoadingStats.value) return
        viewModelScope.launch {
            _isLoadingStats.value = true
            _focusStats.value = repository.computeFullStats()
            _isLoadingStats.value = false
        }
    }

    /** Loads 30 days of daily stats + recent streak records for the calendar. */
    fun loadMonthlyStats() {
        viewModelScope.launch {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            _monthlyDailyStats.value = repository.dao.getWeeklyDailyStats(thirtyDaysAgo)
            _recentStreaks.value     = repository.dao.getAllStreaks()
                .filter { it.date >= java.time.LocalDate.now().minusDays(30)
                    .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) }
        }
    }

    fun loadStreaks() {
        viewModelScope.launch {
            _currentStreak.value = repository.getCurrentStreak()
            _longestStreak.value = repository.getLongestStreak()
        }
    }
}
