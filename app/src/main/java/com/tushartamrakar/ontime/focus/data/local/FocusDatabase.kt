package com.tushartamrakar.ontime.focus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FocusSessionEntity::class,
        FocusStreakEntity::class,
        BlockedAppEntity::class,
        PlannerTaskEntity::class,
        FocusSettingsEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun focusDao(): FocusDao

    companion object {
        const val DATABASE_NAME = "ontime_focus.db"
    }
}
