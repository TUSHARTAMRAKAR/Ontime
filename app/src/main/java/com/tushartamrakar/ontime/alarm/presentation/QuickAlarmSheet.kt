package com.tushartamrakar.ontime.alarm.presentation

import android.media.MediaPlayer
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import java.util.Calendar

// ─── Data ─────────────────────────────────────────────────────────────────────
data class QuickPreset(val minutes: Int, val label: String)

val napPresets = listOf(
    QuickPreset(20, "20 min"),
    QuickPreset(30, "30 min"),
    QuickPreset(45, "45 min"),
)

val focusPresets = listOf(
    QuickPreset(25, "25 min"),
    QuickPreset(50, "50 min"),
)

fun getQuickAlarmLabel(minutes: Int): String = when (minutes) {
    20 -> "Power Nap"
    30 -> "Quick Nap"
    45 -> "Long Nap"
    25 -> "Pomodoro Focus"
    50 -> "Deep Focus"
    else -> when {
        minutes < 20 -> "Quick Break"
        minutes < 45 -> "Focus Session"
        minutes < 90 -> "Deep Work"
        else -> "Long Session"
    }
}

fun getRingAtText(minutes: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MINUTE, minutes)
    val h = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val m = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
    val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "am" else "pm"
    return "Ring at $h:$m $amPm"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAlarmSheet(
    selectedSound: String,
    onSoundClick: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (minutes: Int, label: String, sound: String, volume: Float, vibrate: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ─── State ────────────────────────────────────────────────────────────────
    var selectedMinutes by remember { mutableStateOf(25) }
    var customMinutes by remember { mutableStateOf(25) }
    var selectedCategory by remember { mutableStateOf("focus") } // "nap", "focus", "custom"
    var alarmVolume by remember { mutableStateOf(1.0f) }
    var vibrate by remember { mutableStateOf(true) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // ─── Animated minutes display ─────────────────────────────────────────────
    val animatedMinutes by animateIntAsState(
        targetValue = selectedMinutes,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "minutes",
    )

    // ─── Tone name formatter ──────────────────────────────────────────────────
    val currentToneName = remember(selectedSound) {
        selectedSound.replace("alarm_", "").replace("_", " ")
            .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    // ─── Cleanup ──────────────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.apply { if (isPlaying) stop(); release() }
            previewPlayer = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Background,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {

            // ─── Header ───────────────────────────────────────────────────────
            Text(
                text = "Quick alarm",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Time Display ─────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "$animatedMinutes",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = MulishFamily,
                        color = Primary,
                        letterSpacing = (-2).sp,
                        lineHeight = 72.sp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "min",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                Text(
                    text = getRingAtText(selectedMinutes),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = MulishFamily,
                    color = TextMuted,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getQuickAlarmLabel(selectedMinutes),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily,
                    color = Primary.copy(alpha = 0.8f),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ─── Category: Nap ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "🛌  Nap",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextMuted,
                    modifier = Modifier.width(72.dp),
                )
                napPresets.forEach { preset ->
                    val isSelected = selectedCategory == "nap" && selectedMinutes == preset.minutes
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Primary else SurfaceHigh)
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) Primary else Border,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable {
                                selectedCategory = "nap"
                                selectedMinutes = preset.minutes
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = preset.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = if (isSelected) Color.White else TextMuted,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Category: Focus ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "🎯  Focus",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextMuted,
                    modifier = Modifier.width(72.dp),
                )
                focusPresets.forEach { preset ->
                    val isSelected = selectedCategory == "focus" && selectedMinutes == preset.minutes
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Primary else SurfaceHigh)
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) Primary else Border,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable {
                                selectedCategory = "focus"
                                selectedMinutes = preset.minutes
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = preset.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = if (isSelected) Color.White else TextMuted,
                        )
                    }
                }
                // Empty spacer to fill remaining space
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Category: Custom ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "⏱  Custom",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextMuted,
                    modifier = Modifier.width(72.dp),
                )
                // − button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (selectedCategory == "custom") Primary.copy(alpha = 0.15f) else SurfaceHigh)
                        .border(1.5.dp, if (selectedCategory == "custom") Primary else Border, CircleShape)
                        .clickable {
                            selectedCategory = "custom"
                            customMinutes = maxOf(1, customMinutes - 5)
                            selectedMinutes = customMinutes
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "−",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedCategory == "custom") Primary else TextMuted,
                        fontFamily = MulishFamily,
                    )
                }

                // Custom minutes display
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedCategory == "custom") Primary else SurfaceHigh)
                        .border(
                            width = 1.5.dp,
                            color = if (selectedCategory == "custom") Primary else Border,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable {
                            selectedCategory = "custom"
                            selectedMinutes = customMinutes
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${customMinutes}m",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color = if (selectedCategory == "custom") Color.White else TextMuted,
                    )
                }

                // + button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (selectedCategory == "custom") Primary.copy(alpha = 0.15f) else SurfaceHigh)
                        .border(1.5.dp, if (selectedCategory == "custom") Primary else Border, CircleShape)
                        .clickable {
                            selectedCategory = "custom"
                            customMinutes = minOf(240, customMinutes + 5)
                            selectedMinutes = customMinutes
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedCategory == "custom") Primary else TextMuted,
                        fontFamily = MulishFamily,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ─── Sound + Volume + Vibrate ─────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBackground)
                    .border(width = 1.dp, color = Border, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                // Sound row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isPreviewPlaying) Primary else SurfaceHigh)
                            .clickable {
                                if (isPreviewPlaying) {
                                    previewPlayer?.apply { if (isPlaying) stop(); release() }
                                    previewPlayer = null; isPreviewPlaying = false
                                } else {
                                    val resId = context.resources.getIdentifier(selectedSound, "raw", context.packageName)
                                    if (resId != 0) {
                                        previewPlayer = MediaPlayer.create(context, resId)?.apply {
                                            setOnCompletionListener { isPreviewPlaying = false; release(); previewPlayer = null }
                                            start()
                                        }
                                        isPreviewPlaying = true
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isPreviewPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Preview",
                            tint = if (isPreviewPlaying) Color.White else TextMuted,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Text(
                        text = currentToneName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh)
                            .clickable { onSoundClick() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowForwardIos,
                            contentDescription = "Choose Sound",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Volume
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "🔈", fontSize = 18.sp)
                    Slider(
                        value = alarmVolume,
                        onValueChange = { alarmVolume = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Primary,
                            activeTrackColor = Primary,
                            inactiveTrackColor = SurfaceHigh,
                        ),
                    )
                    Text(text = "🔊", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Vibrate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Vibrate",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MulishFamily,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Phone vibrates when alarm fires",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color = TextMuted,
                        )
                    }
                    Switch(
                        checked = vibrate,
                        onCheckedChange = { vibrate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = Primary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceHigh,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Save Button ──────────────────────────────────────────────────
            Button(
                onClick = {
                    onSave(selectedMinutes, getQuickAlarmLabel(selectedMinutes), selectedSound, alarmVolume, vibrate)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    text = "Save Quick Alarm",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = Color.White,
                    letterSpacing = 0.3.sp,
                )
            }
        }
    }
}
