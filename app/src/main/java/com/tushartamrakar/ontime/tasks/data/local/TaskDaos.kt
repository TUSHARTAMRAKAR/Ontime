package com.tushartamrakar.ontime.tasks.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {
    @Query("SELECT * FROM task_lists ORDER BY position ASC, createdAt ASC")
    fun getAllLists(): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM task_lists WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultList(): TaskListEntity?

    @Query("SELECT * FROM task_lists WHERE id = :id")
    suspend fun getListById(id: Long): TaskListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: TaskListEntity): Long

    @Update
    suspend fun update(list: TaskListEntity)

    @Delete
    suspend fun delete(list: TaskListEntity)
}

@Dao
interface TaskDao {

    // ─── Active tasks per list (root tasks only, not subtasks) ────────────────
    @Query("""
        SELECT * FROM tasks
        WHERE listId = :listId
          AND isCompleted = 0
          AND parentTaskId IS NULL
        ORDER BY position ASC, createdAt DESC
    """)
    fun getTasksByList(listId: Long): Flow<List<TaskEntity>>

    // ─── Starred tasks across ALL lists ──────────────────────────────────────
    @Query("""
        SELECT * FROM tasks
        WHERE isStarred = 1
          AND isCompleted = 0
          AND parentTaskId IS NULL
        ORDER BY priority ASC, position ASC, createdAt DESC
    """)
    fun getStarredTasks(): Flow<List<TaskEntity>>

    // ─── Subtasks of a parent task ────────────────────────────────────────────
    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId ORDER BY position ASC")
    fun getSubtasks(parentId: Int): Flow<List<TaskEntity>>

    // ─── Completed tasks per list ─────────────────────────────────────────────
    @Query("""
        SELECT * FROM tasks
        WHERE listId = :listId
          AND isCompleted = 1
          AND parentTaskId IS NULL
        ORDER BY completedAt DESC
    """)
    fun getCompletedTasks(listId: Long): Flow<List<TaskEntity>>

    // ─── Tasks with due date — used by Calendar for dot indicators ───────────
    @Query("""
        SELECT * FROM tasks
        WHERE dueDate IS NOT NULL
          AND isCompleted = 0
          AND parentTaskId IS NULL
        ORDER BY dueDate ASC
    """)
    fun getAllTasksWithDueDate(): Flow<List<TaskEntity>>

    // ─── Tasks due in date range ───────────────────────────────────────────────
    @Query("""
        SELECT * FROM tasks
        WHERE dueDate BETWEEN :from AND :to
          AND isCompleted = 0
    """)
    fun getTasksInDateRange(from: Long, to: Long): Flow<List<TaskEntity>>

    // ─── Pending Google sync ──────────────────────────────────────────────────
    @Query("SELECT * FROM tasks WHERE syncStatus IN ('PENDING_SYNC', 'PENDING_DELETE')")
    suspend fun getAllPendingSync(): List<TaskEntity>

    // ─── Single task by ID ────────────────────────────────────────────────────
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): TaskEntity?

    // ─── CRUD ─────────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    // ─── Delete all subtasks when parent is deleted ───────────────────────────
    @Query("DELETE FROM tasks WHERE parentTaskId = :parentId")
    suspend fun deleteSubtasks(parentId: Int)

    // ─── Delete all completed tasks in a list ─────────────────────────────────
    @Query("DELETE FROM tasks WHERE listId = :listId AND isCompleted = 1")
    suspend fun deleteCompleted(listId: Long)

    // ─── Delete all tasks in a list (when list is deleted) ───────────────────
    @Query("DELETE FROM tasks WHERE listId = :listId")
    suspend fun deleteAllInList(listId: Long)

    // ─── Count for progress bar ───────────────────────────────────────────────
    @Query("SELECT COUNT(*) FROM tasks WHERE listId = :listId AND isCompleted = 0 AND parentTaskId IS NULL")
    fun getActiveCount(listId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE listId = :listId AND isCompleted = 1 AND parentTaskId IS NULL")
    fun getCompletedCount(listId: Long): Flow<Int>
}
