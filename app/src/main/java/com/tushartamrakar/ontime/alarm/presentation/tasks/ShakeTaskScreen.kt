package com.tushartamrakar.ontime.alarm.presentation.tasks

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.alarm.domain.TaskType
import com.tushartamrakar.ontime.alarm.domain.WakeUpTask
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import kotlin.math.sqrt

// ─── Shake Task Config UI ─────────────────────────────────────────────────────
@Composable
fun ShakeTaskConfigSheet(
    onSave: (WakeUpTask) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedCount by remember { mutableIntStateOf(30) }

    val shakeOptions = listOf(
        5 to "Warm Up\n5 shakes",
        10 to "Easy\n10 shakes",
        30 to "Medium\n30 shakes",
        50 to "Hard\n50 shakes",
        100 to "Extreme\n100 shakes",
        200 to "Beast\n200 shakes",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .padding(24.dp),
    ) {
        Text(
            text = "📳 Shake Task",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Shake your phone to dismiss the alarm",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "NUMBER OF SHAKES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily,
            color = TextMuted,
            letterSpacing = 1.sp,
        )

        Spacer(modifier = Modifier.height(12.dp))

        shakeOptions.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { (count, label) ->
                    val isSelected = selectedCount == count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Primary else SurfaceHigh)
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) Primary else Border,
                                shape = RoundedCornerShape(14.dp),
                            )
                            .clickable { selectedCount = count },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = if (isSelected) Color.White else TextMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (rowItems.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Primary.copy(alpha = 0.1f))
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "📳 Shake $selectedCount times to dismiss",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color = Primary,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Primary)
                .clickable {
                    onSave(
                        WakeUpTask(
                            type = TaskType.SHAKE,
                            shakeCount = selectedCount,
                        )
                    )
                    onDismiss()
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Add Shake Task",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─── Shake Task Runtime UI ────────────────────────────────────────────────────
@Composable
fun ShakeTaskRuntimeScreen(
    task: WakeUpTask,
    onUserActiveChange: (Boolean) -> Unit,
    onTaskCompleted: () -> Unit,
) {
    val context = LocalContext.current
    val targetShakes = task.shakeCount
    var currentShakes by remember { mutableIntStateOf(0) }
    var isShaking by remember { mutableStateOf(false) }

    val progress = (currentShakes.toFloat() / targetShakes).coerceIn(0f, 1f)
    val scale by animateFloatAsState(
        targetValue = if (isShaking) 1.2f else 1f,
        animationSpec = tween(100),
        label = "shake_scale",
    )

    LaunchedEffect(isShaking) {
        onUserActiveChange(isShaking)
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastUpdate = 0L
        var lastX = 0f
        var lastY = 0f
        var lastZ = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdate > 80) {
                    val diffTime = currentTime - lastUpdate
                    lastUpdate = currentTime
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val speed = sqrt(
                        ((x - lastX) * (x - lastX) +
                                (y - lastY) * (y - lastY) +
                                (z - lastZ) * (z - lastZ)).toDouble()
                    ).toFloat() / diffTime * 10000

                    if (speed > 1200) {
                        if (currentShakes < targetShakes) {
                            currentShakes++
                            isShaking = true
                        }
                    } else {
                        isShaking = false
                    }
                    lastX = x
                    lastY = y
                    lastZ = z
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener, accelerometer, SensorManager.SENSOR_DELAY_GAME,
        )
        onDispose { sensorManager.unregisterListener(listener) }
    }

    LaunchedEffect(currentShakes) {
        if (currentShakes >= targetShakes) {
            onUserActiveChange(false)
            onTaskCompleted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Shake your phone!",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$currentShakes / $targetShakes shakes",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(if (isShaking) Primary else SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "📳", fontSize = 52.sp)
        }

        Spacer(modifier = Modifier.height(48.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = Primary,
            trackColor = SurfaceHigh,
            strokeCap = StrokeCap.Round,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${(progress * 100).toInt()}% complete",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily,
            color = Primary,
        )
    }
}