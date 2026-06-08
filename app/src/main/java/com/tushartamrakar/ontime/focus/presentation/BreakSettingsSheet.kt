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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreakSettingsSheet(
    /** Current sessions before long break (1–8) — maps to sessionsBeforeLongBreak. */
    numBreaks: Int,
    /** Current short break duration in minutes (1–30). */
    breakDurationMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (numBreaks: Int, durationMinutes: Int) -> Unit,
) {
    var breaks   by remember { mutableStateOf(numBreaks.coerceIn(1, 8)) }
    var duration by remember { mutableStateOf(breakDurationMinutes.coerceIn(1, 30)) }

    // 1 m … 30 m
    val durationItems = (1..30).map { "${it}m" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Surface,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Border),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text       = "Break Settings",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = MulishFamily,
                color      = TextPrimary,
            )

            Spacer(Modifier.height(24.dp))

            // ── Sessions before long break (stepper) ──────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = "Sessions before long break",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color      = TextPrimary,
                    )
                    Text(
                        text       = "Short breaks in between",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color      = TextMuted,
                    )
                }

                Spacer(Modifier.width(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BreakStepperBtn(
                        icon    = Icons.Filled.Remove,
                        enabled = breaks > 1,
                        onClick = { breaks = (breaks - 1).coerceAtLeast(1) },
                    )
                    Text(
                        text       = breaks.toString(),
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = MulishFamily,
                        color      = TextPrimary,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier
                            .width(44.dp)
                            .padding(horizontal = 4.dp),
                    )
                    BreakStepperBtn(
                        icon    = Icons.Filled.Add,
                        enabled = breaks < 8,
                        onClick = { breaks = (breaks + 1).coerceAtMost(8) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Divider ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Border),
            )

            Spacer(Modifier.height(24.dp))

            // ── Short break duration wheel ────────────────────────────
            Text(
                text       = "Short break duration",
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color      = TextPrimary,
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                WheelPickerColumn(
                    items            = durationItems,
                    selectedIndex    = (duration - 1).coerceIn(0, 29),
                    onSelectedChange = { duration = it + 1 },
                    modifier         = Modifier.width(120.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Save ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Primary)
                    .clickable { onConfirm(breaks, duration) }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "Save",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily,
                    color      = Color.White,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BreakStepperBtn(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(SurfaceHigh)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = if (enabled) TextPrimary else TextMuted,
            modifier           = Modifier.size(18.dp),
        )
    }
}
