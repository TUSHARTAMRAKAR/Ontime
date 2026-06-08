package com.tushartamrakar.ontime.alarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.tushartamrakar.ontime.R
import com.tushartamrakar.ontime.alarm.data.location.LocationHelper
import com.tushartamrakar.ontime.alarm.data.weather.WeatherService
import com.tushartamrakar.ontime.alarm.presentation.AlarmRingActivity
import com.tushartamrakar.ontime.alarm.receiver.AlarmReceiver
import java.util.Calendar
import java.util.Locale

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var tts: TextToSpeech? = null
    private var lastSound = "alarm_digital_alarm"
    private var lastVolume = 1.0f
    private var lastGentleWakeUpSeconds = 0
    private var lastExtraLoud = false
    private val handler = Handler(Looper.getMainLooper())
    private var fadeRunnable: Runnable? = null
    private var extraLoudRunnable: Runnable? = null
    private var heavyToneIndex = 0

    private val heavyTones = listOf(
        "alarm_air_raid_siren", "alarm_buzzer", "alarm_emergency_alert",
        "alarm_fire_alarm", "alarm_klaxon", "alarm_civil_defense",
        "alarm_nuclear_alert", "alarm_submarine", "alarm_tornado_siren",
        "alarm_warning_horn",
    )

    companion object {
        const val CHANNEL_ID = "ontime_alarm_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_DISMISS = "ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE = "ACTION_SNOOZE_ALARM"
        const val ACTION_STOP_SOUND = "ACTION_STOP_SOUND"
        const val ACTION_RESTART_SOUND = "ACTION_RESTART_SOUND"
        const val TTS_UTTERANCE_ID = "alarm_announcement"
        const val EXTRA_LOUD_DELAY_MS = 35_000L
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> { stopAlarm(); return START_NOT_STICKY }
            ACTION_SNOOZE -> { snoozeAlarm(intent); return START_NOT_STICKY }
            ACTION_STOP_SOUND -> {
                fadeRunnable?.let { handler.removeCallbacks(it) }
                extraLoudRunnable?.let { handler.removeCallbacks(it) }
                tts?.stop()
                mediaPlayer?.apply { if (isPlaying) stop(); release() }
                mediaPlayer = null
                vibrator?.cancel()
                return START_NOT_STICKY
            }
            ACTION_RESTART_SOUND -> {
                playAlarmSound(lastSound, lastVolume, lastGentleWakeUpSeconds)
                if (lastExtraLoud) scheduleExtraLoud()
                startVibration()
                return START_NOT_STICKY
            }
        }

        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        val alarmLabel = intent?.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val vibrate = intent?.getBooleanExtra("ALARM_VIBRATE", true) ?: true
        val tasks = intent?.getStringExtra("ALARM_TASKS") ?: "[]"
        val riseCheckMinutes = intent?.getIntExtra("ALARM_RISE_CHECK_MINUTES", 0) ?: 0
        val sound = intent?.getStringExtra("ALARM_SOUND") ?: "alarm_digital_alarm"
        val volume = intent?.getFloatExtra("ALARM_VOLUME", 1.0f) ?: 1.0f
        val gentleWakeUpSeconds = intent?.getIntExtra("ALARM_GENTLE_WAKE_UP_SECONDS", 0) ?: 0
        val timeAnnouncement = intent?.getBooleanExtra("ALARM_TIME_ANNOUNCEMENT", false) ?: false
        val announcementVoice = intent?.getStringExtra("ALARM_ANNOUNCEMENT_VOICE") ?: "female"
        val weatherReminder = intent?.getBooleanExtra("ALARM_WEATHER_REMINDER", false) ?: false
        val labelReminder = intent?.getBooleanExtra("ALARM_LABEL_REMINDER", false) ?: false
        val extraLoud = intent?.getBooleanExtra("ALARM_EXTRA_LOUD", false) ?: false
        val snoozeEnabled = intent?.getBooleanExtra("ALARM_SNOOZE_ENABLED", true) ?: true
        val snoozeInterval = intent?.getIntExtra("ALARM_SNOOZE_INTERVAL", 5) ?: 5
        val snoozeLimit = intent?.getIntExtra("ALARM_SNOOZE_LIMIT", 3) ?: 3
        val snoozeProgressive = intent?.getBooleanExtra("ALARM_SNOOZE_PROGRESSIVE", false) ?: false
        val snoozeCount = intent?.getIntExtra("ALARM_SNOOZE_COUNT", 0) ?: 0

        lastSound = sound
        lastVolume = volume
        lastGentleWakeUpSeconds = gentleWakeUpSeconds
        lastExtraLoud = extraLoud

        acquireWakeLock()
        startForeground(NOTIFICATION_ID, buildNotification(alarmId, alarmLabel, tasks, riseCheckMinutes))
        launchAlarmActivity(
            alarmId, alarmLabel, tasks, riseCheckMinutes,
            snoozeEnabled, snoozeInterval, snoozeLimit, snoozeProgressive, snoozeCount,
        )

        if (vibrate) startVibration()

        val needsAnnouncement = timeAnnouncement || weatherReminder || labelReminder
        if (needsAnnouncement) {
            playAlarmSound(sound, 0.12f, 0)
            buildAndSpeakAnnouncements(
                timeAnnouncement, weatherReminder, labelReminder, alarmLabel, announcementVoice,
                onDone = {
                    fadeVolumeToTarget(volume, gentleWakeUpSeconds)
                    if (extraLoud) scheduleExtraLoud()
                },
            )
        } else {
            playAlarmSound(sound, volume, gentleWakeUpSeconds)
            if (extraLoud) scheduleExtraLoud()
        }

        return START_STICKY
    }

    // ─── Schedule extra loud after 35 seconds ────────────────────────────────
    private fun scheduleExtraLoud() {
        extraLoudRunnable = Runnable { startExtraLoudSequence() }
        handler.postDelayed(extraLoudRunnable!!, EXTRA_LOUD_DELAY_MS)
    }

    // ─── Boost volume + start heavy sequence ─────────────────────────────────
    private fun startExtraLoudSequence() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0
            )
            fadeRunnable?.let { handler.removeCallbacks(it) }
            mediaPlayer?.apply { if (isPlaying) stop(); release() }
            mediaPlayer = null
            heavyToneIndex = 0
            playNextHeavyTone()
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ─── Play heavy tones in sequence ────────────────────────────────────────
    private fun playNextHeavyTone() {
        try {
            val toneName = heavyTones[heavyToneIndex % heavyTones.size]
            val resId = resources.getIdentifier(toneName, "raw", packageName)
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(this, resId)?.apply {
                    isLooping = false
                    setVolume(1.0f, 1.0f)
                    setOnCompletionListener {
                        it.release()
                        mediaPlayer = null
                        heavyToneIndex++
                        playNextHeavyTone()
                    }
                    start()
                }
            } else {
                heavyToneIndex++
                playNextHeavyTone()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ─── Snooze alarm ─────────────────────────────────────────────────────────
    private fun snoozeAlarm(intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val tasks = intent.getStringExtra("ALARM_TASKS") ?: "[]"
        val riseCheckMinutes = intent.getIntExtra("ALARM_RISE_CHECK_MINUTES", 0)
        val sound = intent.getStringExtra("ALARM_SOUND") ?: "alarm_digital_alarm"
        val volume = intent.getFloatExtra("ALARM_VOLUME", 1.0f)
        val gentleWakeUpSeconds = intent.getIntExtra("ALARM_GENTLE_WAKE_UP_SECONDS", 0)
        val timeAnnouncement = intent.getBooleanExtra("ALARM_TIME_ANNOUNCEMENT", false)
        val announcementVoice = intent.getStringExtra("ALARM_ANNOUNCEMENT_VOICE") ?: "female"
        val weatherReminder = intent.getBooleanExtra("ALARM_WEATHER_REMINDER", false)
        val labelReminder = intent.getBooleanExtra("ALARM_LABEL_REMINDER", false)
        val extraLoud = intent.getBooleanExtra("ALARM_EXTRA_LOUD", false)
        val snoozeEnabled = intent.getBooleanExtra("ALARM_SNOOZE_ENABLED", true)
        val snoozeInterval = intent.getIntExtra("ALARM_SNOOZE_INTERVAL", 5)
        val snoozeLimit = intent.getIntExtra("ALARM_SNOOZE_LIMIT", 3)
        val snoozeProgressive = intent.getBooleanExtra("ALARM_SNOOZE_PROGRESSIVE", false)
        val snoozeCount = intent.getIntExtra("ALARM_SNOOZE_COUNT", 0)
        val vibrate = intent.getBooleanExtra("ALARM_VIBRATE", true)

        // ─── Check snooze limit ───────────────────────────────────────────────
        if (snoozeLimit > 0 && snoozeCount >= snoozeLimit) {
            // Limit reached — don't snooze, just stop
            stopAlarm()
            return
        }

        stopAlarm()

        // ─── Calculate interval (progressive mode) ────────────────────────────
        val actualInterval = if (snoozeProgressive && snoozeCount > 0) {
            maxOf(1, snoozeInterval - snoozeCount * 2)
        } else {
            snoozeInterval
        }

        val snoozeTime = System.currentTimeMillis() + (actualInterval * 60 * 1000L)
        val newSnoozeCount = snoozeCount + 1

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
            putExtra("ALARM_TASKS", tasks)
            putExtra("ALARM_RISE_CHECK_MINUTES", riseCheckMinutes)
            putExtra("ALARM_SOUND", sound)
            putExtra("ALARM_VOLUME", volume)
            putExtra("ALARM_GENTLE_WAKE_UP_SECONDS", gentleWakeUpSeconds)
            putExtra("ALARM_TIME_ANNOUNCEMENT", timeAnnouncement)
            putExtra("ALARM_ANNOUNCEMENT_VOICE", announcementVoice)
            putExtra("ALARM_WEATHER_REMINDER", weatherReminder)
            putExtra("ALARM_LABEL_REMINDER", labelReminder)
            putExtra("ALARM_EXTRA_LOUD", extraLoud)
            putExtra("ALARM_VIBRATE", vibrate)
            putExtra("ALARM_SNOOZE_ENABLED", snoozeEnabled)
            putExtra("ALARM_SNOOZE_INTERVAL", snoozeInterval)
            putExtra("ALARM_SNOOZE_LIMIT", snoozeLimit)
            putExtra("ALARM_SNOOZE_PROGRESSIVE", snoozeProgressive)
            putExtra("ALARM_SNOOZE_COUNT", newSnoozeCount)
        }
        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP, snoozeTime,
            PendingIntent.getBroadcast(
                this, alarmId + 1000, snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        )
    }

    // ─── Announcements ────────────────────────────────────────────────────────
    private fun buildAndSpeakAnnouncements(
        timeAnnouncement: Boolean, weatherReminder: Boolean, labelReminder: Boolean,
        alarmLabel: String, announcementVoice: String, onDone: () -> Unit,
    ) {
        Thread {
            val parts = mutableListOf<String>()
            if (timeAnnouncement) parts.add(buildTimeAnnouncementText())
            if (weatherReminder) fetchWeatherFromSavedLocation()?.let { parts.add(it) }
            if (labelReminder && alarmLabel.isNotBlank() && alarmLabel != "Alarm") parts.add(alarmLabel)
            val fullText = parts.joinToString(" ")
            handler.post {
                if (fullText.isNotBlank()) speakText(fullText, announcementVoice, onDone)
                else onDone()
            }
        }.start()
    }

    private fun fetchWeatherFromSavedLocation(): String? {
        return try {
            val location = LocationHelper.getSavedLocation(this) ?: return null
            val data = WeatherService.getWeather(location.first, location.second) ?: return null
            WeatherService.buildWeatherAnnouncement(data)
        } catch (e: Exception) { null }
    }

    private fun buildTimeAnnouncementText(): String {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = cal.get(Calendar.MINUTE)
        val amPm = if (hour < 12) "A M" else "P M"
        val greeting = when (hour) {
            in 5..11 -> "Good Morning!"; in 12..16 -> "Good Afternoon!"
            in 17..23 -> "Good Evening!"; else -> "Wake Up!"
        }
        val dayNames = listOf("", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val monthNames = listOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val minuteText = if (minute == 0) "o'clock" else "$minute minutes"
        return "$greeting Today is ${dayNames[cal.get(Calendar.DAY_OF_WEEK)]}, ${monthNames[cal.get(Calendar.MONTH) + 1]} ${cal.get(Calendar.DAY_OF_MONTH)}. The time is $hour12 hours $minuteText $amPm."
    }

    private fun speakText(text: String, voice: String, onDone: () -> Unit) {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                when (voice) {
                    "male" -> { tts?.setPitch(0.6f); tts?.setSpeechRate(0.9f) }
                    else -> { tts?.setPitch(1.8f); tts?.setSpeechRate(0.9f) }
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) { handler.post { onDone() } }
                    override fun onError(id: String?) { handler.post { onDone() } }
                })
                val params = Bundle()
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, TTS_UTTERANCE_ID)
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, TTS_UTTERANCE_ID)
            } else { handler.post { onDone() } }
        }
    }

    private fun fadeVolumeToTarget(targetVolume: Float, gentleWakeUpSeconds: Int) {
        val totalSteps = 20
        val intervalMs = if (gentleWakeUpSeconds > 0) (gentleWakeUpSeconds * 1000L) / totalSteps else 100L
        var currentStep = 0
        val startVolume = 0.12f
        fadeRunnable = object : Runnable {
            override fun run() {
                if (currentStep <= totalSteps) {
                    val v = startVolume + (currentStep.toFloat() / totalSteps) * (targetVolume - startVolume)
                    mediaPlayer?.setVolume(v, v)
                    currentStep++
                    handler.postDelayed(this, intervalMs)
                }
            }
        }
        handler.post(fadeRunnable!!)
    }

    private fun acquireWakeLock() {
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "Ontime:AlarmWakeLock",
        ).also { it.acquire(10 * 60 * 1000L) }
    }

    private fun launchAlarmActivity(
        alarmId: Int, alarmLabel: String, tasks: String, riseCheckMinutes: Int,
        snoozeEnabled: Boolean, snoozeInterval: Int, snoozeLimit: Int,
        snoozeProgressive: Boolean, snoozeCount: Int,
    ) {
        startActivity(Intent(this, AlarmRingActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
            putExtra("ALARM_TASKS", tasks)
            putExtra("ALARM_RISE_CHECK_MINUTES", riseCheckMinutes)
            putExtra("ALARM_SNOOZE_ENABLED", snoozeEnabled)
            putExtra("ALARM_SNOOZE_INTERVAL", snoozeInterval)
            putExtra("ALARM_SNOOZE_LIMIT", snoozeLimit)
            putExtra("ALARM_SNOOZE_PROGRESSIVE", snoozeProgressive)
            putExtra("ALARM_SNOOZE_COUNT", snoozeCount)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
        })
    }

    private fun playAlarmSound(sound: String = "alarm_digital_alarm", targetVolume: Float = 1.0f, gentleWakeUpSeconds: Int = 0) {
        try {
            val resId = resources.getIdentifier(sound, "raw", packageName)
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(this, resId)?.apply {
                    isLooping = true
                    setVolume(if (gentleWakeUpSeconds > 0) 0f else targetVolume, if (gentleWakeUpSeconds > 0) 0f else targetVolume)
                    start()
                }
            } else {
                val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, uri)
                    setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ALARM).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    isLooping = true; prepare()
                    setVolume(if (gentleWakeUpSeconds > 0) 0f else targetVolume, if (gentleWakeUpSeconds > 0) 0f else targetVolume)
                    start()
                }
            }
            if (gentleWakeUpSeconds > 0) {
                val totalSteps = 100
                val intervalMs = (gentleWakeUpSeconds * 1000L) / totalSteps
                var currentStep = 0
                fadeRunnable = object : Runnable {
                    override fun run() {
                        if (currentStep <= totalSteps) {
                            mediaPlayer?.setVolume((currentStep.toFloat() / totalSteps) * targetVolume, (currentStep.toFloat() / totalSteps) * targetVolume)
                            currentStep++; handler.postDelayed(this, intervalMs)
                        }
                    }
                }
                handler.post(fadeRunnable!!)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 500, 500, 500, 500, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibrator = (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun buildNotification(alarmId: Int, label: String, tasks: String, riseCheckMinutes: Int): Notification {
        val dismissPi = PendingIntent.getService(this, 0, Intent(this, AlarmService::class.java).apply { action = ACTION_DISMISS }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val fullScreenPi = PendingIntent.getActivity(this, 0, Intent(this, AlarmRingActivity::class.java).apply { putExtra("ALARM_ID", alarmId); putExtra("ALARM_LABEL", label); putExtra("ALARM_TASKS", tasks); putExtra("ALARM_RISE_CHECK_MINUTES", riseCheckMinutes); flags = Intent.FLAG_ACTIVITY_NEW_TASK }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher).setContentTitle("⏰ $label").setContentText("Tap to view alarm")
            .setPriority(NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPi, true).addAction(0, "Dismiss", dismissPi)
            .setOngoing(true).setAutoCancel(false).build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Ontime Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alarm notifications"; enableVibration(true); setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun stopAlarm() {
        fadeRunnable?.let { handler.removeCallbacks(it) }; fadeRunnable = null
        extraLoudRunnable?.let { handler.removeCallbacks(it) }; extraLoudRunnable = null
        tts?.apply { stop(); shutdown() }; tts = null
        mediaPlayer?.apply { if (isPlaying) stop(); release() }; mediaPlayer = null
        vibrator?.cancel(); vibrator = null
        wakeLock?.apply { if (isHeld) release() }; wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { super.onDestroy(); stopAlarm() }
}