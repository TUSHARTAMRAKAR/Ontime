package com.tushartamrakar.ontime.focus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per planned task in the Planner section.
 *
 * Tasks are per-day — PlannerScreen shows tasks for LocalDate.now() by default.
 * Lightweight: not a full task manager (Ontime already has Tasks),
 * just session-level planning: "what am I focusing on today + how many pomodoros?"
 *
 * When the user taps a task on PlannerScreen, it starts a focus session
 * with this task's title pre-filled as the taskLabel on FocusSessionEntity.
 */
@Entity(tableName = "planner_tasks")
data class PlannerTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val title: String,
    val date: String,                          // "YYYY-MM-DD" — which day this task is for
    val isCompleted: Boolean = false,
    val estimatedPomodoros: Int = 1,           // how many work sessions user thinks it'll take
    val completedPomodoros: Int = 0,           // incremented when a session with this label completes
    val sortOrder: Int = 0,                    // for drag-to-reorder
    val createdAt: Long = System.currentTimeMillis(),
)
