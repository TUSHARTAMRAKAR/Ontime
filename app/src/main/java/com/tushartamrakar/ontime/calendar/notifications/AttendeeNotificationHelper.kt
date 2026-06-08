package com.tushartamrakar.ontime.calendar.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import com.google.firebase.auth.FirebaseAuth
import com.tushartamrakar.ontime.calendar.data.local.EventAttendeeEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles ALL outgoing notifications to event attendees:
 *
 *   1. Instant invite  — SMS sent silently + Email Intent opened
 *                        called the moment the event is saved
 *
 *   2. Day-of reminder — AlarmManager fires at 9:00 AM on event day
 *                        SMS sent from AttendeeDayOfReceiver (background-safe)
 *                        Push notification shown with tap → email compose
 *
 * Request code space (AlarmManager, must not collide with EventReminderScheduler):
 *   EventReminderScheduler uses: eventId  and  eventId * 10 + index
 *   AttendeeDayOfHelper uses:    eventId * 2000 + attendeeIndex (max 9)
 *   → No overlap as long as eventId < 1_000_000 (Room auto-increment is safe)
 */
@Singleton
class AttendeeNotificationHelper @Inject constructor(
    private val auth: FirebaseAuth,
) {

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Send instant invites immediately after event is saved.
     *
     * SMS: sent silently per-attendee (requires SEND_SMS permission — already
     *      checked in CreateEventScreen before calling this).
     * Email: one Intent opened with all email recipients in To: field.
     *        User confirms and taps Send in their email app.
     *
     * [attendees]         full attendee list for this event
     * [eventTitle]        event display name
     * [startTimeMillis]   event start — used to format the invite message
     * [context]           application context
     */
    fun sendInstantInvites(
        context: Context,
        attendees: List<EventAttendeeEntity>,
        eventTitle: String,
        startTimeMillis: Long,
    ) {
        if (attendees.isEmpty()) return

        val hostName  = auth.currentUser?.displayName?.trim()
                          ?.takeIf { it.isNotBlank() } ?: "Someone"
        val dateText  = formatEventDateTime(startTimeMillis)

        // ── SMS: Intent opens messaging app with pre-filled message ──────────
        // Same approach as email — user sees the message before sending.
        // smsto: URI supports multiple numbers separated by semicolons.
        // No SEND_SMS permission needed for this Intent approach.
        val smsRecipients = attendees.filter { it.notifyViaSms && !it.phone.isNullOrBlank() }
        if (smsRecipients.isNotEmpty()) {
            // Build one message using "Hey!" as greeting since multiple
            // recipients share the same compose screen
            val message = buildInstantSmsMessage(
                recipientName = if (smsRecipients.size == 1)
                    smsRecipients.first().name.split(" ").first()
                else "everyone",
                hostName   = hostName,
                eventTitle = eventTitle,
                dateText   = dateText,
            )
            openSmsCompose(
                context    = context,
                phones     = smsRecipients.mapNotNull { it.phone },
                message    = message,
            )
        }

        // ── Email: one Intent, all recipients in To: ──────────────────────────
        val emailRecipients = attendees
            .filter { it.notifyViaEmail && !it.email.isNullOrBlank() }
            .mapNotNull { it.email }
        if (emailRecipients.isNotEmpty()) {
            val subject = buildEmailSubject(eventTitle)
            val body    = buildInstantEmailBody(
                hostName       = hostName,
                eventTitle     = eventTitle,
                dateText       = dateText,
            )
            openEmailCompose(context, emailRecipients, subject, body)
        }
    }

    /**
     * Schedule a 9:00 AM day-of reminder for every attendee.
     * AttendeeDayOfReceiver handles the actual delivery.
     *
     * Skipped silently if:
     *   - Event date is today or in the past (no point scheduling)
     *   - No attendees want any notification
     */
    fun scheduleDayOfAlarms(
        context: Context,
        eventId: Int,
        attendees: List<EventAttendeeEntity>,
        startTimeMillis: Long,
        eventTitle: String,
    ) {
        val eventDate = Instant.ofEpochMilli(startTimeMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        // Only schedule for future dates (not today — invite already sent)
        if (!eventDate.isAfter(LocalDate.now())) return

        // Build comma-separated lists to pass via Intent extras (no DB call in receiver)
        val smsPhones = attendees
            .filter { it.notifyViaSms && !it.phone.isNullOrBlank() }
            .mapNotNull { it.phone }
            .joinToString(",")

        val emailAddresses = attendees
            .filter { it.notifyViaEmail && !it.email.isNullOrBlank() }
            .mapNotNull { it.email }
            .joinToString(",")

        if (smsPhones.isBlank() && emailAddresses.isBlank()) return

        // 9:00 AM on event day
        val triggerMillis = eventDate
            .atTime(LocalTime.of(9, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, AttendeeDayOfReceiver::class.java).apply {
            putExtra(AttendeeDayOfReceiver.EXTRA_EVENT_ID,       eventId)
            putExtra(AttendeeDayOfReceiver.EXTRA_EVENT_TITLE,    eventTitle)
            putExtra(AttendeeDayOfReceiver.EXTRA_START_MILLIS,   startTimeMillis)
            putExtra(AttendeeDayOfReceiver.EXTRA_SMS_PHONES,     smsPhones)
            putExtra(AttendeeDayOfReceiver.EXTRA_EMAIL_ADDRS,    emailAddresses)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            dayOfRequestCode(eventId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent,
        )
    }

    /**
     * Cancel the day-of AlarmManager alarm for this event.
     * Call from deleteEvent() AND from re-schedule on update.
     */
    fun cancelDayOfAlarms(context: Context, eventId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AttendeeDayOfReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            dayOfRequestCode(eventId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
    }

    // ─── Message builders ─────────────────────────────────────────────────────

    private fun buildInstantSmsMessage(
        recipientName: String,
        hostName: String,
        eventTitle: String,
        dateText: String,
    ): String = "Hey $recipientName! $hostName invited you to \"$eventTitle\" on $dateText. See you there! 🎉"

    private fun buildInstantEmailBody(
        hostName: String,
        eventTitle: String,
        dateText: String,
    ): String = "Hi!\n\n$hostName has invited you to:\n\n" +
                "📅  $eventTitle\n" +
                "🕐  $dateText\n\n" +
                "Looking forward to seeing you there!\n\n— $hostName"

    private fun buildEmailSubject(eventTitle: String): String =
        "You're invited — $eventTitle"

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Opens the default messaging app with all recipients and the message
     * pre-filled. User reviews and taps Send — same UX as email compose.
     *
     * smsto: URI supports semicolon-separated numbers on all major Android
     * messaging apps (Google Messages, Samsung Messages, etc.)
     * FLAG_ACTIVITY_NEW_TASK required when starting from a non-Activity context.
     */
    internal fun openSmsCompose(
        context: Context,
        phones: List<String>,
        message: String,
    ) {
        if (phones.isEmpty()) return
        runCatching {
            val numbersJoined = phones.joinToString(";")
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$numbersJoined")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Opens the device's email app with all recipients pre-filled.
     * Uses ACTION_SENDTO (mailto: URI) — no permission needed.
     * FLAG_ACTIVITY_NEW_TASK required when starting from a non-Activity context.
     */
    internal fun openEmailCompose(
        context: Context,
        recipients: List<String>,
        subject: String,
        body: String,
    ) {
        runCatching {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL,   recipients.toTypedArray())
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT,    body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    @Suppress("DEPRECATION")
    private fun getSmsManager(context: Context): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.getSystemService(SmsManager::class.java)
        else
            SmsManager.getDefault()

    // ─── Request code helpers ─────────────────────────────────────────────────

    companion object {
        /**
         * AlarmManager request code for day-of attendee alarm.
         * Uses eventId * 2000 to avoid collisions with:
         *   EventReminderScheduler: eventId  and  eventId * 10 + index
         */
        fun dayOfRequestCode(eventId: Int): Int = eventId * 2000
    }
}

// ─── Date formatting (shared between helper + receiver) ───────────────────────

/** "Mon, 2 Jun at 10:00 AM" */
fun formatEventDateTime(startTimeMillis: Long): String = runCatching {
    val zdt  = Instant.ofEpochMilli(startTimeMillis).atZone(ZoneId.systemDefault())
    val day  = zdt.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
    val date = zdt.dayOfMonth
    val mon  = zdt.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
    val time = zdt.toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a"))
    "$day, $date $mon at $time"
}.getOrElse { "soon" }

/** "10:00 AM" */
fun formatEventTime(startTimeMillis: Long): String = runCatching {
    Instant.ofEpochMilli(startTimeMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("h:mm a"))
}.getOrElse { "" }
