package com.tushartamrakar.ontime.focus.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.Success
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.Warning
import com.tushartamrakar.ontime.focus.foreground.FocusTimerService
import kotlin.math.sin

// ─── Data model ───────────────────────────────────────────────────────────────

data class CelebrationData(
    val todaySessions: Int,
    val goalSessions:  Int,
    val streakDays:    Int,
    val todaySeconds:  Int = 0,
) {
    val todayFormatted: String get() {
        val m = todaySeconds / 60
        val h = m / 60
        return if (h > 0) "${h}h ${m % 60}m" else "${m}m"
    }
}

// ─── Confetti particles ───────────────────────────────────────────────────────

private data class Particle(
    val startX:        Float,   // 0..1 normalized
    val delayFraction: Float,   // 0..1 — stagger start time
    val color:         Color,
    val size:          Float,   // dp equivalent
    val speed:         Float,   // fall speed multiplier
    val driftAmp:      Float,   // horizontal drift amplitude
    val driftFreq:     Float,   // horizontal drift frequency
    val rotation:      Float,   // initial rotation degrees
    val rotSpeed:      Float,   // rotation speed multiplier
    val isRect:        Boolean, // rectangle vs circle
)

private val CONFETTI_COLORS = listOf(
    Color(0xFF7C3AED), // Primary purple
    Color(0xFFA78BFA), // Light purple
    Color(0xFFF59E0B), // Gold
    Color(0xFF10B981), // Green
    Color(0xFFEC4899), // Pink
    Color(0xFF06B6D4), // Cyan
    Color(0xFFFFFFFF), // White
    Color(0xFFF97316), // Orange
)

private val MOTIVATION_QUOTES = listOf(
    "Great habits are built one session at a time.",
    "Consistency is the key to mastery.",
    "You showed up today. That's what matters.",
    "Small wins compound into big results.",
    "Discipline is choosing what you want most\nover what you want now.",
    "The secret of getting ahead is getting started.",
    "Focus is the gateway to all thinking.",
    "You did it. Now do it again tomorrow.",
)

private fun generateParticles(count: Int = 90): List<Particle> =
    (0 until count).map {
        Particle(
            startX        = (0..1000).random() / 1000f,
            delayFraction = (0..600).random() / 1000f,
            color         = CONFETTI_COLORS.random(),
            size          = (4..10).random().toFloat(),
            speed         = 0.15f + (0..100).random() / 400f,
            driftAmp      = (10..40).random().toFloat(),
            driftFreq     = 1f + (0..30).random() / 10f,
            rotation      = (0..360).random().toFloat(),
            rotSpeed      = (0.5f..3f).let { it.start + (0..100).random() / 40f },
            isRect        = it % 3 != 0,  // 2/3 rectangles, 1/3 circles
        )
    }

// ─── Main overlay ─────────────────────────────────────────────────────────────

@Composable
fun CelebrationOverlay(
    data:      CelebrationData,
    onDismiss: () -> Unit,
) {
    val quote     = remember { MOTIVATION_QUOTES.random() }
    val particles = remember { generateParticles() }
    var visible   by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.92f),
                        Background.copy(alpha = 0.96f),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        // ── Confetti raining layer ─────────────────────────────────────────────
        ConfettiLayer(particles = particles, modifier = Modifier.fillMaxSize())

        // ── Content card ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.85f),
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Surface)
                    .border(1.dp, Primary.copy(alpha = 0.30f), RoundedCornerShape(28.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                // Trophy emoji
                Text(text = "🏆", fontSize = 64.sp)

                // Title
                Text(
                    text       = "Daily Goal Achieved!",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily,
                    color      = TextPrimary,
                    textAlign  = TextAlign.Center,
                )

                // Subtitle
                Text(
                    text       = "You crushed ${data.goalSessions} sessions today 🔥",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color      = TextMuted,
                    textAlign  = TextAlign.Center,
                )

                // Stats row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    CelebrationStat(
                        emoji = "⏱",
                        value = data.todayFormatted.ifBlank { "${data.todaySessions * 25}m" },
                        label = "focused today",
                        color = Primary,
                    )
                    CelebrationStat(
                        emoji = "🔥",
                        value = "${data.streakDays}",
                        label = "day streak",
                        color = Warning,
                    )
                    CelebrationStat(
                        emoji = "✅",
                        value = "${data.todaySessions}/${data.goalSessions}",
                        label = "sessions",
                        color = Success,
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Border.copy(alpha = 0.5f))
                )

                // Motivational quote
                Text(
                    text       = "\"$quote\"",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    fontStyle  = FontStyle.Italic,
                    color      = TextMuted,
                    textAlign  = TextAlign.Center,
                    lineHeight = 20.sp,
                )

                // Dismiss button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Primary, Color(0xFF9333EA))
                            )
                        )
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = "Keep Going! 🚀",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color      = Color.White,
                    )
                }
            }
        }
    }
}

// ─── Confetti Canvas ──────────────────────────────────────────────────────────

@Composable
private fun ConfettiLayer(
    particles: List<Particle>,
    modifier:  Modifier = Modifier,
) {
    // Single infinite float drives all particle positions — 0..1 per cycle
    val transition = rememberInfiniteTransition(label = "confetti")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "confetti_time",
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        particles.forEach { p ->
            // Offset by delay so particles stagger
            val t = ((time - p.delayFraction + 1f) % 1f)

            val x = p.startX * size.width +
                    sin(t * p.driftFreq * 6.28f) * p.driftAmp.dp.toPx()
            val y = t * (size.height * 1.1f) - size.height * 0.05f

            // Fade out in final 20% of fall
            val alpha = if (t > 0.8f) 1f - (t - 0.8f) / 0.2f else 1f
            if (alpha <= 0f) return@forEach

            val rotDeg = p.rotation + t * p.rotSpeed * 360f

            withTransform({
                translate(left = x, top = y)
                rotate(degrees = rotDeg, pivot = Offset.Zero)
            }) {
                val pxSize = p.size.dp.toPx()
                val color  = p.color.copy(alpha = alpha)
                if (p.isRect) {
                    drawRect(
                        color   = color,
                        topLeft = Offset(-pxSize, -pxSize * 0.5f),
                        size    = Size(pxSize * 2f, pxSize),
                    )
                } else {
                    drawCircle(color = color, radius = pxSize)
                }
            }
        }
    }
}

// ─── Stat cell ────────────────────────────────────────────────────────────────

@Composable
private fun CelebrationStat(
    emoji: String,
    value: String,
    label: String,
    color: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(emoji, fontSize = 20.sp)
        Text(
            text       = value,
            fontSize   = 20.sp,
            fontWeight = FontWeight.Black,
            fontFamily = MulishFamily,
            color      = color,
        )
        Text(
            text       = label,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color      = TextMuted,
        )
    }
}
