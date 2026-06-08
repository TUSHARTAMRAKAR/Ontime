package com.tushartamrakar.ontime.alarm.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.alarm.data.local.AlarmEntity
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.Danger
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.PrimaryGlow
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary
import com.tushartamrakar.ontime.navigation.Screen
import java.util.Calendar
import kotlin.math.roundToInt

// ─── Alarm grouping ───────────────────────────────────────────────────────────

private enum class AlarmGroup { TODAY, TOMORROW, THIS_WEEK }

private fun getNextRingCalendar(alarm: AlarmEntity): Calendar {
    val now = Calendar.getInstance()
    if (alarm.repeatDays.isBlank()) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!cal.after(now)) cal.add(Calendar.DAY_OF_MONTH, 1)
        return cal
    }
    val days = alarm.repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
    for (i in 0..6) {
        val candidate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, i)
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (candidate.after(now) && candidate.get(Calendar.DAY_OF_WEEK) in days) return candidate
    }
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, alarm.hour)
        set(Calendar.MINUTE, alarm.minute)
        add(Calendar.DAY_OF_MONTH, 1)
    }
}

private fun ringsInText(nextRing: Calendar): String {
    val diff = nextRing.timeInMillis - System.currentTimeMillis()
    val totalMin = (diff / 60_000).coerceAtLeast(0)
    val h = totalMin / 60; val m = totalMin % 60
    return when {
        h > 0 && m > 0 -> "in ${h}h ${m}m"
        h > 0           -> "in ${h}h"
        m > 0           -> "in ${m}m"
        else            -> "soon"
    }
}

private fun alarmGroup(nextRing: Calendar): AlarmGroup {
    val today    = Calendar.getInstance()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
    return when {
        nextRing.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        nextRing.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)    -> AlarmGroup.TODAY
        nextRing.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
        nextRing.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) -> AlarmGroup.TOMORROW
        else                                                                        -> AlarmGroup.THIS_WEEK
    }
}

private fun greeting(): Pair<String, String> {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (h) {
        in 5..11  -> "Good morning"  to "☀️"
        in 12..16 -> "Good afternoon" to "🌤"
        in 17..20 -> "Good evening"  to "🌆"
        else      -> "Good night"    to "🌙"
    }
}

// Day pill colors — one per day of week (Sun=1..Sat=7)
private val DAY_COLORS = mapOf(
    1 to Color(0xFFEC4899),  // Sun  — pink
    2 to Color(0xFF3B82F6),  // Mon  — blue
    3 to Color(0xFF10B981),  // Tue  — green
    4 to Color(0xFFF59E0B),  // Wed  — amber
    5 to Color(0xFF8B5CF6),  // Thu  — violet
    6 to Color(0xFFEF4444),  // Fri  — red
    7 to Color(0xFF06B6D4),  // Sat  — cyan
)
private val DAY_LABELS = mapOf(1 to "Sun", 2 to "Mon", 3 to "Tue", 4 to "Wed",
    5 to "Thu", 6 to "Fri", 7 to "Sat")

// ─────────────────────────────────────────────────────────────────────────────
//  SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AlarmsScreen(
    navController: NavHostController,
    viewModel: AlarmViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp,
) {
    val alarms by viewModel.alarms.collectAsState()
    var isFabExpanded      by remember { mutableStateOf(false) }
    var showQuickAlarm     by remember { mutableStateOf(false) }
    var wasShowingQuickAlarm by remember { mutableStateOf(false) }
    var quickSelectedSound by remember { mutableStateOf("alarm_digital_alarm") }

    // Re-open quick alarm sheet on sound return
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry?.savedStateHandle
            ?.getStateFlow<String?>("selected_sound", null)?.collect { sound ->
                if (sound != null && wasShowingQuickAlarm) {
                    quickSelectedSound = sound
                    showQuickAlarm = true
                    wasShowingQuickAlarm = false
                    navController.currentBackStackEntry?.savedStateHandle?.remove<String>("selected_sound")
                }
            }
    }

    val fabRotation by animateFloatAsState(
        targetValue   = if (isFabExpanded) 45f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy,
                               stiffness    = Spring.StiffnessMedium),
        label = "fab_rotation",
    )

    // Group enabled alarms only — disabled ones go to bottom as ungrouped
    val enabledAlarms  = remember(alarms) { alarms.filter { it.isEnabled } }
    val disabledAlarms = remember(alarms) { alarms.filter { !it.isEnabled } }

    val todayAlarms     = remember(enabledAlarms) {
        enabledAlarms.filter { alarmGroup(getNextRingCalendar(it)) == AlarmGroup.TODAY }
    }
    val tomorrowAlarms  = remember(enabledAlarms) {
        enabledAlarms.filter { alarmGroup(getNextRingCalendar(it)) == AlarmGroup.TOMORROW }
    }
    val thisWeekAlarms  = remember(enabledAlarms) {
        enabledAlarms.filter { alarmGroup(getNextRingCalendar(it)) == AlarmGroup.THIS_WEEK }
    }

    // Next alarm for banner
    val nextAlarm = remember(enabledAlarms) {
        enabledAlarms.minByOrNull { getNextRingCalendar(it).timeInMillis }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                val (greet, emoji) = greeting()
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("$emoji  $greet",
                        fontSize = 28.sp, fontWeight = FontWeight.Black,
                        fontFamily = MulishFamily, color = TextPrimary,
                        letterSpacing = (-0.5).sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (alarms.isEmpty()) "No alarms set"
                    else "${alarms.count { it.isEnabled }} active",
                    fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily, color = TextMuted,
                )

                // Next alarm banner
                if (nextAlarm != null) {
                    Spacer(Modifier.height(12.dp))
                    val nextRing = remember(nextAlarm) { getNextRingCalendar(nextAlarm) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Primary.copy(alpha = 0.10f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Alarm, null,
                            tint = Primary, modifier = Modifier.size(16.dp))
                        Text(
                            "Next: ${formatTime(nextAlarm.hour, nextAlarm.minute)}" +
                            "  ·  ${ringsInText(nextRing)}",
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily, color = Primary,
                        )
                    }
                }
            }

            // ── Empty state ───────────────────────────────────────────────────
            if (alarms.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)) {
                        Box(modifier = Modifier.size(100.dp).clip(CircleShape)
                            .background(PrimaryGlow), contentAlignment = Alignment.Center) {
                            Text("⏰", fontSize = 48.sp)
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("No alarms yet", fontSize = 22.sp,
                            fontWeight = FontWeight.Black, fontFamily = MulishFamily,
                            color = TextPrimary, letterSpacing = (-0.5).sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Tap + to create your first\nunstoppable alarm",
                            fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily, color = TextMuted,
                            textAlign = TextAlign.Center, lineHeight = 22.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // ── TODAY ──────────────────────────────────────────────────
                    if (todayAlarms.isNotEmpty()) {
                        item { AlarmGroupHeader("TODAY") }
                        items(todayAlarms, key = { it.id }) { alarm ->
                            AlarmCard(alarm = alarm,
                                onToggle    = { viewModel.toggleAlarm(alarm) },
                                onDelete    = { viewModel.deleteAlarm(alarm) },
                                onDuplicate = { viewModel.duplicateAlarm(alarm) },
                                onSkipOnce  = { viewModel.skipOnce(alarm) },
                                onTap       = { navController.navigate(Screen.EditAlarm.createRoute(alarm.id)) })
                        }
                    }

                    // ── TOMORROW ───────────────────────────────────────────────
                    if (tomorrowAlarms.isNotEmpty()) {
                        item { AlarmGroupHeader("TOMORROW") }
                        items(tomorrowAlarms, key = { it.id }) { alarm ->
                            AlarmCard(alarm = alarm,
                                onToggle    = { viewModel.toggleAlarm(alarm) },
                                onDelete    = { viewModel.deleteAlarm(alarm) },
                                onDuplicate = { viewModel.duplicateAlarm(alarm) },
                                onSkipOnce  = { viewModel.skipOnce(alarm) },
                                onTap       = { navController.navigate(Screen.EditAlarm.createRoute(alarm.id)) })
                        }
                    }

                    // ── THIS WEEK ─────────────────────────────────────────────
                    if (thisWeekAlarms.isNotEmpty()) {
                        item { AlarmGroupHeader("THIS WEEK") }
                        items(thisWeekAlarms, key = { it.id }) { alarm ->
                            AlarmCard(alarm = alarm,
                                onToggle    = { viewModel.toggleAlarm(alarm) },
                                onDelete    = { viewModel.deleteAlarm(alarm) },
                                onDuplicate = { viewModel.duplicateAlarm(alarm) },
                                onSkipOnce  = { viewModel.skipOnce(alarm) },
                                onTap       = { navController.navigate(Screen.EditAlarm.createRoute(alarm.id)) })
                        }
                    }

                    // ── DISABLED ALARMS ───────────────────────────────────────
                    if (disabledAlarms.isNotEmpty()) {
                        item { AlarmGroupHeader("DISABLED") }
                        items(disabledAlarms, key = { it.id }) { alarm ->
                            AlarmCard(alarm = alarm,
                                onToggle    = { viewModel.toggleAlarm(alarm) },
                                onDelete    = { viewModel.deleteAlarm(alarm) },
                                onDuplicate = { viewModel.duplicateAlarm(alarm) },
                                onSkipOnce  = { viewModel.skipOnce(alarm) },
                                onTap       = { navController.navigate(Screen.EditAlarm.createRoute(alarm.id)) })
                        }
                    }

                    item { Spacer(Modifier.height(120.dp)) }
                }
            }
        }

        // ── Dim overlay ───────────────────────────────────────────────────────
        AnimatedVisibility(visible = isFabExpanded,
            enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Box(modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { isFabExpanded = false })
        }

        // ── FAB menu ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = bottomPadding + 90.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedVisibility(visible = isFabExpanded,
                enter = fadeIn(tween(150, 60)) + slideInVertically(tween(200, 60)) { it / 2 },
                exit  = fadeOut(tween(100)) + slideOutVertically(tween(150)) { it / 2 }) {
                Column(modifier = Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max)
                    .clip(RoundedCornerShape(16.dp)).background(CardBackground)) {
                    Row(modifier = Modifier.wrapContentWidth()
                        .clickable { isFabExpanded = false; navController.navigate(Screen.HabitAlarm.route) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📅", fontSize = 16.sp)
                        Text("Habit Alarm", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily, color = TextPrimary)
                    }
                    Box(Modifier.fillMaxWidth().height(0.8.dp).background(SurfaceHigh))
                    Row(modifier = Modifier.wrapContentWidth()
                        .clickable { isFabExpanded = false; showQuickAlarm = true }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("⚡", fontSize = 16.sp)
                        Text("Quick Alarm", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily, color = TextPrimary)
                    }
                }
            }

            AnimatedVisibility(visible = isFabExpanded,
                enter = fadeIn(tween(150)) + slideInVertically(tween(200)) { it / 2 },
                exit  = fadeOut(tween(100)) + slideOutVertically(tween(150)) { it / 2 }) {
                Row(modifier = Modifier.wrapContentWidth()
                    .clip(RoundedCornerShape(16.dp)).background(CardBackground)
                    .clickable { isFabExpanded = false; navController.navigate(Screen.CreateAlarm.route) }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⏰", fontSize = 16.sp)
                    Text("Alarm", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily, color = TextPrimary)
                }
            }
        }

        // ── Quick Alarm Sheet ─────────────────────────────────────────────────
        if (showQuickAlarm) {
            QuickAlarmSheet(
                selectedSound = quickSelectedSound,
                onSoundClick = {
                    wasShowingQuickAlarm = true
                    showQuickAlarm = false
                    navController.navigate(Screen.AlarmSound.createRoute(quickSelectedSound))
                },
                onDismiss = { showQuickAlarm = false },
                onSave = { minutes, label, sound, volume, vibrate ->
                    val cal = java.util.Calendar.getInstance()
                    cal.add(java.util.Calendar.MINUTE, minutes)
                    viewModel.createAlarm(
                        hour = cal.get(java.util.Calendar.HOUR_OF_DAY),
                        minute = cal.get(java.util.Calendar.MINUTE),
                        label = label, repeatDays = "",
                        vibrate = vibrate, sound = sound, volume = volume,
                    )
                    showQuickAlarm = false
                },
            )
        }

        // ── Main FAB ──────────────────────────────────────────────────────────
        FloatingActionButton(
            onClick = { isFabExpanded = !isFabExpanded },
            modifier = Modifier.align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = bottomPadding + 16.dp)
                .rotate(fabRotation),
            containerColor = if (isFabExpanded) SurfaceHigh else Primary,
            contentColor = TextPrimary, shape = CircleShape,
        ) {
            Icon(Icons.Filled.Add,
                contentDescription = if (isFabExpanded) "Close" else "Add Alarm",
                modifier = Modifier.size(28.dp))
        }
    }
}

// ─── Group header ─────────────────────────────────────────────────────────────

@Composable
private fun AlarmGroupHeader(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.height(1.dp).width(16.dp)
            .background(Primary.copy(alpha = 0.4f)))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily, color = Primary.copy(alpha = 0.7f),
            letterSpacing = 1.5.sp)
        Box(modifier = Modifier.height(1.dp).weight(1f)
            .background(Border.copy(alpha = 0.3f)))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ALARM CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AlarmCard(
    alarm:       AlarmEntity,
    onToggle:    () -> Unit,
    onDelete:    () -> Unit,
    onDuplicate: () -> Unit,
    onSkipOnce:  () -> Unit,
    onTap:       () -> Unit,
) {
    val nextRing    = remember(alarm) { getNextRingCalendar(alarm) }
    val ringsIn     = remember(alarm) { ringsInText(nextRing) }
    var menuExpanded by remember { mutableStateOf(false) }

    // ── Swipe right → toggle ──────────────────────────────────────────────────
    var swipeOffset by remember(alarm.id) { mutableFloatStateOf(0f) }
    val swipeThresholdPx = 90f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .pointerInput(alarm.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeOffset >= swipeThresholdPx) onToggle()
                        swipeOffset = 0f
                    },
                    onDragCancel = { swipeOffset = 0f },
                    onHorizontalDrag = { change, delta ->
                        change.consume()
                        swipeOffset = (swipeOffset + delta).coerceIn(-10f, 160f)
                    },
                )
            },
    ) {
        // Swipe background
        if (swipeOffset > 0f) {
            Box(
                modifier = Modifier.matchParentSize()
                    .background(if (alarm.isEnabled) SurfaceHigh else Primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.padding(start = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        if (alarm.isEnabled) Icons.Filled.AlarmOff else Icons.Filled.Alarm,
                        null,
                        tint = if (alarm.isEnabled) TextMuted else Primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        if (alarm.isEnabled) "Disable" else "Enable",
                        fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color = if (alarm.isEnabled) TextMuted else Primary,
                    )
                }
            }
        }

        // ── Card content ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .offset { IntOffset(swipeOffset.roundToInt(), 0) }
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (alarm.isEnabled)
                        Brush.horizontalGradient(
                            colors = listOf(
                                Primary.copy(alpha = 0.18f),
                                CardBackground,
                            )
                        )
                    else Brush.horizontalGradient(listOf(CardBackground, CardBackground))
                )
                .clickable { onTap() },
        ) {
            // Left edge glow bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(120.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .background(if (alarm.isEnabled) Primary else TextMuted.copy(alpha = 0.3f)),
            )

            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // ── Main info ──────────────────────────────────────────────────
                Column(modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {

                    // Time + rings-in pill
                    Row(verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text       = formatTime(alarm.hour, alarm.minute),
                            fontSize   = 38.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = MulishFamily,
                            color      = if (alarm.isEnabled) TextPrimary else TextMuted,
                            letterSpacing = (-1.5).sp,
                            lineHeight = 40.sp,
                        )
                        if (alarm.isEnabled) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(ringsIn, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                    fontFamily = MulishFamily, color = Primary)
                            }
                        }
                    }

                    // Label
                    Text(
                        text       = alarm.label.ifEmpty { "Alarm" },
                        fontSize   = 13.sp, fontWeight = FontWeight.SemiBold,
                        fontFamily = MulishFamily,
                        color      = if (alarm.isEnabled) TextSecondary else TextMuted,
                        maxLines   = 1, overflow = TextOverflow.Ellipsis,
                    )

                    // Day chips
                    AlarmDayChips(repeatDays = alarm.repeatDays, enabled = alarm.isEnabled)

                    // Feature indicators
                    AlarmFeatureIndicators(alarm = alarm)
                }

                // ── Switch + three-dot ─────────────────────────────────────────
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Switch(
                        checked  = alarm.isEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor   = TextPrimary,
                            checkedTrackColor   = Primary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceHigh,
                        ),
                    )

                    // Three-dot menu
                    Box {
                        IconButton(onClick = { menuExpanded = true },
                            modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.MoreVert, null,
                                tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded         = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier         = Modifier.background(Surface),
                        ) {
                            // Skip Once
                            DropdownMenuItem(
                                text = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.SkipNext, null,
                                            tint = Primary, modifier = Modifier.size(16.dp))
                                        Text("Skip Once", fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = MulishFamily, color = TextPrimary)
                                    }
                                },
                                onClick = { menuExpanded = false; onSkipOnce() },
                                colors  = MenuDefaults.itemColors(textColor = TextPrimary),
                            )
                            // Duplicate
                            DropdownMenuItem(
                                text = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.ContentCopy, null,
                                            tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        Text("Duplicate", fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = MulishFamily, color = TextPrimary)
                                    }
                                },
                                onClick = { menuExpanded = false; onDuplicate() },
                                colors  = MenuDefaults.itemColors(textColor = TextPrimary),
                            )
                            // Delete
                            DropdownMenuItem(
                                text = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Delete, null,
                                            tint = Danger, modifier = Modifier.size(16.dp))
                                        Text("Delete", fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = MulishFamily, color = Danger)
                                    }
                                },
                                onClick = { menuExpanded = false; onDelete() },
                                colors  = MenuDefaults.itemColors(textColor = Danger),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Day chips row ────────────────────────────────────────────────────────────

@Composable
private fun AlarmDayChips(repeatDays: String, enabled: Boolean) {
    if (repeatDays.isBlank()) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (enabled) PrimaryGlow else SurfaceHigh)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text("ONCE", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily, letterSpacing = 0.8.sp,
                color = if (enabled) Primary else TextMuted)
        }
        return
    }

    val days = repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.sorted()
    val allDays   = days.size == 7
    val isWeekday = days == listOf(2,3,4,5,6)
    val isWeekend = days == listOf(1,7) || days == listOf(1,6,7)

    when {
        allDays -> SingleChip("EVERY DAY", Primary, enabled)
        isWeekday -> SingleChip("WEEKDAYS", Color(0xFF3B82F6), enabled)
        isWeekend -> SingleChip("WEEKENDS", Color(0xFF10B981), enabled)
        else -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            days.forEach { day ->
                val color = if (enabled) (DAY_COLORS[day] ?: Primary) else TextMuted
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)) {
                    Text(DAY_LABELS[day] ?: "", fontSize = 9.sp,
                        fontWeight = FontWeight.Bold, fontFamily = MulishFamily,
                        letterSpacing = 0.5.sp, color = color)
                }
            }
        }
    }
}

@Composable
private fun SingleChip(label: String, color: Color, enabled: Boolean) {
    val c = if (enabled) color else TextMuted
    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
        .background(c.copy(alpha = 0.15f))
        .padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily, letterSpacing = 0.8.sp, color = c)
    }
}

// ─── Feature indicators ───────────────────────────────────────────────────────

@Composable
private fun AlarmFeatureIndicators(alarm: AlarmEntity) {
    val indicators = buildList {
        if (alarm.vibrate)       add("📳" to "Vibrate")
        if (alarm.snoozeEnabled) add("💤" to "${alarm.snoozeIntervalMinutes}m snooze")
        if (alarm.extraLoud)     add("💥" to "Extra loud")
        if (alarm.tasks != "[]" && alarm.tasks.isNotBlank() && alarm.tasks != "[]")
                                 add("⚡" to "Tasks")
    }
    if (indicators.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        indicators.forEach { (emoji, _) ->
            Text(emoji, fontSize = 11.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HELPERS (keep existing functions)
// ─────────────────────────────────────────────────────────────────────────────

fun formatTime(hour: Int, minute: Int): String {
    val period      = if (hour >= 12) "PM" else "AM"
    val displayHour = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
    return "$displayHour:${minute.toString().padStart(2, '0')} $period"
}

fun getRepeatLabel(repeatDays: String): String {
    if (repeatDays.isBlank()) return "Once"
    val days = repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
    return when {
        days.size == 7           -> "Every day"
        days == listOf(2,3,4,5,6) -> "Weekdays"
        days == listOf(1,7)       -> "Weekends"
        else -> days.joinToString(" · ") {
            when (it) { 1->"Sun"; 2->"Mon"; 3->"Tue"; 4->"Wed";
                        5->"Thu"; 6->"Fri"; 7->"Sat"; else->"" }
        }
    }
}
