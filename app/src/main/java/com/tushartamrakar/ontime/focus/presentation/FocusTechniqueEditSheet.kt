package com.tushartamrakar.ontime.focus.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.PrimaryGlow
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary
import com.tushartamrakar.ontime.focus.data.local.AmbientSound
import com.tushartamrakar.ontime.focus.data.local.CustomPreset
import com.tushartamrakar.ontime.focus.data.local.TechniqueType
import com.tushartamrakar.ontime.core.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTechniqueEditSheet(
    viewModel: FocusViewModel,
    onDismiss: () -> Unit,
) {
    val settings      by viewModel.settings.collectAsState()
    val selectedSound by viewModel.selectedSound.collectAsState()

    // ── Local editable state (initialized from settings) ──────────────────────
    var activeTab by remember {
        mutableStateOf(
            runCatching { TechniqueType.valueOf(settings.techniqueType) }
                .getOrDefault(TechniqueType.POMODORO)
        )
    }
    var workMinutes       by remember { mutableStateOf(settings.workMinutes) }
    var shortBreakMinutes by remember { mutableStateOf(settings.shortBreakMinutes) }
    var numSessions       by remember { mutableStateOf(settings.sessionsBeforeLongBreak) }
    var taskLabel         by remember { mutableStateOf("") }
    var currentSound      by remember { mutableStateOf(selectedSound) }
    var strictMode        by remember { mutableStateOf(settings.strictMode) }
    var blockHomeScreen   by remember { mutableStateOf(settings.blockHomeScreen) }
    var customPreset      by remember {
        mutableStateOf(
            runCatching { CustomPreset.valueOf(settings.customPreset) }
                .getOrDefault(CustomPreset.MANUAL)
        )
    }

    // ── Sub-sheet open flags ──────────────────────────────────────────────────
    var showWorkPicker       by remember { mutableStateOf(false) }
    var showBreakSheet       by remember { mutableStateOf(false) }
    var showBlockedAppsSheet by remember { mutableStateOf(false) }

    val enabledBlockedAppCount by viewModel.enabledBlockedAppCount.collectAsState()

    // ── Apply & Start ─────────────────────────────────────────────────────────
    fun onApplyAndStart() {
        viewModel.saveSettings(
            settings.copy(
                techniqueType           = activeTab.name,
                lastUsedTechnique       = activeTab.name,
                workMinutes             = workMinutes,
                shortBreakMinutes       = shortBreakMinutes,
                sessionsBeforeLongBreak = numSessions,
                strictMode              = strictMode,
                blockHomeScreen         = blockHomeScreen,
                customPreset            = customPreset.name,
            )
        )
        when (activeTab) {
            TechniqueType.STOPWATCH ->
                viewModel.startStopwatchSession(taskLabel, currentSound)
            else ->
                viewModel.startFocusSession(
                    taskLabel                       = taskLabel,
                    sound                           = currentSound,
                    workMinutesOverride             = workMinutes,
                    shortBreakOverride              = shortBreakMinutes,
                    sessionsBeforeLongBreakOverride = numSessions,
                )
        }
        onDismiss()
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Sheet
    // ─────────────────────────────────────────────────────────────────────────

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Surface,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Border),
            )
        },
    ) {
        val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.65f

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {

            // ── Header ────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = "Technique",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily,
                    color      = TextPrimary,
                )
                Spacer(Modifier.height(16.dp))

                // ── Tab buttons ──────────────────────────────────────
                TechniqueTabRow(
                    activeTab       = activeTab,
                    onTabSelected   = { activeTab = it },
                )

                Spacer(Modifier.height(20.dp))
            }

            // ── Tab content ───────────────────────────────────────────
            item {
                when (activeTab) {
                    TechniqueType.POMODORO -> PomodoroTabContent(
                        workMinutes          = workMinutes,
                        shortBreakMinutes    = shortBreakMinutes,
                        numSessions          = numSessions,
                        onWorkClick          = { showWorkPicker = true },
                        onBreakSettingsClick = { showBreakSheet = true },
                        onSessionsChange     = { numSessions = it },
                    )
                    TechniqueType.STOPWATCH -> StopwatchTabContent()
                    TechniqueType.CUSTOM -> CustomTabContent(
                        workMinutes          = workMinutes,
                        shortBreakMinutes    = shortBreakMinutes,
                        numSessions          = numSessions,
                        selectedPreset       = customPreset,
                        onPresetSelected     = { preset ->
                            customPreset = preset
                            if (preset != CustomPreset.MANUAL) {
                                workMinutes       = preset.workMin
                                shortBreakMinutes = preset.breakMin
                            }
                        },
                        onWorkClick          = { showWorkPicker = true },
                        onBreakSettingsClick = { showBreakSheet = true },
                        onSessionsChange     = { numSessions = it },
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            // ── Task label ────────────────────────────────────────────
            item {
                TechniqueTextField(
                    value         = taskLabel,
                    onValueChange = { taskLabel = it },
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Sound ─────────────────────────────────────────────────
            item {
                SheetSectionLabel("Ambient sound")
                Spacer(Modifier.height(8.dp))
                TechniqueSoundRow(
                    currentSound    = currentSound,
                    onSoundSelected = { currentSound = it; viewModel.selectSound(it) },
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Blocked Apps (all tabs) ───────────────────────────────
            item {
                BlockedAppsRow(
                    count   = enabledBlockedAppCount,
                    onClick = {
                        viewModel.loadInstalledApps()
                        showBlockedAppsSheet = true
                    },
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Strict Mode (Pomodoro / Custom only) ──────────────────
            if (activeTab != TechniqueType.STOPWATCH) {
                item {
                    TechniqueToggleRow(
                        label           = "Strict Mode",
                        subtitle        = "Disables Stop button mid work session",
                        checked         = strictMode,
                        onCheckedChange = { strictMode = it },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Block Home Screen (all tabs) ───────────────────────────
            item {
                TechniqueToggleRow(
                    label           = "Block Home Screen",
                    subtitle        = "Prevent returning to launcher",
                    checked         = blockHomeScreen,
                    onCheckedChange = { blockHomeScreen = it },
                )
                Spacer(Modifier.height(24.dp))
            }

            // ── Apply & Start Now ─────────────────────────────────────
            item {
                val btnLabel = when (activeTab) {
                    TechniqueType.STOPWATCH -> "Start Stopwatch"
                    TechniqueType.CUSTOM    -> "Start Custom Session"
                    else                    -> "Start Focus Now"
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Primary)
                        .clickable { onApplyAndStart() }
                        .padding(vertical = 17.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector        = if (activeTab == TechniqueType.STOPWATCH) Icons.Filled.Timer
                                                 else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(20.dp),
                        )
                        Text(
                            text          = btnLabel,
                            fontSize      = 16.sp,
                            fontWeight    = FontWeight.Black,
                            fontFamily    = MulishFamily,
                            color         = Color.White,
                            letterSpacing = 0.4.sp,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ── Sub-sheets ────────────────────────────────────────────────────────────

    if (showBlockedAppsSheet) {
        BlockedAppsEditSheet(
            viewModel = viewModel,
            onDismiss = { showBlockedAppsSheet = false },
        )
    }

    if (showWorkPicker) {
        DurationWheelPickerSheet(
            title          = "Work Duration",
            initialMinutes = workMinutes,
            maxHours       = if (activeTab == TechniqueType.CUSTOM) 23 else 5,
            onDismiss      = { showWorkPicker = false },
            onConfirm      = { mins ->
                workMinutes   = mins
                customPreset  = CustomPreset.MANUAL  // manual override
                showWorkPicker = false
            },
        )
    }

    if (showBreakSheet) {
        BreakSettingsSheet(
            numBreaks            = numSessions,
            breakDurationMinutes = shortBreakMinutes,
            onDismiss            = { showBreakSheet = false },
            onConfirm            = { n, d ->
                numSessions       = n
                shortBreakMinutes = d
                showBreakSheet    = false
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tab row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TechniqueTabRow(
    activeTab: TechniqueType,
    onTabSelected: (TechniqueType) -> Unit,
) {
    val tabs = listOf(
        TechniqueType.POMODORO  to "🍅 Pomodoro",
        TechniqueType.STOPWATCH to "⏱ Stopwatch",
        TechniqueType.CUSTOM    to "⚙️ Custom",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceHigh),
    ) {
        tabs.forEach { (type, label) ->
            val isActive = type == activeTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isActive) Primary else Color.Transparent)
                    .clickable { onTabSelected(type) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = label,
                    fontSize   = 12.sp,
                    fontWeight = if (isActive) FontWeight.Black else FontWeight.SemiBold,
                    fontFamily = MulishFamily,
                    color      = if (isActive) Color.White else TextMuted,
                    textAlign  = TextAlign.Center,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Pomodoro tab content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PomodoroTabContent(
    workMinutes: Int,
    shortBreakMinutes: Int,
    numSessions: Int,
    onWorkClick: () -> Unit,
    onBreakSettingsClick: () -> Unit,
    onSessionsChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Session pattern preview
        SessionPatternPreview(
            workMinutes       = workMinutes,
            breakMinutes      = shortBreakMinutes,
            sessionCount      = numSessions.coerceIn(1, 4),
        )

        // Work duration
        DurationRow(
            label    = "Work",
            minutes  = workMinutes,
            onClick  = onWorkClick,
        )

        // Sessions stepper
        SessionsRow(
            numSessions      = numSessions,
            onSessionsChange = onSessionsChange,
        )

        // Break settings
        SettingsLinkRow(
            label    = "Break Settings",
            value    = "${shortBreakMinutes}m break",
            onClick  = onBreakSettingsClick,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Stopwatch tab content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StopwatchTabContent() {
    Column(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceHigh)
            .padding(20.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector        = Icons.Filled.Timer,
            contentDescription = null,
            tint               = Primary,
            modifier           = Modifier.size(36.dp),
        )
        Text(
            text       = "Count up until you stop",
            fontSize   = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color      = TextPrimary,
            textAlign  = TextAlign.Center,
        )
        Text(
            text       = "No time limit — records as a completed focus session when stopped",
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color      = TextMuted,
            textAlign  = TextAlign.Center,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Custom tab content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CustomTabContent(
    workMinutes: Int,
    shortBreakMinutes: Int,
    numSessions: Int,
    selectedPreset: CustomPreset,
    onPresetSelected: (CustomPreset) -> Unit,
    onWorkClick: () -> Unit,
    onBreakSettingsClick: () -> Unit,
    onSessionsChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Preset chips
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CustomPreset.values().forEach { preset ->
                val isActive = preset == selectedPreset
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) Primary else SurfaceHigh)
                        .border(
                            width = 1.dp,
                            color = if (isActive) Primary else Border,
                            shape = RoundedCornerShape(10.dp),
                        )
                        .clickable { onPresetSelected(preset) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = preset.displayLabel,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = MulishFamily,
                            color      = if (isActive) Color.White else TextPrimary,
                        )
                        Text(
                            text       = "${preset.workMin}/${preset.breakMin}m",
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color      = if (isActive) Color.White.copy(alpha = 0.8f) else TextMuted,
                        )
                    }
                }
            }
        }

        // Session pattern preview
        SessionPatternPreview(
            workMinutes  = workMinutes,
            breakMinutes = shortBreakMinutes,
            sessionCount = numSessions.coerceIn(1, 4),
        )

        // Work duration (always editable, even when preset chosen)
        DurationRow(label = "Work", minutes = workMinutes, onClick = onWorkClick)

        // Sessions stepper
        SessionsRow(numSessions = numSessions, onSessionsChange = onSessionsChange)

        // Break settings
        SettingsLinkRow(
            label   = "Break Settings",
            value   = "${shortBreakMinutes}m break",
            onClick = onBreakSettingsClick,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Session pattern preview — floating label timeline
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SessionPatternPreview(
    workMinutes: Int,
    breakMinutes: Int,
    sessionCount: Int,
) {
    val cycles      = sessionCount.coerceIn(1, 6)
    val totalMins   = cycles * workMinutes + (cycles - 1) * breakMinutes
    val totalH      = totalMins / 60
    val totalM      = totalMins % 60
    val totalStr    = if (totalH > 0) "${totalH}h ${totalM}m" else "${totalM}m"
    // Give breaks a minimum visual weight so they're always distinguishable in the bar
    val workW       = workMinutes.toFloat()
    val breakW      = breakMinutes.toFloat().coerceAtLeast(workW * 0.30f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {

        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text       = "Session pattern",
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color      = TextMuted,
            )
            Text(
                text       = "≈ $totalStr / cycle",
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = MulishFamily,
                color      = TextMuted,
            )
        }

        Spacer(Modifier.height(10.dp))

        // ── Row 1: work duration labels (above bar, same weights as bar) ──
        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(cycles) { i ->
                Column(
                    modifier            = Modifier.weight(workW),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text       = "S${i + 1}",
                        fontSize   = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color      = TextMuted,
                    )
                    Text(
                        text       = "${workMinutes}m",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color      = TextPrimary,
                    )
                }
                if (i < cycles - 1) Spacer(Modifier.weight(breakW))
            }
        }

        Spacer(Modifier.height(5.dp))

        // ── Row 2: the bar — thin, no text inside, just color blocks ─────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        ) {
            repeat(cycles) { i ->
                Box(
                    modifier = Modifier
                        .weight(workW)
                        .fillMaxHeight()
                        .background(Primary),
                )
                if (i < cycles - 1) {
                    Box(
                        modifier = Modifier
                            .weight(breakW)
                            .fillMaxHeight()
                            .background(Success.copy(alpha = 0.45f)),
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Row 3: break duration labels (below bar, same weights as bar) ─
        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(cycles) { i ->
                Spacer(Modifier.weight(workW))
                if (i < cycles - 1) {
                    Text(
                        text       = "☕ ${breakMinutes}m",
                        modifier   = Modifier.weight(breakW),
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color      = Success,
                        textAlign  = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Legend ────────────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            PatternLegendDot(color = Primary, label = "Focus")
            PatternLegendDot(color = Success,  label = "Break")
        }
    }
}

@Composable
private fun PatternLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text       = label,
            fontSize   = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MulishFamily,
            color      = TextMuted,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Reusable row helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DurationRow(
    label: String,
    minutes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text       = label,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily,
            color      = TextPrimary,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceHigh)
                .border(1.dp, Border, RoundedCornerShape(10.dp))
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text       = "$minutes min",
                fontSize   = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color      = Primary,
            )
        }
    }
}

@Composable
private fun SessionsRow(
    numSessions: Int,
    onSessionsChange: (Int) -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text       = "Sessions",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color      = TextPrimary,
            )
            Text(
                text       = "before long break",
                fontSize   = 11.sp,
                fontFamily = MulishFamily,
                color      = TextMuted,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepBtn(
                icon    = Icons.Filled.Remove,
                enabled = numSessions > 1,
                onClick = { onSessionsChange((numSessions - 1).coerceAtLeast(1)) },
            )
            Text(
                text       = numSessions.toString(),
                fontSize   = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = MulishFamily,
                color      = TextPrimary,
                textAlign  = TextAlign.Center,
                modifier   = Modifier
                    .width(40.dp)
                    .padding(horizontal = 4.dp),
            )
            StepBtn(
                icon    = Icons.Filled.Add,
                enabled = numSessions < 8,
                onClick = { onSessionsChange((numSessions + 1).coerceAtMost(8)) },
            )
        }
    }
}

@Composable
private fun StepBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(SurfaceHigh)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = if (enabled) TextPrimary else TextMuted,
            modifier           = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun SettingsLinkRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceHigh)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text       = label,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily,
            color      = TextPrimary,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text       = value,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color      = TextMuted,
            )
            Text(
                text       = "›",
                fontSize   = 16.sp,
                color      = TextMuted,
                fontFamily = MulishFamily,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Task label input
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TechniqueTextField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    TextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = Modifier.fillMaxWidth(),
        placeholder   = {
            Text(
                text       = "What are you working on? (optional)",
                fontFamily = MulishFamily,
                color      = TextMuted,
                fontSize   = 14.sp,
            )
        },
        singleLine    = true,
        shape         = RoundedCornerShape(12.dp),
        colors        = TextFieldDefaults.colors(
            focusedContainerColor     = SurfaceHigh,
            unfocusedContainerColor   = SurfaceHigh,
            focusedIndicatorColor     = Color.Transparent,
            unfocusedIndicatorColor   = Color.Transparent,
            focusedTextColor          = TextPrimary,
            unfocusedTextColor        = TextPrimary,
            cursorColor               = Primary,
        ),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = MulishFamily,
            fontSize   = 14.sp,
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Sound row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TechniqueSoundRow(
    currentSound: AmbientSound,
    onSoundSelected: (AmbientSound) -> Unit,
) {
    LazyRow(
        modifier          = Modifier.fillMaxWidth(),
        contentPadding    = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(AmbientSound.values()) { sound ->
            val isSelected = sound == currentSound
            val label = when (sound) {
                AmbientSound.RAIN        -> "🌧 Rain"
                AmbientSound.WHITE_NOISE -> "🌊 White"
                AmbientSound.BROWN_NOISE -> "🟤 Brown"
                AmbientSound.FOREST      -> "🌲 Forest"
                AmbientSound.OCEAN       -> "🌊 Ocean"
                AmbientSound.CAFE        -> "☕ Café"
                AmbientSound.LOFI        -> "🎵 Lofi"
                AmbientSound.SILENCE     -> "🔇 None"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Primary else SurfaceHigh)
                    .border(
                        1.dp,
                        if (isSelected) Primary else Border,
                        RoundedCornerShape(20.dp),
                    )
                    .clickable { onSoundSelected(sound) }
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
    }
}


// ─────────────────────────────────────────────────────────────────────────────
//  Blocked apps row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BlockedAppsRow(
    count: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceHigh)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text       = "Blocked Apps",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color      = TextPrimary,
            )
            Text(
                text       = if (count > 0) "$count app${if (count > 1) "s" else ""} blocked during session"
                             else "Tap to choose apps to block",
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color      = TextMuted,
            )
        }
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Primary.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text       = "$count",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color      = Primary,
                    )
                }
            }
            Text(
                text       = "›",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Light,
                fontFamily = MulishFamily,
                color      = TextMuted,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Toggle row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TechniqueToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceHigh)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = label,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color      = TextPrimary,
            )
            Text(
                text       = subtitle,
                fontSize   = 11.sp,
                fontFamily = MulishFamily,
                color      = TextMuted,
            )
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor       = Color.White,
                checkedTrackColor       = Primary,
                uncheckedThumbColor     = TextMuted,
                uncheckedTrackColor     = SurfaceHigh,
                uncheckedBorderColor    = Border,
            ),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text       = text,
        fontSize   = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = MulishFamily,
        color      = TextMuted,
        letterSpacing = 0.5.sp,
    )
}
