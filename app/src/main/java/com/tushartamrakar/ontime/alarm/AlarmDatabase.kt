package com.tushartamrakar.ontime.alarm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AlarmEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        const val DATABASE_NAME = "ontime_alarm_db"
    }
}