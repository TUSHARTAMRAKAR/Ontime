package com.tushartamrakar.ontime.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tushartamrakar.ontime.alarm.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.startForegroundService(Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_ID", intent.getIntExtra("ALARM_ID", -1))
            putExtra("ALARM_LABEL", intent.getStringExtra("ALARM_LABEL") ?: "Alarm")
            putExtra("ALARM_VIBRATE", intent.getBooleanExtra("ALARM_VIBRATE", true))
            putExtra("ALARM_TASKS", intent.getStringExtra("ALARM_TASKS") ?: "[]")
            putExtra("ALARM_RISE_CHECK_MINUTES", intent.getIntExtra("ALARM_RISE_CHECK_MINUTES", 0))
            putExtra("ALARM_SOUND", intent.getStringExtra("ALARM_SOUND") ?: "alarm_digital_alarm")
            putExtra("ALARM_VOLUME", intent.getFloatExtra("ALARM_VOLUME", 1.0f))
            putExtra("ALARM_GENTLE_WAKE_UP_SECONDS", intent.getIntExtra("ALARM_GENTLE_WAKE_UP_SECONDS", 0))
            putExtra("ALARM_TIME_ANNOUNCEMENT", intent.getBooleanExtra("ALARM_TIME_ANNOUNCEMENT", false))
            putExtra("ALARM_ANNOUNCEMENT_VOICE", intent.getStringExtra("ALARM_ANNOUNCEMENT_VOICE") ?: "female")
            putExtra("ALARM_WEATHER_REMINDER", intent.getBooleanExtra("ALARM_WEATHER_REMINDER", false))
            putExtra("ALARM_LABEL_REMINDER", intent.getBooleanExtra("ALARM_LABEL_REMINDER", false))
            putExtra("ALARM_EXTRA_LOUD", intent.getBooleanExtra("ALARM_EXTRA_LOUD", false))
            putExtra("ALARM_SNOOZE_ENABLED", intent.getBooleanExtra("ALARM_SNOOZE_ENABLED", true))
            putExtra("ALARM_SNOOZE_INTERVAL", intent.getIntExtra("ALARM_SNOOZE_INTERVAL", 5))
            putExtra("ALARM_SNOOZE_LIMIT", intent.getIntExtra("ALARM_SNOOZE_LIMIT", 3))
            putExtra("ALARM_SNOOZE_PROGRESSIVE", intent.getBooleanExtra("ALARM_SNOOZE_PROGRESSIVE", false))
            putExtra("ALARM_SNOOZE_COUNT", intent.getIntExtra("ALARM_SNOOZE_COUNT", 0))
        })
    }
}