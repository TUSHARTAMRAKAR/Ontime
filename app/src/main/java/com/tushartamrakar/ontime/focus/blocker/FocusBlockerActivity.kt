package com.tushartamrakar.ontime.focus.blocker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.OntimeTheme
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.focus.foreground.FocusTimerService
import com.tushartamrakar.ontime.focus.foreground.FocusTimerState

/**
 * Full-screen overlay shown immediately when a blocked app is detected.
 *
 * Appears on top of every app — including the blocked app — using:
 *   FLAG_SHOW_WHEN_LOCKED   → visible on lock screen
 *   FLAG_KEEP_SCREEN_ON     → screen stays on while overlay is showing
 *   FLAG_TURN_SCREEN_ON     → wakes screen if it was off
 *
 * The user cannot "back" into the blocked app — pressing back returns
 * them to their home launcher, not the blocked app.
 *
 * Shows:
 *   - Large shield icon (pulsing ring animation)
 *   - Blocked app name
 *   - Current timer countdown synced from FocusTimerService.timerState
 *   - Motivational message
 *   - "Go back to focus" button → closes overlay (returns to home)
 */
class FocusBlockerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BLOCKED_APP_NAME = "blocked_app_name"
        const val EXTRA_BLOCK_REASON     = "block_reason"   // "FOCUS" or "ADULT"

        fun createIntent(
            context: android.content.Context,
            appName: String,
            reason: String = "FOCUS",
        ) = Intent(context, FocusBlockerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_BLOCKED_APP_NAME, appName)
            putExtra(EXTRA_BLOCK_REASON, reason)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Window flags — show over everything including lock screen ──────────
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON  or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
        )

        val blockedAppName = intent.getStringExtra(EXTRA_BLOCKED_APP_NAME) ?: "that app"
        val blockReason    = intent.getStringExtra(EXTRA_BLOCK_REASON) ?: "FOCUS"

        setContent {
            OntimeTheme {
                BlockerOverlayScreen(
                    blockedAppName = blockedAppName,
                    blockReason    = blockReason,
                    onGoBack       = {
                        // Send user to home screen — not back to the blocked app
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish()
                    },
                )
            }
        }
    }

    /** Block hardware back button — user must use our "Go back" button. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

// ─── Overlay UI ───────────────────────────────────────────────────────────────

@Composable
private fun BlockerOverlayScreen(
    blockedAppName: String,
    blockReason: String,
    onGoBack: () -> Unit,
) {
    val timerState by FocusTimerService.timerState.collectAsState()

    val isAdult  = blockReason == "ADULT"
    val accentColor = if (isAdult) Color(0xFFEF4444) else Primary  // red for adult, purple for focus

    // Pulsing ring animation
    val pulse = rememberInfiniteTransition(label = "pulse")
    val ringScale by pulse.animateFloat(
        initialValue = 0.85f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ring_scale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 36.dp),
        ) {

            // ── Pulsing shield circle ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.08f * ringScale)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isAdult) "🛡️" else "🔒",
                        fontSize = 44.sp,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Main heading ───────────────────────────────────────────────────
            Text(
                text = if (isAdult) "Blocked" else "Stay focused.",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                fontFamily = MulishFamily,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(10.dp))

            // ── Blocked app name ───────────────────────────────────────────────
            Text(
                text = if (isAdult)
                    "$blockedAppName is blocked by your content filter."
                else
                    "$blockedAppName is blocked during your focus session.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.height(28.dp))

            // ── Timer countdown (only during focus sessions) ───────────────────
            if (!isAdult) {
                val timeText = when (val state = timerState) {
                    is FocusTimerState.Running ->
                        "%02d:%02d left".format(state.secondsLeft / 60, state.secondsLeft % 60)
                    is FocusTimerState.Paused  -> "Session paused"
                    else -> ""
                }

                if (timeText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Surface)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = timeText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = MulishFamily,
                            color = accentColor,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── Motivational message ──────────────────────────────────────────
            val messages = if (isAdult) listOf(
                "You're building better habits.",
                "Every moment of resistance strengthens you.",
                "The person you want to be is watching.",
            ) else listOf(
                "You got this. Keep going.",
                "Every second counts. Stay locked in.",
                "Champions don't quit mid-session.",
                "Distractions are the enemy of greatness.",
            )
            val message = remember(blockedAppName) {
                messages[(blockedAppName.hashCode().and(0x7fffffff)) % messages.size]
            }
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color = TextMuted.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )

            Spacer(Modifier.height(40.dp))

            // ── Go back button ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface)
                    .padding(horizontal = 28.dp, vertical = 14.dp)
                    .clickable { onGoBack() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isAdult) "Go back" else "Back to focus",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = accentColor,
                )
            }
        }
    }
}

// ─── Remember helper (doesn't need import in same file) ──────────────────────
@Composable
private fun <T> remember(key: Any, calculation: () -> T): T =
    androidx.compose.runtime.remember(key) { calculation() }
