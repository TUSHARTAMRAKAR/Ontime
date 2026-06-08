package com.tushartamrakar.ontime.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.room.Room
import com.tushartamrakar.ontime.alarm.data.local.AlarmDatabase
import com.tushartamrakar.ontime.alarm.domain.AlarmScheduler
import com.tushartamrakar.ontime.calendar.data.local.CalendarDatabase
import com.tushartamrakar.ontime.calendar.data.repository.CalendarRepository
import com.tushartamrakar.ontime.calendar.notification.EventReminderScheduler
import com.tushartamrakar.ontime.period.data.local.PeriodDatabase
import com.tushartamrakar.ontime.period.data.repository.PeriodRepository
import com.tushartamrakar.ontime.period.notification.PeriodNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BootReceiver — re-arms every AlarmManager alarm after phone restart.
 *
 * Android cancels ALL AlarmManager alarms on reboot. Without this receiver,
 * every alarm clock alarm, calendar reminder, and period notification would
 * silently disappear after a restart.
 *
 * Re-schedules in order:
 *  1. Main alarm clock alarms        (AlarmScheduler)
 *  2. Calendar event reminders       (EventReminderScheduler)
 *  3. Period tracker reminders       (PeriodNotificationScheduler)
 *
 * Uses goAsync() so Android doesn't kill the process before the coroutine
 * finishes — BroadcastReceivers get ~10 seconds of life by default.
 *
 * NOTE: Database DAO accessor method names below (calendarEventDao,
 * cycleDao, periodLogDao, periodSettingsDao) follow standard Room naming
 * conventions. If your @Database abstract functions are named differently,
 * the compiler will tell you exactly what to change.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        Log.d(TAG, "Boot completed — starting alarm rescheduling")

        // goAsync() keeps the BroadcastReceiver alive until pendingResult.finish()
        // Without this, Android may kill the process mid-coroutine after ~10s
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleAlarmClockAlarms(context)
                rescheduleCalendarReminders(context)
                reschedulePeriodReminders(context)
                Log.d(TAG, "All alarms rescheduled successfully")
            } catch (e: Exception) {
                // Never crash in a BroadcastReceiver — log and move on
                Log.e(TAG, "Error during boot rescheduling: ${e.message}", e)
            } finally {
                // Always finish — even on error — or Android will ANR
                pendingResult.finish()
            }
        }
    }

    // ─── 1. Main alarm clock alarms ───────────────────────────────────────────

    private suspend fun rescheduleAlarmClockAlarms(context: Context) {
        val db = Room.databaseBuilder(
            context,
            AlarmDatabase::class.java,
            AlarmDatabase.DATABASE_NAME,
        ).build()

        try {
            val scheduler = AlarmScheduler(context)
            val alarms    = db.alarmDao().getAllAlarms().first()
            val enabled   = alarms.filter { it.isEnabled }

            enabled.forEach { alarm -> scheduler.schedule(alarm) }

            Log.d(TAG, "Alarm clocks: rescheduled ${enabled.size} of ${alarms.size}")
        } finally {
            db.close()
        }
    }

    // ─── 2. Calendar event reminders ──────────────────────────────────────────

    private suspend fun rescheduleCalendarReminders(context: Context) {
        val db = Room.databaseBuilder(
            context,
            CalendarDatabase::class.java,
            CalendarDatabase.DATABASE_NAME,   // "ontime_calendar_db"
        )
            // Migrations must be included — without them Room crashes when
            // opening an existing database that was created at an older version
            .addMigrations(
                CalendarDatabase.MIGRATION_1_2,
                CalendarDatabase.MIGRATION_2_3,
                CalendarDatabase.MIGRATION_3_4,
                CalendarDatabase.MIGRATION_4_5,
                CalendarDatabase.MIGRATION_5_6,
                CalendarDatabase.MIGRATION_6_7,
            )
            .build()

        try {
            val repository   = CalendarRepository(db.calendarEventDao())
            val futureEvents = repository.getUpcomingEventsWithReminders(
                nowMillis = System.currentTimeMillis()
            )

            futureEvents.forEach { event ->
                EventReminderScheduler.schedule(context, event)
            }

            Log.d(TAG, "Calendar reminders: rescheduled ${futureEvents.size} event(s)")
        } finally {
            db.close()
        }
    }

    // ─── 3. Period tracker reminders ──────────────────────────────────────────

    private suspend fun reschedulePeriodReminders(context: Context) {
        val db = Room.databaseBuilder(
            context,
            PeriodDatabase::class.java,
            "ontime_period.db",         // from AppModule
        ).build()

        try {
            val repository = PeriodRepository(
                cycleDao    = db.cycleDao(),
                logDao      = db.periodLogDao(),
                settingsDao = db.periodSettingsDao(),
            )

            // Skip entirely if user hasn't completed period tracker onboarding
            if (!repository.isOnboardingComplete()) {
                Log.d(TAG, "Period reminders: skipped — onboarding not complete")
                return
            }

            // Gather all the data PeriodNotificationScheduler needs
            val settings           = repository.getSettings().first()
            val nextPeriodDate     = repository.predictNextPeriodStart()
            val ovulationDate      = repository.getOvulationDay()
            val fertileWindowStart = repository.getFertileWindow().firstOrNull()
            val isLate             = repository.isLatePeriod()
            val daysLate           = repository.getDaysLate()

            PeriodNotificationScheduler.scheduleAll(
                context            = context,
                nextPeriodDate     = nextPeriodDate,
                ovulationDate      = ovulationDate,
                fertileWindowStart = fertileWindowStart,
                showFertileWindow  = settings.showFertileWindow,
                remindDaysBefore   = settings.remindDaysBefore,
                isLate             = isLate,
                daysLate           = daysLate,
            )

            Log.d(TAG, "Period reminders: rescheduled — nextPeriod=$nextPeriodDate " +
                       "isLate=$isLate daysLate=$daysLate")
        } finally {
            db.close()
        }
    }
}
