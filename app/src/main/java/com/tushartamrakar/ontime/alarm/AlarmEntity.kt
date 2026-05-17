package com.tushartamrakar.ontime.alarm.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val isEnabled: Boolean = true,
    val repeatDays: String = "",
    val sound: String = "default",
    val vibrate: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)