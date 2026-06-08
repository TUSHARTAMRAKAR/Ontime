package com.tushartamrakar.ontime.alarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.tushartamrakar.ontime.alarm.presentation.RiseCheckActivity

class RiseCheckService : Service() {

    companion object {
        const val ACTION_SCHEDULE = "ACTION_SCHEDULE_RISE_CHECK"
        const val EXTRA_DELAY_MINUTES = "EXTRA_DELAY_MINUTES"
        const val EXTRA_ALARM_LABEL = "EXTRA_ALARM_LABEL"

        fun schedule(context: Context, delayMinutes: Int, alarmLabel: String) {
            val intent = Intent(context, RiseCheckService::class.java).apply {
                action = ACTION_SCHEDULE
                putExtra(EXTRA_DELAY_MINUTES, delayMinutes)
                putExtra(EXTRA_ALARM_LABEL, alarmLabel)
            }
            context.startService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val delayMinutes = intent?.getIntExtra(EXTRA_DELAY_MINUTES, 5) ?: 5
        val alarmLabel = intent?.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm"

        val triggerTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)

        val riseCheckIntent = Intent(this, RiseCheckActivity::class.java).apply {
            putExtra("ALARM_LABEL", alarmLabel)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            9999,
            riseCheckIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent,
        )

        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}