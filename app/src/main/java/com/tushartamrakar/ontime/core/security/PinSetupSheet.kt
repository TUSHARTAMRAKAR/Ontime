package com.tushartamrakar.ontime.core.security

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.Danger
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.Success
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary

private const val PIN_LEN = 4

/**
 * PinSetupSheet
 *
 * Bottom sheet shown when enabling App Lock.
 * Two-step flow: enter new PIN → confirm PIN → save.
 *
 * onSuccess: called after PIN is confirmed and saved.
 * onDismiss: called if user cancels — App Lock should NOT be enabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupSheet(
    appLockManager: AppLockManager,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    var step         by remember { mutableStateOf(Step.ENTER) }
    var firstPin     by remember { mutableStateOf("") }
    var pin          by remember { mutableStateOf("") }
    var errorMsg     by remember { mutableStateOf("") }

    fun appendDigit(d: String) {
        if (pin.length >= PIN_LEN) return
        pin     += d
        errorMsg = ""

        if (pin.length == PIN_LEN) {
            when (step) {
                Step.ENTER   -> {
                    firstPin = pin
                    pin      = ""
                    step     = Step.CONFIRM
                }
                Step.CONFIRM -> {
                    if (pin == firstPin) {
                        appLockManager.setPin(pin)
                        onSuccess()
                    } else {
                        errorMsg = "PINs don't match — try again"
                        pin      = ""
                        step     = Step.ENTER
                        firstPin = ""
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Background,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle       = {},
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Header
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text       = when (step) {
                            Step.ENTER   -> "Create PIN"
                            Step.CONFIRM -> "Confirm PIN"
                        },
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = MulishFamily,
                        color      = TextPrimary,
                    )
                    Text(
                        text       = when (step) {
                            Step.ENTER   -> "Enter a 4-digit PIN to lock Ontime"
                            Step.CONFIRM -> "Re-enter your PIN to confirm"
                        },
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color      = TextMuted,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceHigh)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }

            // Step indicator
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Step.values().forEachIndexed { i, s ->
                    val active  = s.ordinal <= step.ordinal
                    val current = s == step
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    current -> Primary
                                    active  -> Success
                                    else    -> SurfaceHigh
                                }
                            ),
                    )
                }
            }

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(PIN_LEN) { i ->
                    val filled = i < pin.length
                    val color by animateColorAsState(
                        targetValue = if (filled) Primary else SurfaceHigh,
                        label       = "dot_$i",
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, if (!filled) Border else Color.Transparent, CircleShape),
                    )
                }
            }

            // Error
            Text(
                text       = errorMsg,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color      = Danger,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.height(16.dp),
            )

            // Numpad
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    listOf("1","2","3"),
                    listOf("4","5","6"),
                    listOf("7","8","9"),
                ).forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        row.forEach { d ->
                            SheetNumpadKey(d) { appendDigit(d) }
                        }
                    }
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Spacer(Modifier.size(60.dp))
                    SheetNumpadKey("0") { appendDigit("0") }
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh)
                            .clickable { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Backspace, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

private enum class Step { ENTER, CONFIRM }

@Composable
private fun SheetNumpadKey(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Surface)
            .border(1.dp, Border, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = label,
            fontSize   = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color      = TextPrimary,
        )
    }
}
