package com.tushartamrakar.ontime.tasks.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tushartamrakar.ontime.tasks.data.local.*
import com.tushartamrakar.ontime.tasks.data.repository.TaskRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

const val STARRED_LIST_ID = -1L

// ─── Sort order enum ───────────────────────────────────────────────────────────
enum class TaskSortOrder(val label: String) {
    MY_ORDER("My Order"),
    DUE_DATE("Due Date"),
    PRIORITY("Priority"),
    STARRED_FIRST("Starred First"),
    TITLE("Title (A → Z)"),
    DATE_CREATED("Date Created"),
}

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val firebaseAuth: FirebaseAuth,
) : ViewModel() {

    // ─── Lists ─────────────────────────────────────────────────────────────────
    val lists: StateFlow<List<TaskListEntity>> = repository.getAllLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── Selected tab ──────────────────────────────────────────────────────────
    private val _selectedListId = MutableStateFlow(STARRED_LIST_ID)
    val selectedListId: StateFlow<Long> = _selectedListId

    // ─── Sort order (per session — can be persisted to DataStore later) ────────
    private val _sortOrder = MutableStateFlow(TaskSortOrder.MY_ORDER)
    val sortOrder: StateFlow<TaskSortOrder> = _sortOrder

    fun setSortOrder(order: TaskSortOrder) {
        _sortOrder.value = order
    }

    // ─── Helper: apply sort to a list of tasks ─────────────────────────────────
    private fun List<TaskEntity>.applySortOrder(order: TaskSortOrder): List<TaskEntity> =
        when (order) {
            TaskSortOrder.MY_ORDER      -> sortedBy { it.position }
            TaskSortOrder.DUE_DATE      -> sortedWith(
                compareBy(nullsLast()) { it.dueDate }
            )
            TaskSortOrder.PRIORITY      -> sortedWith(
                compareBy {
                    when (it.priority) {
                        TaskPriority.P1.name   -> 0
                        TaskPriority.P2.name   -> 1
                        TaskPriority.P3.name   -> 2
                        else                    -> 3
                    }
                }
            )
            TaskSortOrder.STARRED_FIRST -> sortedWith(
                compareByDescending<TaskEntity> { it.isStarred }.thenBy { it.position }
            )
            TaskSortOrder.TITLE         -> sortedBy { it.title.lowercase() }
            TaskSortOrder.DATE_CREATED  -> sortedByDescending { it.createdAt }
        }

    // ─── Starred tasks ─────────────────────────────────────────────────────────
    @OptIn(ExperimentalCoroutinesApi::class)
    val starredTasks: StateFlow<List<TaskEntity>> = _sortOrder.flatMapLatest { order ->
        repository.getStarredTasks().map { it.applySortOrder(order) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── Active tasks for selected list ───────────────────────────────────────
    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<TaskEntity>> = combine(
        _selectedListId, _sortOrder
    ) { listId, order -> Pair(listId, order) }.flatMapLatest { (listId, order) ->
        if (listId == STARRED_LIST_ID) flowOf(emptyList())
        else repository.getTasksByList(listId).map { it.applySortOrder(order) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── Completed tasks ───────────────────────────────────────────────────────
    @OptIn(ExperimentalCoroutinesApi::class)
    val completedTasks: StateFlow<List<TaskEntity>> = _selectedListId.flatMapLatest { listId ->
        if (listId == STARRED_LIST_ID) flowOf(emptyList())
        else repository.getCompletedTasks(listId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── Subtasks ──────────────────────────────────────────────────────────────
    private val _subtaskFlows = HashMap<Int, StateFlow<List<TaskEntity>>>()
    fun getSubtasks(parentId: Int): StateFlow<List<TaskEntity>> {
        return _subtaskFlows.getOrPut(parentId) {
            repository.getSubtasks(parentId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    // ─── Counts for progress bar ────────────────────────────────────────────────
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeCount: StateFlow<Int> = _selectedListId.flatMapLatest { listId ->
        if (listId == STARRED_LIST_ID) flowOf(0)
        else repository.getActiveCount(listId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val completedCount: StateFlow<Int> = _selectedListId.flatMapLatest { listId ->
        if (listId == STARRED_LIST_ID) flowOf(0)
        else repository.getCompletedCount(listId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ─── Sync ──────────────────────────────────────────────────────────────────
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    // ─── Google account connection status ──────────────────────────────────────
    // True if user is signed in AND has Google provider linked
    val isGoogleConnected: StateFlow<Boolean> = MutableStateFlow(
        firebaseAuth.currentUser?.providerData
            ?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true
    )

    // ─── Init ──────────────────────────────────────────────────────────────────
    init {
        viewModelScope.launch {
            repository.ensureDefaultListExists()
            val default = repository.getDefaultList()
            if (_selectedListId.value == STARRED_LIST_ID && default != null) {
                _selectedListId.value = default.id
            }
        }
    }

    // ─── Tab selection ─────────────────────────────────────────────────────────
    fun selectList(listId: Long) {
        _selectedListId.value = listId
    }

    fun selectStarredTab() {
        _selectedListId.value = STARRED_LIST_ID
    }

    // ─── Task CRUD ─────────────────────────────────────────────────────────────
    fun createTask(
        title: String,
        notes: String? = null,
        dueDate: Long? = null,
        priority: TaskPriority = TaskPriority.NONE,
        listId: Long = _selectedListId.value.takeIf { it != STARRED_LIST_ID } ?: 1L,
        parentTaskId: Int? = null,
        isStarred: Boolean = false,
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.createTask(
                title = title.trim(),
                notes = notes?.trim()?.takeIf { it.isNotBlank() },
                dueDate = dueDate,
                priority = priority,
                listId = listId,
                parentTaskId = parentTaskId,
                isStarred = isStarred,
            )
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch { repository.updateTask(task) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    fun toggleComplete(task: TaskEntity) {
        viewModelScope.launch { repository.toggleComplete(task) }
    }

    fun toggleStar(task: TaskEntity) {
        viewModelScope.launch { repository.toggleStar(task) }
    }

    fun clearCompleted() {
        val listId = _selectedListId.value.takeIf { it != STARRED_LIST_ID } ?: return
        viewModelScope.launch { repository.deleteCompleted(listId) }
    }

    // ─── List management ───────────────────────────────────────────────────────
    fun createList(title: String, colorHex: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val id = repository.createList(title.trim(), colorHex)
            _selectedListId.value = id
        }
    }

    fun renameList(list: TaskListEntity, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch { repository.updateList(list.copy(title = newTitle.trim())) }
    }

    fun deleteList(list: TaskListEntity) {
        if (list.isDefault) return
        viewModelScope.launch {
            repository.deleteList(list)
            _selectedListId.value = repository.getDefaultList()?.id ?: STARRED_LIST_ID
        }
    }

    // ─── Google Tasks sync ─────────────────────────────────────────────────────
    fun syncWithGoogle(accessToken: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            try {
                repository.syncWithGoogle(accessToken)
            } catch (e: Exception) {
                _syncError.value = "Sync failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearSyncError() {
        _syncError.value = null
    }
}
