package com.tushartamrakar.ontime.alarm.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tushartamrakar.ontime.alarm.data.local.AlarmEntity
import com.tushartamrakar.ontime.alarm.data.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val repository: AlarmRepository,
) : ViewModel() {

    // ─── All alarms as StateFlow ──────────────────────────────────────────────
    val alarms: StateFlow<List<AlarmEntity>> = repository
        .getAllAlarms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    // ─── Create alarm ─────────────────────────────────────────────────────────
    fun createAlarm(
        hour: Int,
        minute: Int,
        label: String,
        repeatDays: String = "",
        vibrate: Boolean = true,
    ) {
        viewModelScope.launch {
            val alarm = AlarmEntity(
                hour = hour,
                minute = minute,
                label = label,
                repeatDays = repeatDays,
                vibrate = vibrate,
                isEnabled = true,
            )
            repository.createAlarm(alarm)
        }
    }

    // ─── Toggle alarm ─────────────────────────────────────────────────────────
    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.toggleAlarm(alarm)
        }
    }

    // ─── Delete alarm ─────────────────────────────────────────────────────────
    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
        }
    }

    // ─── Update alarm ─────────────────────────────────────────────────────────
    fun updateAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.updateAlarm(alarm)
        }
    }
}