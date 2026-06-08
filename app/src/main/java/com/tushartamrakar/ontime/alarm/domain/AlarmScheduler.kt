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

    fun schedule(alarm: AlarmEntity) {
        val repeatDays = parseRepeatDays(alarm.repeatDays)
        if (repeatDays.isEmpty()) scheduleOneTime(alarm)
        else repeatDays.forEach { day -> scheduleWeekly(alarm, day) }
    }

    private fun scheduleOneTime(alarm: AlarmEntity) {
        setExactAlarm(getNextAlarmTime(alarm.hour, alarm.minute, -1), createPendingIntent(alarm, 0))
    }

    private fun scheduleWeekly(alarm: AlarmEntity, dayOfWeek: Int) {
        setExactAlarm(getNextAlarmTime(alarm.hour, alarm.minute, dayOfWeek), createPendingIntent(alarm, dayOfWeek))
    }

    private fun getNextAlarmTime(hour: Int, minute: Int, dayOfWeek: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (dayOfWeek != -1) {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, 1)
            } else {
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
            }
        }.timeInMillis
    }

    private fun createPendingIntent(alarm: AlarmEntity, dayOfWeek: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("ALARM_VIBRATE", alarm.vibrate)
            putExtra("ALARM_TASKS", alarm.tasks)
            putExtra("ALARM_RISE_CHECK_MINUTES", alarm.riseCheckMinutes)
            putExtra("ALARM_SOUND", alarm.sound)
            putExtra("ALARM_VOLUME", alarm.volume)
            putExtra("ALARM_GENTLE_WAKE_UP_SECONDS", alarm.gentleWakeUpSeconds)
            putExtra("ALARM_TIME_ANNOUNCEMENT", alarm.timeAnnouncement)
            putExtra("ALARM_ANNOUNCEMENT_VOICE", alarm.announcementVoice)
            putExtra("ALARM_WEATHER_REMINDER", alarm.weatherReminder)
            putExtra("ALARM_LABEL_REMINDER", alarm.labelReminder)
            putExtra("ALARM_EXTRA_LOUD", alarm.extraLoud)
            putExtra("ALARM_SNOOZE_ENABLED", alarm.snoozeEnabled)
            putExtra("ALARM_SNOOZE_INTERVAL", alarm.snoozeIntervalMinutes)
            putExtra("ALARM_SNOOZE_LIMIT", alarm.snoozeLimit)
            putExtra("ALARM_SNOOZE_PROGRESSIVE", alarm.snoozeProgressiveMode)
            putExtra("ALARM_SNOOZE_COUNT", 0)
        }
        return PendingIntent.getBroadcast(
            context, alarm.id * 10 + dayOfWeek, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun setExactAlarm(triggerTime: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms())
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancel(alarmId: Int) {
        for (day in 0..7) {
            val pendingIntent = PendingIntent.getBroadcast(
                context, alarmId * 10 + day,
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun parseRepeatDays(repeatDays: String): List<Int> {
        if (repeatDays.isBlank()) return emptyList()
        return repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
}