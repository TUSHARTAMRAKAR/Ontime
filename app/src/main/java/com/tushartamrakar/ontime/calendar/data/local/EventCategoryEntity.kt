package com.tushartamrakar.ontime.calendar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_categories")
data class EventCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String,   // e.g. "#FF5252"
    val emoji: String,      // e.g. "💼"
    val isDefault: Boolean = false,
)
