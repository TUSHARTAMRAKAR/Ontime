package com.tushartamrakar.ontime.alarm.presentation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.alarm.domain.TaskType
import com.tushartamrakar.ontime.alarm.domain.WakeUpTask
import com.tushartamrakar.ontime.alarm.domain.toWakeUpTasks
import com.tushartamrakar.ontime.alarm.presentation.tasks.BarcodeTaskRuntimeScreen
import com.tushartamrakar.ontime.alarm.presentation.tasks.MathTaskRuntimeScreen
import com.tushartamrakar.ontime.alarm.presentation.tasks.ShakeTaskRuntimeScreen
import com.tushartamrakar.ontime.alarm.presentation.tasks.TypingTaskRuntimeScreen
import com.tushartamrakar.ontime.alarm.service.AlarmService
import com.tushartamrakar.ontime.alarm.service.RiseCheckService
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.Danger
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.OntimeTheme
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary
import com.tushartamrakar.ontime.core.ui.theme.Warning
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  ACTIVITY
// ─────────────────────────────────────────────────────────────────────────────

class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }

        // ── Read ALL intent extras ────────────────────────────────────────────
        val alarmId          = intent.getIntExtra("ALARM_ID", -1)
        val alarmLabel       = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val tasksJson        = intent.getStringExtra("ALARM_TASKS") ?: "[]"
        val tasks            = tasksJson.toWakeUpTasks()
        val riseCheckMinutes = intent.getIntExtra("ALARM_RISE_CHECK_MINUTES", 0)
        val isEventReminder  = intent.getBooleanExtra("IS_EVENT_REMINDER", false)

        // Snooze params — all required for count display + limit enforcement
        val snoozeEnabled    = intent.getBooleanExtra("ALARM_SNOOZE_ENABLED", true)
        val snoozeInterval   = intent.getIntExtra("ALARM_SNOOZE_INTERVAL", 5)
        val snoozeLimit      = intent.getIntExtra("ALARM_SNOOZE_LIMIT", 3)  // 0 = unlimited
        val snoozeCount      = intent.getIntExtra("ALARM_SNOOZE_COUNT", 0)

        setContent {
            OntimeTheme {
                AlarmRingScreen(
                    alarmLabel       = alarmLabel,
                    tasks            = tasks,
                    riseCheckMinutes = riseCheckMinutes,
                    isEventReminder  = isEventReminder,
                    snoozeEnabled    = snoozeEnabled,
                    snoozeInterval   = snoozeInterval,
                    snoozeLimit      = snoozeLimit,
                    snoozeCount      = snoozeCount,
                    onDismiss        = { dismissAlarm() },
                    onSnooze         = { snoozeAlarm(alarmId, alarmLabel) },
                    onStopSound      = { stopSound() },
                    onRestartAlarm   = { restartAlarm() },
                )
            }
        }
    }

    private fun dismissAlarm() {
        startService(Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
        })
        finish()
    }

    private fun snoozeAlarm(alarmId: Int, alarmLabel: String) {
        startService(Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
        })
        finish()
    }

    private fun stopSound() {
        startService(Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_SOUND
        })
    }

    private fun restartAlarm() {
        startService(Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_RESTART_SOUND
        })
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  IDLE TIMER BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun IdleTimerBar(
    isIdle: Boolean,
    onTimerExpired: () -> Unit,
) {
    var timeLeft by remember { mutableFloatStateOf(30f) }
    val progress by animateFloatAsState(
        targetValue   = (timeLeft / 30f).coerceIn(0f, 1f),
        animationSpec = tween(200),
        label         = "timer_progress",
    )
    LaunchedEffect(isIdle, timeLeft) {
        if (isIdle && timeLeft > 0f) {
            delay(100)
            timeLeft -= 0.1f
        } else if (timeLeft <= 0f) {
            onTimerExpired()
        }
    }
    LinearProgressIndicator(
        progress      = { progress },
        modifier      = Modifier.fillMaxWidth().height(3.dp),
        color         = if (timeLeft <= 10f) Danger else Primary,
        trackColor    = SurfaceHigh,
        strokeCap     = StrokeCap.Square,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  MAIN RING SCREEN — State machine
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AlarmRingScreen(
    alarmLabel:       String,
    tasks:            List<WakeUpTask>,
    riseCheckMinutes: Int = 0,
    isEventReminder:  Boolean = false,
    snoozeEnabled:    Boolean = true,
    snoozeInterval:   Int = 5,
    snoozeLimit:      Int = 3,
    snoozeCount:      Int = 0,
    onDismiss:        () -> Unit,
    onSnooze:         () -> Unit,
    onStopSound:      () -> Unit,
    onRestartAlarm:   () -> Unit,
) {
    var screenState      by remember { mutableStateOf(if (tasks.isEmpty()) "DONE" else "RINGING") }
    var currentTaskIndex by remember { mutableIntStateOf(0) }
    var isUserActive     by remember { mutableStateOf(false) }

    // Derived snooze state — computed once and passed down
    val canSnooze = snoozeEnabled && (snoozeLimit == 0 || snoozeCount < snoozeLimit)
    val snoozeLabel = buildSnoozeLabel(snoozeEnabled, snoozeInterval, snoozeLimit, snoozeCount)

    when (screenState) {

        // ─── RINGING ──────────────────────────────────────────────────────────
        "RINGING" -> AlarmRingingScreen(
            alarmLabel   = alarmLabel,
            tasks        = tasks,
            canSnooze    = canSnooze,
            snoozeEnabled = snoozeEnabled,
            snoozeLabel  = snoozeLabel,
            snoozeCount  = snoozeCount,
            snoozeLimit  = snoozeLimit,
            onStartTask  = { onStopSound(); screenState = "TASKS" },
            onDismiss    = onDismiss,
            onSnooze     = onSnooze,
        )

        // ─── TASKS ────────────────────────────────────────────────────────────
        "TASKS" -> Box(
            modifier = Modifier.fillMaxSize().background(Background),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                IdleTimerBar(
                    isIdle = !isUserActive,
                    onTimerExpired = {
                        onRestartAlarm()
                        screenState      = "RINGING"
                        currentTaskIndex = 0
                        isUserActive     = false
                    },
                )

                // Task progress dots
                if (tasks.size > 1) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        tasks.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (index == currentTaskIndex) 10.dp else 8.dp)
                                    .background(
                                        if (index <= currentTaskIndex) Primary else SurfaceHigh,
                                        CircleShape,
                                    ),
                            )
                        }
                    }
                }

                val currentTask = tasks.getOrNull(currentTaskIndex)
                if (currentTask != null) {
                    Box(modifier = Modifier.weight(1f)) {
                        val onCompleted: () -> Unit = {
                            isUserActive = false
                            if (currentTaskIndex + 1 >= tasks.size) {
                                screenState = "DONE"
                            } else {
                                currentTaskIndex++
                            }
                        }
                        when (currentTask.type) {
                            TaskType.MATH    -> MathTaskRuntimeScreen(currentTask,
                                { isUserActive = it }, onCompleted)
                            TaskType.TYPING  -> TypingTaskRuntimeScreen(currentTask,
                                { isUserActive = it }, onCompleted)
                            TaskType.SHAKE   -> ShakeTaskRuntimeScreen(currentTask,
                                { isUserActive = it }, onCompleted)
                            TaskType.BARCODE -> BarcodeTaskRuntimeScreen(currentTask,
                                { isUserActive = it }, onCompleted)
                        }
                    }
                }

                // Snooze row at bottom of task screen
                if (canSnooze) {
                    OutlinedButton(
                        onClick  = onSnooze,
                        modifier = Modifier.fillMaxWidth().padding(24.dp).height(52.dp),
                        shape    = RoundedCornerShape(16.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                    ) {
                        Text(snoozeLabel, fontSize = 15.sp,
                            fontWeight = FontWeight.Bold, fontFamily = MulishFamily)
                    }
                } else if (snoozeEnabled) {
                    NoMoreSnoozesRow()
                }
            }
        }

        // ─── DONE ─────────────────────────────────────────────────────────────
        "DONE" -> AlarmDoneScreen(
            alarmLabel        = alarmLabel,
            tasksWereRequired = tasks.isNotEmpty(),
            riseCheckMinutes  = riseCheckMinutes,
            isEventReminder   = isEventReminder,
            canSnooze         = canSnooze,
            snoozeLabel       = snoozeLabel,
            snoozeEnabled     = snoozeEnabled,
            onDismiss         = onDismiss,
            onSnooze          = onSnooze,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  RINGING SCREEN — Ultra premium
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AlarmRingingScreen(
    alarmLabel:    String,
    tasks:         List<WakeUpTask>,
    canSnooze:     Boolean,
    snoozeEnabled: Boolean,
    snoozeLabel:   String,
    snoozeCount:   Int,
    snoozeLimit:   Int,
    onStartTask:   () -> Unit,
    onDismiss:     () -> Unit,
    onSnooze:      () -> Unit,
) {
    // Live ticking clock
    var currentTime   by remember { mutableStateOf("") }
    var currentPeriod by remember { mutableStateOf("") }
    var currentDate   by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime   = SimpleDateFormat("hh:mm", Locale.getDefault()).format(now)
            currentPeriod = SimpleDateFormat("a",     Locale.getDefault()).format(now)
            currentDate   = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now)
            delay(1000)
        }
    }

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "ring")

    // Pulsing bell scale
    val bellScale by infiniteTransition.animateFloat(
        initialValue  = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900), repeatMode = RepeatMode.Reverse,
        ), label = "bell_scale",
    )

    // Three expanding ripple rings
    val ripple1 by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing), repeatMode = RepeatMode.Restart,
        ), label = "ripple1",
    )
    val ripple2 by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, 700, easing = LinearEasing), repeatMode = RepeatMode.Restart,
        ), label = "ripple2",
    )
    val ripple3 by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, 1400, easing = LinearEasing), repeatMode = RepeatMode.Restart,
        ), label = "ripple3",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Background,
                        Color(0xFF0D0820),   // deep purple at center
                        Background,
                    )
                )
            ),
    ) {
        Column(
            modifier              = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.SpaceBetween,
        ) {

            // ── Top: date + time ──────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = currentDate,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color      = TextMuted,
                    letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text          = currentTime,
                        fontSize      = 80.sp,
                        fontWeight    = FontWeight.Black,
                        fontFamily    = MulishFamily,
                        color         = TextPrimary,
                        letterSpacing = (-3).sp,
                        lineHeight    = 80.sp,
                    )
                    Text(
                        text       = currentPeriod,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color      = Primary,
                        modifier   = Modifier.padding(bottom = 12.dp),
                    )
                }
            }

            // ── Center: ripple + bell ─────────────────────────────────────────
            Box(contentAlignment = Alignment.Center,
                modifier         = Modifier.size(220.dp)) {

                // Ripple rings drawn on Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center    = Offset(size.width / 2, size.height / 2)
                    val maxRadius = size.minDimension / 2

                    listOf(ripple1, ripple2, ripple3).forEach { progress ->
                        val radius = maxRadius * progress
                        val alpha  = (1f - progress) * 0.35f
                        drawCircle(
                            color  = Primary.copy(alpha = alpha),
                            radius = radius,
                            center = center,
                            style  = Stroke(width = 2.dp.toPx()),
                        )
                        drawCircle(
                            color  = Primary.copy(alpha = alpha * 0.3f),
                            radius = radius,
                            center = center,
                        )
                    }
                }

                // Bell circle
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(bellScale)
                        .background(
                            Brush.radialGradient(
                                listOf(Primary.copy(alpha = 0.25f), Primary.copy(alpha = 0.08f))
                            ),
                            CircleShape,
                        )
                        .border(1.5.dp, Primary.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⏰", fontSize = 52.sp)
                }
            }

            // ── Label + task info ─────────────────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text       = alarmLabel,
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = TextPrimary,
                    textAlign  = TextAlign.Center,
                    lineHeight = 34.sp,
                )
                if (tasks.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Primary.copy(alpha = 0.12f))
                            .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                    ) {
                        Text(
                            "⚡ ${tasks.size} wake-up task${if (tasks.size > 1) "s" else ""} required",
                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily, color = Primary,
                        )
                    }
                } else {
                    Text("Alarm ringing...", fontSize = 14.sp,
                        fontFamily = MulishFamily, color = TextSecondary)
                }
            }

            // ── Bottom: action buttons ────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Primary action: Start Task or Dismiss
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Primary, Color(0xFF9333EA)))
                        )
                        .clickable { if (tasks.isNotEmpty()) onStartTask() else onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = if (tasks.isNotEmpty()) "⚡  START TASK" else "✓  Dismiss",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color      = Color.White,
                        letterSpacing = 0.5.sp,
                    )
                }

                // Snooze button — dynamic label + hides when limit hit
                if (canSnooze) {
                    val isLastSnooze = snoozeLimit > 0 && snoozeCount == snoozeLimit - 1
                    OutlinedButton(
                        onClick  = onSnooze,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape    = RoundedCornerShape(18.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isLastSnooze) Warning else Primary,
                        ),
                        border   = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isLastSnooze) Warning.copy(alpha = 0.5f) else Primary.copy(alpha = 0.4f),
                        ),
                    ) {
                        Text(
                            text       = snoozeLabel,
                            fontSize   = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MulishFamily,
                        )
                    }
                } else if (snoozeEnabled) {
                    NoMoreSnoozesRow()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DONE SCREEN — Premium
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AlarmDoneScreen(
    alarmLabel:        String,
    tasksWereRequired: Boolean,
    riseCheckMinutes:  Int = 0,
    isEventReminder:   Boolean = false,
    canSnooze:         Boolean = true,
    snoozeLabel:       String = "Snooze 5 min",
    snoozeEnabled:     Boolean = true,
    onDismiss:         () -> Unit,
    onSnooze:          () -> Unit,
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Background, Color(0xFF0D0820), Background))
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.SpaceEvenly,
            modifier              = Modifier
                .fillMaxSize()
                .padding(32.dp),
        ) {
            // Success icon
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Primary.copy(alpha = 0.25f), Primary.copy(alpha = 0.05f))
                        ),
                        CircleShape,
                    )
                    .border(1.5.dp, Primary.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (isEventReminder) "🔔" else if (tasksWereRequired) "✅" else "🎉",
                    fontSize = 52.sp,
                )
            }

            // Messages
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = when {
                        isEventReminder   -> alarmLabel
                        tasksWereRequired -> "All Tasks Done!"
                        else              -> "Good Morning!"
                    },
                    fontSize      = 30.sp,
                    fontWeight    = FontWeight.Black,
                    fontFamily    = MulishFamily,
                    color         = TextPrimary,
                    textAlign     = TextAlign.Center,
                    lineHeight    = 38.sp,
                    letterSpacing = (-0.5).sp,
                )

                if (isEventReminder) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Primary.copy(alpha = 0.12f))
                            .border(1.dp, Primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Text("🔔  Event Reminder", fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily, color = Primary)
                    }
                } else {
                    Text(
                        text       = alarmLabel,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color      = TextMuted,
                        textAlign  = TextAlign.Center,
                    )
                }

                if (tasksWereRequired) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                    ) {
                        Text("✅  Wake-up tasks completed!", fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily, color = Color(0xFF10B981))
                    }
                }

                if (riseCheckMinutes > 0) {
                    Text(
                        text       = "⏰ Are You Up? check in ${riseCheckMinutes}m",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color      = Primary,
                    )
                }
            }

            // Buttons
            Column(modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Primary, Color(0xFF9333EA)))
                        )
                        .clickable {
                            if (riseCheckMinutes > 0) {
                                RiseCheckService.schedule(context, riseCheckMinutes, alarmLabel)
                            }
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = "✓  Dismiss",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color      = Color.White,
                    )
                }

                if (canSnooze) {
                    OutlinedButton(
                        onClick  = onSnooze,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape    = RoundedCornerShape(18.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                        border   = androidx.compose.foundation.BorderStroke(
                            1.5.dp, Primary.copy(alpha = 0.4f)
                        ),
                    ) {
                        Text(snoozeLabel, fontSize = 17.sp,
                            fontWeight = FontWeight.Bold, fontFamily = MulishFamily)
                    }
                } else if (snoozeEnabled) {
                    NoMoreSnoozesRow()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HELPERS
// ─────────────────────────────────────────────────────────────────────────────

/** Builds the snooze button label with count indicator. */
fun buildSnoozeLabel(
    enabled:  Boolean,
    interval: Int,
    limit:    Int,
    count:    Int,
): String {
    if (!enabled) return ""
    val countPart = when {
        limit == 0 -> "∞"                          // unlimited
        else       -> "${count + 1}/$limit"         // e.g. "2/3"
    }
    return "💤  Snooze ${interval}m  ·  $countPart"
}

/** Small row shown when snooze limit is exhausted. */
@Composable
fun NoMoreSnoozesRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Danger.copy(alpha = 0.08f))
            .border(1.dp, Danger.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = "🚫  No more snoozes — time to wake up!",
            fontSize   = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color      = Danger,
            textAlign  = TextAlign.Center,
        )
    }
}
