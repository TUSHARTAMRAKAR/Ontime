package com.tushartamrakar.ontime.alarm.data.repository

import android.util.Log
import com.tushartamrakar.ontime.alarm.data.local.AlarmDao
import com.tushartamrakar.ontime.alarm.data.local.AlarmEntity
import com.tushartamrakar.ontime.alarm.data.sync.AlarmSyncService
import com.tushartamrakar.ontime.alarm.domain.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val alarmDao:      AlarmDao,
    private val alarmScheduler: AlarmScheduler,
    private val syncService:   AlarmSyncService,
) {
    companion object {
        private const val TAG = "AlarmRepository"
    }

    // Dedicated scope for fire-and-forget sync operations.
    // SupervisorJob means a failed sync doesn't cancel other syncs.
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ─── Get all alarms as Flow ───────────────────────────────────────────────

    fun getAllAlarms(): Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    // ─── Get single alarm ─────────────────────────────────────────────────────

    suspend fun getAlarmById(id: Int): AlarmEntity? = alarmDao.getAlarmById(id)

    // ─── Create alarm ─────────────────────────────────────────────────────────

    suspend fun createAlarm(alarm: AlarmEntity): Long {
        val id        = alarmDao.insertAlarm(alarm)
        val savedAlarm = alarm.copy(id = id.toInt())
        if (savedAlarm.isEnabled) alarmScheduler.schedule(savedAlarm)
        fireAndForgetSync { syncService.uploadAlarm(savedAlarm) }
        return id
    }

    // ─── Update alarm ─────────────────────────────────────────────────────────

    suspend fun updateAlarm(alarm: AlarmEntity) {
        alarmDao.updateAlarm(alarm)
        alarmScheduler.cancel(alarm.id)
        if (alarm.isEnabled) alarmScheduler.schedule(alarm)
        fireAndForgetSync { syncService.uploadAlarm(alarm) }
    }

    // ─── Toggle alarm on/off ──────────────────────────────────────────────────

    suspend fun toggleAlarm(alarm: AlarmEntity) {
        val updated = alarm.copy(isEnabled = !alarm.isEnabled)
        alarmDao.updateAlarm(updated)
        if (updated.isEnabled) alarmScheduler.schedule(updated)
        else                   alarmScheduler.cancel(alarm.id)
        fireAndForgetSync { syncService.uploadAlarm(updated) }
    }

    // ─── Delete alarm ─────────────────────────────────────────────────────────

    suspend fun deleteAlarm(alarm: AlarmEntity) {
        alarmScheduler.cancel(alarm.id)
        alarmDao.deleteAlarm(alarm)
        fireAndForgetSync { syncService.deleteAlarm(alarm.id) }
    }

    // ─── Upsert (used by BlockedAppsManager + quick edits) ───────────────────

    suspend fun upsertBlockedApp(alarm: AlarmEntity) {
        alarmDao.updateAlarm(alarm)
        fireAndForgetSync { syncService.uploadAlarm(alarm) }
    }

    // ─── Cloud restore ────────────────────────────────────────────────────────

    /**
     * Pulls all alarms from Firestore and inserts them into Room.
     * Called automatically when Room is empty on first launch.
     * Also callable manually from AppSettingsScreen ("Restore from Cloud").
     *
     * After inserting each alarm, Firestore is updated with the new Room-generated
     * ID so future syncs stay in sync.
     *
     * Returns the number of alarms successfully restored.
     */
    suspend fun restoreFromCloud(): Int {
        if (!syncService.isUserLoggedIn) {
            Log.d(TAG, "Restore skipped — not logged in")
            return 0
        }

        val cloudAlarms = syncService.restoreAlarms()
        if (cloudAlarms.isEmpty()) {
            Log.d(TAG, "Nothing to restore — Firestore is empty")
            return 0
        }

        var count = 0
        cloudAlarms.forEach { alarm ->
            try {
                // Insert with id=0 → Room generates a new PK
                val newId      = alarmDao.insertAlarm(alarm.copy(id = 0))
                val savedAlarm = alarm.copy(id = newId.toInt())

                // Schedule if enabled
                if (savedAlarm.isEnabled) alarmScheduler.schedule(savedAlarm)

                // Re-upload with the correct Room-generated ID so Firestore
                // document names stay in sync with Room primary keys
                fireAndForgetSync {
                    syncService.deleteAlarm(alarm.id)   // remove old doc (id=0 remnant)
                    syncService.uploadAlarm(savedAlarm)  // write with real id
                }
                count++
            } catch (e: Exception) {
                Log.w(TAG, "Failed to restore alarm \"${alarm.label}\": ${e.message}")
            }
        }

        Log.d(TAG, "✅ Restored $count / ${cloudAlarms.size} alarms from Firestore")
        return count
    }

    // ─── Internal: fire-and-forget sync ──────────────────────────────────────

    /**
     * Runs [block] on the IO thread without blocking the caller.
     * Errors are caught and logged — a Firestore failure will NEVER
     * surface to the user or affect local Room data.
     */
    private fun fireAndForgetSync(block: suspend () -> Unit) {
        syncScope.launch {
            try {
                block()
            } catch (e: Exception) {
                Log.w(TAG, "Background sync error: ${e.message}")
            }
        }
    }
}
