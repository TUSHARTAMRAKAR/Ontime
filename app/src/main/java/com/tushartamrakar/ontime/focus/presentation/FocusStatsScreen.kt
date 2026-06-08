package com.tushartamrakar.ontime.focus.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.Success
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary
import com.tushartamrakar.ontime.core.ui.theme.Warning
import com.tushartamrakar.ontime.focus.data.local.DailyStatRow
import com.tushartamrakar.ontime.focus.data.local.FocusStreakEntity
import com.tushartamrakar.ontime.focus.data.local.HourlyStatRow
import com.tushartamrakar.ontime.focus.data.repository.FocusStats
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun FocusStatsScreen(
    navController: NavController,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val stats            by viewModel.focusStats.collectAsState()
    val isLoading        by viewModel.isLoadingStats.collectAsState()
    val monthlyStats     by viewModel.monthlyDailyStats.collectAsState()
    val recentStreaks     by viewModel.recentStreaks.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStats()
        viewModel.loadMonthlyStats()
    }

    LazyColumn(
        modifier       = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {

        // ── Top bar ───────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector        = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = TextPrimary,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text       = "Focus Stats",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily,
                    color      = TextPrimary,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(color = Primary, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                        Text("Computing your stats…", fontSize = 13.sp, fontFamily = MulishFamily, color = TextMuted)
                    }
                }
            }
            return@LazyColumn
        }

        // ── Focus Score card ──────────────────────────────────────────────────
        item {
            FocusScoreCard(
                score    = stats.focusScore,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        // ── Streak cards ──────────────────────────────────────────────────────
        item {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StreakStatCard(
                    emoji   = "🔥",
                    value   = "${stats.currentStreakDays}",
                    label   = "Current\nStreak",
                    unit    = "days",
                    color   = Warning,
                    modifier = Modifier.weight(1f),
                )
                StreakStatCard(
                    emoji   = "🏆",
                    value   = "${stats.longestStreakDays}",
                    label   = "Longest\nStreak",
                    unit    = "days",
                    color   = Primary,
                    modifier = Modifier.weight(1f),
                )
                StreakStatCard(
                    emoji   = "🎯",
                    value   = "${stats.goalMetDaysThisMonth}",
                    label   = "Goals Met\nThis Month",
                    unit    = "days",
                    color   = Success,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Weekly bar chart ──────────────────────────────────────────────────
        item {
            StatsCard(
                title    = "This Week",
                subtitle = "Daily focus minutes",
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                WeeklyBarChart(
                    dailyStats = stats.weeklyDailyStats,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── 30-day streak calendar ────────────────────────────────────────────
        item {
            StatsCard(
                title    = "30-Day Streak Calendar",
                subtitle = "Your focus activity over the last month",
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                StreakCalendar(
                    dailyStats   = monthlyStats,
                    streakData   = recentStreaks,
                    goalSessions = stats.todayGoalSessions,
                    modifier     = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── 4-week efficiency trend ───────────────────────────────────────────
        item {
            StatsCard(
                title    = "Efficiency Trend",
                subtitle = "Daily focus minutes — last 4 weeks",
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                EfficiencyTrendChart(
                    dailyStats = monthlyStats,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Quick stats 2x2 grid ──────────────────────────────────────────────
        item {
            Column(
                modifier              = Modifier.padding(horizontal = 20.dp),
                verticalArrangement   = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickStatCard(
                        icon     = Icons.Filled.Timer,
                        iconTint = Primary,
                        label    = "Today",
                        value    = stats.todayFocusFormatted.ifBlank { "0m" },
                        modifier = Modifier.weight(1f),
                    )
                    QuickStatCard(
                        icon     = Icons.Filled.Schedule,
                        iconTint = Success,
                        label    = "This Week",
                        value    = formatMinutes(stats.weekFocusMinutes),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickStatCard(
                        icon     = Icons.Filled.EmojiEvents,
                        iconTint = Warning,
                        label    = "All Time",
                        value    = formatMinutes(stats.allTimeFocusMinutes),
                        modifier = Modifier.weight(1f),
                    )
                    QuickStatCard(
                        icon     = Icons.Filled.Shield,
                        iconTint = Warning,
                        label    = "Blocked",
                        value    = "${stats.allTimeDistractionsBlocked}",
                        subValue = "distractions",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Quality metrics ───────────────────────────────────────────────────
        item {
            StatsCard(
                title    = "Session Quality",
                subtitle = "How well your sessions go",
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    QualityRow(
                        label    = "Completion rate",
                        value    = "${stats.completionRatePct}%",
                        progress = stats.completionRatePct / 100f,
                        color    = Success,
                    )
                    QualityRow(
                        label    = "Avg session length",
                        value    = "${stats.avgSessionMinutes}m",
                        progress = (stats.avgSessionMinutes / 60f).coerceIn(0f, 1f),
                        color    = Primary,
                    )
                    QualityRow(
                        label    = "Today's progress",
                        value    = "${stats.todaySessionsCompleted} / ${stats.todayGoalSessions}",
                        progress = stats.todayGoalProgress,
                        color    = if (stats.isTodayGoalMet) Success else Primary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Hourly heat map ───────────────────────────────────────────────────
        item {
            StatsCard(
                title    = "Best Focus Hours",
                subtitle = if (stats.bestFocusHour >= 0)
                    "You focus best around ${formatHour(stats.bestFocusHour)}"
                else
                    "Complete more sessions to see your pattern",
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                HourlyHeatMap(
                    heatmap  = stats.hourlyHeatmap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Monthly overview ──────────────────────────────────────────────────
        item {
            StatsCard(
                title    = "This Month",
                subtitle = "Monthly focus overview",
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    MonthStatItem(
                        value    = formatMinutes(stats.monthFocusMinutes),
                        label    = "Focus time",
                        color    = Primary,
                    )
                    MonthStatItem(
                        value    = "${stats.monthSessionsCompleted}",
                        label    = "Sessions",
                        color    = Success,
                    )
                    MonthStatItem(
                        value    = "${stats.goalMetDaysThisMonth}",
                        label    = "Goals met",
                        color    = Warning,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ─── 30-Day Streak Calendar ───────────────────────────────────────────────────

@Composable
private fun StreakCalendar(
    dailyStats: List<DailyStatRow>,
    streakData: List<FocusStreakEntity>,
    goalSessions: Int,
    modifier: Modifier = Modifier,
) {
    val today       = LocalDate.now()
    val dayLabels   = listOf("M", "T", "W", "T", "F", "S", "S")

    // Build a map: dateStr → DailyStatRow
    val statsByDay  = dailyStats.associateBy { it.day }
    // Build a map: dateStr → FocusStreakEntity
    val streakByDay = streakData.associateBy { it.date }

    // Start from 29 days ago through today = 30 slots
    val startDate   = today.minusDays(29)

    // Pad so column 0 = Monday — find how many blanks before startDate
    val startDowOrdinal = startDate.dayOfWeek.value - 1 // 0=Mon…6=Sun
    val totalCells  = startDowOrdinal + 30
    val totalCols   = ((totalCells + 6) / 7)           // number of weeks shown

    Column(modifier = modifier) {
        // Day-of-week header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            dayLabels.forEach { d ->
                Text(
                    text       = d,
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily,
                    color      = TextMuted,
                    modifier   = Modifier.weight(1f),
                    textAlign  = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // Grid: rows = weeks, cols = 7 days
        for (week in 0 until totalCols) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                for (dow in 0..6) {
                    val cellIndex = week * 7 + dow
                    val dayOffset = cellIndex - startDowOrdinal
                    val cellDate  = if (dayOffset in 0..29)
                        startDate.plusDays(dayOffset.toLong()) else null

                    if (cellDate == null) {
                        // Blank padding cell
                        Box(modifier = Modifier.weight(1f).height(22.dp))
                    } else {
                        val dateKey = cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val streak  = streakByDay[dateKey]
                        val stat    = statsByDay[dateKey]
                        val sessions = streak?.sessionsCompleted
                            ?: if (stat != null && stat.sessionCount > 0) stat.sessionCount else 0
                        val isFuture = cellDate.isAfter(today)
                        val isToday  = cellDate == today

                        val cellColor = when {
                            isFuture              -> Color.Transparent
                            streak?.goalMet == true -> Success
                            sessions > 0          -> Primary.copy(alpha = 0.55f)
                            else                  -> SurfaceHigh
                        }
                        val borderCol = when {
                            isToday  -> Primary
                            isFuture -> Border.copy(alpha = 0.3f)
                            else     -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(cellColor)
                                .border(
                                    width = if (isToday) 1.5.dp else 0.dp,
                                    color = borderCol,
                                    shape = RoundedCornerShape(4.dp),
                                ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
        }

        Spacer(Modifier.height(6.dp))

        // Legend
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            CalendarLegendDot(Success,       "Goal met")
            CalendarLegendDot(Primary.copy(alpha = 0.55f), "Partial")
            CalendarLegendDot(SurfaceHigh,   "No session")
        }
    }
}

@Composable
private fun CalendarLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Text(
            text       = label,
            fontSize   = 9.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color      = TextMuted,
        )
    }
}

// ─── Efficiency Trend Chart ───────────────────────────────────────────────────

@Composable
private fun EfficiencyTrendChart(
    dailyStats: List<DailyStatRow>,
    modifier: Modifier = Modifier,
) {
    if (dailyStats.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "Complete sessions to see your trend",
                fontSize   = 12.sp,
                fontFamily = MulishFamily,
                color      = TextMuted,
                textAlign  = TextAlign.Center,
            )
        }
        return
    }

    val today     = LocalDate.now()
    val startDay  = today.minusDays(27) // 4 weeks = 28 days
    val statMap   = dailyStats.associateBy { it.day }

    // Build 28 data points (one per day)
    val points    = (0..27).map { offset ->
        val date = startDay.plusDays(offset.toLong())
        val key  = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        (statMap[key]?.totalSeconds ?: 0) / 60  // minutes
    }
    val maxMins   = points.max().coerceAtLeast(1)

    // Animate on load
    var trigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { trigger = true }
    val animFraction by animateFloatAsState(
        targetValue   = if (trigger) 1f else 0f,
        animationSpec = tween(1000),
        label         = "trend_anim",
    )

    // Capture theme color in composable context before entering Canvas DrawScope
    val dotHoleColor = Background

    Canvas(modifier = modifier) {
        val w         = size.width
        val h         = size.height
        val stepX     = w / (points.size - 1)
        val primaryColor = Primary

        // Build path of points
        val path  = androidx.compose.ui.graphics.Path()
        val fillPath = androidx.compose.ui.graphics.Path()

        points.forEachIndexed { i, mins ->
            val x = i * stepX
            val y = h - (h * (mins.toFloat() / maxMins) * animFraction)
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                // Smooth curve via cubic bezier
                val prevX = (i - 1) * stepX
                val prevY = h - (h * (points[i - 1].toFloat() / maxMins) * animFraction)
                val cp1x  = prevX + stepX / 2
                val cp2x  = x - stepX / 2
                path.cubicTo(cp1x, prevY, cp2x, y, x, y)
                fillPath.cubicTo(cp1x, prevY, cp2x, y, x, y)
            }
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        // Gradient fill under line
        drawPath(
            path  = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.25f), primaryColor.copy(alpha = 0.0f)),
            ),
        )
        // Line
        drawPath(
            path  = path,
            color = primaryColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
        // Dot for today (last point)
        val lastX = (points.size - 1) * stepX
        val lastY = h - (h * (points.last().toFloat() / maxMins) * animFraction)
        drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = Offset(lastX, lastY))
        drawCircle(color = dotHoleColor,  radius = 2.dp.toPx(), center = Offset(lastX, lastY))
    }
}

// ─── Focus Score Card ─────────────────────────────────────────────────────────

@Composable
private fun FocusScoreCard(score: Int, modifier: Modifier = Modifier) {
    val animScore by animateFloatAsState(
        targetValue    = score.toFloat(),
        animationSpec  = tween(1000),
        label          = "score_anim",
    )
    val scoreColor = when {
        score >= 80 -> Success
        score >= 50 -> Primary
        score >= 30 -> Warning
        else        -> TextMuted
    }

    // Capture theme color in composable context before entering Canvas DrawScope
    val trackColor = Border

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Primary.copy(alpha = 0.15f), scoreColor.copy(alpha = 0.08f))
                )
            )
            .border(1.dp, Primary.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Circular score
        Box(
            modifier         = Modifier.size(80.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(80.dp)) {
                val stroke = 7.dp.toPx()
                val inset  = stroke / 2
                drawArc(
                    color      = trackColor,
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    topLeft    = Offset(inset, inset),
                    size       = Size(size.width - stroke, size.height - stroke),
                    style      = Stroke(stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color      = scoreColor,
                    startAngle = -90f,
                    sweepAngle = 360f * (animScore / 100f),
                    useCenter  = false,
                    topLeft    = Offset(inset, inset),
                    size       = Size(size.width - stroke, size.height - stroke),
                    style      = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = "${animScore.toInt()}",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily,
                    color      = scoreColor,
                )
                Text(
                    text       = "/100",
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily,
                    color      = TextMuted,
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "Focus Score",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = MulishFamily,
                color      = TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            val label = when {
                score >= 80 -> "Excellent focus discipline 🔥"
                score >= 60 -> "Good consistency! Keep it up"
                score >= 40 -> "Building your habit"
                score >= 20 -> "Early stages — keep going"
                else        -> "Start your first session"
            }
            Text(
                text       = label,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color      = TextSecondary,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text       = "40% completion · 30% goal · 30% streak",
                fontSize   = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color      = TextMuted,
            )
        }
    }
}

// ─── Streak Stat Card ─────────────────────────────────────────────────────────

@Composable
private fun StreakStatCard(
    emoji: String, value: String, label: String,
    unit: String, color: Color, modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = emoji, fontSize = 24.sp)
        Text(
            text       = value,
            fontSize   = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = MulishFamily,
            color      = color,
        )
        Text(
            text       = label,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily,
            color      = TextMuted,
            textAlign  = TextAlign.Center,
            lineHeight = 14.sp,
        )
    }
}

// ─── Weekly Bar Chart ─────────────────────────────────────────────────────────

@Composable
private fun WeeklyBarChart(
    dailyStats: List<DailyStatRow>,
    modifier: Modifier = Modifier,
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    // Build 7-slot array aligned to Mon–Sun from today
    val today       = LocalDate.now()
    val monday      = today.minusDays(today.dayOfWeek.value.toLong() - 1)
    val statsByDay  = dailyStats.associateBy { it.day }
    val slots       = (0..6).map { offset ->
        val date   = monday.plusDays(offset.toLong())
        val key    = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val mins   = (statsByDay[key]?.totalSeconds ?: 0) / 60
        date to mins
    }
    val maxMins     = slots.maxOfOrNull { it.second } ?: 1
    val todayStr    = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

    // Animate bars on appearance
    var trigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { trigger = true }
    val animatedFraction by animateFloatAsState(
        targetValue    = if (trigger) 1f else 0f,
        animationSpec  = tween(800),
        label          = "bar_anim",
    )

    // Capture theme color in composable context before entering Canvas DrawScope
    val emptyBarColor = SurfaceHigh

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val barCount   = 7
            val barSpacing = size.width / barCount
            val barWidth   = barSpacing * 0.55f
            val maxBarH    = size.height

            slots.forEachIndexed { i, (date, mins) ->
                val fraction    = if (maxMins > 0) mins.toFloat() / maxMins else 0f
                val barH        = maxBarH * fraction * animatedFraction
                val x           = i * barSpacing + (barSpacing - barWidth) / 2f
                val y           = size.height - barH
                val dateStr2    = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val isToday     = dateStr2 == todayStr
                val barColor    = if (isToday) Primary else Primary.copy(alpha = 0.35f)

                if (barH > 0) {
                    drawRoundRect(
                        color        = barColor,
                        topLeft      = Offset(x, y),
                        size         = Size(barWidth, barH),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                } else {
                    // Empty bar trace
                    drawRoundRect(
                        color        = emptyBarColor,
                        topLeft      = Offset(x, size.height - 4.dp.toPx()),
                        size         = Size(barWidth, 4.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Day labels
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            slots.forEachIndexed { i, (date, mins) ->
                val dateStr2 = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val isToday  = dateStr2 == todayStr
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = dayLabels[i],
                        fontSize   = 11.sp,
                        fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color      = if (isToday) Primary else TextMuted,
                    )
                    if (mins > 0) {
                        Text(
                            text       = "${mins}m",
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color      = TextMuted,
                        )
                    }
                }
            }
        }
    }
}

// ─── Hourly Heat Map ──────────────────────────────────────────────────────────

@Composable
private fun HourlyHeatMap(
    heatmap: List<HourlyStatRow>,
    modifier: Modifier = Modifier,
) {
    val maxSecs = heatmap.maxOfOrNull { it.totalSeconds } ?: 1
    val byHour  = heatmap.associateBy { it.hour }

    Column(modifier = modifier) {
        // 24 boxes in 2 rows of 12
        listOf((0..11), (12..23)).forEach { range ->
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                range.forEach { hour ->
                    val secs      = byHour[hour]?.totalSeconds ?: 0
                    val intensity = if (maxSecs > 0) secs.toFloat() / maxSecs else 0f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Primary.copy(alpha = 0.08f + intensity * 0.72f)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (intensity > 0.5f) {
                            Text(
                                text       = formatHourShort(hour),
                                fontSize   = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = MulishFamily,
                                color      = Color.White.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("12 AM", fontSize = 9.sp, fontFamily = MulishFamily, color = TextMuted)
            Text("12 PM", fontSize = 9.sp, fontFamily = MulishFamily, color = TextMuted)
            Text("11 PM", fontSize = 9.sp, fontFamily = MulishFamily, color = TextMuted)
        }
    }
}

// ─── Quick Stat Card ──────────────────────────────────────────────────────────

@Composable
private fun QuickStatCard(
    icon: ImageVector, iconTint: Color,
    label: String, value: String,
    subValue: String = "",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Text(
                text       = label,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color      = TextMuted,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text       = value,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Black,
            fontFamily = MulishFamily,
            color      = TextPrimary,
        )
        if (subValue.isNotBlank()) {
            Text(
                text       = subValue,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color      = TextMuted,
            )
        }
    }
}

// ─── Quality Row ─────────────────────────────────────────────────────────────

@Composable
private fun QualityRow(
    label: String, value: String,
    progress: Float, color: Color,
) {
    val animProgress by animateFloatAsState(
        targetValue   = progress.coerceIn(0f, 1f),
        animationSpec = tween(800),
        label         = "quality_bar",
    )
    Column {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text       = label,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color      = TextSecondary,
            )
            Text(
                text       = value,
                fontSize   = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color      = color,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SurfaceHigh)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animProgress)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

// ─── Stats Card wrapper ───────────────────────────────────────────────────────

@Composable
private fun StatsCard(
    title: String, subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .padding(18.dp),
    ) {
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = TextPrimary)
        Text(text = subtitle, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = TextMuted)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

// ─── Month Stat Item ──────────────────────────────────────────────────────────

@Composable
private fun MonthStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = MulishFamily, color = color)
        Spacer(Modifier.height(2.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = TextMuted)
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0           -> "${h}h"
        else            -> "${m}m"
    }
}

private fun formatHour(hour: Int): String {
    return when {
        hour == 0    -> "12 AM"
        hour < 12    -> "$hour AM"
        hour == 12   -> "12 PM"
        else         -> "${hour - 12} PM"
    }
}

private fun formatHourShort(hour: Int): String = when {
    hour == 0  -> "12a"
    hour < 12  -> "${hour}a"
    hour == 12 -> "12p"
    else       -> "${hour - 12}p"
}
