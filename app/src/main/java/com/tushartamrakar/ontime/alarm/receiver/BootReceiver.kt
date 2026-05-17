package com.tushartamrakar.ontime.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tushartamrakar.ontime.alarm.data.local.AlarmDatabase
import com.tushartamrakar.ontime.alarm.domain.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.room.Room

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Reschedule all enabled alarms after phone restart
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                context,
                AlarmDatabase::class.java,
                AlarmDatabase.DATABASE_NAME,
            ).build()

            val scheduler = AlarmScheduler(context)
            val alarms = db.alarmDao().getAllAlarms().first()

            alarms.filter { it.isEnabled }.forEach { alarm ->
                scheduler.schedule(alarm)
            }

            db.close()
        }
    }
}