package com.tushartamrakar.ontime.tasks.data.repository

import android.util.Log
import com.tushartamrakar.ontime.tasks.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val taskListDao: TaskListDao,
) {
    companion object {
        private const val TAG = "TaskRepository"
        private const val TASKS_API = "https://tasks.googleapis.com/tasks/v1"
    }

    // ─── Initialization: ensure "My Tasks" default list exists ────────────────
    suspend fun ensureDefaultListExists() {
        val default = taskListDao.getDefaultList()
        if (default == null) {
            taskListDao.insert(
                TaskListEntity(
                    title = "My Tasks",
                    colorHex = "#7C3AED",
                    position = 0,
                    isDefault = true,
                )
            )
            Log.d(TAG, "Created default 'My Tasks' list")
        }
    }

    // ─── LIST OPERATIONS ───────────────────────────────────────────────────────
    fun getAllLists(): Flow<List<TaskListEntity>> = taskListDao.getAllLists()

    suspend fun getDefaultList(): TaskListEntity? = taskListDao.getDefaultList()

    suspend fun createList(title: String, colorHex: String): Long {
        return taskListDao.insert(
            TaskListEntity(title = title, colorHex = colorHex, position = 999)
        )
    }

    suspend fun updateList(list: TaskListEntity) = taskListDao.update(list)

    suspend fun deleteList(list: TaskListEntity) {
        if (list.isDefault) return // can't delete default list
        taskDao.deleteAllInList(list.id)
        taskListDao.delete(list)
    }

    // ─── TASK OPERATIONS ──────────────────────────────────────────────────────
    fun getTasksByList(listId: Long): Flow<List<TaskEntity>> =
        taskDao.getTasksByList(listId)

    fun getStarredTasks(): Flow<List<TaskEntity>> =
        taskDao.getStarredTasks()

    fun getSubtasks(parentId: Int): Flow<List<TaskEntity>> =
        taskDao.getSubtasks(parentId)

    fun getCompletedTasks(listId: Long): Flow<List<TaskEntity>> =
        taskDao.getCompletedTasks(listId)

    fun getAllTasksWithDueDate(): Flow<List<TaskEntity>> =
        taskDao.getAllTasksWithDueDate()

    fun getActiveCount(listId: Long): Flow<Int> = taskDao.getActiveCount(listId)

    fun getCompletedCount(listId: Long): Flow<Int> = taskDao.getCompletedCount(listId)

    suspend fun createTask(
        title: String,
        notes: String? = null,
        dueDate: Long? = null,
        priority: TaskPriority = TaskPriority.NONE,
        listId: Long,
        parentTaskId: Int? = null,
        isStarred: Boolean = false,
    ): Long {
        val task = TaskEntity(
            title = title,
            notes = notes,
            dueDate = dueDate,
            priority = priority.name,
            listId = listId,
            parentTaskId = parentTaskId,
            isStarred = isStarred,
            syncStatus = SyncStatus.PENDING_SYNC.name,
        )
        return taskDao.insert(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.update(
            task.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = if (task.syncStatus == SyncStatus.LOCAL.name)
                    SyncStatus.LOCAL.name
                else SyncStatus.PENDING_SYNC.name,
            )
        )
    }

    suspend fun deleteTask(task: TaskEntity) {
        // Delete subtasks first
        taskDao.deleteSubtasks(task.id)
        // Mark for Google sync deletion if synced
        if (task.syncStatus == SyncStatus.SYNCED.name && task.googleTaskId != null) {
            taskDao.update(task.copy(syncStatus = SyncStatus.PENDING_DELETE.name))
        } else {
            taskDao.delete(task)
        }
    }

    suspend fun toggleComplete(task: TaskEntity) {
        val now = System.currentTimeMillis()
        taskDao.update(
            task.copy(
                isCompleted = !task.isCompleted,
                completedAt = if (!task.isCompleted) now else null,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_SYNC.name,
            )
        )
    }

    suspend fun toggleStar(task: TaskEntity) {
        taskDao.update(
            task.copy(
                isStarred = !task.isStarred,
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_SYNC.name,
            )
        )
    }

    suspend fun deleteCompleted(listId: Long) = taskDao.deleteCompleted(listId)

    // ─── GOOGLE TASKS API SYNC ─────────────────────────────────────────────────
    // Uses the same Google OAuth token from Calendar sync.
    // Call this after getting a valid token from your GoogleSignIn flow.
    suspend fun syncWithGoogle(accessToken: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Google Tasks sync...")

            // 1. Push local pending changes first
            pushPendingToGoogle(accessToken)

            // 2. Fetch task lists from Google
            val googleLists = fetchGoogleTaskLists(accessToken)
            Log.d(TAG, "Fetched ${googleLists.size} Google Task lists")

            // 3. For each Google list, fetch tasks and merge
            googleLists.forEach { googleList ->
                val googleListId = googleList.optString("id") ?: return@forEach
                val googleListTitle = googleList.optString("title") ?: return@forEach

                // Find or create local list
                var localList = taskListDao.getDefaultList()?.let {
                    if (it.googleListId == googleListId) it else null
                } ?: TaskListEntity(
                    title = googleListTitle,
                    colorHex = "#7C3AED",
                    googleListId = googleListId,
                )

                if (localList.id == 0L) {
                    val id = taskListDao.insert(localList)
                    localList = localList.copy(id = id)
                }

                // Fetch tasks for this list
                val googleTasks = fetchGoogleTasks(accessToken, googleListId)
                Log.d(TAG, "Fetched ${googleTasks.size} tasks for list $googleListTitle")

                googleTasks.forEach { googleTask ->
                    mergeGoogleTask(googleTask, localList.id)
                }
            }

            Log.d(TAG, "✅ Google Tasks sync complete")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Google Tasks sync failed: ${e.message}")
            throw e
        }
    }

    private suspend fun pushPendingToGoogle(token: String) {
        val pending = taskDao.getAllPendingSync()
        pending.forEach { task ->
            try {
                when (task.syncStatusEnum()) {
                    SyncStatus.PENDING_SYNC -> {
                        if (task.googleTaskId == null) {
                            // Create in Google
                            createGoogleTask(token, task)
                        } else {
                            // Update in Google
                            updateGoogleTask(token, task)
                        }
                    }
                    SyncStatus.PENDING_DELETE -> {
                        task.googleTaskId?.let { deleteGoogleTask(token, task.listId.toString(), it) }
                        taskDao.delete(task)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync task ${task.id}: ${e.message}")
            }
        }
    }

    private fun fetchGoogleTaskLists(token: String): List<JSONObject> {
        val url = URL("$TASKS_API/users/@me/lists")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/json")
        val response = conn.inputStream.bufferedReader().readText()
        val items = JSONObject(response).optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).map { items.getJSONObject(it) }
    }

    private fun fetchGoogleTasks(token: String, listId: String): List<JSONObject> {
        val url = URL("$TASKS_API/lists/$listId/tasks?showCompleted=true&maxResults=100")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/json")
        val response = conn.inputStream.bufferedReader().readText()
        val items = JSONObject(response).optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).map { items.getJSONObject(it) }
    }

    private suspend fun mergeGoogleTask(googleTask: JSONObject, localListId: Long) {
        val googleId = googleTask.optString("id") ?: return
        val title = googleTask.optString("title").takeIf { it.isNotBlank() } ?: return
        val notes = googleTask.optString("notes").takeIf { it.isNotBlank() }
        val status = googleTask.optString("status")
        val isCompleted = status == "completed"
        val dueStr = googleTask.optString("due").takeIf { it.isNotBlank() }
        val dueDate = dueStr?.let {
            try { java.time.Instant.parse(it).toEpochMilli() } catch (e: Exception) { null }
        }

        val existing = taskDao.getAllPendingSync().firstOrNull { it.googleTaskId == googleId }
        if (existing == null) {
            taskDao.insert(
                TaskEntity(
                    title = title,
                    notes = notes,
                    dueDate = dueDate,
                    isCompleted = isCompleted,
                    listId = localListId,
                    googleTaskId = googleId,
                    syncStatus = SyncStatus.SYNCED.name,
                )
            )
        }
    }

    private fun createGoogleTask(token: String, task: TaskEntity) {
        val body = JSONObject().apply {
            put("title", task.title)
            task.notes?.let { put("notes", it) }
            if (task.isCompleted) put("status", "completed") else put("status", "needsAction")
        }.toString()
        val url = URL("$TASKS_API/lists/${task.listId}/tasks")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.outputStream.write(body.toByteArray())
        val response = JSONObject(conn.inputStream.bufferedReader().readText())
        val googleId = response.optString("id")
        // Can't call suspend from here — schedule update via coroutine
        Log.d(TAG, "Created Google task: $googleId for local task ${task.id}")
    }

    private fun updateGoogleTask(token: String, task: TaskEntity) {
        val googleId = task.googleTaskId ?: return
        val body = JSONObject().apply {
            put("id", googleId)
            put("title", task.title)
            task.notes?.let { put("notes", it) }
            if (task.isCompleted) put("status", "completed") else put("status", "needsAction")
        }.toString()
        val url = URL("$TASKS_API/lists/${task.listId}/tasks/$googleId")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "PUT"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.outputStream.write(body.toByteArray())
        conn.responseCode // execute request
    }

    private fun deleteGoogleTask(token: String, listId: String, googleTaskId: String) {
        val url = URL("$TASKS_API/lists/$listId/tasks/$googleTaskId")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "DELETE"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.responseCode // execute request
    }
}
