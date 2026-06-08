package com.tushartamrakar.ontime.alarm.presentation

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.alarm.domain.TaskType
import com.tushartamrakar.ontime.alarm.domain.WakeUpTask
import com.tushartamrakar.ontime.alarm.domain.availableTasks
import com.tushartamrakar.ontime.alarm.presentation.tasks.BarcodeTaskConfigSheet
import com.tushartamrakar.ontime.alarm.presentation.tasks.MathTaskConfigSheet
import com.tushartamrakar.ontime.alarm.presentation.tasks.ShakeTaskConfigSheet
import com.tushartamrakar.ontime.alarm.presentation.tasks.TypingTaskConfigSheet
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeUpTaskPickerSheet(
    onTaskSelected: (WakeUpTask) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var showMathConfig by remember { mutableStateOf(false) }
    var showTypingConfig by remember { mutableStateOf(false) }
    var showShakeConfig by remember { mutableStateOf(false) }
    var showBarcodeConfig by remember { mutableStateOf(false) }

    // ─── Math Config ──────────────────────────────────────────────────────────
    if (showMathConfig) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Background,
            dragHandle = null,
        ) {
            MathTaskConfigSheet(
                onSave = { task -> onTaskSelected(task) },
                onDismiss = onDismiss,
            )
        }
        return
    }

    // ─── Typing Config ────────────────────────────────────────────────────────
    if (showTypingConfig) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Background,
            dragHandle = null,
        ) {
            TypingTaskConfigSheet(
                onSave = { task -> onTaskSelected(task) },
                onDismiss = onDismiss,
            )
        }
        return
    }

    // ─── Shake Config ─────────────────────────────────────────────────────────
    if (showShakeConfig) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Background,
            dragHandle = null,
        ) {
            ShakeTaskConfigSheet(
                onSave = { task -> onTaskSelected(task) },
                onDismiss = onDismiss,
            )
        }
        return
    }

    // ─── Barcode Config ───────────────────────────────────────────────────────
    if (showBarcodeConfig) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Background,
            dragHandle = null,
        ) {
            BarcodeTaskConfigSheet(
                onSave = { task -> onTaskSelected(task) },
                onDismiss = onDismiss,
            )
        }
        return
    }

    // ─── Main Task Picker ─────────────────────────────────────────────────────
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Background,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Choose Task",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextPrimary,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = TextMuted,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select a challenge to complete before dismissing your alarm",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color = TextMuted,
            )

            Spacer(modifier = Modifier.height(24.dp))

            availableTasks.forEach { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(
                            width = 1.dp,
                            color = Border,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clickable {
                            when (task.type) {
                                TaskType.MATH -> showMathConfig = true
                                TaskType.TYPING -> showTypingConfig = true
                                TaskType.SHAKE -> showShakeConfig = true
                                TaskType.BARCODE -> showBarcodeConfig = true
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = task.emoji,
                            fontSize = 26.sp,
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = task.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = MulishFamily,
                                color = TextPrimary,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = task.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = MulishFamily,
                                    color = Primary,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color = TextMuted,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}