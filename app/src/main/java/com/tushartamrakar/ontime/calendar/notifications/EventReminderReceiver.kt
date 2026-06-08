package com.tushartamrakar.ontime.calendar.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tushartamrakar.ontime.R
import com.tushartamrakar.ontime.alarm.presentation.AlarmRingActivity
import com.tushartamrakar.ontime.alarm.service.AlarmService
import com.tushartamrakar.ontime.core.navigation.DeepLinkHandler
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class EventReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "ontime_event_reminders"
        const val CHANNEL_NAME = "Event Reminders"
        // ✅ Flag to tell AlarmRingActivity this is an event, not a regular alarm
        const val EXTRA_IS_EVENT_REMINDER = "IS_EVENT_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getIntExtra("event_id", -1)
        val title = intent.getStringExtra("event_title") ?: "Upcoming Event"
        val description = intent.getStringExtra("event_description") ?: ""
        val reminderType = intent.getStringExtra("reminder_type") ?: "NOTIFICATION"
        val reminderMinutes = intent.getIntExtra("reminder_minutes", 10)
        val startTimeMillis = intent.getLongExtra("start_time_millis", 0L)
        // ✅ Get user chosen sound
        val reminderSound = intent.getStringExtra("reminder_sound") ?: "alarm_digital_alarm"
        val announceLabel = intent.getBooleanExtra("announce_label", false)

        if (eventId == -1) return

        createNotificationChannel(context)

        val timeText = formatEventTime(startTimeMillis)
        val bodyText = when {
            description.isNotBlank() -> description
            else -> "Starts at $timeText"
        }

        when (reminderType) {
            "ALARM" -> {
                // ─── Start AlarmService with chosen sound ─────────────────────
                val alarmServiceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra("ALARM_ID", eventId)
                    putExtra("ALARM_LABEL", title)
                    putExtra("ALARM_VIBRATE", true)
                    putExtra("ALARM_TASKS", "[]")
                    putExtra("ALARM_RISE_CHECK_MINUTES", 0)
                    putExtra("ALARM_SOUND", reminderSound)        // ✅ User chosen sound
                    putExtra("ALARM_VOLUME", 1.0f)
                    putExtra("ALARM_GENTLE_WAKE_UP_SECONDS", 0)
                    putExtra("ALARM_TIME_ANNOUNCEMENT", false)    // ✅ NO greeting
                    putExtra("ALARM_ANNOUNCEMENT_VOICE", "female")
                    putExtra("ALARM_WEATHER_REMINDER", false)
                    putExtra("ALARM_LABEL_REMINDER", announceLabel)  // ✅ TTS only if user enabled
                    putExtra("ALARM_EXTRA_LOUD", false)
                    putExtra("ALARM_SNOOZE_ENABLED", true)
                    putExtra("ALARM_SNOOZE_INTERVAL", 5)
                    putExtra("ALARM_SNOOZE_LIMIT", 3)
                    putExtra("ALARM_SNOOZE_PROGRESSIVE", false)
                    putExtra("ALARM_SNOOZE_COUNT", 0)
                }
                context.startForegroundService(alarmServiceIntent)

                // ─── Launch AlarmRingActivity directly from receiver ──────────
                val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
                    putExtra("ALARM_ID", eventId)
                    putExtra("ALARM_LABEL", title)
                    putExtra("ALARM_TASKS", "[]")
                    putExtra("ALARM_RISE_CHECK_MINUTES", 0)
                    putExtra("ALARM_SNOOZE_ENABLED", true)
                    putExtra("ALARM_SNOOZE_INTERVAL", 5)
                    putExtra("ALARM_SNOOZE_LIMIT", 3)
                    putExtra("ALARM_SNOOZE_PROGRESSIVE", false)
                    putExtra("ALARM_SNOOZE_COUNT", 0)
                    // ✅ Tell AlarmRingActivity to skip "Good Morning" greeting
                    putExtra(EXTRA_IS_EVENT_REMINDER, true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(ringIntent)

                // ✅ Also show our notification with description
                showNotification(context, eventId, title, bodyText, timeText)
            }
            "NOTIFICATION" -> {
                showNotification(context, eventId, title, bodyText, timeText)
            }
        }
    }

    private fun showNotification(context: Context, eventId: Int, title: String, body: String, timeText: String = "") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return
        }
        // Deep link → opens app directly on the specific event's detail screen
        val tapIntent = DeepLinkHandler.buildIntent(
            context,
            if (eventId != -1) DeepLinkHandler.routeEventDetail(eventId)
            else DeepLinkHandler.ROUTE_CALENDAR,
        )
        val tapPendingIntent = PendingIntent.getActivity(
            context, eventId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notifTitle = if (timeText.isNotBlank()) "📅 $title  ·  $timeText" else "📅 $title"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notifTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(eventId, notification)
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Reminders for your calendar events"
            enableVibration(true)
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun formatEventTime(millis: Long): String {
        return try {
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
                .format(DateTimeFormatter.ofPattern("h:mm a"))
        } catch (e: Exception) { "" }
    }
}
