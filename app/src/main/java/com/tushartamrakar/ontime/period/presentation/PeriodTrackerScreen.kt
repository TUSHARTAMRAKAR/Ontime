package com.tushartamrakar.ontime.period.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.core.ui.theme.*
import com.tushartamrakar.ontime.period.data.local.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import kotlin.math.*

// ─── Phase colors ──────────────────────────────────────────────────────────────
fun CyclePhase.color(): Color = when (this) {
    CyclePhase.MENSTRUATION -> Color(0xFFEF4444)  // Red
    CyclePhase.FOLLICULAR   -> Color(0xFFF59E0B)  // Yellow
    CyclePhase.OVULATION    -> Color(0xFF7C3AED)  // Purple
    CyclePhase.LUTEAL       -> Color(0xFF3B82F6)  // Blue
    CyclePhase.PREDICTED    -> Color(0xFFFF6B9D)  // Pink
    CyclePhase.FERTILE      -> Color(0xFF22C55E)  // Green
    CyclePhase.NONE         -> Color.Transparent
}

fun CyclePhase.label(): String = when (this) {
    CyclePhase.MENSTRUATION -> "Period"
    CyclePhase.FOLLICULAR   -> "Follicular"
    CyclePhase.OVULATION    -> "Ovulation"
    CyclePhase.LUTEAL       -> "Luteal"
    CyclePhase.PREDICTED    -> "Predicted Period"
    CyclePhase.FERTILE      -> "Fertile Window"
    CyclePhase.NONE         -> "—"
}

fun FlowIntensity.label(): String = when (this) {
    FlowIntensity.NONE     -> "None"
    FlowIntensity.SPOTTING -> "Spotting"
    FlowIntensity.LIGHT    -> "Light"
    FlowIntensity.MEDIUM   -> "Medium"
    FlowIntensity.HEAVY    -> "Heavy"
}

fun FlowIntensity.color(): Color = when (this) {
    FlowIntensity.NONE     -> Color.Transparent
    FlowIntensity.SPOTTING -> Color(0xFFFFB3CC)
    FlowIntensity.LIGHT    -> Color(0xFFFF6B9D)
    FlowIntensity.MEDIUM   -> Color(0xFFE91E8C)
    FlowIntensity.HEAVY    -> Color(0xFFAD1457)
}

fun DailyMood.emoji(): String = when (this) {
    DailyMood.GREAT  -> "😊"
    DailyMood.OKAY   -> "😐"
    DailyMood.TIRED  -> "😴"
    DailyMood.MOODY  -> "😤"
    DailyMood.SAD    -> "😢"
    DailyMood.CRAMPS -> "😣"
}

fun DailyMood.label(): String = when (this) {
    DailyMood.GREAT  -> "Great"
    DailyMood.OKAY   -> "Okay"
    DailyMood.TIRED  -> "Tired"
    DailyMood.MOODY  -> "Moody"
    DailyMood.SAD    -> "Sad"
    DailyMood.CRAMPS -> "Cramps"
}

// ─── Main Screen ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodTrackerScreen(navController: NavHostController) {
    val viewModel: PeriodViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentPhase by viewModel.currentPhase.collectAsStateWithLifecycle()
    val daysUntilPeriod by viewModel.daysUntilPeriod.collectAsStateWithLifecycle()
    val currentCycleDay by viewModel.currentCycleDay.collectAsStateWithLifecycle()
    val nextPeriodDate by viewModel.nextPeriodDate.collectAsStateWithLifecycle()
    val ovulationDate by viewModel.ovulationDate.collectAsStateWithLifecycle()
    val fertileWindow by viewModel.fertileWindow.collectAsStateWithLifecycle()
    val avgCycleLength by viewModel.averageCycleLength.collectAsStateWithLifecycle()
    val avgPeriodLength by viewModel.averagePeriodLength.collectAsStateWithLifecycle()
    val isRegular by viewModel.isRegular.collectAsStateWithLifecycle()
    val isLate by viewModel.isLate.collectAsStateWithLifecycle()
    val daysLate by viewModel.daysLate.collectAsStateWithLifecycle()
    val phaseTip by viewModel.phaseTip.collectAsStateWithLifecycle()
    val moodInsight by viewModel.moodInsight.collectAsStateWithLifecycle()
    val recentCycles by viewModel.recentCycles.collectAsStateWithLifecycle()
    val cycleCount by viewModel.cycleCount.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedDateLog by viewModel.selectedDateLog.collectAsStateWithLifecycle()
    val calendarMonth by viewModel.calendarMonth.collectAsStateWithLifecycle()
    val phaseMap by viewModel.phaseMap.collectAsStateWithLifecycle()

    // ─── Onboarding gate ──────────────────────────────────────────────────────
    // Wait until DB has actually loaded (settingsLoaded = true), THEN check.
    // Using LaunchedEffect(Unit) prevents re-firing on every recomposition.
    // Without the guard, the default PeriodSettings(onboardingComplete=false)
    // would always trigger a redirect before DB has a chance to load.
    val settingsLoaded by viewModel.settingsLoaded.collectAsStateWithLifecycle()

    // TWO keys: settingsLoaded + onboardingComplete
    // settingsLoaded alone is NOT enough — it stays true after clearAllData()
    // and LaunchedEffect would never re-fire. Adding onboardingComplete as a key
    // means the effect re-fires whenever EITHER value changes, so:
    //   • First load:     settingsLoaded false→true, onboardingComplete=false → navigate
    //   • Normal use:     settingsLoaded=true, onboardingComplete=true → stay
    //   • After reset:    settingsLoaded=true, onboardingComplete true→false → navigate ✓
    LaunchedEffect(settingsLoaded, settings.onboardingComplete) {
        if (settingsLoaded && !settings.onboardingComplete) {
            navController.navigate("period_onboarding") {
                popUpTo("period_tracker") { inclusive = true }
            }
        }
    }

    var selectedTab by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

    // While DB is loading, show a minimal loading screen
    // This prevents any null/default-value crashes during initial render
    if (!settingsLoaded) {
        Box(
            modifier = Modifier.fillMaxSize().background(Background),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("🌸", fontSize = 36.sp)
                CircularProgressIndicator(
                    color = RosePrimary,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp,
                )
            }
        }
        return
    }

    if (selectedDate != null) {
        DailyLogSheet(
            date = selectedDate!!,
            existingLog = selectedDateLog,
            onDismiss = { viewModel.clearSelectedDate() },
            onSave = { flow, symptoms, mood, notes, temperature ->
                viewModel.saveLog(selectedDate!!, flow, symptoms, mood, notes, temperature)
            },
        )
    }

    if (showSettings) {
        CycleSettingsSheet(
            settings = settings,
            cycleCount = cycleCount,
            onDismiss = { showSettings = false },
            onToggleFertile = { viewModel.toggleFertileWindow() },
            onCycleLengthChange = { viewModel.updateCycleLength(it) },
            onRemindDaysChange = { viewModel.updateRemindDays(it) },
            onStartOver = { viewModel.clearAllData() },
        )
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null,
                            tint = TextPrimary, modifier = Modifier.size(22.dp))
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("🌸", fontSize = 18.sp)
                        Text("Period Tracker", fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = MulishFamily, color = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, null,
                            tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            // ── Late period banner ───────────────────────────────────────────
            AnimatedVisibility(visible = isLate) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🌙", fontSize = 16.sp)
                    Column {
                        Text(
                            "Your period is taking its time",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            fontFamily = MulishFamily, color = TextPrimary,
                        )
                        Text(
                            "$daysLate day${if (daysLate > 1) "s" else ""} later than expected. This is completely normal — log when it arrives 💙",
                            fontSize = 11.sp, fontFamily = MulishFamily,
                            color = TextMuted, lineHeight = 15.sp,
                        )
                    }
                }
            }

            // ── Tabs ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Dashboard", "Calendar", "History").forEachIndexed { i, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selectedTab == i) RosePrimary.copy(alpha = 0.18f)
                                        else Color.White.copy(alpha = 0.05f))
                            .border(1.dp,
                                if (selectedTab == i) RosePrimary.copy(alpha = 0.6f)
                                else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(20.dp))
                            .clickable { selectedTab = i }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            fontFamily = MulishFamily,
                            color = if (selectedTab == i) RosePrimary else TextMuted)
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f),
                modifier = Modifier.padding(horizontal = 16.dp))

            // ── Tab content ─────────────────────────────────────────────────
            when (selectedTab) {
                0 -> DashboardTab(
                    currentPhase = currentPhase,
                    daysUntilPeriod = daysUntilPeriod,
                    currentCycleDay = currentCycleDay,
                    avgCycleLength = avgCycleLength,
                    avgPeriodLength = avgPeriodLength,
                    nextPeriodDate = nextPeriodDate,
                    ovulationDate = ovulationDate,
                    fertileWindow = fertileWindow,
                    isRegular = isRegular,
                    phaseTip = phaseTip,
                    moodInsight = moodInsight,
                    showFertileWindow = settings.showFertileWindow,
                    cycleCount = cycleCount,
                    onLogToday = { viewModel.selectDate(LocalDate.now()) },
                )
                1 -> CalendarTab(
                    month = calendarMonth,
                    phaseMap = phaseMap,
                    onPrevMonth = { viewModel.navigateMonth(false) },
                    onNextMonth = { viewModel.navigateMonth(true) },
                    onDateTap = { viewModel.selectDate(it) },
                    onLogToday = { viewModel.selectDate(LocalDate.now()) },
                )
                2 -> HistoryTab(
                    cycles = recentCycles,
                    avgCycleLength = avgCycleLength,
                    avgPeriodLength = avgPeriodLength,
                    isRegular = isRegular,
                    cycleCount = cycleCount,
                )
            }
        }
    }
}

// ─── DASHBOARD TAB ────────────────────────────────────────────────────────────
@Composable
private fun DashboardTab(
    currentPhase: CyclePhase,
    daysUntilPeriod: Int?,
    currentCycleDay: Int?,
    avgCycleLength: Int,
    avgPeriodLength: Int,
    nextPeriodDate: LocalDate?,
    ovulationDate: LocalDate?,
    fertileWindow: List<LocalDate>,
    isRegular: Boolean,
    phaseTip: String,
    moodInsight: String?,
    showFertileWindow: Boolean,
    cycleCount: Int,
    onLogToday: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── Phase Ring ────────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PhaseRing(
                    currentPhase = currentPhase,
                    cycleDay = currentCycleDay ?: 1,
                    totalCycleDays = avgCycleLength,
                    periodDays = avgPeriodLength,
                )
            }
        }

        // ── Stats row ─────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard(
                    label = "Cycle Day",
                    value = currentCycleDay?.toString() ?: "—",
                    unit = "of $avgCycleLength",
                    color = currentPhase.color(),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = if ((daysUntilPeriod ?: 1) >= 0) "Period In" else "Period Late",
                    value = daysUntilPeriod?.let { if (it >= 0) "$it" else "${-it}" } ?: "—",
                    unit = "days",
                    color = if ((daysUntilPeriod ?: 1) < 0) Color(0xFFEF4444) else RosePrimary,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Avg Cycle",
                    value = "$avgCycleLength",
                    unit = "days",
                    color = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── Phase card with tip ───────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(currentPhase.color().copy(alpha = 0.1f))
                    .border(1.dp, currentPhase.color().copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(currentPhase.color()),
                    )
                    Text(
                        currentPhase.label().uppercase(),
                        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily, color = currentPhase.color(),
                        letterSpacing = 1.sp,
                    )
                }
                Text(
                    phaseTip,
                    fontSize = 14.sp, fontFamily = MulishFamily,
                    color = TextPrimary, lineHeight = 20.sp,
                )
            }
        }

        // ── Next period card ───────────────────────────────────────────────────
        if (nextPeriodDate != null) {
            item {
                InfoCard(
                    icon = "🌸",
                    title = "Next Period",
                    value = nextPeriodDate.format(DateTimeFormatter.ofPattern("d MMMM")),
                    subtitle = "Keep supplies ready 2 days before",
                    color = RosePrimary,
                )
            }
        }

        // ── Fertile window (if enabled) ────────────────────────────────────────
        if (showFertileWindow && fertileWindow.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF22C55E).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("🌱", fontSize = 16.sp)
                        Text("FERTILE WINDOW", fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily,
                            color = Color(0xFF22C55E), letterSpacing = 1.sp)
                    }
                    val start = fertileWindow.first()
                    val end = fertileWindow.last()
                    Text(
                        "${start.format(DateTimeFormatter.ofPattern("d MMM"))} – ${end.format(DateTimeFormatter.ofPattern("d MMM"))}",
                        fontSize = 18.sp, fontWeight = FontWeight.Black,
                        fontFamily = MulishFamily, color = TextPrimary,
                    )
                    ovulationDate?.let { ov ->
                        Text(
                            "Ovulation: ${ov.format(DateTimeFormatter.ofPattern("d MMMM"))} 💜",
                            fontSize = 13.sp, fontFamily = MulishFamily, color = TextMuted,
                        )
                    }
                }
            }
        }

        // ── Cycle insights ─────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("📊  Cycle Insights", fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily,
                    color = TextPrimary, letterSpacing = 0.5.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Regularity", fontSize = 13.sp,
                        fontFamily = MulishFamily, color = TextMuted)
                    Text(
                        if (isRegular) "✅ Very Regular" else "↔ Slightly Variable",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = MulishFamily,
                        color = if (isRegular) Color(0xFF22C55E) else Color(0xFFF59E0B),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Cycles tracked", fontSize = 13.sp,
                        fontFamily = MulishFamily, color = TextMuted)
                    Text("$cycleCount cycle${if (cycleCount != 1) "s" else ""}",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = MulishFamily, color = TextPrimary)
                }
                if (cycleCount < 3) {
                    Text(
                        "Track 3 cycles for personalised predictions 🌸",
                        fontSize = 12.sp, fontFamily = MulishFamily,
                        color = RosePrimary.copy(alpha = 0.8f),
                    )
                }
                moodInsight?.let {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    Text(it, fontSize = 12.sp, fontFamily = MulishFamily,
                        color = TextMuted, lineHeight = 17.sp)
                }
            }
        }

        // ── Log Today button ──────────────────────────────────────────────────
        item {
            Button(
                onClick = onLogToday,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Log Today", fontFamily = MulishFamily,
                    fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}

// ─── PHASE RING ───────────────────────────────────────────────────────────────
@Composable
private fun PhaseRing(
    currentPhase: CyclePhase,
    cycleDay: Int,
    totalCycleDays: Int,
    periodDays: Int,
) {
    val infinite = rememberInfiniteTransition(label = "ring")
    val dotPulse by infinite.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot_pulse",
    )

    val progressAngle = (cycleDay.toFloat() / totalCycleDays.toFloat()) * 360f

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            val sweepPeriod = (periodDays.toFloat() / totalCycleDays) * 360f
            val sweepFollicular = ((totalCycleDays / 2f - periodDays) / totalCycleDays) * 360f
            val sweepOvulation = (1f / totalCycleDays) * 360f
            val sweepLuteal = 360f - sweepPeriod - sweepFollicular - sweepOvulation

            // Draw 4 arcs
            val phases = listOf(
                Color(0xFFEF4444) to sweepPeriod,
                Color(0xFFF59E0B) to sweepFollicular,
                Color(0xFF7C3AED) to sweepOvulation,
                Color(0xFF3B82F6) to sweepLuteal,
            )
            var startAngle = -90f
            phases.forEach { (color, sweep) ->
                drawArc(
                    color = color.copy(alpha = 0.5f),
                    startAngle = startAngle + 1f,
                    sweepAngle = sweep - 2f,
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                )
                startAngle += sweep
            }

            // Progress highlight arc
            drawArc(
                color = Color.White.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = progressAngle,
                useCenter = false,
                style = Stroke(strokeWidth * 0.4f, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
            )

            // Today dot
            val dotAngle = Math.toRadians((progressAngle - 90f).toDouble())
            val dotX = center.x + (radius * cos(dotAngle)).toFloat()
            val dotY = center.y + (radius * sin(dotAngle)).toFloat()
            drawCircle(
                color = Color.White,
                radius = strokeWidth / 2 * dotPulse,
                center = Offset(dotX, dotY),
            )
            drawCircle(
                color = currentPhase.color(),
                radius = strokeWidth / 3 * dotPulse,
                center = Offset(dotX, dotY),
            )
        }

        // Center content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Day $cycleDay",
                fontSize = 32.sp, fontWeight = FontWeight.Black,
                fontFamily = MulishFamily, color = TextPrimary,
            )
            Text(
                currentPhase.label(),
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily, color = currentPhase.color(),
            )
        }
    }
}

// ─── CALENDAR TAB ─────────────────────────────────────────────────────────────
@Composable
private fun CalendarTab(
    month: LocalDate,
    phaseMap: Map<String, CyclePhase>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateTap: (LocalDate) -> Unit,
    onLogToday: () -> Unit,
) {
    val today = LocalDate.now()
    val firstDay = month.withDayOfMonth(1)
    val daysInMonth = month.lengthOfMonth()
    val startDayOfWeek = (firstDay.dayOfWeek.value % 7) // Sun=0

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevMonth) {
                Icon(Icons.Filled.ChevronLeft, null, tint = TextMuted)
            }
            Text(
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                fontSize = 18.sp, fontWeight = FontWeight.Black,
                fontFamily = MulishFamily, color = TextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Filled.ChevronRight, null, tint = TextMuted)
            }
        }

        // Day headers
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            listOf("S","M","T","W","T","F","S").forEach { d ->
                Text(d, modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, fontFamily = MulishFamily,
                    color = TextMuted)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Calendar grid
        val totalCells = startDayOfWeek + daysInMonth
        val rows = ceil(totalCells / 7f).toInt()

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(rows) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(7) { col ->
                        val cellIndex = row * 7 + col
                        val day = cellIndex - startDayOfWeek + 1
                        if (day < 1 || day > daysInMonth) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val date = month.withDayOfMonth(day)
                            val phase = phaseMap[date.toString()]
                            val isToday = date == today
                            val hasPhase = phase != null && phase != CyclePhase.NONE

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isToday && phase != null -> phase.color().copy(alpha = 0.25f)
                                            isToday -> RosePrimary.copy(alpha = 0.15f)
                                            phase == CyclePhase.MENSTRUATION -> phase.color().copy(alpha = 0.18f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isToday) 2.dp else 0.dp,
                                        color = if (isToday) RosePrimary else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                    .clickable { onDateTap(date) }
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "$day",
                                    fontSize = 14.sp,
                                    fontWeight = if (isToday) FontWeight.Black else FontWeight.Medium,
                                    fontFamily = MulishFamily,
                                    color = if (isToday) RosePrimary else TextPrimary,
                                )
                                // Phase dot
                                if (hasPhase) {
                                    Spacer(Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(phase!!.color()),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Legend
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("LEGEND", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = TextMuted, letterSpacing = 1.5.sp)
            listOf(
                CyclePhase.MENSTRUATION, CyclePhase.FOLLICULAR,
                CyclePhase.OVULATION, CyclePhase.FERTILE, CyclePhase.LUTEAL,
            ).forEach { phase ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(phase.color()))
                    Text(phase.label(), fontSize = 12.sp, fontFamily = MulishFamily, color = TextMuted)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onLogToday,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Log Today", fontFamily = MulishFamily,
                fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(80.dp))
    }
}

// ─── HISTORY TAB ──────────────────────────────────────────────────────────────
@Composable
private fun HistoryTab(
    cycles: List<CycleEntity>,
    avgCycleLength: Int,
    avgPeriodLength: Int,
    isRegular: Boolean,
    cycleCount: Int,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Based on $cycleCount cycle${if (cycleCount != 1) "s" else ""}",
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily, color = TextMuted,
            )
        }

        // Stats cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard("Avg Cycle", "$avgCycleLength", "days",
                    RosePrimary, Modifier.weight(1f))
                StatCard("Avg Period", "$avgPeriodLength", "days",
                    Color(0xFFEF4444), Modifier.weight(1f))
                StatCard("Pattern",
                    if (isRegular) "Regular" else "Variable", "",
                    if (isRegular) Color(0xFF22C55E) else Color(0xFFF59E0B),
                    Modifier.weight(1f))
            }
        }

        // Cycle bars
        if (cycles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("🌸", fontSize = 48.sp)
                        Text("No cycles logged yet",
                            fontSize = 16.sp, fontWeight = FontWeight.Black,
                            fontFamily = MulishFamily, color = TextPrimary)
                        Text("Your cycle history will appear here\nas you track each month",
                            fontSize = 13.sp, fontFamily = MulishFamily,
                            color = TextMuted, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(cycles) { cycle ->
                CycleHistoryRow(cycle = cycle)
            }
        }
    }
}

@Composable
private fun CycleHistoryRow(cycle: CycleEntity) {
    val start = java.time.Instant.ofEpochMilli(cycle.startDate)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val end = cycle.endDate?.let {
        java.time.Instant.ofEpochMilli(it)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(RosePrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                start.format(DateTimeFormatter.ofPattern("MMM")).uppercase(),
                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = RosePrimary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                start.format(DateTimeFormatter.ofPattern("d MMMM yyyy")),
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily, color = TextPrimary,
            )
            Text(
                buildString {
                    cycle.periodLength?.let { append("$it day period") }
                    cycle.cycleLength?.let {
                        if (isNotEmpty()) append(" · ")
                        append("$it day cycle")
                    }
                    if (isEmpty()) append("Period started")
                },
                fontSize = 12.sp, fontFamily = MulishFamily, color = TextMuted,
            )
        }
        // Mini bar showing period proportion
        cycle.cycleLength?.let { cycleLen ->
            val periodLen = cycle.periodLength ?: 5
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.1f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width((60 * periodLen / cycleLen).dp.coerceAtMost(60.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(RosePrimary),
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    "$cycleLen days", fontSize = 10.sp,
                    fontFamily = MulishFamily, color = TextMuted,
                )
            }
        }
    }
}

// ─── DAILY LOG SHEET ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailyLogSheet(
    date: LocalDate,
    existingLog: PeriodDailyLog?,
    onDismiss: () -> Unit,
    onSave: (FlowIntensity, List<String>, DailyMood, String?, Float?) -> Unit,
) {
    var flow by remember { mutableStateOf(existingLog?.flowEnum() ?: FlowIntensity.NONE) }
    var selectedSymptoms by remember { mutableStateOf(existingLog?.symptomList()?.toSet() ?: emptySet()) }
    var mood by remember { mutableStateOf(existingLog?.moodEnum() ?: DailyMood.OKAY) }
    var notes by remember { mutableStateOf(existingLog?.notes ?: "") }
    // Pre-fill temperature from existing log; store as string for keyboard input
    var temperature by remember {
        mutableStateOf(existingLog?.temperature?.let {
            if (it == it.toLong().toFloat()) it.toLong().toString()
            else "%.1f".format(it)
        } ?: "")
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A2E),
        dragHandle = {
            Box(
                modifier = Modifier.padding(vertical = 12.dp)
                    .width(36.dp).height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "Log — ${date.format(DateTimeFormatter.ofPattern("d MMMM"))}",
                fontSize = 17.sp, fontWeight = FontWeight.Black,
                fontFamily = MulishFamily, color = TextPrimary,
            )

            // Flow
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("How's your flow?", fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, fontFamily = MulishFamily, color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowIntensity.entries.forEach { f ->
                        val selected = flow == f
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) f.color().copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    1.dp,
                                    if (selected) f.color() else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable { flow = f }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (f != FlowIntensity.NONE) {
                                Box(
                                    modifier = Modifier.size(10.dp).clip(CircleShape)
                                        .background(f.color()),
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(
                                f.label(), fontSize = 9.sp,
                                fontWeight = FontWeight.Bold, fontFamily = MulishFamily,
                                color = if (selected) TextPrimary else TextMuted,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            // Mood
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("How are you feeling?", fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, fontFamily = MulishFamily, color = TextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DailyMood.entries.forEach { m ->
                        val selected = mood == m
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) RosePrimary.copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    1.dp,
                                    if (selected) RosePrimary.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable { mood = m }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(m.emoji(), fontSize = 18.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(m.label(), fontSize = 9.sp,
                                fontWeight = FontWeight.Bold, fontFamily = MulishFamily,
                                color = if (selected) RosePrimary else TextMuted,
                                textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // Symptoms
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Symptoms", fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, fontFamily = MulishFamily, color = TextMuted)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Symptoms.all.forEach { (key, label) ->
                        val selected = key in selectedSymptoms
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (selected) RosePrimary.copy(alpha = 0.18f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    1.dp,
                                    if (selected) RosePrimary.copy(alpha = 0.5f)
                                    else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(20.dp),
                                )
                                .clickable {
                                    selectedSymptoms = if (selected)
                                        selectedSymptoms - key
                                    else selectedSymptoms + key
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Text(label, fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold, fontFamily = MulishFamily,
                                color = if (selected) RosePrimary else TextMuted)
                        }
                    }
                }
            }

            // ── Temperature (Basal Body Temperature) ─────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("🌡️", fontSize = 15.sp)
                    Text(
                        "Basal body temperature",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily, color = TextMuted,
                    )
                    Text(
                        "(optional)",
                        fontSize = 11.sp, fontFamily = MulishFamily,
                        color = TextMuted.copy(alpha = 0.45f),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(
                            width = 1.dp,
                            color = if (temperature.isNotEmpty())
                                RosePrimary.copy(alpha = 0.5f)
                            else Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = temperature,
                        onValueChange = { input ->
                            // Allow up to 2 digits, optional decimal, up to 1 decimal place
                            // e.g. "36", "36.", "36.5" — not "36.55" or "999"
                            if (input.isEmpty() || input.matches(Regex("^\\d{1,2}\\.?\\d{0,1}$"))) {
                                temperature = input
                            }
                        },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 20.sp,
                            fontFamily = MulishFamily,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(RosePrimary),
                        decorationBox = { inner ->
                            if (temperature.isEmpty()) {
                                Text(
                                    "36.5",
                                    fontSize = 20.sp, fontFamily = MulishFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted.copy(alpha = 0.3f),
                                )
                            }
                            inner()
                        },
                    )
                    Text(
                        "°C",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color = if (temperature.isNotEmpty()) TextPrimary
                                else TextMuted.copy(alpha = 0.4f),
                    )
                }
                Text(
                    "Normal range: 35.5 – 37.5 °C. Track daily for best accuracy.",
                    fontSize = 11.sp, fontFamily = MulishFamily,
                    color = TextMuted.copy(alpha = 0.5f),
                )
            }

            // Notes
            BasicTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(14.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp, fontFamily = MulishFamily, color = TextPrimary,
                ),
                cursorBrush = SolidColor(RosePrimary),
                decorationBox = { inner ->
                    if (notes.isEmpty()) Text("Add notes...", fontSize = 14.sp,
                        fontFamily = MulishFamily, color = TextMuted)
                    inner()
                },
            )

            Button(
                onClick = {
                    onSave(
                        flow,
                        selectedSymptoms.toList(),
                        mood,
                        notes.takeIf { it.isNotBlank() },
                        temperature.toFloatOrNull(),   // null if field is empty
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Save Log 🌸", fontFamily = MulishFamily,
                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

// ─── SETTINGS SHEET ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CycleSettingsSheet(
    settings: PeriodSettings,
    cycleCount: Int,
    onDismiss: () -> Unit,
    onToggleFertile: () -> Unit,
    onCycleLengthChange: (Int) -> Unit,
    onRemindDaysChange: (Int) -> Unit,
    onStartOver: () -> Unit,
) {
    var sliderValue by remember { mutableFloatStateOf(settings.estimatedCycleLength.toFloat()) }
    var remindDays by remember { mutableFloatStateOf(settings.remindDaysBefore.toFloat()) }
    var showResetDialog by remember { mutableStateOf(false) }

    // ── Confirmation dialog — shown before wiping any data ───────────────────
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = Color(0xFF1A1A2E),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Start over?",
                    fontSize = 17.sp, fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily, color = TextPrimary,
                )
            },
            text = {
                Text(
                    "This will permanently delete all your cycle history, " +
                    "daily logs, and settings. You'll go back to the setup screen. " +
                    "\n\nThis cannot be undone.",
                    fontSize = 14.sp, fontFamily = MulishFamily,
                    color = TextMuted, lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onDismiss()          // close settings sheet first
                        onStartOver()        // then wipe everything
                    },
                ) {
                    Text(
                        "Yes, start over",
                        color = Color(0xFFEF4444),
                        fontFamily = MulishFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(
                        "Cancel",
                        color = RosePrimary,
                        fontFamily = MulishFamily,
                        fontSize = 14.sp,
                    )
                }
            },
        )
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState,
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                "Cycle Settings",
                fontSize = 17.sp, fontWeight = FontWeight.Black,
                fontFamily = MulishFamily, color = TextPrimary,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            // ─── Section 1: Cycle length ──────────────────────────────────────
            Text(
                "CYCLE",
                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = TextMuted,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Cycle length", fontSize = 14.sp, fontFamily = MulishFamily, color = TextPrimary)
                    Text(
                        "${sliderValue.toInt()} days",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily, color = RosePrimary,
                    )
                }
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onCycleLengthChange(sliderValue.toInt()) },
                    valueRange = 21f..42f,
                    steps = 20,
                    colors = SliderDefaults.colors(
                        thumbColor = RosePrimary,
                        activeTrackColor = RosePrimary,
                        inactiveTrackColor = RosePrimary.copy(alpha = 0.2f),
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("21 days", fontSize = 11.sp, fontFamily = MulishFamily, color = TextMuted)
                    Text("42 days", fontSize = 11.sp, fontFamily = MulishFamily, color = TextMuted)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(bottom = 20.dp))

            // ─── Section 2: Privacy & visibility ─────────────────────────────
            Text(
                "PRIVACY & VISIBILITY",
                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = TextMuted,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        "Show fertile window",
                        fontSize = 14.sp, fontFamily = MulishFamily,
                        color = TextPrimary, fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Shows ovulation day and fertile dates in calendar",
                        fontSize = 12.sp, fontFamily = MulishFamily,
                        color = TextMuted, lineHeight = 16.sp,
                    )
                }
                Switch(
                    checked = settings.showFertileWindow,
                    onCheckedChange = { onToggleFertile() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = RosePrimary,
                    ),
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(bottom = 20.dp))

            // ─── Section 3: Reminders ─────────────────────────────────────────
            Text(
                "REMINDERS",
                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = TextMuted,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Remind me before period", fontSize = 14.sp, fontFamily = MulishFamily, color = TextPrimary)
                    Text(
                        "${remindDays.toInt()} days before",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily, color = RosePrimary,
                    )
                }
                Slider(
                    value = remindDays,
                    onValueChange = { remindDays = it },
                    onValueChangeFinished = { onRemindDaysChange(remindDays.toInt()) },
                    valueRange = 1f..7f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = RosePrimary,
                        activeTrackColor = RosePrimary,
                        inactiveTrackColor = RosePrimary.copy(alpha = 0.2f),
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("1 day", fontSize = 11.sp, fontFamily = MulishFamily, color = TextMuted)
                    Text("7 days", fontSize = 11.sp, fontFamily = MulishFamily, color = TextMuted)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(bottom = 20.dp))

            // ─── Section 4: Prediction engine status (read-only) ─────────────
            Text(
                "PREDICTION ENGINE",
                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = TextMuted,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Cycles tracked", fontSize = 13.sp, fontFamily = MulishFamily, color = TextMuted)
                    Text(
                        "$cycleCount cycle${if (cycleCount != 1) "s" else ""}",
                        fontSize = 13.sp, fontFamily = MulishFamily, color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Prediction mode", fontSize = 13.sp, fontFamily = MulishFamily, color = TextMuted)
                    Text(
                        if (settings.onboardingComplete) "Active ✓" else "Setup needed",
                        fontSize = 13.sp, fontFamily = MulishFamily,
                        color = if (settings.onboardingComplete) Color(0xFF22C55E) else RosePrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Base cycle length", fontSize = 13.sp, fontFamily = MulishFamily, color = TextMuted)
                    Text(
                        "${settings.estimatedCycleLength} days",
                        fontSize = 13.sp, fontFamily = MulishFamily,
                        color = TextPrimary, fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f),
                modifier = Modifier.padding(bottom = 20.dp))

            // ── Start Over button ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = 1.dp,
                        color = Color(0xFFEF4444).copy(alpha = 0.35f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { showResetDialog = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("🗑️", fontSize = 16.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Start over",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = MulishFamily, color = Color(0xFFEF4444),
                    )
                    Text(
                        "Clear all cycle data and begin fresh",
                        fontSize = 11.sp, fontFamily = MulishFamily,
                        color = Color(0xFFEF4444).copy(alpha = 0.55f),
                    )
                }
                Text(
                    "→",
                    fontSize = 16.sp,
                    color = Color(0xFFEF4444).copy(alpha = 0.55f),
                )
            }
        }
    }
}

// ─── HELPER COMPOSABLES ───────────────────────────────────────────────────────
@Composable
private fun StatCard(
    label: String, value: String, unit: String,
    color: Color, modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black,
            fontFamily = MulishFamily, color = color)
        if (unit.isNotEmpty()) Text(unit, fontSize = 11.sp,
            fontFamily = MulishFamily, color = TextMuted)
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily, color = TextMuted, letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun InfoCard(
    icon: String, title: String, value: String,
    subtitle: String, color: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(icon, fontSize = 24.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = TextMuted, letterSpacing = 1.sp)
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Black,
                fontFamily = MulishFamily, color = TextPrimary)
            Text(subtitle, fontSize = 11.sp, fontFamily = MulishFamily, color = TextMuted)
        }
    }
}
