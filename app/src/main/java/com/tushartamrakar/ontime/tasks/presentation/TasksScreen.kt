package com.tushartamrakar.ontime.tasks.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.core.ui.theme.*
import com.tushartamrakar.ontime.tasks.data.local.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

// ─── Priority colors ───────────────────────────────────────────────────────────
val PriorityP1Color = Color(0xFFEF4444)   // Red
val PriorityP2Color = Color(0xFFF97316)   // Orange
val PriorityP3Color = Color(0xFF7C3AED)   // Purple (= Primary)
val CompleteGreen   = Color(0xFF22C55E)
val StarGold        = Color(0xFFF59E0B)

fun TaskPriority.color(): Color = when (this) {
    TaskPriority.P1   -> PriorityP1Color
    TaskPriority.P2   -> PriorityP2Color
    TaskPriority.P3   -> PriorityP3Color
    TaskPriority.NONE -> Color.Transparent
}

fun TaskPriority.label(): String = when (this) {
    TaskPriority.P1   -> "P1"
    TaskPriority.P2   -> "P2"
    TaskPriority.P3   -> "P3"
    TaskPriority.NONE -> ""
}

fun Long.toDateString(): String {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val tomorrow = today + 86_400_000L
    val taskDay = Calendar.getInstance().apply { timeInMillis = this@toDateString }
        .apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    return when {
        taskDay < today    -> "Overdue"
        taskDay == today   -> "Today"
        taskDay == tomorrow -> "Tomorrow"
        else               -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(this))
    }
}

@Composable
fun Long.dueDateColor(): Color = when {
    this < System.currentTimeMillis() -> PriorityP1Color  // overdue = red
    this - System.currentTimeMillis() < 86_400_000L -> PriorityP2Color  // <24h = orange
    else -> TextMuted
}

// ─── Main TasksScreen ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(navController: NavHostController) {
    val viewModel: TasksViewModel = hiltViewModel()

    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val selectedListId by viewModel.selectedListId.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val starredTasks by viewModel.starredTasks.collectAsStateWithLifecycle()
    val completedTasks by viewModel.completedTasks.collectAsStateWithLifecycle()
    val activeCount by viewModel.activeCount.collectAsStateWithLifecycle()
    val completedCount by viewModel.completedCount.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isGoogleConnected by viewModel.isGoogleConnected.collectAsStateWithLifecycle()
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()

    var showCreateSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var createParentId by remember { mutableStateOf<Int?>(null) }
    var showNewListDialog by remember { mutableStateOf(false) }
    var showCompletedSection by remember { mutableStateOf(false) }
    var showListMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    // Sync card state lives at screen level — NOT inside TopBar
    // This prevents the card from pushing tab row down when it appears
    val spinAnim = remember { Animatable(0f) }
    var showSyncCard by remember { mutableStateOf(false) }
    val syncScope = rememberCoroutineScope()

    LaunchedEffect(showSyncCard) {
        if (showSyncCard) {
            kotlinx.coroutines.delay(3000)
            showSyncCard = false
        }
    }

    var showSortSheet by remember { mutableStateOf(false) }
    var showListMenuSheet by remember { mutableStateOf(false) }

    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    // Create / Edit sheet
    if (showCreateSheet || editingTask != null) {
        CreateEditTaskSheet(
            task = editingTask,
            defaultListId = selectedListId.takeIf { it != STARRED_LIST_ID } ?: lists.firstOrNull()?.id ?: 1L,
            parentTaskId = createParentId,
            lists = lists,
            onDismiss = { showCreateSheet = false; editingTask = null; createParentId = null },
            onSave = { title, notes, dueDate, priority, listId, isStarred ->
                if (editingTask != null) {
                    viewModel.updateTask(editingTask!!.copy(
                        title = title, notes = notes, dueDate = dueDate,
                        priority = priority.name, listId = listId, isStarred = isStarred,
                        updatedAt = System.currentTimeMillis(),
                    ))
                } else {
                    viewModel.createTask(title, notes, dueDate, priority, listId, createParentId, isStarred)
                }
                showCreateSheet = false; editingTask = null; createParentId = null
            },
        )
    }

    if (showNewListDialog) {
        NewListDialog(
            onDismiss = { showNewListDialog = false },
            onCreate = { title, color -> viewModel.createList(title, color) },
        )
    }

    // Sync error snackbar
    if (syncError != null) {
        LaunchedEffect(syncError) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearSyncError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TasksTopBar(
                isSyncing = isSyncing,
                isGoogleConnected = isGoogleConnected,
                onSyncClick = {
                    syncScope.launch {
                        showSyncCard = false
                        spinAnim.snapTo(0f)
                        spinAnim.animateTo(720f,
                            animationSpec = tween(1400, easing = FastOutSlowInEasing))
                        spinAnim.snapTo(0f)
                        showSyncCard = true
                    }
                },
                syncSpinAngle = spinAnim.value,
                selectedList = lists.find { it.id == selectedListId },
                isDefaultOrStarred = selectedListId == STARRED_LIST_ID ||
                    lists.find { it.id == selectedListId }?.isDefault == true,
                completedCount = completedCount,
                showListMenu = showListMenu,
                onMenuClick = { showListMenu = !showListMenu },
                onListMenuDismiss = { showListMenu = false },
                onRenameList = { showRenameDialog = true; showListMenu = false },
                onDeleteList = {
                    lists.find { it.id == selectedListId }?.let { viewModel.deleteList(it) }
                    showListMenu = false
                },
                onClearCompleted = { viewModel.clearCompleted(); showListMenu = false },
                onBackClick = { navController.popBackStack() },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = Primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(58.dp),
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(26.dp))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            // ─── Tab row ───────────────────────────────────────────────────────
            TasksTabRow(
                lists = lists,
                selectedListId = selectedListId,
                activeCount = activeCount,
                completedCount = completedCount,
                onSelectStarred = { viewModel.selectStarredTab() },
                onSelectList = { viewModel.selectList(it) },
                onNewList = { showNewListDialog = true },
            )

            // ─── List header: name + sort + 3-dot ──────────────────────────────
            val currentListName = when {
                selectedListId == STARRED_LIST_ID -> "Starred"
                else -> lists.find { it.id == selectedListId }?.title ?: "My Tasks"
            }
            val listAccentColor = when {
                selectedListId == STARRED_LIST_ID -> StarGold
                else -> try {
                    Color(android.graphics.Color.parseColor(
                        lists.find { it.id == selectedListId }?.colorHex ?: "#7C3AED"))
                } catch (e: Exception) { Primary }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    currentListName,
                    fontSize = 18.sp, fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily, color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                // Active sort badge — tap to reset
                if (sortOrder != TaskSortOrder.MY_ORDER && selectedListId != STARRED_LIST_ID) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(listAccentColor.copy(alpha = 0.15f))
                            .border(1.dp, listAccentColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .clickable { viewModel.setSortOrder(TaskSortOrder.MY_ORDER) }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(sortOrder.label, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, fontFamily = MulishFamily,
                            color = listAccentColor)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                // Sort button
                IconButton(onClick = { showSortSheet = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.SwapVert, null,
                        tint = if (sortOrder != TaskSortOrder.MY_ORDER) listAccentColor else TextMuted,
                        modifier = Modifier.size(20.dp))
                }
                // 3-dot list actions
                IconButton(onClick = { showListMenuSheet = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.MoreVert, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                }
            }

            // ─── Thin progress bar for selected list ───────────────────────────
            if (selectedListId != STARRED_LIST_ID) {
                val total = activeCount + completedCount
                if (total > 0) {
                    val progress = completedCount.toFloat() / total.toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = Primary.copy(alpha = 0.7f),
                        trackColor = Primary.copy(alpha = 0.1f),
                    )
                }
            }

            // ─── Content ───────────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 12.dp, bottom = 100.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (selectedListId == STARRED_LIST_ID) {
                    // ── Starred tab ────────────────────────────────────────────
                    if (starredTasks.isEmpty()) {
                        item { TasksEmptyState(
                        "No Starred Tasks",
                        "Nothing pinned yet. Tap ⭐ on any task to pull your most important ones right here, front and centre."
                    ) }
                    } else {
                        item {
                            TaskSectionHeader(
                                title = "STARRED",
                                icon = Icons.Filled.Star,
                                iconTint = StarGold,
                                count = starredTasks.size,
                            )
                        }
                        items(starredTasks, key = { "starred_${it.id}" }) { task ->
                            TaskRow(
                                task = task,
                                subtasksFlow = viewModel.getSubtasks(task.id),
                                onToggleComplete = { viewModel.toggleComplete(task) },
                                onToggleStar = { viewModel.toggleStar(task) },
                                onEdit = { editingTask = task },
                                onDelete = { viewModel.deleteTask(task) },
                                onAddSubtask = { createParentId = task.id; showCreateSheet = true },
                            )
                        }
                    }
                } else {
                    // ── Regular list tab ───────────────────────────────────────
                    if (tasks.isEmpty() && completedTasks.isEmpty()) {
                        item {
                            TasksEmptyState(
                                title = "No Tasks Yet",
                                subtitle = "Your productivity story starts here. Add tasks, crush goals, and stay on top of what actually matters.",
                            )
                        }
                    }

                    if (tasks.isNotEmpty()) {
                        item {
                            TaskSectionHeader(
                                title = lists.find { it.id == selectedListId }?.title?.uppercase()
                                    ?: "MY TASKS",
                                count = null,
                            )
                        }
                        items(tasks, key = { "task_${it.id}" }) { task ->
                            TaskRow(
                                task = task,
                                subtasksFlow = viewModel.getSubtasks(task.id),
                                onToggleComplete = { viewModel.toggleComplete(task) },
                                onToggleStar = { viewModel.toggleStar(task) },
                                onEdit = { editingTask = task },
                                onDelete = { viewModel.deleteTask(task) },
                                onAddSubtask = { createParentId = task.id; showCreateSheet = true },
                            )
                        }
                    }

                    // ── Completed section (collapsible) ────────────────────────
                    if (completedTasks.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showCompletedSection = !showCompletedSection }
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    if (showCompletedSection) Icons.Filled.KeyboardArrowDown
                                    else Icons.Filled.ChevronRight,
                                    null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    "COMPLETED  ${completedTasks.size}",
                                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                    fontFamily = MulishFamily, color = TextMuted,
                                    letterSpacing = 1.5.sp,
                                )
                            }
                        }
                        if (showCompletedSection) {
                            items(completedTasks, key = { "done_${it.id}" }) { task ->
                                CompletedTaskRow(
                                    task = task,
                                    onUnComplete = { viewModel.toggleComplete(task) },
                                    onDelete = { viewModel.deleteTask(task) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Sort bottom sheet
    if (showSortSheet) {
        SortBottomSheet(
            currentSort = sortOrder,
            onSelect = { viewModel.setSortOrder(it); showSortSheet = false },
            onDismiss = { showSortSheet = false },
        )
    }

    // List actions bottom sheet
    if (showListMenuSheet) {
        val currentList = lists.find { it.id == selectedListId }
        ListActionsSheet(
            listName = currentList?.title ?: "My Tasks",
            isDefault = currentList?.isDefault == true || selectedListId == STARRED_LIST_ID,
            hasCompleted = completedCount > 0,
            onRename = { showListMenuSheet = false; showRenameDialog = true },
            onDelete = {
                currentList?.let { viewModel.deleteList(it) }
                showListMenuSheet = false
            },
            onClearCompleted = { viewModel.clearCompleted(); showListMenuSheet = false },
            onDismiss = { showListMenuSheet = false },
        )
    }

    // Rename list dialog
    if (showRenameDialog) {
        val currentList = lists.find { it.id == selectedListId }
        if (currentList != null) {
            RenameListDialog(
                currentName = currentList.title,
                onDismiss = { showRenameDialog = false },
                onRename = { newName ->
                    viewModel.renameList(currentList, newName)
                    showRenameDialog = false
                },
            )
        }
    }

    // ── Floating sync info card overlay ─────────────────────────────────────
    // Lives OUTSIDE the Scaffold — zero layout impact on tabs below
    val syncCardColor = if (isGoogleConnected) Color(0xFF22C55E) else Color(0xFFF97316)
    AnimatedVisibility(
        visible = showSyncCard,
        enter = fadeIn(tween(150)) + slideInVertically(tween(200, easing = FastOutSlowInEasing)) { -it },
        exit = fadeOut(tween(300)),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 62.dp, end = 12.dp)
            .zIndex(10f),
    ) {
        Row(
            modifier = Modifier
                .width(242.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1A1A2E))
                .border(1.dp, syncCardColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                .clickable { showSyncCard = false },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(66.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                    .background(syncCardColor),
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(syncCardColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isGoogleConnected) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                    null, tint = syncCardColor, modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(
                    if (isGoogleConnected) "Synced with Google Tasks" else "No Account Connected",
                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily, color = syncCardColor,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (isGoogleConnected)
                        "All tasks are up to date across your devices."
                    else
                        "Tasks saved locally. Connect your Google account to sync everywhere.",
                    fontSize = 11.sp, fontFamily = MulishFamily,
                    color = TextMuted, lineHeight = 15.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
        }
    }

    } // end Box overlay wrapper
}

// ─── Top Bar ───────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksTopBar(
    isSyncing: Boolean,
    isGoogleConnected: Boolean,
    syncSpinAngle: Float = 0f,
    selectedList: TaskListEntity?,
    isDefaultOrStarred: Boolean,
    completedCount: Int,
    showListMenu: Boolean,
    onSyncClick: () -> Unit = {},
    onMenuClick: () -> Unit,
    onListMenuDismiss: () -> Unit,
    onRenameList: () -> Unit,
    onDeleteList: () -> Unit,
    onClearCompleted: () -> Unit,
    onBackClick: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
        navigationIcon = {
            // ── Premium back badge ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceHigh)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ArrowBack, null,
                    tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
        },
        title = {
            Text(
                "Tasks",
                fontSize = 20.sp, fontWeight = FontWeight.Black,
                fontFamily = MulishFamily, color = TextPrimary,
            )
        },
        actions = {
            // ── Sync badge ────────────────────────────────────────────────────
            val syncColor = when {
                syncSpinAngle > 0f -> if (isGoogleConnected) Color(0xFF22C55E) else Color(0xFFF97316)
                isGoogleConnected  -> Color(0xFF22C55E).copy(alpha = 0.8f)
                else               -> TextMuted
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(syncColor.copy(alpha = if (syncSpinAngle > 0f) 0.14f else 0.07f))
                    .then(
                        if (isGoogleConnected || syncSpinAngle > 0f)
                            Modifier.border(1.dp, syncColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        else Modifier
                    )
                    .clickable { onSyncClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Sync, null,
                    tint = syncColor,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = syncSpinAngle },
                )
            }

            Spacer(Modifier.width(4.dp))

            // ── 3-dot badge ───────────────────────────────────────────────────
            Box {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceHigh)
                        .clickable { onMenuClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.MoreVert, null,
                        tint = TextMuted, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = showListMenu,
                    onDismissRequest = onListMenuDismiss,
                    containerColor = Color(0xFF1E1E3A),
                ) {
                    if (!isDefaultOrStarred) {
                        DropdownMenuItem(
                            text = { Text("Rename list", color = TextPrimary, fontFamily = MulishFamily) },
                            leadingIcon = { Icon(Icons.Filled.Edit, null, tint = TextMuted) },
                            onClick = onRenameList,
                        )
                    }
                    if (completedCount > 0) {
                        DropdownMenuItem(
                            text = { Text("Clear completed", color = TextPrimary, fontFamily = MulishFamily) },
                            leadingIcon = { Icon(Icons.Filled.DeleteSweep, null, tint = TextMuted) },
                            onClick = onClearCompleted,
                        )
                    }
                    if (!isDefaultOrStarred) {
                        DropdownMenuItem(
                            text = { Text("Delete list", color = PriorityP1Color, fontFamily = MulishFamily) },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = PriorityP1Color) },
                            onClick = onDeleteList,
                        )
                    }
                }
            }

            Spacer(Modifier.width(4.dp))
        },
    )

}

// ─── Scrollable tab row ────────────────────────────────────────────────────────
@Composable
private fun TasksTabRow(
    lists: List<TaskListEntity>,
    selectedListId: Long,
    activeCount: Int,
    completedCount: Int,
    onSelectStarred: () -> Unit,
    onSelectList: (Long) -> Unit,
    onNewList: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ⭐ Starred tab
        TaskTab(
            label = "⭐  Starred",
            isSelected = selectedListId == STARRED_LIST_ID,
            color = StarGold,
            onClick = onSelectStarred,
        )

        lists.forEach { list ->
            TaskTab(
                label = list.title,
                isSelected = list.id == selectedListId,
                color = Color(android.graphics.Color.parseColor(
                    try { list.colorHex } catch (e: Exception) { "#7C3AED" }
                )),
                progress = if (list.id == selectedListId && activeCount + completedCount > 0)
                    completedCount.toFloat() / (activeCount + completedCount).toFloat()
                else null,
                onClick = { onSelectList(list.id) },
            )
        }

        // + New list button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .clickable { onNewList() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Filled.Add, null, tint = TextMuted, modifier = Modifier.size(14.dp))
            Text("New list", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily, color = TextMuted)
        }
    }

    HorizontalDivider(
        color = Color.White.copy(alpha = 0.06f),
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun TaskTab(
    label: String,
    isSelected: Boolean,
    color: Color,
    progress: Float? = null,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) color.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f)
    val border = if (isSelected) color.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)
    val textColor = if (isSelected) color else TextMuted

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily, color = textColor)
        if (progress != null && progress > 0f) {
            Spacer(Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.width(50.dp).height(2.dp).clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
            )
        }
    }
}

// ─── Section header ────────────────────────────────────────────────────────────
@Composable
private fun TaskSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = TextMuted,
    count: Int? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(14.dp))
        }
        Text(
            if (count != null) "$title  $count" else title,
            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily, color = TextMuted, letterSpacing = 1.5.sp,
        )
    }
}

// ─── Task Row (active) ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRow(
    task: TaskEntity,
    subtasksFlow: kotlinx.coroutines.flow.StateFlow<List<TaskEntity>>,
    onToggleComplete: () -> Unit,
    onToggleStar: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddSubtask: () -> Unit,
) {
    val subtasks by subtasksFlow.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    val priority = task.priorityEnum()

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onToggleComplete(); false }
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); false }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground(dismissState) },
        modifier = Modifier.animateContentSize(),
    ) {
        Column {
            // Main task card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(
                        1.dp,
                        if (priority != TaskPriority.NONE) priority.color().copy(alpha = 0.3f)
                        else Color.White.copy(alpha = 0.07f),
                        RoundedCornerShape(14.dp),
                    )
                    .clickable { onEdit() }
                    .padding(start = 0.dp, end = 12.dp, top = 0.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Priority left accent bar
                if (priority != TaskPriority.NONE) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                            .background(priority.color()),
                    )
                    Spacer(Modifier.width(12.dp))
                } else {
                    Spacer(Modifier.width(16.dp))
                }

                // Checkbox
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            if (priority != TaskPriority.NONE) priority.color()
                            else Color.White.copy(alpha = 0.3f),
                            CircleShape,
                        )
                        .clickable { onToggleComplete() },
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                    Text(
                        task.title,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = MulishFamily, color = TextPrimary,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    if (!task.notes.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            task.notes,
                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        // Due date
                        task.dueDate?.let { due ->
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Filled.CalendarToday, null,
                                    tint = due.dueDateColor(),
                                    modifier = Modifier.size(11.dp))
                                Text(due.toDateString(), fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = MulishFamily, color = due.dueDateColor())
                            }
                        }
                        // Priority badge
                        if (priority != TaskPriority.NONE) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(priority.color().copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(priority.label(), fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = MulishFamily, color = priority.color())
                            }
                        }
                        // Subtask count
                        if (subtasks.isNotEmpty()) {
                            Row(
                                modifier = Modifier.clickable { expanded = !expanded },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Icon(
                                    if (expanded) Icons.Filled.KeyboardArrowDown
                                    else Icons.Filled.ChevronRight,
                                    null, tint = TextMuted, modifier = Modifier.size(12.dp))
                                Text("${subtasks.count { it.isCompleted }}/${subtasks.size}",
                                    fontSize = 11.sp, color = TextMuted,
                                    fontFamily = MulishFamily, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // Star button
                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        if (task.isStarred) Icons.Filled.Star else Icons.Filled.StarBorder,
                        null,
                        tint = if (task.isStarred) StarGold else TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Subtasks (expanded)
            AnimatedVisibility(visible = expanded && subtasks.isNotEmpty()) {
                Column(modifier = Modifier.padding(start = 36.dp, top = 4.dp)) {
                    subtasks.forEach { sub ->
                        SubtaskRow(
                            subtask = sub,
                            onToggleComplete = { /* handle in viewmodel */ },
                        )
                    }
                    // Add subtask button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAddSubtask() }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Filled.Add, null, tint = Primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp))
                        Text("Add subtask", fontSize = 12.sp,
                            fontFamily = MulishFamily, color = Primary.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

// ─── Subtask row ───────────────────────────────────────────────────────────────
@Composable
private fun SubtaskRow(subtask: TaskEntity, onToggleComplete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .then(
                    if (subtask.isCompleted) Modifier.background(CompleteGreen)
                    else Modifier.border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                )
                .clickable { onToggleComplete() },
            contentAlignment = Alignment.Center,
        ) {
            if (subtask.isCompleted) {
                Icon(Icons.Filled.Check, null, tint = Color.White,
                    modifier = Modifier.size(10.dp))
            }
        }
        Text(
            subtask.title,
            fontSize = 12.sp, fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color = if (subtask.isCompleted) TextMuted else TextPrimary,
            textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
        )
    }
}

// ─── Completed task row ────────────────────────────────────────────────────────
@Composable
private fun CompletedTaskRow(
    task: TaskEntity,
    onUnComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Filled circle = completed
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(CompleteGreen.copy(alpha = 0.2f))
                .border(1.5.dp, CompleteGreen.copy(alpha = 0.5f), CircleShape)
                .clickable { onUnComplete() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, null, tint = CompleteGreen,
                modifier = Modifier.size(12.dp))
        }
        Text(
            task.title, fontSize = 13.sp,
            fontFamily = MulishFamily, color = TextMuted,
            textDecoration = TextDecoration.LineThrough,
            modifier = Modifier.weight(1f),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, null, tint = TextMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp))
        }
    }
}

// ─── Swipe background ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(state: SwipeToDismissBoxState) {
    val direction = state.dismissDirection
    val isStart = direction == SwipeToDismissBoxValue.StartToEnd
    val bg = if (isStart) CompleteGreen.copy(alpha = 0.8f) else PriorityP1Color.copy(alpha = 0.8f)
    val icon = if (isStart) Icons.Filled.Check else Icons.Filled.Delete
    val alignment = if (isStart) Alignment.CenterStart else Alignment.CenterEnd

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

// ─── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun TasksEmptyState(title: String, subtitle: String) {
    // ── Continuous animations ─────────────────────────────────────────────────
    val infinite = rememberInfiniteTransition(label = "empty_anim")

    // Pulse scale: 1.0 → 1.08 → 1.0 (3s cycle)
    val pulseScale by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ), label = "pulse",
    )
    // Outer ring slow rotation (8s full turn)
    val outerRotation by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(8000, easing = LinearEasing),
        ), label = "outer_rot",
    )
    // Three floating card offsets — staggered
    val float1 by infinite.animateFloat(
        initialValue = 0f, targetValue = -12f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ), label = "float1",
    )
    val float2 by infinite.animateFloat(
        initialValue = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(
            tween(1800, delayMillis = 400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ), label = "float2",
    )
    val float3 by infinite.animateFloat(
        initialValue = 0f, targetValue = -14f,
        animationSpec = infiniteRepeatable(
            tween(2600, delayMillis = 800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ), label = "float3",
    )
    // Text fade-in once
    var textVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(400)
        textVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // ── Animated graphic ──────────────────────────────────────────────────
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Outer dashed-style ring (rotates)
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer { rotationZ = outerRotation }
                    .clip(CircleShape)
                    .border(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                            listOf(
                                Primary.copy(alpha = 0.0f),
                                Primary.copy(alpha = 0.15f),
                                Primary.copy(alpha = 0.3f),
                                Primary.copy(alpha = 0.0f),
                            )
                        ),
                        shape = CircleShape,
                    ),
            )
            // Middle pulsing glow circle
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            listOf(
                                Primary.copy(alpha = 0.18f),
                                Primary.copy(alpha = 0.08f),
                                Color.Transparent,
                            )
                        )
                    ),
            )
            // Inner solid circle
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.12f))
                    .border(1.5.dp, Primary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.CheckCircleOutline, null,
                    tint = Primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(40.dp),
                )
            }
            // Floating task card 1 (top left)
            Box(
                modifier = Modifier
                    .offset(x = (-60).dp, y = (-40).dp)
                    .graphicsLayer { translationY = float1 }
                    .width(70.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.fillMaxWidth().height(6.dp)
                        .clip(CircleShape).background(Primary.copy(alpha = 0.4f)))
                    Box(Modifier.fillMaxWidth(0.7f).height(4.dp)
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.15f)))
                    Box(Modifier.fillMaxWidth(0.5f).height(4.dp)
                        .clip(CircleShape).background(PriorityP2Color.copy(alpha = 0.5f)))
                }
            }
            // Floating task card 2 (top right)
            Box(
                modifier = Modifier
                    .offset(x = 55.dp, y = (-50).dp)
                    .graphicsLayer { translationY = float2 }
                    .width(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, PriorityP1Color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(7.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.fillMaxWidth().height(6.dp)
                        .clip(CircleShape).background(PriorityP1Color.copy(alpha = 0.5f)))
                    Box(Modifier.fillMaxWidth(0.8f).height(4.dp)
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.15f)))
                }
            }
            // Floating task card 3 (bottom right)
            Box(
                modifier = Modifier
                    .offset(x = 62.dp, y = 40.dp)
                    .graphicsLayer { translationY = float3 }
                    .width(65.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, CompleteGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(7.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Check, null,
                            tint = CompleteGreen, modifier = Modifier.size(10.dp))
                        Box(Modifier.weight(1f).height(5.dp)
                            .clip(CircleShape).background(CompleteGreen.copy(alpha = 0.3f)))
                    }
                    Box(Modifier.fillMaxWidth(0.6f).height(4.dp)
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.1f)))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Text section ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = textVisible,
            enter = fadeIn(tween(600)) + slideInVertically(
                tween(600, easing = FastOutSlowInEasing)
            ) { it / 2 },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily,
                    color = TextPrimary,
                )
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    fontFamily = MulishFamily,
                    color = TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }
        }
    }
}

// ─── Create / Edit bottom sheet ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditTaskSheet(
    task: TaskEntity? = null,
    defaultListId: Long,
    parentTaskId: Int? = null,
    lists: List<TaskListEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String?, Long?, TaskPriority, Long, Boolean) -> Unit,
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var notes by remember { mutableStateOf(task?.notes ?: "") }
    var dueDate by remember { mutableStateOf(task?.dueDate) }
    var priority by remember { mutableStateOf(task?.priorityEnum() ?: TaskPriority.NONE) }
    var listId by remember { mutableStateOf(if (task != null) task.listId else defaultListId) }
    var isStarred by remember { mutableStateOf(task?.isStarred ?: false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPriorityPicker by remember { mutableStateOf(false) }
    var showListPicker by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        if (task == null) focusRequester.requestFocus()
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = dateState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK", color = Primary, fontFamily = MulishFamily) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextMuted, fontFamily = MulishFamily)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color(0xFF1A1A2E)),
        ) { DatePicker(state = dateState) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A2E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp).height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily, color = TextPrimary,
                ),
                cursorBrush = SolidColor(Primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                decorationBox = { inner ->
                    if (title.isEmpty()) Text("Task title", fontSize = 17.sp,
                        fontFamily = MulishFamily, color = TextMuted,
                        fontWeight = FontWeight.Bold)
                    inner()
                },
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Notes
            BasicTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp, fontFamily = MulishFamily, color = TextPrimary,
                ),
                cursorBrush = SolidColor(Primary),
                decorationBox = { inner ->
                    if (notes.isEmpty()) Text("Add notes...", fontSize = 14.sp,
                        fontFamily = MulishFamily, color = TextMuted)
                    inner()
                },
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // ── Options row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Due date chip
                SheetOptionChip(
                    icon = Icons.Filled.CalendarToday,
                    label = dueDate?.toDateString() ?: "Due date",
                    isSet = dueDate != null,
                    color = dueDate?.dueDateColor() ?: TextMuted,
                    onClick = { showDatePicker = true },
                    onClear = { dueDate = null },
                )

                // Priority chip
                SheetOptionChip(
                    icon = Icons.Filled.Flag,
                    label = if (priority == TaskPriority.NONE) "Priority" else priority.label(),
                    isSet = priority != TaskPriority.NONE,
                    color = if (priority != TaskPriority.NONE) priority.color() else TextMuted,
                    onClick = { showPriorityPicker = !showPriorityPicker },
                    onClear = { priority = TaskPriority.NONE },
                )

                // Star chip
                SheetOptionChip(
                    icon = if (isStarred) Icons.Filled.Star else Icons.Filled.StarBorder,
                    label = "Star",
                    isSet = isStarred,
                    color = if (isStarred) StarGold else TextMuted,
                    onClick = { isStarred = !isStarred },
                    onClear = null,
                )
            }

            // Priority picker inline
            AnimatedVisibility(visible = showPriorityPicker) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TaskPriority.P1, TaskPriority.P2, TaskPriority.P3, TaskPriority.NONE)
                        .forEach { p ->
                            val isSelected = priority == p
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected && p != TaskPriority.NONE)
                                            p.color().copy(alpha = 0.2f)
                                        else Color.White.copy(alpha = 0.06f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected && p != TaskPriority.NONE) p.color()
                                        else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp),
                                    )
                                    .clickable { priority = p; showPriorityPicker = false }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (p == TaskPriority.NONE) "None" else p.label(),
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    fontFamily = MulishFamily,
                                    color = if (p != TaskPriority.NONE) p.color() else TextMuted,
                                )
                            }
                        }
                }
            }

            // List selector
            if (lists.size > 1 && parentTaskId == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .clickable { showListPicker = !showListPicker }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.List, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    Text(
                        lists.find { it.id == listId }?.title ?: "My Tasks",
                        fontSize = 13.sp, fontFamily = MulishFamily, color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.ExpandMore, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
                if (showListPicker) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF16213E))
                    ) {
                        lists.forEach { l ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { listId = l.id; showListPicker = false }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                val color = try {
                                    Color(android.graphics.Color.parseColor(l.colorHex))
                                } catch (e: Exception) { Primary }
                                Box(
                                    modifier = Modifier.size(10.dp).clip(CircleShape).background(color)
                                )
                                Text(l.title, fontSize = 13.sp,
                                    fontFamily = MulishFamily, color = TextPrimary)
                                if (l.id == listId) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Filled.Check, null, tint = Primary,
                                        modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Save button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                ) {
                    Text("Cancel", fontFamily = MulishFamily, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(title, notes.takeIf { it.isNotBlank() }, dueDate, priority, listId, isStarred)
                        }
                    },
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        disabledContainerColor = Primary.copy(alpha = 0.4f),
                    ),
                    enabled = title.isNotBlank(),
                ) {
                    Text(
                        if (task != null) "Save changes" else "Add task",
                        fontFamily = MulishFamily, fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetOptionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSet: Boolean,
    color: Color,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSet) color.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                if (isSet) color.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            fontFamily = MulishFamily, color = color)
        if (isSet && onClear != null) {
            Icon(Icons.Filled.Close, null, tint = color.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onClear() })
        }
    }
}

// ─── New List Dialog ───────────────────────────────────────────────────────────
@Composable
fun NewListDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#7C3AED") }
    val focusRequester = remember { FocusRequester() }

    val palette = listOf(
        "#7C3AED", "#EF4444", "#F97316", "#EAB308",
        "#22C55E", "#06B6D4", "#3B82F6", "#EC4899",
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        title = {
            Text("New list", fontFamily = MulishFamily,
                fontWeight = FontWeight.Black, color = TextPrimary, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp, fontFamily = MulishFamily, color = TextPrimary,
                    ),
                    cursorBrush = SolidColor(Primary),
                    decorationBox = { inner ->
                        if (name.isEmpty()) Text("List name", fontSize = 14.sp,
                            fontFamily = MulishFamily, color = TextMuted)
                        inner()
                    },
                )
                // Color palette
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    palette.forEach { hex ->
                        val color = try { Color(android.graphics.Color.parseColor(hex)) }
                                    catch (e: Exception) { Primary }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (selectedColor == hex)
                                        Modifier.border(3.dp, Color.White, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selectedColor == hex) {
                                Icon(Icons.Filled.Check, null, tint = Color.White,
                                    modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) { onCreate(name, selectedColor); onDismiss() } },
                enabled = name.isNotBlank(),
            ) {
                Text("Create", color = Primary, fontFamily = MulishFamily,
                    fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted, fontFamily = MulishFamily)
            }
        },
    )
}

// ─── Rename List Dialog ────────────────────────────────────────────────────────
@Composable
private fun RenameListDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        title = {
            Text("Rename list", fontFamily = MulishFamily,
                fontWeight = FontWeight.Black, color = TextPrimary, fontSize = 16.sp)
        },
        text = {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp, fontFamily = MulishFamily, color = TextPrimary,
                ),
                cursorBrush = SolidColor(Primary),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onRename(name) },
                enabled = name.isNotBlank()) {
                Text("Rename", color = Primary, fontFamily = MulishFamily,
                    fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted, fontFamily = MulishFamily)
            }
        },
    )
}


// ─── Sort bottom sheet ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBottomSheet(
    currentSort: TaskSortOrder,
    onSelect: (TaskSortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A2E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp).height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            Text(
                "Sort by",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color = TextMuted,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )

            TaskSortOrder.entries.forEach { order ->
                val isSelected = order == currentSort
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(order) }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Radio dot
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                if (isSelected) Primary else Color.White.copy(alpha = 0.3f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Primary),
                            )
                        }
                    }
                    Column {
                        Text(
                            order.label,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color = if (isSelected) TextPrimary else TextMuted,
                        )
                    }
                    if (isSelected) {
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Filled.Check, null,
                            tint = Primary, modifier = Modifier.size(16.dp))
                    }
                }
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.04f),
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
    }
}

// ─── List actions bottom sheet ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListActionsSheet(
    listName: String,
    isDefault: Boolean,
    hasCompleted: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onClearCompleted: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A2E),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp).height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            // Sheet header with list name
            Text(
                listName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = MulishFamily,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.07f))

            // Rename list
            ListActionItem(
                icon = Icons.Filled.Edit,
                label = "Rename list",
                enabled = true,
                onClick = onRename,
            )

            // Delete list — disabled for default
            ListActionItem(
                icon = Icons.Filled.Delete,
                label = "Delete list",
                subtitle = if (isDefault) "Default list cannot be deleted" else null,
                enabled = !isDefault,
                dangerous = true,
                onClick = onDelete,
            )

            // Clear completed — disabled if none
            ListActionItem(
                icon = Icons.Filled.DeleteSweep,
                label = "Delete all completed tasks",
                subtitle = if (!hasCompleted) "No completed tasks" else null,
                enabled = hasCompleted,
                onClick = onClearCompleted,
            )
        }
    }
}

@Composable
private fun ListActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    dangerous: Boolean = false,
    onClick: () -> Unit,
) {
    val contentColor = when {
        !enabled  -> TextMuted.copy(alpha = 0.35f)
        dangerous -> PriorityP1Color
        else      -> TextPrimary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = contentColor, modifier = Modifier.size(20.dp))
        Column {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = MulishFamily, color = contentColor)
            if (subtitle != null) {
                Text(subtitle, fontSize = 11.sp, fontFamily = MulishFamily,
                    color = TextMuted.copy(alpha = 0.5f))
            }
        }
    }
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.04f),
        modifier = Modifier.padding(horizontal = 24.dp),
    )
}
