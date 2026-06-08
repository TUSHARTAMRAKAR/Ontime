package com.tushartamrakar.ontime.period.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules and cancels period-related notifications using AlarmManager.
 * Follows the same pattern as EventReminderScheduler.
 *
 * Call scheduleAll() whenever predictions change (after every log save,
 * settings update, or cycle data change).
 *
 * Notification IDs / request codes:
 *   3001  →  X days before period  (user-configurable)
 *   3002  →  2 days before period  (supplies reminder)
 *   3003  →  Ovulation day         (only if showFertileWindow = true)
 *   3004  →  Fertile window opens  (only if showFertileWindow = true)
 */
object PeriodNotificationScheduler {

    private const val REQ_BEFORE    = 3001
    private const val REQ_SUPPLIES  = 3002
    private const val REQ_OVULATION = 3003
    private const val REQ_FERTILE   = 3004
    private const val REQ_LATE      = 3005  // period overdue notification

    // ─── Public API ───────────────────────────────────────────────────────────

    fun scheduleAll(
        context: Context,
        nextPeriodDate: LocalDate?,
        ovulationDate: LocalDate?,
        fertileWindowStart: LocalDate?,
        showFertileWindow: Boolean,
        remindDaysBefore: Int,
        isLate: Boolean = false,
        daysLate: Int = 0,
    ) {
        // Cancel everything first so stale reminders don't linger
        cancelAll(context)

        nextPeriodDate?.let { periodDate ->

            // ── Primary reminder: N days before period ────────────────────────
            schedule(
                context    = context,
                reqCode    = REQ_BEFORE,
                notifId    = REQ_BEFORE,
                targetDate = periodDate.minusDays(remindDaysBefore.toLong()),
                title      = "Period arriving soon 🌸",
                body       = "Your cycle is wrapping up. Period expected in " +
                             "$remindDaysBefore ${if (remindDaysBefore == 1) "day" else "days"}. " +
                             "Keep your supplies ready and take it easy.",
            )

            // ── Supplies reminder: always 2 days before ───────────────────────
            // Skip if user already set their reminder to 2 days (would be duplicate)
            if (remindDaysBefore != 2) {
                schedule(
                    context    = context,
                    reqCode    = REQ_SUPPLIES,
                    notifId    = REQ_SUPPLIES,
                    targetDate = periodDate.minusDays(2),
                    title      = "Supplies check 🛍",
                    body       = "Your period may start in 2 days. A good time to make " +
                                 "sure you have everything you need.",
                )
            }
        }

        // ── Fertile window + ovulation: only if user opted in ─────────────────
        if (showFertileWindow) {

            ovulationDate?.let { ovDate ->
                schedule(
                    context    = context,
                    reqCode    = REQ_OVULATION,
                    notifId    = REQ_OVULATION,
                    targetDate = ovDate,
                    title      = "Ovulation day 💜",
                    body       = "Today is your estimated ovulation day. " +
                                 "Peak energy and confidence — enjoy it!",
                )
            }

            fertileWindowStart?.let { fwStart ->
                schedule(
                    context    = context,
                    reqCode    = REQ_FERTILE,
                    notifId    = REQ_FERTILE,
                    targetDate = fwStart,
                    title      = "Fertile window opens 🌱",
                    body       = "Your fertile window opens today. " +
                                 "This is your most fertile time of the cycle.",
                )
            }
        }

        // ── Late period notification ────────────────────────────────────────────
        // Fires next morning at 9 AM if her period hasn't arrived yet.
        // Gentle and reassuring — never alarming.
        if (isLate && daysLate > 0) {
            val daysText = if (daysLate == 1) "a day" else "$daysLate days"
            scheduleLate(
                context = context,
                title   = "Your period is taking its time 🌸",
                body    = "Your period is $daysText late. This is usually normal — " +
                          "your body has its own rhythm. Log it when it arrives.",
            )
        }
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(REQ_BEFORE, REQ_SUPPLIES, REQ_OVULATION, REQ_FERTILE, REQ_LATE).forEach { reqCode ->
            val intent = Intent(context, PeriodReminderReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, reqCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.cancel(pi)
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    // Schedules a late-period notification for the NEXT 9 AM:
    //   • before 9 AM today  → fires this morning
    //   • after  9 AM today  → fires tomorrow morning
    private fun scheduleLate(context: Context, title: String, body: String) {
        val nineAm = LocalTime.of(9, 0)
        val triggerDateTime = if (LocalTime.now().isBefore(nineAm)) {
            LocalDate.now().atTime(nineAm)          // today at 9 AM
        } else {
            LocalDate.now().plusDays(1).atTime(nineAm)  // tomorrow at 9 AM
        }
        val triggerMillis = triggerDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerMillis <= System.currentTimeMillis() + 60_000) return

        val intent = Intent(context, PeriodReminderReceiver::class.java).apply {
            putExtra(PeriodReminderReceiver.EXTRA_NOTIF_ID, REQ_LATE)
            putExtra(PeriodReminderReceiver.EXTRA_TITLE,    title)
            putExtra(PeriodReminderReceiver.EXTRA_BODY,     body)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQ_LATE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    private fun schedule(
        context: Context,
        reqCode: Int,
        notifId: Int,
        targetDate: LocalDate,
        title: String,
        body: String,
    ) {
        // Don't schedule anything in the past
        val today = LocalDate.now()
        if (targetDate.isBefore(today)) return

        // Fire at 9:00 AM on the target date
        val triggerMillis = targetDate
            .atTime(9, 0, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // Also skip if the trigger is less than 60 seconds away (too close to bother)
        if (triggerMillis <= System.currentTimeMillis() + 60_000) return

        val intent = Intent(context, PeriodReminderReceiver::class.java).apply {
            putExtra(PeriodReminderReceiver.EXTRA_NOTIF_ID, notifId)
            putExtra(PeriodReminderReceiver.EXTRA_TITLE,    title)
            putExtra(PeriodReminderReceiver.EXTRA_BODY,     body)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, reqCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // setExactAndAllowWhileIdle fires even in Doze mode — same flag used by event reminders
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent,
        )
    }
}
