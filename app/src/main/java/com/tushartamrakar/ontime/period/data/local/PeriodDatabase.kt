package com.tushartamrakar.ontime.period.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CycleEntity::class, PeriodDailyLog::class, PeriodSettings::class],
    version = 1,
    exportSchema = false,
)
abstract class PeriodDatabase : RoomDatabase() {
    abstract fun cycleDao(): CycleDao
    abstract fun periodLogDao(): PeriodLogDao
    abstract fun periodSettingsDao(): PeriodSettingsDao
}
