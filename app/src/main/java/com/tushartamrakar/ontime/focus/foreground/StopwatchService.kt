package com.tushartamrakar.ontime.focus.foreground

import android.content.IntentFilter
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.tushartamrakar.ontime.MainActivity
import com.tushartamrakar.ontime.R
import com.tushartamrakar.ontime.focus.accessibility.FocusSessionPrefs
import com.tushartamrakar.ontime.focus.data.local.AmbientSound
import com.tushartamrakar.ontime.focus.data.local.FocusSessionEntity
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
import javax.inject.Inject

// ─── State ────────────────────────────────────────────────────────────────────

sealed class StopwatchTimerState {
    object Idle : StopwatchTimerState()
    data class Running(val elapsedSeconds: Int, val taskLabel: String = "") : StopwatchTimerState()
    data class Paused(val elapsedSeconds: Int, val taskLabel: String = "") : StopwatchTimerState()
}

// ─── Service ──────────────────────────────────────────────────────────────────

/**
 * Open-ended upward-counting foreground service.
 * Saves the full elapsed time as a completed WORK session on stop.
 * Uses the same AmbientSoundPlayer as FocusTimerService.
 */
@AndroidEntryPoint
class StopwatchService : Service() {

    @Inject lateinit var repository: FocusRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    private lateinit var soundPlayer: AmbientSoundPlayer

    // ── Power state receiver ──────────────────────────────────────────────────
    private val powerReceiver = PowerStateReceiver().apply {
        onScreenOff = {
            // Auto-pause when screen turns off during a running stopwatch
            val current = _stopwatchState.value
            if (current is StopwatchTimerState.Running) {
                pauseCounting()
                Log.d(TAG, "Stopwatch auto-paused — screen turned off")
            }
        }
        onShutdown = {
            // Save session before device shuts down
            val current = _stopwatchState.value
            val elapsed = when (current) {
                is StopwatchTimerState.Running -> current.elapsedSeconds
                is StopwatchTimerState.Paused  -> current.elapsedSeconds
                else -> 0
            }
            if (elapsed > 0) {
                serviceScope.launch {
                    repository.dao.insertSession(
                        FocusSessionEntity(
                            startTime              = sessionStartTime,
                            endTime                = System.currentTimeMillis(),
                            plannedDurationSeconds = elapsed,
                            actualDurationSeconds  = elapsed,
                            type                   = "WORK",
                            taskLabel              = currentTaskLabel,
                            wasCompleted           = false,
                            sessionIndexToday      = 1,
                            distractionsBlocked    = 0,
                            soundUsed              = currentSound.name,
                        )
                    )
                    Log.d(TAG, "Stopwatch session auto-saved before shutdown: ${elapsed}s")
                }
            }
            FocusSessionPrefs.setSessionActive(applicationContext, false)
        }
    }

    private var sessionStartTime: Long = 0L
    private var currentTaskLabel: String = ""
    private var currentSound: AmbientSound = AmbientSound.SILENCE

    companion object {
        private const val TAG = "StopwatchService"

        const val CHANNEL_ID = "ontime_stopwatch"
        const val NOTIF_ID   = 7002

        const val ACTION_START  = "stopwatch.START"
        const val ACTION_PAUSE  = "stopwatch.PAUSE"
        const val ACTION_RESUME = "stopwatch.RESUME"
        const val ACTION_STOP   = "stopwatch.STOP"

        const val EXTRA_TASK_LABEL = "sw.TASK_LABEL"
        const val EXTRA_SOUND      = "sw.SOUND"

        /** App-wide StateFlow — collected by FocusViewModel without binding. */
        private val _stopwatchState = MutableStateFlow<StopwatchTimerState>(StopwatchTimerState.Idle)
        val stopwatchState: StateFlow<StopwatchTimerState> = _stopwatchState.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        soundPlayer = AmbientSoundPlayer(this)
        createNotificationChannel()

        // Register power receiver dynamically (ACTION_SCREEN_OFF needs dynamic registration)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SHUTDOWN)
        }
        registerReceiver(powerReceiver, filter)

        // Clear any stale session state from a previous crash
        FocusSessionPrefs.setSessionActive(applicationContext, false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START  -> {
                val label = intent.getStringExtra(EXTRA_TASK_LABEL) ?: ""
                val sound = intent.getStringExtra(EXTRA_SOUND)
                    ?.let { runCatching { AmbientSound.valueOf(it) }.getOrNull() }
                    ?: AmbientSound.SILENCE
                startCounting(label, sound)
            }
            ACTION_PAUSE  -> pauseCounting()
            ACTION_RESUME -> resumeCounting()
            ACTION_STOP   -> stopCounting()
        }
        return START_NOT_STICKY
    }

    // ─── Control ──────────────────────────────────────────────────────────────

    private fun startCounting(taskLabel: String, sound: AmbientSound) {
        tickJob?.cancel()
        currentTaskLabel = taskLabel
        currentSound     = sound
        sessionStartTime = System.currentTimeMillis()

        // Notify AccessibilityService — blocked apps now active
        FocusSessionPrefs.setSessionActive(applicationContext, true)
        pushBlockedPackagesToPrefs()

        soundPlayer.play(sound)
        beginTick(startElapsed = 0)
        Log.d(TAG, "Stopwatch started — label='$taskLabel'")
    }

    private fun pauseCounting() {
        val current = _stopwatchState.value as? StopwatchTimerState.Running ?: return
        tickJob?.cancel()
        soundPlayer.pause()
        _stopwatchState.value = StopwatchTimerState.Paused(current.elapsedSeconds, current.taskLabel)
        updateNotification(current.elapsedSeconds)
        Log.d(TAG, "Stopwatch paused at ${current.elapsedSeconds}s")
    }

    private fun resumeCounting() {
        val paused = _stopwatchState.value as? StopwatchTimerState.Paused ?: return
        soundPlayer.resume()
        beginTick(startElapsed = paused.elapsedSeconds)
        Log.d(TAG, "Stopwatch resumed from ${paused.elapsedSeconds}s")
    }

    private fun stopCounting() {
        tickJob?.cancel()
        val current = _stopwatchState.value
        val elapsed = when (current) {
            is StopwatchTimerState.Running -> current.elapsedSeconds
            is StopwatchTimerState.Paused  -> current.elapsedSeconds
            else -> 0
        }
        serviceScope.launch {
            if (elapsed > 0) {
                repository.dao.insertSession(
                    FocusSessionEntity(
                        startTime              = sessionStartTime,
                        endTime                = System.currentTimeMillis(),
                        plannedDurationSeconds = elapsed,
                        actualDurationSeconds  = elapsed,
                        type                   = "WORK",
                        taskLabel              = currentTaskLabel,
                        wasCompleted           = true,
                        sessionIndexToday      = 1,
                        distractionsBlocked    = 0,
                        soundUsed              = currentSound.name,
                    )
                )
                Log.d(TAG, "Stopwatch session saved: ${elapsed}s")
            }
        }

        // Clear accessibility session state — blocked apps no longer active
        FocusSessionPrefs.setSessionActive(applicationContext, false)

        soundPlayer.stop()
        _stopwatchState.value = StopwatchTimerState.Idle
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "Stopwatch stopped")
    }

    // ─── Tick loop ────────────────────────────────────────────────────────────

    private fun beginTick(startElapsed: Int) {
        startForeground(NOTIF_ID, buildNotification(startElapsed))
        tickJob = serviceScope.launch {
            var elapsed = startElapsed
            while (true) {
                _stopwatchState.value = StopwatchTimerState.Running(
                    elapsedSeconds = elapsed,
                    taskLabel      = currentTaskLabel,
                )
                if (elapsed % 5 == 0) updateNotification(elapsed)  // update notif every 5s (battery)
                delay(1000L)
                elapsed++
            }
        }
    }

    // ─── Notification ─────────────────────────────────────────────────────────

    private fun updateNotification(elapsed: Int) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { nm.notify(NOTIF_ID, buildNotification(elapsed)) }
    }

    private fun buildNotification(elapsed: Int): Notification {
        val h = elapsed / 3600
        val m = (elapsed % 3600) / 60
        val s = elapsed % 60
        val timeStr = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, StopwatchService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val pauseIntent = PendingIntent.getService(
            this, 2,
            Intent(this, StopwatchService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Stopwatch running")
            .setContentText(timeStr + if (currentTaskLabel.isNotBlank()) " — $currentTaskLabel" else "")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .addAction(Notification.Action.Builder(null, "Pause", pauseIntent).build())
            .addAction(Notification.Action.Builder(null, "Stop",  stopIntent).build())
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Stopwatch",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows your active stopwatch session"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    // ─── Prefs bridge for AccessibilityService ────────────────────────────────

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

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    override fun onDestroy() {
        tickJob?.cancel()
        serviceScope.cancel()
        soundPlayer.stop()
        // Unregister power receiver
        runCatching { unregisterReceiver(powerReceiver) }
        // Safety: always clear session state on destroy
        FocusSessionPrefs.setSessionActive(applicationContext, false)
        super.onDestroy()
        Log.d(TAG, "StopwatchService destroyed")
    }
}
