package com.tushartamrakar.ontime.focus.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.Success
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary
import com.tushartamrakar.ontime.focus.data.local.SessionType

/**
 * Circular progress ring for the Pomodoro timer.
 *
 * Draws three concentric layers using Canvas:
 *   1. Dark track ring (background)
 *   2. Animated progress arc (fills clockwise as time passes)
 *   3. Center content (time text + phase label)
 *
 * Progress animates smoothly with a 600ms tween so ticks don't jump.
 * Arc color shifts: indigo (WORK) → green (breaks) for instant phase recognition.
 */
@Composable
fun CircularTimer(
    modifier: Modifier = Modifier,
    progress: Float,           // 0.0 → 1.0 (how much of the ring is filled)
    timeText: String,          // "25:00"
    phaseLabel: String,        // "Focus" / "Short break" / "Long break"
    phase: SessionType = SessionType.WORK,
    size: Dp = 240.dp,
    strokeWidth: Dp = 14.dp,
    content: @Composable () -> Unit = {},  // extra content inside the circle
) {
    // Smooth progress animation — prevents jarring jumps on each tick
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "timer_progress",
    )

    // Arc color changes with phase for instant visual feedback
    val arcColor = when (phase) {
        SessionType.WORK        -> Primary   // indigo — focus mode
        SessionType.SHORT_BREAK -> Success   // green — break
        SessionType.LONG_BREAK  -> Success   // green — break
    }

    // Capture theme colors in composable context BEFORE entering the Canvas
    // DrawScope lambda — @Composable getters cannot be called inside Canvas.
    val trackColor = Border

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        // ── Canvas ring ────────────────────────────────────────────────────────
        Canvas(modifier = Modifier.size(size)) {
            val strokePx   = strokeWidth.toPx()
            val diameter   = size.toPx() - strokePx
            val topLeft    = Offset(strokePx / 2f, strokePx / 2f)
            val arcSize    = Size(diameter, diameter)
            val startAngle = -90f   // start at 12 o'clock

            // 1. Dark track ring
            drawArc(
                color       = trackColor,
                startAngle  = startAngle,
                sweepAngle  = 360f,
                useCenter   = false,
                topLeft     = topLeft,
                size        = arcSize,
                style       = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            // 2. Progress arc
            if (animatedProgress > 0f) {
                drawArc(
                    color      = arcColor,
                    startAngle = startAngle,
                    sweepAngle = 360f * animatedProgress,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = strokePx, cap = StrokeCap.Round),
                )

                // 3. Glowing dot at the tip of the progress arc
                val angleRad  = Math.toRadians(
                    (startAngle + 360f * animatedProgress).toDouble()
                )
                val radius    = diameter / 2f
                val centerX   = size.toPx() / 2f
                val centerY   = size.toPx() / 2f
                val dotX      = centerX + radius * Math.cos(angleRad).toFloat()
                val dotY      = centerY + radius * Math.sin(angleRad).toFloat()
                drawCircle(
                    color  = arcColor,
                    radius = strokePx / 2f,
                    center = Offset(dotX, dotY),
                )
                // Outer glow
                drawCircle(
                    color  = arcColor.copy(alpha = 0.25f),
                    radius = strokePx,
                    center = Offset(dotX, dotY),
                )
            }
        }

        // ── Center content ─────────────────────────────────────────────────────
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Phase label — small text above the time
            Text(
                text       = phaseLabel.uppercase(),
                fontSize   = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color      = arcColor.copy(alpha = 0.8f),
                letterSpacing = 2.sp,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Big time text
            Text(
                text       = timeText,
                fontSize   = 52.sp,
                fontWeight = FontWeight.Black,
                fontFamily = MulishFamily,
                color      = TextPrimary,
                letterSpacing = (-1).sp,
            )

            // Extra slot — caller can inject "3 apps blocked" indicator etc.
            content()
        }
    }
}
