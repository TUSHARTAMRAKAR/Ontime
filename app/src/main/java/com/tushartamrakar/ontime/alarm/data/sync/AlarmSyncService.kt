package com.tushartamrakar.ontime.alarm.data.sync

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tushartamrakar.ontime.alarm.data.local.AlarmEntity
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AlarmSyncService
 *
 * Single responsibility: move AlarmEntity data between Room and Firestore.
 * All public functions are fire-and-forget safe — they catch every exception
 * and log it without rethrowing, so a Firestore outage never breaks the app.
 *
 * Firestore structure:
 *   users/{userId}/alarms/{alarmId}
 *
 * Sync strategy: Last Write Wins
 *   Create  → Room insert  → uploadAlarm()
 *   Update  → Room update  → uploadAlarm()
 *   Toggle  → Room update  → uploadAlarm()
 *   Delete  → Room delete  → deleteAlarm()
 *   Restore → restoreAlarms() → Room insert all
 *
 * The `updatedAt` field is stored in Firestore (not in Room) so future
 * conflict resolution can compare timestamps across devices.
 */
@Singleton
class AlarmSyncService @Inject constructor(
    private val auth:      FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {
    companion object {
        private const val TAG              = "AlarmSync"
        private const val COL_USERS        = "users"
        private const val COL_ALARMS       = "alarms"
    }

    // ── Current user helper ───────────────────────────────────────────────────

    private val userId: String? get() = auth.currentUser?.uid

    private fun alarmsRef(uid: String) =
        firestore.collection(COL_USERS).document(uid).collection(COL_ALARMS)

    val isUserLoggedIn: Boolean get() = userId != null

    // ── Upload (create or overwrite) ──────────────────────────────────────────

    /**
     * Upload a single alarm to Firestore.
     * Uses Room `id` as the Firestore document name so they stay in sync.
     * Safe to call with any alarm — silently skips if user not logged in.
     */
    suspend fun uploadAlarm(alarm: AlarmEntity) {
        val uid = userId ?: return
        try {
            alarmsRef(uid)
                .document(alarm.id.toString())
                .set(alarm.toFirestoreMap())
                .await()
            Log.d(TAG, "✅ Uploaded alarm ${alarm.id} — \"${alarm.label}\"")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Upload failed for alarm ${alarm.id}: ${e.message}")
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    suspend fun deleteAlarm(alarmId: Int) {
        val uid = userId ?: return
        try {
            alarmsRef(uid)
                .document(alarmId.toString())
                .delete()
                .await()
            Log.d(TAG, "🗑 Deleted alarm $alarmId from Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Delete failed for alarm $alarmId: ${e.message}")
        }
    }

    // ── Restore ───────────────────────────────────────────────────────────────

    /**
     * Fetch all alarms for the current user from Firestore.
     * Returns an empty list on any error or if user is not logged in.
     * The returned list has id = 0 so Room auto-generates new primary keys.
     */
    suspend fun restoreAlarms(): List<AlarmEntity> {
        val uid = userId ?: run {
            Log.d(TAG, "Restore skipped — user not logged in")
            return emptyList()
        }
        return try {
            val snapshot = alarmsRef(uid).get().await()
            val alarms = snapshot.documents.mapNotNull { doc ->
                doc.data?.toAlarmEntity()
            }
            Log.d(TAG, "📥 Restored ${alarms.size} alarms from Firestore")
            alarms
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Restore failed: ${e.message}")
            emptyList()
        }
    }

    // ── AlarmEntity ↔ Firestore map ───────────────────────────────────────────

    private fun AlarmEntity.toFirestoreMap(): Map<String, Any> = mapOf(
        "id"                    to id,
        "hour"                  to hour,
        "minute"                to minute,
        "label"                 to label,
        "isEnabled"             to isEnabled,
        "repeatDays"            to repeatDays,
        "sound"                 to sound,
        "vibrate"               to vibrate,
        "volume"                to volume.toDouble(),       // Firestore stores as Double
        "gentleWakeUpSeconds"   to gentleWakeUpSeconds,
        "timeAnnouncement"      to timeAnnouncement,
        "announcementVoice"     to announcementVoice,
        "weatherReminder"       to weatherReminder,
        "labelReminder"         to labelReminder,
        "extraLoud"             to extraLoud,
        "snoozeEnabled"         to snoozeEnabled,
        "snoozeIntervalMinutes" to snoozeIntervalMinutes,
        "snoozeLimit"           to snoozeLimit,
        "snoozeProgressiveMode" to snoozeProgressiveMode,
        "tasks"                 to tasks,
        "riseCheckMinutes"      to riseCheckMinutes,
        "createdAt"             to createdAt,
        "updatedAt"             to System.currentTimeMillis(),  // not in Room — for future conflict resolution
    )

    /**
     * Firestore returns all numbers as Long, booleans as Boolean, strings as String.
     * Every cast uses `as?` with a safe default so a single bad field
     * doesn't prevent the entire alarm from restoring.
     */
    private fun Map<String, Any>.toAlarmEntity(): AlarmEntity? = try {
        AlarmEntity(
            id                    = 0,  // let Room auto-generate on restore
            hour                  = (this["hour"]                  as? Long)?.toInt() ?: 0,
            minute                = (this["minute"]                as? Long)?.toInt() ?: 0,
            label                 = this["label"]                  as? String  ?: "",
            isEnabled             = this["isEnabled"]              as? Boolean ?: true,
            repeatDays            = this["repeatDays"]             as? String  ?: "",
            sound                 = this["sound"]                  as? String  ?: "alarm_digital_alarm",
            vibrate               = this["vibrate"]                as? Boolean ?: true,
            volume                = (this["volume"]                as? Double)?.toFloat() ?: 1.0f,
            gentleWakeUpSeconds   = (this["gentleWakeUpSeconds"]   as? Long)?.toInt() ?: 0,
            timeAnnouncement      = this["timeAnnouncement"]       as? Boolean ?: false,
            announcementVoice     = this["announcementVoice"]      as? String  ?: "female",
            weatherReminder       = this["weatherReminder"]        as? Boolean ?: false,
            labelReminder         = this["labelReminder"]          as? Boolean ?: false,
            extraLoud             = this["extraLoud"]              as? Boolean ?: false,
            snoozeEnabled         = this["snoozeEnabled"]          as? Boolean ?: true,
            snoozeIntervalMinutes = (this["snoozeIntervalMinutes"] as? Long)?.toInt() ?: 5,
            snoozeLimit           = (this["snoozeLimit"]           as? Long)?.toInt() ?: 3,
            snoozeProgressiveMode = this["snoozeProgressiveMode"]  as? Boolean ?: false,
            tasks                 = this["tasks"]                  as? String  ?: "[]",
            riseCheckMinutes      = (this["riseCheckMinutes"]      as? Long)?.toInt() ?: 0,
            createdAt             = this["createdAt"]              as? Long    ?: System.currentTimeMillis(),
        )
    } catch (e: Exception) {
        Log.w(TAG, "⚠️ Failed to parse Firestore alarm document: ${e.message}")
        null
    }
}
