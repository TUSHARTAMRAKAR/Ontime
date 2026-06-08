package com.tushartamrakar.ontime.alarm.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val isEnabled: Boolean = true,
    val repeatDays: String = "",
    val sound: String = "alarm_digital_alarm",
    val vibrate: Boolean = true,
    val volume: Float = 1.0f,
    val gentleWakeUpSeconds: Int = 0,
    val timeAnnouncement: Boolean = false,
    val announcementVoice: String = "female",
    val weatherReminder: Boolean = false,
    val labelReminder: Boolean = false,
    val extraLoud: Boolean = false,
    val snoozeEnabled: Boolean = true,
    val snoozeIntervalMinutes: Int = 5,
    val snoozeLimit: Int = 3,
    val snoozeProgressiveMode: Boolean = false,
    val tasks: String = "[]",
    val riseCheckMinutes: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)