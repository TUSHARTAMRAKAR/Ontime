package com.tushartamrakar.ontime.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tushartamrakar.ontime.alarm.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val vibrate = intent.getBooleanExtra("ALARM_VIBRATE", true)

        // Start the AlarmService as a foreground service
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
            putExtra("ALARM_VIBRATE", vibrate)
        }

        context.startForegroundService(serviceIntent)
    }
}