package com.tushartamrakar.ontime.alarm.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tushartamrakar.ontime.alarm.data.local.AlarmEntity
import com.tushartamrakar.ontime.alarm.data.repository.AlarmRepository
import com.tushartamrakar.ontime.alarm.domain.WakeUpTask
import com.tushartamrakar.ontime.alarm.domain.toJsonString
import com.tushartamrakar.ontime.alarm.domain.toWakeUpTasks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val repository: AlarmRepository,
) : ViewModel() {

    val alarms: StateFlow<List<AlarmEntity>> = repository.getAllAlarms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    // ── Cloud sync state ──────────────────────────────────────────────────────

    /** True while a cloud restore is in progress — drives UI loading indicator. */
    val isRestoring = MutableStateFlow(false)

    /** How many alarms were restored on last restore attempt (-1 = never run). */
    val restoredCount = MutableStateFlow(-1)

    /**
     * Guard that ensures restoreFromCloud() is only triggered ONCE on startup,
     * regardless of whether init{} or addAuthStateListener fires first.
     * AtomicBoolean.compareAndSet(false, true) is thread-safe — only the first
     * caller wins; the second sees true and skips.
     */
    private val restoreAttempted = AtomicBoolean(false)

    init {
        // Auto-restore: if Room is empty on first collect, pull alarms from
        // Firestore (fresh install / new device scenario).
        viewModelScope.launch {
            alarms.collect { localAlarms ->
                if (localAlarms.isEmpty() && restoreAttempted.compareAndSet(false, true)) {
                    isRestoring.value = true
                    val count = repository.restoreFromCloud()
                    if (count > 0) restoredCount.value = count
                    isRestoring.value = false
                    // collect only fires once for the auto-restore check
                    return@collect
                }
            }
        }

        // Auth state listener — if user logs in while app is already open
        // and alarm list is empty, trigger restore automatically.
        // restoreAttempted guard prevents a double-call with the init{} block above.
        com.google.firebase.auth.FirebaseAuth.getInstance()
            .addAuthStateListener { auth ->
                if (auth.currentUser != null && alarms.value.isEmpty() &&
                    restoreAttempted.compareAndSet(false, true)
                ) {
                    viewModelScope.launch {
                        isRestoring.value = true
                        val count = repository.restoreFromCloud()
                        if (count > 0) restoredCount.value = count
                        isRestoring.value = false
                    }
                }
            }
    }

    fun createAlarm(
        hour: Int, minute: Int, label: String, repeatDays: String,
        vibrate: Boolean, tasks: List<WakeUpTask> = emptyList(),
        riseCheckMinutes: Int = 0, sound: String = "alarm_digital_alarm",
        volume: Float = 1.0f, gentleWakeUpSeconds: Int = 0,
        timeAnnouncement: Boolean = false, announcementVoice: String = "female",
        weatherReminder: Boolean = false, labelReminder: Boolean = false,
        extraLoud: Boolean = false, snoozeEnabled: Boolean = true,
        snoozeIntervalMinutes: Int = 5, snoozeLimit: Int = 3,
        snoozeProgressiveMode: Boolean = false,
    ) {
        viewModelScope.launch {
            repository.createAlarm(
                AlarmEntity(
                    hour = hour, minute = minute, label = label,
                    repeatDays = repeatDays, vibrate = vibrate,
                    tasks = tasks.toJsonString(), riseCheckMinutes = riseCheckMinutes,
                    sound = sound, volume = volume,
                    gentleWakeUpSeconds = gentleWakeUpSeconds,
                    timeAnnouncement = timeAnnouncement,
                    announcementVoice = announcementVoice,
                    weatherReminder = weatherReminder,
                    labelReminder = labelReminder,
                    extraLoud = extraLoud,
                    snoozeEnabled = snoozeEnabled,
                    snoozeIntervalMinutes = snoozeIntervalMinutes,
                    snoozeLimit = snoozeLimit,
                    snoozeProgressiveMode = snoozeProgressiveMode,
                )
            )
        }
    }

    fun updateAlarmById(
        alarmId: Int, hour: Int, minute: Int, label: String, repeatDays: String,
        vibrate: Boolean, tasks: List<WakeUpTask> = emptyList(),
        riseCheckMinutes: Int = 0, sound: String = "alarm_digital_alarm",
        volume: Float = 1.0f, gentleWakeUpSeconds: Int = 0,
        timeAnnouncement: Boolean = false, announcementVoice: String = "female",
        weatherReminder: Boolean = false, labelReminder: Boolean = false,
        extraLoud: Boolean = false, snoozeEnabled: Boolean = true,
        snoozeIntervalMinutes: Int = 5, snoozeLimit: Int = 3,
        snoozeProgressiveMode: Boolean = false,
    ) {
        viewModelScope.launch {
            val existing = repository.getAlarmById(alarmId) ?: return@launch
            repository.updateAlarm(
                existing.copy(
                    hour = hour, minute = minute, label = label,
                    repeatDays = repeatDays, vibrate = vibrate,
                    tasks = tasks.toJsonString(), riseCheckMinutes = riseCheckMinutes,
                    sound = sound, volume = volume,
                    gentleWakeUpSeconds = gentleWakeUpSeconds,
                    timeAnnouncement = timeAnnouncement,
                    announcementVoice = announcementVoice,
                    weatherReminder = weatherReminder,
                    labelReminder = labelReminder,
                    extraLoud = extraLoud,
                    snoozeEnabled = snoozeEnabled,
                    snoozeIntervalMinutes = snoozeIntervalMinutes,
                    snoozeLimit = snoozeLimit,
                    snoozeProgressiveMode = snoozeProgressiveMode,
                )
            )
        }
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch { repository.toggleAlarm(alarm) }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch { repository.deleteAlarm(alarm) }
    }

    fun duplicateAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.createAlarm(
                alarm.copy(
                    id        = 0,
                    label     = if (alarm.label.isBlank()) "Alarm copy" else "${alarm.label} (copy)",
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun skipOnce(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.updateAlarm(alarm.copy(isEnabled = false))
        }
    }

    // ── Manual cloud restore ──────────────────────────────────────────────────

    fun restoreFromCloud() {
        viewModelScope.launch {
            isRestoring.value = true
            val count = repository.restoreFromCloud()
            restoredCount.value = count
            isRestoring.value = false
        }
    }

    suspend fun getAlarmById(id: Int): AlarmEntity? = repository.getAlarmById(id)
}
