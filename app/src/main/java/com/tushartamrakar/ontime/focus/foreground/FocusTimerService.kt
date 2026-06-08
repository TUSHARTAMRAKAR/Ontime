package com.tushartamrakar.ontime.focus.foreground

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.tushartamrakar.ontime.MainActivity
import com.tushartamrakar.ontime.R
import com.tushartamrakar.ontime.core.navigation.DeepLinkHandler
import com.tushartamrakar.ontime.focus.accessibility.FocusSessionPrefs
import com.tushartamrakar.ontime.widget.FocusWidgetDataStore
import com.tushartamrakar.ontime.focus.data.local.AmbientSound
import com.tushartamrakar.ontime.focus.data.local.FocusSessionEntity
import com.tushartamrakar.ontime.focus.data.local.FocusSettingsEntity
import com.tushartamrakar.ontime.focus.data.local.SessionType
import com.tushartamrakar.ontime.focus.data.repository.FocusRepository
import com.tushartamrakar.ontime.focus.receiver.PowerStateReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class FocusTimerService : Service() {

    @Inject lateinit var repository:       FocusRepository
    @Inject lateinit var focusWebBlocklist: com.tushartamrakar.ontime.focus.blocker.FocusWebBlocklist

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    inner class FocusTimerBinder : Binder() {
        fun getService(): FocusTimerService = this@FocusTimerService
    }
    private val binder = FocusTimerBinder()

    private lateinit var soundPlayer: AmbientSoundPlayer

    private var settings: FocusSettingsEntity = FocusSettingsEntity()
    private var currentSessionStartTime: Long = 0L
    private var currentSessionIndex: Int = 1
    private var distractionsThisSession: Int = 0
    private var currentTaskLabel: String = ""
    private var currentSound: AmbientSound = AmbientSound.SILENCE

    private var dndEnabledByUs: Boolean = false

    // ── Power state receiver ──────────────────────────────────────────────────
    private val powerReceiver = PowerStateReceiver().apply {
        onScreenOff = {
            // Auto-pause when screen turns off during a WORK phase
            val current = _timerState.value
            if (current is FocusTimerState.Running && current.phase == SessionType.WORK) {
                pauseSession()
                Log.d(TAG, "Session auto-paused — screen turned off")
            }
        }
        onShutdown = {
            // Persist any in-flight session before the device dies
            val current = _timerState.value
            serviceScope.launch {
                if (current is FocusTimerState.Running && current.phase == SessionType.WORK) {
                    saveSession(
                        durationSeconds = current.totalSeconds - current.secondsLeft,
                        plannedSeconds  = current.totalSeconds,
                        phase           = current.phase,
                        wasCompleted    = false,
                    )
                    Log.d(TAG, "Session auto-saved before shutdown")
                }
            }
            FocusSessionPrefs.setSessionActive(applicationContext, false)
        }
    }

    companion object {
        private const val TAG = "FocusTimerService"

        const val CHANNEL_ID       = "ontime_focus_timer"
        const val CHANNEL_ALARM_ID = "ontime_alarms"        // bypasses DND
        const val NOTIF_ID         = 7001
        const val NOTIF_ALARM_ID   = 7002

        // Intent actions
        const val ACTION_START       = "focus.START"
        const val ACTION_PAUSE       = "focus.PAUSE"
        const val ACTION_RESUME      = "focus.RESUME"
        const val ACTION_STOP        = "focus.STOP"
        const val ACTION_SKIP        = "focus.SKIP"
        const val ACTION_FORCE_STOP  = "focus.FORCE_STOP"  // bypasses strict mode

        // Intent extras
        const val EXTRA_TASK_LABEL           = "focus.TASK_LABEL"
        const val EXTRA_SOUND                = "focus.SOUND"
        const val EXTRA_WORK_MINUTES         = "focus.WORK_MINUTES"
        const val EXTRA_SHORT_BREAK_MINUTES  = "focus.SHORT_BREAK_MINUTES"
        const val EXTRA_SESSIONS_BEFORE_LONG = "focus.SESSIONS_BEFORE_LONG"

        private val _timerState = MutableStateFlow<FocusTimerState>(FocusTimerState.Idle)
        val timerState: StateFlow<FocusTimerState> = _timerState.asStateFlow()

        // Emits once when the user hits their daily session goal.
        // OntimeApp observes this and shows CelebrationOverlay.
        // Reset to null after the user dismisses the overlay.
        val celebrationEvent = MutableStateFlow<com.tushartamrakar.ontime.focus.presentation.CelebrationData?>(null)
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        soundPlayer = AmbientSoundPlayer(this)
        createNotificationChannel()

        // Observe settings changes
        serviceScope.launch {
            repository.dao.getSettings().collect { s ->
                settings = s ?: FocusSettingsEntity()
            }
        }

        // Register power state receiver (must be dynamic — ACTION_SCREEN_OFF
        // cannot be received by manifest-declared receivers)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SHUTDOWN)
        }
        registerReceiver(powerReceiver, filter)

        // Safety: clear any stale session state from a previous crash
        FocusSessionPrefs.setSessionActive(applicationContext, false)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val label = intent.getStringExtra(EXTRA_TASK_LABEL) ?: ""
                val sound = intent.getStringExtra(EXTRA_SOUND)
                    ?.let { runCatching { AmbientSound.valueOf(it) }.getOrNull() }
                    ?: AmbientSound.SILENCE
                val workMinOverride    = intent.getIntExtra(EXTRA_WORK_MINUTES, -1).takeIf { it > 0 }
                val shortBreakOverride = intent.getIntExtra(EXTRA_SHORT_BREAK_MINUTES, -1).takeIf { it > 0 }
                val sessionsOverride   = intent.getIntExtra(EXTRA_SESSIONS_BEFORE_LONG, -1).takeIf { it > 0 }
                startWorkSession(label, sound, workMinOverride, shortBreakOverride, sessionsOverride)
            }
            ACTION_PAUSE      -> pauseSession()
            ACTION_RESUME     -> resumeSession()
            ACTION_STOP       -> {
                val current = _timerState.value
                if (settings.strictMode &&
                    current is FocusTimerState.Running &&
                    current.phase == SessionType.WORK
                ) {
                    Log.d(TAG, "Stop blocked by strict mode — use FORCE_STOP to override")
                    return START_NOT_STICKY
                }
                stopSession()
            }
            ACTION_FORCE_STOP -> stopSession()
            ACTION_SKIP       -> skipPhase()
            "focus.INCREMENT_DISTRACTIONS" -> incrementDistractions()
        }
        return START_NOT_STICKY
    }

    // ─── Core session control ─────────────────────────────────────────────────

    fun startWorkSession(
        taskLabel: String = "",
        sound: AmbientSound = AmbientSound.SILENCE,
        workMinutesOverride: Int? = null,
        shortBreakOverride: Int? = null,
        sessionsOverride: Int? = null,
    ) {
        timerJob?.cancel()
        currentTaskLabel        = taskLabel
        currentSound            = sound
        distractionsThisSession = 0
        currentSessionStartTime = System.currentTimeMillis()

        if (workMinutesOverride != null) settings = settings.copy(workMinutes = workMinutesOverride)
        if (shortBreakOverride  != null) settings = settings.copy(shortBreakMinutes = shortBreakOverride)
        if (sessionsOverride    != null) settings = settings.copy(sessionsBeforeLongBreak = sessionsOverride)

        // ── Notify AccessibilityService that a session is now active ──────────
        FocusSessionPrefs.setSessionActive(applicationContext, true)
        pushBlockedPackagesToPrefs()

        // Start VPN web blocker if focus web blocking is enabled
        if (focusWebBlocklist.isEnabled) {
            startService(
                android.content.Intent(applicationContext,
                    com.tushartamrakar.ontime.focus.blocker.AdultContentVpnService::class.java
                ).apply {
                    action = com.tushartamrakar.ontime.focus.blocker.AdultContentVpnService.ACTION_START
                }
            )
            Log.d(TAG, "VPN web blocker activated for focus session")
        }

        enableDnd()
        soundPlayer.play(sound)
        startCountdown(
            durationSeconds = settings.workMinutes * 60,
            phase           = SessionType.WORK,
        )
        Log.d(TAG, "Work session started — $taskLabel — ${settings.workMinutes}min")
    }

    fun pauseSession() {
        val current = _timerState.value as? FocusTimerState.Running ?: return
        timerJob?.cancel()
        soundPlayer.pause()
        _timerState.value = FocusTimerState.Paused(
            secondsLeft         = current.secondsLeft,
            totalSeconds        = current.totalSeconds,
            phase               = current.phase,
            sessionIndex        = current.sessionIndex,
            taskLabel           = current.taskLabel,
            distractionsBlocked = current.distractionsBlocked,
        )
        updateNotification("Paused — ${formatTime(current.secondsLeft)} left")
        Log.d(TAG, "Paused at ${current.secondsLeft}s")
    }

    fun resumeSession() {
        val paused = _timerState.value as? FocusTimerState.Paused ?: return
        soundPlayer.resume()
        startCountdown(
            durationSeconds = paused.secondsLeft,
            totalSeconds    = paused.totalSeconds,
            phase           = paused.phase,
        )
        Log.d(TAG, "Resumed from ${paused.secondsLeft}s")
    }

    fun stopSession() {
        timerJob?.cancel()
        val current = _timerState.value
        serviceScope.launch {
            if (current is FocusTimerState.Running || current is FocusTimerState.Paused) {
                val secondsLeft = when (current) {
                    is FocusTimerState.Running -> current.secondsLeft
                    is FocusTimerState.Paused  -> current.secondsLeft
                    else -> 0
                }
                val total = when (current) {
                    is FocusTimerState.Running -> current.totalSeconds
                    is FocusTimerState.Paused  -> current.totalSeconds
                    else -> 0
                }
                val phase = when (current) {
                    is FocusTimerState.Running -> current.phase
                    is FocusTimerState.Paused  -> current.phase
                    else -> SessionType.WORK
                }
                if (phase == SessionType.WORK) {
                    saveSession(
                        durationSeconds = total - secondsLeft,
                        plannedSeconds  = total,
                        phase           = phase,
                        wasCompleted    = false,
                    )
                }
            }
        }

        // ── Clear accessibility session state ─────────────────────────────────
        FocusSessionPrefs.setSessionActive(applicationContext, false)

        soundPlayer.stop()
        disableDnd()
        _timerState.value = FocusTimerState.Idle
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "Session stopped")
    }

    fun skipPhase() {
        timerJob?.cancel()
        val current = _timerState.value
        val phase = when (current) {
            is FocusTimerState.Running -> current.phase
            is FocusTimerState.Paused  -> current.phase
            else -> return
        }
        advanceToNextPhase(completedPhase = phase, wasCompleted = false)
    }

    fun incrementDistractions() {
        distractionsThisSession++
        val current = _timerState.value
        if (current is FocusTimerState.Running) {
            _timerState.value = current.copy(distractionsBlocked = distractionsThisSession)
        }
    }

    // ─── Countdown loop ───────────────────────────────────────────────────────

    private fun startCountdown(
        durationSeconds: Int,
        totalSeconds: Int = durationSeconds,
        phase: SessionType = SessionType.WORK,
    ) {
        startForeground(NOTIF_ID, buildNotification(phase, durationSeconds))
        timerJob = serviceScope.launch {
            var secondsLeft = durationSeconds
            while (secondsLeft >= 0) {
                _timerState.value = FocusTimerState.Running(
                    secondsLeft         = secondsLeft,
                    totalSeconds        = totalSeconds,
                    phase               = phase,
                    sessionIndex        = currentSessionIndex,
                    taskLabel           = currentTaskLabel,
                    distractionsBlocked = distractionsThisSession,
                )
                updateNotification(buildNotifText(phase, secondsLeft))
                if (secondsLeft == 0) break
                delay(1000L)
                secondsLeft--
            }
            advanceToNextPhase(completedPhase = phase, wasCompleted = true)
        }
    }

    private fun advanceToNextPhase(completedPhase: SessionType, wasCompleted: Boolean) {
        serviceScope.launch {
            val totalSecs = when (completedPhase) {
                SessionType.WORK        -> settings.workMinutes * 60
                SessionType.SHORT_BREAK -> settings.shortBreakMinutes * 60
                SessionType.LONG_BREAK  -> settings.longBreakMinutes * 60
            }
            if (completedPhase == SessionType.WORK) {
                saveSession(
                    durationSeconds = totalSecs,
                    plannedSeconds  = totalSecs,
                    phase           = completedPhase,
                    wasCompleted    = wasCompleted,
                )
                if (wasCompleted) {
                    updateStreakForToday()
                    updateWidgetData()   // refresh home screen widget
                    currentSessionIndex++
                }
            }

            val nextPhase = when {
                completedPhase != SessionType.WORK -> SessionType.WORK
                wasCompleted && (currentSessionIndex % settings.sessionsBeforeLongBreak == 0) ->
                    SessionType.LONG_BREAK
                else -> SessionType.SHORT_BREAK
            }

            _timerState.value = FocusTimerState.PhaseCompleted(
                completedPhase = completedPhase,
                nextPhase      = nextPhase,
                sessionIndex   = currentSessionIndex,
            )

            // ── Fire alarm notification (bypasses DND if permission granted) ──
            fireAlarmNotification(completedPhase, nextPhase)

            delay(1500L)
            disableDnd()
            soundPlayer.stop()

            val nextDuration = when (nextPhase) {
                SessionType.WORK        -> settings.workMinutes * 60
                SessionType.SHORT_BREAK -> settings.shortBreakMinutes * 60
                SessionType.LONG_BREAK  -> settings.longBreakMinutes * 60
            }
            currentSessionStartTime = System.currentTimeMillis()
            distractionsThisSession = 0

            // Update AccessibilityService: WORK resumes → still active
            // BREAK → accessibility blocking paused (user can use phone on break)
            FocusSessionPrefs.setSessionActive(applicationContext, nextPhase == SessionType.WORK)

            if (nextPhase == SessionType.WORK) {
                enableDnd()
                soundPlayer.play(currentSound)
            }
            startCountdown(durationSeconds = nextDuration, phase = nextPhase)
        }
    }

    // ─── Prefs bridge for AccessibilityService ────────────────────────────────

    /**
     * Reads enabled blocked apps from DB and pushes them to SharedPreferences
     * so OntimeFocusAccessibilityService can check them without DB access.
     */
    private fun pushBlockedPackagesToPrefs() {
        serviceScope.launch {
            try {
                val packages = repository.dao.getAllBlockedApps()
                    .first()
                    .filter { it.isEnabled }
                    .map { it.packageName }
                    .toSet()
                FocusSessionPrefs.setBlockedPackages(applicationContext, packages)
                Log.d(TAG, "Pushed ${packages.size} blocked packages to prefs")
            } catch (e: Exception) {
                Log.w(TAG, "Could not push blocked packages: ${e.message}")
            }
        }
    }

    // ─── DB helpers ───────────────────────────────────────────────────────────

    private suspend fun saveSession(
        durationSeconds: Int,
        plannedSeconds: Int,
        phase: SessionType,
        wasCompleted: Boolean,
    ) {
        repository.dao.insertSession(
            FocusSessionEntity(
                startTime              = currentSessionStartTime,
                endTime                = System.currentTimeMillis(),
                plannedDurationSeconds = plannedSeconds,
                actualDurationSeconds  = durationSeconds.coerceAtLeast(0),
                type                   = phase.name,
                taskLabel              = currentTaskLabel,
                wasCompleted           = wasCompleted,
                sessionIndexToday      = currentSessionIndex,
                distractionsBlocked    = distractionsThisSession,
                soundUsed              = currentSound.name,
            )
        )
        Log.d(TAG, "Session saved: phase=$phase completed=$wasCompleted duration=${durationSeconds}s")
    }

    // ─── Widget update ────────────────────────────────────────────────────────

    /**
     * Reads today's focus stats from the DB and pushes them to the
     * home screen widget via FocusWidgetDataStore (SharedPrefs bridge).
     * Called after every completed WORK session.
     */
    private fun updateWidgetData() {
        serviceScope.launch {
            try {
                val todayMillis = LocalDate.now()
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                val dailyStats  = repository.dao.getWeeklyDailyStats(todayMillis)
                val todayRow    = dailyStats.firstOrNull()
                val settings    = repository.dao.getSettingsOnce()
                val streak      = repository.getCurrentStreak()

                FocusWidgetDataStore.save(
                    context       = applicationContext,
                    todaySeconds  = todayRow?.totalSeconds  ?: 0,
                    todaySessions = todayRow?.sessionCount  ?: 0,
                    goalSessions  = settings?.dailyGoalSessions ?: 4,
                    streakDays    = streak,
                )
                FocusWidgetDataStore.notifyWidgetUpdate(applicationContext)
                Log.d(TAG, "Widget updated: ${todayRow?.totalSeconds}s, ${todayRow?.sessionCount} sessions, ${streak} streak")

                // ── Daily goal celebration ─────────────────────────────────────
                // Fire EXACTLY when sessions == goal (not > goal) so it triggers
                // once per goal hit, not on every subsequent session.
                val sessions = todayRow?.sessionCount ?: 0
                val goal     = settings?.dailyGoalSessions ?: 4
                if (sessions == goal && celebrationEvent.value == null) {
                    celebrationEvent.value = com.tushartamrakar.ontime.focus.presentation.CelebrationData(
                        todaySessions = sessions,
                        goalSessions  = goal,
                        streakDays    = streak,
                        todaySeconds  = todayRow?.totalSeconds ?: 0,
                    )
                    Log.d(TAG, "🎉 Daily goal achieved! Firing celebration.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Widget update failed (non-fatal): ${e.message}")
            }
        }
    }

    private suspend fun updateStreakForToday() {
        val today    = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existing = repository.dao.getStreakForDate(today)
        val newCompleted = (existing?.sessionsCompleted ?: 0) + 1
        val newSeconds   = (existing?.totalFocusSeconds ?: 0) + (settings.workMinutes * 60)
        val goal         = settings.dailyGoalSessions
        repository.dao.upsertStreak(
            (existing ?: com.tushartamrakar.ontime.focus.data.local.FocusStreakEntity(
                date              = today,
                dailyGoalSessions = goal,
            )).copy(
                sessionsCompleted = newCompleted,
                totalFocusSeconds = newSeconds,
                goalMet           = newCompleted >= goal,
                dailyGoalSessions = goal,
            )
        )
    }

    // ─── DND ──────────────────────────────────────────────────────────────────

    private fun enableDnd() {
        if (!settings.enableDndDuringFocus) return
        runCatching {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                dndEnabledByUs = true
                Log.d(TAG, "DND enabled")
            }
        }
    }

    private fun disableDnd() {
        if (!dndEnabledByUs) return
        runCatching {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                dndEnabledByUs = false
                Log.d(TAG, "DND disabled")
            }
        }
    }

    // ─── Alarm notification (DND bypass) ─────────────────────────────────────

    private fun fireAlarmNotification(
        completedPhase: SessionType,
        nextPhase: SessionType,
    ) {
        // Deep link → opens app directly on Focus tab
        val openPendingIntent = android.app.PendingIntent.getActivity(
            this, 10,
            DeepLinkHandler.buildIntent(this, DeepLinkHandler.ROUTE_FOCUS),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val (title, body) = when (completedPhase) {
            SessionType.WORK        -> "🎉 Focus session complete!" to
                                       "Great work. Time for a ${if (nextPhase == SessionType.LONG_BREAK) "long" else "short"} break."
            SessionType.SHORT_BREAK -> "⚡ Break over!" to "Back to work. You've got this."
            SessionType.LONG_BREAK  -> "🔥 Long break done!" to "Recharged and ready — let's focus."
        }

        val notification = Notification.Builder(this, CHANNEL_ALARM_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ALARM_ID, notification)

        Log.d(TAG, "Alarm notification fired for $completedPhase → $nextPhase")
    }

    // ─── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // ── Timer channel (low importance, silent) ────────────────────────────
        val timerChannel = NotificationChannel(
            CHANNEL_ID,
            "Focus Timer",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows your active focus session timer"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }

        // ── Alarm channel (high importance, bypasses DND) ─────────────────────
        // setBypassDnd(true) only takes effect if the user has granted
        // Notification Policy Access via Settings → Apps → Special App Access.
        // DndPermissionManager.hasPermission() checks this before enabling.
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM_ID,
            "Focus Alarms",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description      = "Session end alerts — rings even in Do Not Disturb mode"
            enableVibration(true)
            setShowBadge(true)
            setBypassDnd(nm.isNotificationPolicyAccessGranted)  // only set if granted
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }

        nm.createNotificationChannel(timerChannel)
        nm.createNotificationChannel(alarmChannel)
    }

    private fun buildNotification(phase: SessionType, secondsLeft: Int): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val pauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, FocusTimerService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, FocusTimerService::class.java).apply { action = ACTION_FORCE_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(
                when (phase) {
                    SessionType.WORK        -> "Focus session"
                    SessionType.SHORT_BREAK -> "Short break"
                    SessionType.LONG_BREAK  -> "Long break"
                }
            )
            .setContentText(buildNotifText(phase, secondsLeft))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .addAction(Notification.Action.Builder(null, "Pause", pauseIntent).build())
            .addAction(Notification.Action.Builder(null, "Stop",  stopIntent).build())
            .build()
    }

    private fun updateNotification(text: String) {
        val nm      = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val current = _timerState.value
        val phase   = when (current) {
            is FocusTimerState.Running -> current.phase
            is FocusTimerState.Paused  -> current.phase
            else -> SessionType.WORK
        }
        val secondsLeft = when (current) {
            is FocusTimerState.Running -> current.secondsLeft
            is FocusTimerState.Paused  -> current.secondsLeft
            else -> 0
        }
        runCatching { nm.notify(NOTIF_ID, buildNotification(phase, secondsLeft)) }
    }

    private fun buildNotifText(phase: SessionType, secondsLeft: Int): String {
        val timeStr = formatTime(secondsLeft)
        return when (phase) {
            SessionType.WORK        -> "$timeStr remaining — stay focused"
            SessionType.SHORT_BREAK -> "$timeStr — take a short break"
            SessionType.LONG_BREAK  -> "$timeStr — enjoy your long break"
        }
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }

    // ─── Destroy ──────────────────────────────────────────────────────────────

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        soundPlayer.stop()
        disableDnd()

        // Unregister power receiver
        runCatching { unregisterReceiver(powerReceiver) }

        // Safety: always clear session state on destroy
        FocusSessionPrefs.setSessionActive(applicationContext, false)

        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }
}
