package com.tushartamrakar.ontime.calendar.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.tushartamrakar.ontime.calendar.data.local.CalendarEventEntity
import com.tushartamrakar.ontime.calendar.domain.toReminderItems

object EventReminderScheduler {

    fun schedule(context: Context, event: CalendarEventEntity) {
        val reminders = event.remindersJson.toReminderItems()

        if (reminders.isEmpty()) {
            // ─── Fallback to legacy single reminder ───────────────────────────
            if (event.reminderType == "NONE") return
            scheduleSingle(
                context = context,
                eventId = event.id,
                requestCode = event.id,
                title = event.title,
                description = event.description,
                reminderType = event.reminderType,
                minutesBefore = event.reminderMinutesBefore,
                startTimeMillis = event.startTimeMillis,
                sound = event.reminderSound,
                announceLabel = event.announceLabelOnReminder,
            )
        } else {
            // ─── Schedule each reminder ───────────────────────────────────────
            reminders.forEachIndexed { index, reminder ->
                scheduleSingle(
                    context = context,
                    eventId = event.id,
                    requestCode = event.id * 10 + index, // unique request code per reminder
                    title = event.title,
                    description = event.description,
                    reminderType = reminder.type,
                    minutesBefore = reminder.minutesBefore,
                    startTimeMillis = event.startTimeMillis,
                    sound = reminder.sound,
                    announceLabel = event.announceLabelOnReminder,
                )
            }
        }
    }

    private fun scheduleSingle(
        context: Context,
        eventId: Int,
        requestCode: Int,
        title: String,
        description: String,
        reminderType: String,
        minutesBefore: Int,
        startTimeMillis: Long,
        sound: String,
        announceLabel: Boolean,
    ) {
        val triggerMillis = startTimeMillis - (minutesBefore * 60 * 1000L)
        if (triggerMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, EventReminderReceiver::class.java).apply {
            putExtra("event_id", eventId)
            putExtra("event_title", title)
            putExtra("event_description", description)
            putExtra("reminder_type", reminderType)
            putExtra("reminder_minutes", minutesBefore)
            putExtra("start_time_millis", startTimeMillis)
            putExtra("reminder_sound", sound)
            putExtra("announce_label", announceLabel)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    fun cancel(context: Context, eventId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Cancel up to 3 reminders (index 0,1,2) + legacy single
        (0..3).forEach { index ->
            val requestCode = if (index == 3) eventId else eventId * 10 + index
            val intent = Intent(context, EventReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
