package com.tushartamrakar.ontime.focus.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.LocalOntimeColors
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
import com.tushartamrakar.ontime.focus.data.UsageData
import com.tushartamrakar.ontime.focus.data.local.AmbientSound
import com.tushartamrakar.ontime.focus.data.local.SessionType
import com.tushartamrakar.ontime.focus.data.local.TechniqueType
import com.tushartamrakar.ontime.focus.foreground.FocusTimerState
import com.tushartamrakar.ontime.focus.foreground.StopwatchTimerState
import com.tushartamrakar.ontime.navigation.Screen
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
//  FocusScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FocusScreen(
    navController: NavController,
    bottomPadding: Dp = 80.dp,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()

    // ── Collect all state ────────────────────────────────────────────────────
    val timerState              by viewModel.timerState.collectAsState()
    val stopwatchState          by viewModel.stopwatchState.collectAsState()
    val techniqueType           by viewModel.techniqueType.collectAsState()
    val settings                by viewModel.settings.collectAsState()
    val todayFocusSeconds       by viewModel.todayFocusSeconds.collectAsState()
    val todaySessionCount       by viewModel.todaySessionCount.collectAsState()
    val currentStreak           by viewModel.currentStreak.collectAsState()
    val selectedSound           by viewModel.selectedSound.collectAsState()
    val plannerTasksToday       by viewModel.plannerTasksToday.collectAsState()
    val completedTaskCount      by viewModel.todayCompletedTaskCount.collectAsState()
    val totalTaskCount          by viewModel.todayTotalTaskCount.collectAsState()
    val enabledBlockedAppCount  by viewModel.enabledBlockedAppCount.collectAsState()
    val totalDistractionsBlocked by viewModel.totalDistractionsBlocked.collectAsState()
    val usageData               by viewModel.usageData.collectAsState()

    // ── Refresh usage stats whenever today's focus seconds change ────────────
    LaunchedEffect(todayFocusSeconds) {
        viewModel.refreshUsageStats()
    }

    // ── Sheet state ──────────────────────────────────────────────────────────
    var showTechniqueSheet by remember { mutableStateOf(false) }

    // Derived: idle check covers both modes
    val isIdle = when (techniqueType) {
        TechniqueType.STOPWATCH -> stopwatchState is StopwatchTimerState.Idle
        else                    -> timerState is FocusTimerState.Idle
    }

    // ── Animated screen tint — changes with selected sound ──────────────────
    val screenTintColor by animateColorAsState(
        targetValue    = selectedSound.screenTint,
        animationSpec  = tween(durationMillis = 800),
        label          = "screenTint",
    )

    // In dark mode: full atmospheric tint (beautiful immersive effect).
    // In light mode: reduce to a whisper (10%) so the warm ivory background
    // stays visible while still hinting at the ambient sound colour.
    val isDark       = LocalOntimeColors.current.isDark
    val tintStrength = if (isDark) 1.0f else 0.10f

    ModalNavigationDrawer(
        drawerState    = drawerState,
        gesturesEnabled = true,
        drawerContent  = {
            FocusDrawer(
                currentStreakDays       = currentStreak,
                todayFocusSeconds       = todayFocusSeconds,
                todaySessionsCompleted  = todaySessionCount,
                dailyGoalSessions       = settings.dailyGoalSessions,
                plannerTotalTasks       = totalTaskCount,
                plannerCompletedTasks   = completedTaskCount,
                enabledBlockedAppsCount = enabledBlockedAppCount,
                isAdultFilterOn         = settings.adultFilterEnabled,
                onPlannerClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Planner.route)
                },
                onBlockerClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Blocker.route)
                },
                bottomPadding = bottomPadding,
            )
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .background(
                    Brush.verticalGradient(
                        0.00f to screenTintColor.copy(alpha = 1.00f * tintStrength),
                        0.30f to screenTintColor.copy(alpha = 0.75f * tintStrength),
                        0.60f to screenTintColor.copy(alpha = 0.15f * tintStrength),
                        0.80f to Color.Transparent,
                        1.00f to Color.Transparent,
                    )
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = bottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                // ── Top bar ──────────────────────────────────────────────────
                FocusTopBar(
                    onMenuClick     = { scope.launch { drawerState.open() } },
                    onSettingsClick = { navController.navigate(Screen.FocusSettings.route) },
                )

                // ── Usage / focus / efficiency pills ─────────────────────────
                UsageStatsPillsRow(
                    usageData           = usageData,
                    isSessionRunning    = timerState is FocusTimerState.Running ||
                                          stopwatchState is StopwatchTimerState.Running,
                    hasUsagePermission  = viewModel.hasUsagePermission(),
                    onRequestPermission = { viewModel.openUsagePermissionSettings() },
                    onStatsClick        = { navController.navigate(Screen.FocusStats.route) },
                )

                Spacer(Modifier.height(24.dp))

                // ── Circular timer + +/- adjustment ─────────────────────────
                TimerWithAdjustment(
                    timerState             = timerState,
                    stopwatchState         = stopwatchState,
                    techniqueType          = techniqueType,
                    workMinutes            = settings.workMinutes,
                    enabledBlockedAppCount = enabledBlockedAppCount,
                    onEditTechnique        = { showTechniqueSheet = true },
                )

                Spacer(Modifier.height(20.dp))

                // ── Sound picker (only when idle) ────────────────────────────
                AnimatedVisibility(visible = isIdle) {
                    SoundPickerRow(
                        selectedSound   = selectedSound,
                        onSoundSelected = { viewModel.selectSound(it) },
                    )
                }

                Spacer(Modifier.height(if (isIdle) 20.dp else 0.dp))
                Spacer(Modifier.weight(1f))

                // ── Streak banner ────────────────────────────────────────────
                StreakMiniCard(
                    currentStreak      = currentStreak,
                    todayFocusSeconds  = todayFocusSeconds,
                    dailyGoalSessions  = settings.dailyGoalSessions,
                    todaySessionCount  = todaySessionCount,
                    onTap              = { navController.navigate(Screen.FocusStats.route) },
                    modifier           = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )

                Spacer(Modifier.height(16.dp))

                // ── Primary action button ────────────────────────────────────
                PrimaryActionButton(
                    timerState       = timerState,
                    techniqueType    = techniqueType,
                    stopwatchState   = stopwatchState,
                    onStart          = { showTechniqueSheet = true },
                    onPause          = { viewModel.pauseSession() },
                    onResume         = { viewModel.resumeSession() },
                    onStop           = { viewModel.stopSession() },
                    onSkip           = { viewModel.skipPhase() },
                    onStopwatchPause  = { viewModel.pauseStopwatch() },
                    onStopwatchResume = { viewModel.resumeStopwatch() },
                    onStopwatchStop   = { viewModel.stopStopwatch() },
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // ── Technique / setup bottom sheet ───────────────────────────────────────
    if (showTechniqueSheet) {
        FocusTechniqueEditSheet(
            viewModel = viewModel,
            onDismiss = { showTechniqueSheet = false },
        )
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
private fun FocusTopBar(
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector        = Icons.Filled.Menu,
                contentDescription = "Menu",
                tint               = TextPrimary,
                modifier           = Modifier.size(24.dp),
            )
        }

        Text(
            text       = "Focus",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Black,
            fontFamily = MulishFamily,
            color      = TextPrimary,
        )

        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector        = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint               = TextMuted,
                modifier           = Modifier.size(22.dp),
            )
        }
    }
}

// ─── Usage Stats Pills Row ────────────────────────────────────────────────────

@Composable
private fun UsageStatsPillsRow(
    usageData: UsageData,
    isSessionRunning: Boolean,
    hasUsagePermission: Boolean,
    onRequestPermission: () -> Unit,
    onStatsClick: () -> Unit,
) {
    // Pulsing glow for focus pill when a session is running
    val pulseAnim = rememberInfiniteTransition(label = "focus_pill_pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue  = 0.35f,
        targetValue   = 0.85f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    // Efficiency colour
    val effColor = when {
        usageData.efficiencyPercent >= 40 -> Success
        usageData.efficiencyPercent >= 20 -> Warning
        else                              -> Danger
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── Pill 1: Usage time ──────────────────────────────────────────────
        StatPill(
            modifier  = Modifier.weight(1f),
            label     = "Usage",
            value     = if (hasUsagePermission) {
                val h = usageData.usageMinutes / 60
                val m = usageData.usageMinutes % 60
                if (h > 0) "${h}h ${m}m" else "${m}m"
            } else "Tap ➜",
            valueColor = if (hasUsagePermission) TextPrimary else Primary,
            borderColor = Border,
            onClick    = if (hasUsagePermission) null else onRequestPermission,
        )

        // ── Pill 2: Focus time ──────────────────────────────────────────────
        val focusH = usageData.focusMinutes / 60
        val focusM = usageData.focusMinutes % 60
        val focusStr = if (focusH > 0) "${focusH}h ${focusM}m"
                       else "${focusM}m"

        StatPill(
            modifier    = Modifier.weight(1f),
            label       = "Focus",
            value       = focusStr,
            valueColor  = Primary,
            borderColor = if (isSessionRunning)
                Primary.copy(alpha = pulseAlpha)
            else Border,
            onClick     = onStatsClick,
        )

        // ── Pill 3: Efficiency ──────────────────────────────────────────────
        StatPill(
            modifier    = Modifier.weight(1f),
            label       = "Efficiency",
            value       = "${usageData.efficiencyPercent}%",
            valueColor  = effColor,
            borderColor = Border,
            onClick     = onStatsClick,
        )
    }
}

@Composable
private fun StatPill(
    modifier: Modifier,
    label: String,
    value: String,
    valueColor: Color,
    borderColor: Color,
    onClick: (() -> Unit)?,
) {
    val baseModifier = modifier
        .clip(RoundedCornerShape(12.dp))
        .background(Surface)
        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
        .padding(horizontal = 10.dp, vertical = 10.dp)

    Box(
        modifier         = if (onClick != null)
            baseModifier.then(Modifier.clickable { onClick() })
        else baseModifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text       = value,
                fontSize   = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color      = valueColor,
                textAlign  = TextAlign.Center,
            )
            Text(
                text       = label,
                fontSize   = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = MulishFamily,
                color      = TextMuted,
                textAlign  = TextAlign.Center,
            )
            // Colored accent bar — subtle underline in the value's color
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(valueColor.copy(alpha = 0.45f)),
            )
        }
    }
}

// ─── Circular Timer + Adjustment Buttons ─────────────────────────────────────

@Composable
private fun TimerWithAdjustment(
    timerState: FocusTimerState,
    stopwatchState: StopwatchTimerState,
    techniqueType: TechniqueType,
    workMinutes: Int,
    enabledBlockedAppCount: Int,
    onEditTechnique: () -> Unit,
) {
    val isStopwatch = techniqueType == TechniqueType.STOPWATCH
    val isIdle = when {
        isStopwatch -> stopwatchState is StopwatchTimerState.Idle
        else        -> timerState is FocusTimerState.Idle
    }

    // Derive display values
    val (progress, timeText, phase) = when {
        isStopwatch -> {
            val elapsed = when (stopwatchState) {
                is StopwatchTimerState.Running -> stopwatchState.elapsedSeconds
                is StopwatchTimerState.Paused  -> stopwatchState.elapsedSeconds
                else                           -> 0
            }
            Triple(
                (elapsed % 60).toFloat() / 60f,
                formatElapsed(elapsed),
                SessionType.WORK,
            )
        }
        timerState is FocusTimerState.Running ->
            Triple(
                timerState.progress,
                "%02d:%02d".format(timerState.secondsLeft / 60, timerState.secondsLeft % 60),
                timerState.phase,
            )
        timerState is FocusTimerState.Paused  ->
            Triple(
                timerState.progress,
                "%02d:%02d".format(timerState.secondsLeft / 60, timerState.secondsLeft % 60),
                timerState.phase,
            )
        timerState is FocusTimerState.PhaseCompleted ->
            Triple(1f, "Done!", timerState.completedPhase)
        else ->
            Triple(0f, "%02d:00".format(workMinutes), SessionType.WORK)
    }

    val phaseLabel = when {
        isStopwatch -> "Flowing…"
        phase == SessionType.WORK        -> "Focus"
        phase == SessionType.SHORT_BREAK -> "Short Break"
        else                             -> "Long Break"
    }

    // Pulsing glow on running state
    val pulseAnim = rememberInfiniteTransition(label = "timer_pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue  = 0.04f,
        targetValue   = 0.12f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )
    val isRunning = timerState is FocusTimerState.Running ||
                    stopwatchState is StopwatchTimerState.Running
    val glowAlpha = if (isRunning) pulseAlpha else 0f

    // Task label for running states
    val taskLabel = when {
        isStopwatch && stopwatchState is StopwatchTimerState.Running ->
            stopwatchState.taskLabel
        isStopwatch && stopwatchState is StopwatchTimerState.Paused  ->
            stopwatchState.taskLabel
        timerState is FocusTimerState.Running ->
            timerState.taskLabel
        else -> ""
    }

    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {

        // ── Timer ring ────────────────────────────────────────────────────────
        Box(contentAlignment = Alignment.Center) {
            // Pulsing glow behind ring
            Box(
                modifier = Modifier
                    .size(270.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = glowAlpha))
            )

            CircularTimer(
                progress    = progress,
                timeText    = timeText,
                phaseLabel  = phaseLabel,
                phase       = phase,
                size        = 240.dp,
                strokeWidth = 14.dp,
                content     = {
                    Spacer(Modifier.height(8.dp))

                    // Apps blocked indicator
                    if (enabledBlockedAppCount > 0) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector        = Icons.Filled.Shield,
                                contentDescription = null,
                                tint               = Primary.copy(alpha = 0.7f),
                                modifier           = Modifier.size(12.dp),
                            )
                            Text(
                                text       = "$enabledBlockedAppCount blocked",
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = MulishFamily,
                                color      = TextMuted,
                            )
                        }
                    }

                    // Task label (when active)
                    if (taskLabel.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text       = taskLabel,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color      = TextMuted,
                            maxLines   = 1,
                        )
                    }

                    // Edit technique button (only when idle)
                    if (isIdle) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceHigh)
                                .clickable { onEditTechnique() }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector        = Icons.Filled.Edit,
                                    contentDescription = "Edit technique",
                                    tint               = Primary.copy(alpha = 0.8f),
                                    modifier           = Modifier.size(10.dp),
                                )
                                Text(
                                    text       = "Edit",
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = MulishFamily,
                                    color      = TextSecondary,
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}

/** Format elapsed seconds as H:MM:SS when ≥ 1 hour, else MM:SS. */
private fun formatElapsed(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}

// ─── Per-sound screen tint (applied to the full Focus screen background) ──────

private val AmbientSound.screenTint: Color get() = when (this) {
    AmbientSound.RAIN        -> Color(0xFF0A1828)   // stormy dark blue
    AmbientSound.WHITE_NOISE -> Color(0xFF141422)   // cool grey-slate
    AmbientSound.BROWN_NOISE -> Color(0xFF2A1000)   // warm burnt umber
    AmbientSound.FOREST      -> Color(0xFF05180A)   // deep forest green
    AmbientSound.OCEAN       -> Color(0xFF031D3A)   // deep ocean navy
    AmbientSound.CAFE        -> Color(0xFF271000)   // espresso warm
    AmbientSound.LOFI        -> Color(0xFF14062E)   // deep lofi purple
    AmbientSound.SILENCE     -> Color(0xFF0A0A0F)   // same as Background = no effect
}

// ─── Sound Picker Row ─────────────────────────────────────────────────────────

@Composable
private fun SoundPickerRow(
    selectedSound: AmbientSound,
    onSoundSelected: (AmbientSound) -> Unit,
) {
    val sounds = AmbientSound.values().toList()

    // Pulsing dot for "Now Playing" badge
    val pulse    = rememberInfiniteTransition(label = "dot_pulse")
    val dotAlpha by pulse.animateFloat(
        initialValue  = 0.5f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label         = "dot_alpha",
    )

    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {

        // ── Section header ────────────────────────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text          = "AMBIENT ATMOSPHERE",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.ExtraBold,
                fontFamily    = MulishFamily,
                color         = TextMuted,
                letterSpacing = 1.2.sp,
            )
            if (selectedSound != AmbientSound.SILENCE) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Primary.copy(alpha = 0.15f))
                        .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = dotAlpha)),
                    )
                    Text(
                        text       = "Now Playing",
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color      = Primary,
                    )
                }
            }
        }

        // ── Sound chips ───────────────────────────────────────────────────
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(sounds) { sound ->
                SoundChip(
                    sound      = sound,
                    isSelected = sound == selectedSound,
                    onClick    = { onSoundSelected(sound) },
                )
            }
        }
    }
}

@Composable
private fun SoundChip(
    sound: AmbientSound,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val label = when (sound) {
        AmbientSound.RAIN        -> "🌧 Rain"
        AmbientSound.WHITE_NOISE -> "🌬 White"
        AmbientSound.BROWN_NOISE -> "🍂 Brown"
        AmbientSound.FOREST      -> "🌲 Forest"
        AmbientSound.OCEAN       -> "🌊 Ocean"
        AmbientSound.CAFE        -> "☕ Café"
        AmbientSound.LOFI        -> "🎵 Lofi"
        AmbientSound.SILENCE     -> "🔇 None"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Primary else Surface)
            .border(1.dp, if (isSelected) Primary else Border, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text       = label,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily,
            color      = if (isSelected) Color.White else TextSecondary,
        )
    }
}

// ─── Streak Mini Card ─────────────────────────────────────────────────────────

@Composable
private fun StreakMiniCard(
    currentStreak: Int,
    todayFocusSeconds: Int,
    dailyGoalSessions: Int,
    todaySessionCount: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val goalMet = todaySessionCount >= dailyGoalSessions && dailyGoalSessions > 0

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .clickable { onTap() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text     = if (currentStreak > 0) "🔥" else "💤",
            fontSize = 16.sp,
        )
        Text(
            text       = if (currentStreak > 0)
                "Day $currentStreak streak"
            else
                "No streak yet — start today!",
            fontSize   = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily,
            color      = if (currentStreak > 0) TextPrimary else TextMuted,
            modifier   = Modifier.weight(1f),
        )
        if (goalMet) {
            Text(
                text       = "Goal ✓",
                fontSize   = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color      = Success,
            )
        } else if (dailyGoalSessions > 0) {
            Text(
                text       = "$todaySessionCount / $dailyGoalSessions",
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color      = TextMuted,
            )
        }
        Text(
            text       = "↗",
            fontSize   = 13.sp,
            color      = TextMuted,
            fontFamily = MulishFamily,
        )
    }
}

// ─── Primary Action Button ────────────────────────────────────────────────────

@Composable
private fun PrimaryActionButton(
    timerState: FocusTimerState,
    techniqueType: TechniqueType,
    stopwatchState: StopwatchTimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSkip: () -> Unit,
    onStopwatchPause: () -> Unit,
    onStopwatchResume: () -> Unit,
    onStopwatchStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (techniqueType == TechniqueType.STOPWATCH) {
        // ── Stopwatch mode ────────────────────────────────────────────────────
        when (stopwatchState) {
            is StopwatchTimerState.Idle -> {
                // Single full-width "Start Stopwatch" button
                Box(
                    modifier = modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(Primary)
                        .clickable { onStart() }
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(22.dp),
                        )
                        Text(
                            text          = "Start Stopwatch",
                            fontSize      = 16.sp,
                            fontWeight    = FontWeight.Black,
                            fontFamily    = MulishFamily,
                            color         = Color.White,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
            }

            is StopwatchTimerState.Running -> {
                Row(
                    modifier              = modifier,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Stop
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh)
                            .clickable { onStopwatchStop() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.Stop,
                            contentDescription = "Stop",
                            tint               = TextMuted,
                            modifier           = Modifier.size(24.dp),
                        )
                    }
                    // Pause
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Primary)
                            .clickable { onStopwatchPause() }
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector        = Icons.Filled.Pause,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(22.dp),
                            )
                            Text(
                                text       = "Pause",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = MulishFamily,
                                color      = Color.White,
                            )
                        }
                    }
                }
            }

            is StopwatchTimerState.Paused -> {
                Row(
                    modifier              = modifier,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Stop
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh)
                            .clickable { onStopwatchStop() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.Stop,
                            contentDescription = "Stop",
                            tint               = TextMuted,
                            modifier           = Modifier.size(24.dp),
                        )
                    }
                    // Resume
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Success)
                            .clickable { onStopwatchResume() }
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector        = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(22.dp),
                            )
                            Text(
                                text       = "Resume",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = MulishFamily,
                                color      = Color.White,
                            )
                        }
                    }
                }
            }
        }
    } else {
        // ── Pomodoro / Custom mode ─────────────────────────────────────────────
        when (timerState) {
            // Idle: single full-width start button
            is FocusTimerState.Idle -> {
                Box(
                    modifier = modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(Primary)
                        .clickable { onStart() }
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(22.dp),
                        )
                        Text(
                            text          = "Start Focus Now",
                            fontSize      = 16.sp,
                            fontWeight    = FontWeight.Black,
                            fontFamily    = MulishFamily,
                            color         = Color.White,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
            }

            // Running: stop + pause + skip
            is FocusTimerState.Running -> {
                Row(
                    modifier              = modifier,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh)
                            .clickable { onStop() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.Stop,
                            contentDescription = "Stop",
                            tint               = TextMuted,
                            modifier           = Modifier.size(24.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Primary)
                            .clickable { onPause() }
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector        = Icons.Filled.Pause,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(22.dp),
                            )
                            Text(
                                text       = "Pause",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = MulishFamily,
                                color      = Color.White,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh)
                            .clickable { onSkip() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.SkipNext,
                            contentDescription = "Skip",
                            tint               = TextMuted,
                            modifier           = Modifier.size(24.dp),
                        )
                    }
                }
            }

            // Paused: stop + resume
            is FocusTimerState.Paused -> {
                Row(
                    modifier              = modifier,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh)
                            .clickable { onStop() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.Stop,
                            contentDescription = "Stop",
                            tint               = TextMuted,
                            modifier           = Modifier.size(24.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Success)
                            .clickable { onResume() }
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector        = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(22.dp),
                            )
                            Text(
                                text       = "Resume",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = MulishFamily,
                                color      = Color.White,
                            )
                        }
                    }
                }
            }

            // PhaseCompleted: loading banner → auto advances
            is FocusTimerState.PhaseCompleted -> {
                Box(
                    modifier = modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(Success.copy(alpha = 0.15f))
                        .border(1.dp, Success.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val msg = when (timerState.completedPhase) {
                        SessionType.WORK        -> "Well done! Starting break…"
                        SessionType.SHORT_BREAK -> "Break done. Back to work…"
                        SessionType.LONG_BREAK  -> "Great break! Back to work…"
                    }
                    Text(
                        text       = msg,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color      = Success,
                    )
                }
            }
        }
    }
}
