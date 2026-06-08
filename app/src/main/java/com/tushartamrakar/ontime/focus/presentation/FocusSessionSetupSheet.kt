package com.tushartamrakar.ontime.focus.presentation

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.BorderLight
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary
import com.tushartamrakar.ontime.focus.data.local.AmbientSound

/**
 * Bottom sheet shown when the user taps "Start Focus Now".
 *
 * Lets the user:
 *   1. Enter a task label (what they're focusing on)
 *   2. Choose a timer preset (25/5, 45/10, 50/10, or their custom setting)
 *   3. Pick an ambient sound
 *
 * onBegin fires with the final label + sound — FocusScreen starts the service.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSessionSetupSheet(
    viewModel: FocusViewModel,
    initialTaskLabel: String = "",
    onDismiss: () -> Unit,
    onBegin: (label: String, sound: AmbientSound) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    var taskLabel by remember { mutableStateOf(initialTaskLabel) }
    var selectedPreset by remember { mutableStateOf(TimerPreset.CLASSIC) }
    var selectedSound by remember { mutableStateOf(AmbientSound.SILENCE) }

    ModalBottomSheet(
        onDismissRequest   = onDismiss,
        sheetState         = sheetState,
        containerColor     = Surface,
        dragHandle         = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(BorderLight)
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text       = "New Focus Session",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily,
                    color      = TextPrimary,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector        = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint               = TextMuted,
                        modifier           = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Task label ────────────────────────────────────────────────────
            SectionLabel(icon = "📝", text = "What are you focusing on?")
            Spacer(Modifier.height(8.dp))

            BasicTextField(
                value         = taskLabel,
                onValueChange = { taskLabel = it },
                textStyle     = TextStyle(
                    fontFamily  = MulishFamily,
                    fontWeight  = FontWeight.Medium,
                    fontSize    = 15.sp,
                    color       = TextPrimary,
                ),
                cursorBrush   = SolidColor(Primary),
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceHigh)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                decorationBox = { inner ->
                    if (taskLabel.isEmpty()) {
                        Text(
                            text       = "e.g. Study for exam, Code feature, Read chapter…",
                            fontFamily = MulishFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize   = 15.sp,
                            color      = TextMuted,
                        )
                    }
                    inner()
                },
            )

            Spacer(Modifier.height(20.dp))

            // ── Timer preset ──────────────────────────────────────────────────
            SectionLabel(icon = "⏱", text = "Session length")
            Spacer(Modifier.height(8.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TimerPreset.values().forEach { preset ->
                    PresetChip(
                        preset     = preset,
                        isSelected = preset == selectedPreset,
                        onClick    = { selectedPreset = preset },
                        modifier   = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Sound picker ──────────────────────────────────────────────────
            SectionLabel(icon = "🎵", text = "Ambient sound")
            Spacer(Modifier.height(8.dp))

            // Two rows of 4
            val sounds = AmbientSound.values().toList()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    sounds.take(4).forEach { sound ->
                        SoundChipSheet(
                            sound      = sound,
                            isSelected = sound == selectedSound,
                            onClick    = { selectedSound = it },
                            modifier   = Modifier.weight(1f),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    sounds.drop(4).forEach { sound ->
                        SoundChipSheet(
                            sound      = sound,
                            isSelected = sound == selectedSound,
                            onClick    = { selectedSound = it },
                            modifier   = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Begin button ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Primary)
                    .clickable { onBegin(taskLabel.trim(), selectedSound) }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(22.dp),
                    )
                    Text(
                        text       = "Begin Session",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = MulishFamily,
                        color      = Color.White,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
    }
}

// ─── Timer Preset ─────────────────────────────────────────────────────────────

enum class TimerPreset(val label: String, val subLabel: String, val workMinutes: Int, val breakMinutes: Int) {
    CLASSIC ("25 / 5",  "Classic",  25, 5),
    DEEP    ("45 / 10", "Deep",     45, 10),
    ULTRA   ("50 / 10", "Ultra",    50, 10),
    CUSTOM  ("Custom",  "Your settings", 0, 0),
}

@Composable
private fun PresetChip(
    preset: TimerPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Primary.copy(alpha = 0.15f) else SurfaceHigh)
            .border(
                1.dp,
                if (isSelected) Primary.copy(alpha = 0.6f) else Color.Transparent,
                RoundedCornerShape(12.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text       = preset.label,
            fontSize   = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color      = if (isSelected) Primary else TextPrimary,
        )
        Text(
            text       = preset.subLabel,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color      = if (isSelected) Primary.copy(alpha = 0.7f) else TextMuted,
        )
    }
}

// ─── Sound Chip ───────────────────────────────────────────────────────────────

@Composable
private fun SoundChipSheet(
    sound: AmbientSound,
    isSelected: Boolean,
    onClick: (AmbientSound) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (emoji, label) = when (sound) {
        AmbientSound.RAIN        -> "🌧" to "Rain"
        AmbientSound.WHITE_NOISE -> "🌊" to "White"
        AmbientSound.BROWN_NOISE -> "🟤" to "Brown"
        AmbientSound.FOREST      -> "🌲" to "Forest"
        AmbientSound.OCEAN       -> "🌊" to "Ocean"
        AmbientSound.CAFE        -> "☕" to "Café"
        AmbientSound.LOFI        -> "🎵" to "Lofi"
        AmbientSound.SILENCE     -> "🔇" to "None"
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Primary.copy(alpha = 0.15f) else SurfaceHigh)
            .border(
                1.dp,
                if (isSelected) Primary.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .clickable { onClick(sound) }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = emoji, fontSize = 16.sp)
        Text(
            text       = label,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily,
            color      = if (isSelected) Primary else TextSecondary,
        )
    }
}

// ─── Section Label ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(icon: String, text: String) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = icon, fontSize = 14.sp)
        Text(
            text       = text,
            fontSize   = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color      = TextSecondary,
        )
    }
}
