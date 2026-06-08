package com.tushartamrakar.ontime.alarm.presentation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.alarm.service.AlarmService
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.Danger
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.OntimeTheme
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import kotlinx.coroutines.delay

class RiseCheckActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on lock screen
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

        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"

        setContent {
            OntimeTheme {
                RiseCheckScreen(
                    alarmLabel = alarmLabel,
                    onYesAwake = { finish() },
                    onTimerExpired = { ringAlarmAgain(alarmLabel) },
                )
            }
        }
    }

    private fun ringAlarmAgain(alarmLabel: String) {
        val serviceIntent = Intent(this, AlarmService::class.java).apply {
            putExtra("ALARM_ID", -1)
            putExtra("ALARM_LABEL", alarmLabel)
            putExtra("ALARM_VIBRATE", true)
            putExtra("ALARM_TASKS", "[]")
        }
        startForegroundService(serviceIntent)
        finish()
    }
}

@Composable
fun RiseCheckScreen(
    alarmLabel: String,
    onYesAwake: () -> Unit,
    onTimerExpired: () -> Unit,
) {
    var timeLeft by remember { mutableFloatStateOf(30f) }
    val progress by animateFloatAsState(
        targetValue = (timeLeft / 30f).coerceIn(0f, 1f),
        animationSpec = tween(200),
        label = "rise_check_timer",
    )

    // Countdown
    LaunchedEffect(Unit) {
        while (timeLeft > 0f) {
            delay(100)
            timeLeft -= 0.1f
        }
        onTimerExpired()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Timer bar at very top
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = if (timeLeft <= 10f) Danger else Primary,
                trackColor = SurfaceHigh,
                strokeCap = StrokeCap.Square,
            )

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Big emoji
                Text(
                    text = "🌅",
                    fontSize = 80.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Are You Up?",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = alarmLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap YES or the alarm rings again!",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(48.dp))

                // YES button
                Button(
                    onClick = onYesAwake,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        text = "✅  Yes, I'm Up!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timer text
                Text(
                    text = "${timeLeft.toInt()}s remaining",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily,
                    color = if (timeLeft <= 10f) Danger else TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}