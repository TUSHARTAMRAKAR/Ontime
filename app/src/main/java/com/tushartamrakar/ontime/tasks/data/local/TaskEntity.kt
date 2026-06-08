package com.tushartamrakar.ontime.tasks.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── Priority levels ───────────────────────────────────────────────────────────
// Stored as String to match AlarmEntity pattern (no TypeConverters needed)
// P1 = Critical (red), P2 = Important (orange), P3 = Nice to have (purple)
enum class TaskPriority { NONE, P1, P2, P3 }

// ─── Sync status for Google Tasks API ─────────────────────────────────────────
enum class SyncStatus { LOCAL, SYNCED, PENDING_SYNC, PENDING_DELETE }

// ─── Task List entity ──────────────────────────────────────────────────────────
@Entity(tableName = "task_lists")
data class TaskListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val colorHex: String = "#7C3AED",       // default = Primary purple
    val position: Int = 0,
    val isDefault: Boolean = false,          // true for "My Tasks" — can't be deleted
    val googleListId: String? = null,        // Google Tasks API list ID
    val createdAt: Long = System.currentTimeMillis(),
)

// ─── Task entity ───────────────────────────────────────────────────────────────
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val notes: String? = null,
    val dueDate: Long? = null,               // timestamp, null = no due date
    val isStarred: Boolean = false,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val listId: Long = 1L,                   // FK → task_lists.id
    val parentTaskId: Int? = null,           // null = root task, set = subtask
    val priority: String = TaskPriority.NONE.name,    // stored as String
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val googleTaskId: String? = null,        // Google Tasks API task ID
    val syncStatus: String = SyncStatus.LOCAL.name,   // stored as String
) {
    // Convenience helpers — NOT named get*() to avoid Java getter collision with Room
    fun priorityEnum(): TaskPriority = TaskPriority.valueOf(priority)
    fun syncStatusEnum(): SyncStatus = SyncStatus.valueOf(syncStatus)
}
