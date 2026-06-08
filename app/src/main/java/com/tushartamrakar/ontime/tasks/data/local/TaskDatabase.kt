package com.tushartamrakar.ontime.tasks.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// ─── Separate database for Tasks — AlarmDatabase (v8) stays UNTOUCHED ─────────
// Starting at version 1: clean start, no migrations needed on fresh install.
// Users upgrading from an earlier version get an empty task DB (correct behavior).
@Database(
    entities = [TaskListEntity::class, TaskEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskListDao(): TaskListDao
}
