package com.tushartamrakar.ontime.focus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BootCompletedReceiver
 *
 * Android cancels all AlarmManager alarms when the device reboots.
 * This receiver fires on boot and re-schedules:
 *  - All period tracker reminders
 *  - Focus session reminders / daily focus nudges
 *
 * Wire it to your existing notification schedulers here.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        Log.d(TAG, "Boot completed — re-arming Ontime alarms")

        // TODO: inject your scheduler here via goAsync() + coroutine, e.g.:
        // val pendingResult = goAsync()
        // CoroutineScope(Dispatchers.IO).launch {
        //     try {
        //         eventReminderScheduler.rescheduleAll(context)
        //         periodReminderScheduler.rescheduleAll(context)
        //     } finally {
        //         pendingResult.finish()
        //     }
        // }
    }
}
