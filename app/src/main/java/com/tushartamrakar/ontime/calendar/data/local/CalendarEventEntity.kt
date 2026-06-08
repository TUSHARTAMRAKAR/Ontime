package com.tushartamrakar.ontime.calendar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val location: String = "",
    val categoryId: Int = 1,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val isAllDay: Boolean = false,
    val recurrenceType: String = "NONE",
    val recurrenceEndMillis: Long? = null,

    // ─── Single reminder (kept for backward compat) ───────────────────────────
    val reminderType: String = "NONE",
    val reminderMinutesBefore: Int = 10,
    val reminderSound: String = "alarm_digital_alarm",
    val announceLabelOnReminder: Boolean = false,

    // ─── Multiple reminders as JSON ───────────────────────────────────────────
    // Format: [{"type":"ALARM","minutesBefore":60,"sound":"alarm_digital_alarm"},...]
    val remindersJson: String = "[]",

    // ─── Priority ─────────────────────────────────────────────────────────────
    val priority: String = "NONE", // NONE, LOW, MEDIUM, HIGH

    // ─── Google Calendar sync ─────────────────────────────────────────────────
    val googleEventId: String? = null,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
