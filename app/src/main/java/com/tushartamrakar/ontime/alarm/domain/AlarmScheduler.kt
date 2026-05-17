package com.tushartamrakar.ontime.alarm.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tushartamrakar.ontime.alarm.data.local.AlarmEntity
import com.tushartamrakar.ontime.alarm.receiver.AlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // ─── Schedule alarm ───────────────────────────────────────────────────────
    fun schedule(alarm: AlarmEntity) {
        val repeatDays = parseRepeatDays(alarm.repeatDays)

        if (repeatDays.isEmpty()) {
            // One-time alarm
            scheduleOneTime(alarm)
        } else {
            // Recurring alarm — schedule for each day
            repeatDays.forEach { day ->
                scheduleWeekly(alarm, day)
            }
        }
    }

    // ─── Schedule one-time alarm ──────────────────────────────────────────────
    private fun scheduleOneTime(alarm: AlarmEntity) {
        val triggerTime = getNextAlarmTime(alarm.hour, alarm.minute, -1)
        val pendingIntent = createPendingIntent(alarm, 0)
        setExactAlarm(triggerTime, pendingIntent)
    }

    // ─── Schedule weekly recurring alarm ─────────────────────────────────────
    private fun scheduleWeekly(alarm: AlarmEntity, dayOfWeek: Int) {
        val triggerTime = getNextAlarmTime(alarm.hour, alarm.minute, dayOfWeek)
        val pendingIntent = createPendingIntent(alarm, dayOfWeek)
        setExactAlarm(triggerTime, pendingIntent)
    }

    // ─── Get next alarm time ──────────────────────────────────────────────────
    private fun getNextAlarmTime(hour: Int, minute: Int, dayOfWeek: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (dayOfWeek != -1) {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                // If this day has already passed this week, schedule for next week
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            } else {
                // One-time: if time passed today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }
        return calendar.timeInMillis
    }

    // ─── Create PendingIntent ─────────────────────────────────────────────────
    private fun createPendingIntent(alarm: AlarmEntity, dayOfWeek: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("ALARM_VIBRATE", alarm.vibrate)
        }

        // Unique request code per alarm per day
        val requestCode = alarm.id * 10 + dayOfWeek

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    // ─── Set exact alarm ──────────────────────────────────────────────────────
    private fun setExactAlarm(triggerTime: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent,
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent,
            )
        }
    }

    // ─── Cancel alarm ─────────────────────────────────────────────────────────
    fun cancel(alarmId: Int) {
        // Cancel for all possible days (0-7)
        for (day in 0..7) {
            val intent = Intent(context, AlarmReceiver::class.java)
            val requestCode = alarmId * 10 + day
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    // ─── Parse repeat days ────────────────────────────────────────────────────
    private fun parseRepeatDays(repeatDays: String): List<Int> {
        if (repeatDays.isBlank()) return emptyList()
        return repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
}