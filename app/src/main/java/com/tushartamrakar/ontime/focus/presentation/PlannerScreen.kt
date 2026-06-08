package com.tushartamrakar.ontime.focus.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.Danger
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.PrimaryGlow
import com.tushartamrakar.ontime.core.ui.theme.Success
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary
import com.tushartamrakar.ontime.core.ui.theme.Warning
import com.tushartamrakar.ontime.focus.data.local.PlannerTaskEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    navController: NavController,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val tasks              by viewModel.plannerTasksToday.collectAsState()
    val completedCount     by viewModel.todayCompletedTaskCount.collectAsState()
    val settings           by viewModel.settings.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var showSessionSetup by remember { mutableStateOf(false) }
    var sessionTaskLabel by remember { mutableStateOf("") }

    // Date header
    val today    = LocalDate.now()
    val dayName  = today.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())
    val dateStr  = today.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector        = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = TextPrimary,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = "Planner",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = MulishFamily,
                        color      = TextPrimary,
                    )
                    Text(
                        text       = "$dayName, $dateStr",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color      = TextMuted,
                    )
                }
                // Right spacer for symmetry
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(8.dp))

            // ── Daily goal + progress ────────────────────────────────────────
            DailyGoalCard(
                todayCompleted = completedCount,
                totalTasks     = tasks.size,
                dailyGoal      = settings.dailyGoalSessions,
                modifier       = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(16.dp))

            // ── Task list ────────────────────────────────────────────────────
            if (tasks.isEmpty()) {
                PlannerEmptyState(
                    modifier = Modifier.weight(1f),
                    onAdd    = { showAddSheet = true },
                )
            } else {
                LazyColumn(
                    modifier            = Modifier.weight(1f),
                    contentPadding      = androidx.compose.foundation.layout.PaddingValues(
                        start  = 20.dp, end = 20.dp,
                        top    = 0.dp,  bottom = 100.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Progress summary
                    item {
                        Text(
                            text = if (completedCount == 0)
                                "${tasks.size} tasks planned"
                            else
                                "$completedCount of ${tasks.size} tasks completed",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MulishFamily,
                            color      = TextMuted,
                            modifier   = Modifier.padding(bottom = 4.dp),
                        )
                    }

                    items(tasks, key = { it.id }) { task ->
                        val swipeState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart ||
                                    value == SwipeToDismissBoxValue.StartToEnd) {
                                    viewModel.deletePlannerTask(task.id)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state             = swipeState,
                            backgroundContent = { SwipeDeleteBackground(swipeState = swipeState) },
                        ) {
                            PlannerTaskCard(
                                task         = task,
                                onComplete   = { viewModel.toggleTaskComplete(task.id, !task.isCompleted) },
                                onStartFocus = {
                                    sessionTaskLabel = task.title
                                    showSessionSetup = true
                                },
                            )
                        }
                    }
                }
            }
        }

        // ── FAB ──────────────────────────────────────────────────────────────
        FloatingActionButton(
            onClick          = { showAddSheet = true },
            modifier         = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor   = Primary,
            contentColor     = Color.White,
            shape            = CircleShape,
        ) {
            Icon(
                imageVector        = Icons.Filled.Add,
                contentDescription = "Add task",
                modifier           = Modifier.size(24.dp),
            )
        }
    }

    // ── Add task bottom sheet ─────────────────────────────────────────────────
    if (showAddSheet) {
        AddTaskSheet(
            onDismiss = { showAddSheet = false },
            onAdd     = { title, pomodoros ->
                viewModel.addPlannerTask(title, pomodoros)
                showAddSheet = false
            },
        )
    }

    // ── Start session setup sheet (when tapping play on a task) ──────────────
    if (showSessionSetup) {
        FocusSessionSetupSheet(
            viewModel        = viewModel,
            initialTaskLabel = sessionTaskLabel,
            onDismiss        = { showSessionSetup = false },
            onBegin          = { label, sound ->
                showSessionSetup = false
                viewModel.startFocusSession(taskLabel = label, sound = sound)
                navController.navigateUp()  // go back to FocusScreen to see timer
            },
        )
    }
}

// ─── Daily Goal Card ──────────────────────────────────────────────────────────

@Composable
private fun DailyGoalCard(
    todayCompleted: Int,
    totalTasks: Int,
    dailyGoal: Int,
    modifier: Modifier = Modifier,
) {
    val goalMet = todayCompleted >= dailyGoal && dailyGoal > 0
    val progress = if (dailyGoal > 0)
        (todayCompleted.toFloat() / dailyGoal).coerceIn(0f, 1f)
    else 0f
    val animatedProgress by animateFloatAsState(
        targetValue    = progress,
        animationSpec  = androidx.compose.animation.core.tween(600),
        label          = "goal_progress",
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .padding(16.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text       = "Today's Goal",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = TextSecondary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text       = if (goalMet)
                        "🎉 Goal met! $todayCompleted sessions completed"
                    else if (dailyGoal > 0)
                        "$todayCompleted / $dailyGoal sessions completed"
                    else
                        "Set a daily goal in Focus Settings",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color      = if (goalMet) Success else TextMuted,
                )
            }
            // Session count badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (goalMet) Success.copy(alpha = 0.15f)
                        else Primary.copy(alpha = 0.15f)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text       = if (dailyGoal > 0) "$todayCompleted / $dailyGoal" else "$todayCompleted",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily,
                    color      = if (goalMet) Success else Primary,
                )
            }
        }

        if (dailyGoal > 0) {
            Spacer(Modifier.height(10.dp))
            // Thin progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (goalMet) Success else Primary)
                )
            }
        }
    }
}

// ─── Planner Task Card ────────────────────────────────────────────────────────

@Composable
private fun PlannerTaskCard(
    task: PlannerTaskEntity,
    onComplete: () -> Unit,
    onStartFocus: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Checkbox ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    if (task.isCompleted) Success.copy(alpha = 0.2f) else SurfaceHigh
                )
                .border(
                    1.5.dp,
                    if (task.isCompleted) Success else Border,
                    CircleShape,
                )
                .clickable { onComplete() },
            contentAlignment = Alignment.Center,
        ) {
            if (task.isCompleted) {
                Icon(
                    imageVector        = Icons.Filled.Check,
                    contentDescription = "Done",
                    tint               = Success,
                    modifier           = Modifier.size(14.dp),
                )
            }
        }

        // ── Task info ─────────────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text            = task.title,
                fontSize        = 14.sp,
                fontWeight      = FontWeight.Bold,
                fontFamily      = MulishFamily,
                color           = if (task.isCompleted) TextMuted else TextPrimary,
                textDecoration  = if (task.isCompleted) TextDecoration.LineThrough else null,
                maxLines        = 2,
            )
            Spacer(Modifier.height(4.dp))
            // Pomodoro progress pips
            PomodoroProgress(
                completed  = task.completedPomodoros,
                estimated  = task.estimatedPomodoros,
            )
        }

        // ── Start focus button (only when not completed) ──────────────────────
        if (!task.isCompleted) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.15f))
                    .clickable { onStartFocus() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Filled.PlayArrow,
                    contentDescription = "Start focus session",
                    tint               = Primary,
                    modifier           = Modifier.size(18.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Success.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Filled.Check,
                    contentDescription = "Completed",
                    tint               = Success,
                    modifier           = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ─── Pomodoro Progress Pips ───────────────────────────────────────────────────

@Composable
private fun PomodoroProgress(
    completed: Int,
    estimated: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text       = "🍅",
            fontSize   = 10.sp,
        )
        (1..estimated.coerceAtLeast(1)).forEach { i ->
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (i <= completed) Primary else SurfaceHigh
                    )
            )
        }
        if (completed > 0) {
            Text(
                text       = "$completed / $estimated",
                fontSize   = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color      = TextMuted,
            )
        }
    }
}

// ─── Swipe Delete Background ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDeleteBackground(
    swipeState: androidx.compose.material3.SwipeToDismissBoxState,
) {
    val color = if (swipeState.targetValue != SwipeToDismissBoxValue.Settled)
        Danger.copy(alpha = 0.15f)
    else
        Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector        = Icons.Filled.Delete,
            contentDescription = "Delete",
            tint               = Danger,
            modifier           = Modifier.size(20.dp),
        )
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun PlannerEmptyState(
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier              = modifier.fillMaxWidth(),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center,
    ) {
        Text("📋", fontSize = 52.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text       = "No tasks planned yet",
            fontSize   = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color      = TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text       = "Add what you want to focus on today.\nEach task can be started as a Pomodoro session.",
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color      = TextMuted,
            textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Primary)
                .clickable { onAdd() }
                .padding(horizontal = 28.dp, vertical = 14.dp),
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector        = Icons.Filled.Add,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(18.dp),
                )
                Text(
                    text       = "Add First Task",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = Color.White,
                )
            }
        }
    }
}

// ─── Add Task Bottom Sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskSheet(
    onDismiss: () -> Unit,
    onAdd: (title: String, pomodoros: Int) -> Unit,
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState()
    var title by remember { mutableStateOf("") }
    var pomodoroCount by remember { mutableStateOf(1) }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Surface,
        dragHandle       = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Border)
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text       = "Add Task",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily,
                    color      = TextPrimary,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector        = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint               = TextMuted,
                        modifier           = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Task title input
            BasicTextField(
                value         = title,
                onValueChange = { title = it },
                textStyle     = TextStyle(
                    fontFamily  = MulishFamily,
                    fontWeight  = FontWeight.Medium,
                    fontSize    = 15.sp,
                    color       = TextPrimary,
                ),
                cursorBrush   = SolidColor(Primary),
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceHigh)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                decorationBox = { inner ->
                    if (title.isEmpty()) {
                        Text(
                            text       = "Task name (e.g. Study Maths, Read chapter 3…)",
                            fontFamily = MulishFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize   = 15.sp,
                            color      = TextMuted,
                        )
                    }
                    inner()
                },
            )

            Spacer(Modifier.height(16.dp))

            // Pomodoro estimate
            Text(
                text       = "Estimated pomodoros",
                fontSize   = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color      = TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..6).forEach { count ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (count == pomodoroCount)
                                    Primary.copy(alpha = 0.2f)
                                else SurfaceHigh
                            )
                            .border(
                                1.dp,
                                if (count == pomodoroCount) Primary else Color.Transparent,
                                RoundedCornerShape(10.dp),
                            )
                            .clickable { pomodoroCount = count },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text       = "$count",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color      = if (count == pomodoroCount) Primary else TextSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Add button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (title.isNotBlank()) Primary
                        else Surface
                    )
                    .alpha(if (title.isNotBlank()) 1f else 0.4f)
                    .clickable(enabled = title.isNotBlank()) {
                        onAdd(title.trim(), pomodoroCount)
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "Add Task",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily,
                    color      = Color.White,
                )
            }
        }
    }
}
