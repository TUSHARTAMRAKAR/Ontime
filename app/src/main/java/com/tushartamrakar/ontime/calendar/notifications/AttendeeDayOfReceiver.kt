package com.tushartamrakar.ontime.calendar.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tushartamrakar.ontime.MainActivity
import com.tushartamrakar.ontime.R

/**
 * Fires at 9:00 AM on the day of the event.
 * Scheduled by AttendeeNotificationHelper.scheduleDayOfAlarms().
 *
 * On receipt:
 *   1. Sends SMS to all phone-notified attendees (background-safe ✓)
 *   2. Shows a push notification for email attendees:
 *        "Team meeting is today at 10 AM — tap to remind guests 📧"
 *      Tapping the notification opens email compose (E1 approach —
 *      starting an Activity from background is not reliable on
 *      Android 10+, so the push notification acts as the bridge)
 */
class AttendeeDayOfReceiver : BroadcastReceiver() {

    companion object {
        // ── Intent extras (set by AttendeeNotificationHelper) ─────────────────
        const val EXTRA_EVENT_ID     = "attendee_event_id"
        const val EXTRA_EVENT_TITLE  = "attendee_event_title"
        const val EXTRA_START_MILLIS = "attendee_start_millis"
        const val EXTRA_SMS_PHONES   = "attendee_sms_phones"   // comma-separated
        const val EXTRA_EMAIL_ADDRS  = "attendee_email_addrs"  // comma-separated

        // ── Notification channel ───────────────────────────────────────────────
        const val CHANNEL_ID   = "ontime_attendee_reminders"
        const val CHANNEL_NAME = "Guest reminders"

        // ── Email action extra (for the PendingIntent inside the notification) ─
        const val ACTION_SEND_EMAIL = "com.tushartamrakar.ontime.SEND_ATTENDEE_EMAIL"
        const val EXTRA_RECIPIENTS  = "email_recipients"
        const val EXTRA_SUBJECT     = "email_subject"
        const val EXTRA_BODY        = "email_body"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val eventId       = intent.getIntExtra(EXTRA_EVENT_ID, -1)
        val eventTitle    = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: return
        val startMillis   = intent.getLongExtra(EXTRA_START_MILLIS, 0L)
        val smsPhones     = intent.getStringExtra(EXTRA_SMS_PHONES)?.trim() ?: ""
        val emailAddresses= intent.getStringExtra(EXTRA_EMAIL_ADDRS)?.trim() ?: ""

        if (eventId == -1) return

        createNotificationChannel(context)

        val timeText = formatEventTime(startMillis)

        // ── 1. SMS: send silently for each phone number ────────────────────────
        if (smsPhones.isNotBlank() && hasSmsPermission(context)) {
            val smsManager = getSmsManager(context)
            smsPhones.split(",").forEach { phone ->
                val trimmed = phone.trim()
                if (trimmed.isNotBlank()) {
                    runCatching {
                        smsManager.sendTextMessage(
                            trimmed, null,
                            "Reminder: \"$eventTitle\" is today" +
                            (if (timeText.isNotBlank()) " at $timeText" else "") +
                            ". See you there! 🗓️",
                            null, null,
                        )
                    }
                }
            }
        }

        // ── 2. Push notification for email recipients (E1 approach) ───────────
        if (emailAddresses.isNotBlank() && hasNotificationPermission(context)) {
            showEmailReminderNotification(
                context        = context,
                eventId        = eventId,
                eventTitle     = eventTitle,
                timeText       = timeText,
                emailAddresses = emailAddresses,
                startMillis    = startMillis,
            )
        }
    }

    // ─── Notification that acts as the bridge for email ───────────────────────

    private fun showEmailReminderNotification(
        context: Context,
        eventId: Int,
        eventTitle: String,
        timeText: String,
        emailAddresses: String,
        startMillis: Long,
    ) {
        val recipients = emailAddresses.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (recipients.isEmpty()) return

        val subject = "Today — $eventTitle"
        val body    = "Just a reminder that \"$eventTitle\" is happening today" +
                      (if (timeText.isNotBlank()) " at $timeText" else "") + ".\n\nSee you there!"

        // ── Tap → open email compose via MainActivity (workaround for bg restriction) ─
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL,   recipients.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT,    body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Wrap in PendingIntent — when user taps notification, email compose opens
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            AttendeeNotificationHelper.dayOfRequestCode(eventId) + 1, // +1 to avoid collision
            emailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Also a "Open app" secondary tap goes to MainActivity
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            eventId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val guestCount = recipients.size
        val guestText  = if (guestCount == 1) "1 guest" else "$guestCount guests"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$eventTitle is today 🗓️")
            .setContentText("Tap to send a day-of reminder to $guestText")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Today" +
                        (if (timeText.isNotBlank()) " at $timeText" else "") +
                        " — \"$eventTitle\"\n\nTap to remind $guestText via email 📧"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setColor(0xFF5C6BC0.toInt())              // indigo — matches calendar theme
            .setContentIntent(tapPendingIntent)        // tap → email compose
            .addAction(                                // secondary: open app
                R.drawable.ic_launcher_foreground,
                "Open app",
                openAppPendingIntent,
            )
            .build()

        // Use a unique notification ID (won't collide with EventReminderReceiver)
        val notifId = AttendeeNotificationHelper.dayOfRequestCode(eventId)
        NotificationManagerCompat.from(context).notify(notifId, notification)
    }

    // ─── Channel + permission helpers ─────────────────────────────────────────

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Day-of reminders for people you invited to events"
            enableVibration(false)
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasSmsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS,
        ) == PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    private fun getSmsManager(context: Context): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.getSystemService(SmsManager::class.java)
        else
            SmsManager.getDefault()
}
