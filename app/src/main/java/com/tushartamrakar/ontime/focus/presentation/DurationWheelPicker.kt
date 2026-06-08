package com.tushartamrakar.ontime.focus.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
//  WheelPickerColumn
//  Core drum-roll scroll wheel. Pass any list of display strings.
//
//  Snap logic:
//    paddedItems = ["", ""] + items + ["", ""]   (padCount = visibleCount / 2 = 2)
//    When firstVisibleItemIndex = k, the CENTER item (position 2 of 5) shows
//    paddedItems[k + 2] = items[k] → selectedRealIndex = k
//    graphicsLayer reads firstVisibleItemIndex at DRAW time → no recomposition per frame
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WheelPickerColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemHeight    = 52.dp
    val visibleCount  = 5
    val padCount      = visibleCount / 2      // 2

    val paddedItems = remember(items) {
        List(padCount) { "" } + items + List(padCount) { "" }
    }

    val clampedInit = selectedIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))
    val state        = rememberLazyListState(initialFirstVisibleItemIndex = clampedInit)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = state)

    // Report selection once scroll settles (handles both fling-snap and slow drag)
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) {
            val idx = state.firstVisibleItemIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))
            onSelectedChange(idx)
        }
    }

    Box(
        modifier          = modifier.height(itemHeight * visibleCount),
        contentAlignment  = Alignment.Center,
    ) {
        // Selection highlight strip — sits behind the column at center position
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(Primary.copy(alpha = 0.12f))
                .border(1.dp, Primary.copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
        )

        LazyColumn(
            state         = state,
            flingBehavior = flingBehavior,
            modifier      = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(paddedItems) { paddedIdx, label ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        // ── graphicsLayer runs at DRAW time — reads snapshot state without
                        //    triggering recomposition for every scroll pixel ──
                        .graphicsLayer {
                            val center = state.firstVisibleItemIndex + padCount
                            val dist   = abs(paddedIdx - center).toFloat()
                            val scale  = 1f - (dist * 0.12f).coerceIn(0f, 0.40f)
                            scaleX = scale
                            scaleY = scale
                            alpha  = 1f - (dist * 0.25f).coerceIn(0f, 0.75f)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (label.isNotEmpty()) {
                        // isSelected is stable (only changes when settled) → minimal recompose
                        val isSelected = (paddedIdx == selectedIndex + padCount)
                        Text(
                            text       = label,
                            fontSize   = 22.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            fontFamily = MulishFamily,
                            color      = if (isSelected) TextPrimary else TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DurationWheelPickerSheet
//  ModalBottomSheet containing two WheelPickerColumns:
//    Left  — hours  0 h … 5 h
//    Right — minutes 00 m … 55 m (5-min increments)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationWheelPickerSheet(
    title: String,
    initialMinutes: Int,          // total minutes fed in; e.g. 90 → 1 h 30 m
    maxHours: Int = 5,            // 5 for Pomodoro, 23 for Custom
    onDismiss: () -> Unit,
    onConfirm: (totalMinutes: Int) -> Unit,
) {
    // Decompose into hours + minutes (snapped to nearest 5)
    var selHours   by remember { mutableStateOf((initialMinutes / 60).coerceIn(0, maxHours)) }
    var selMinsIdx by remember {
        val snapped = ((initialMinutes % 60) / 5).coerceIn(0, 11)
        mutableStateOf(snapped)
    }

    val hourItems   = (0..maxHours).map { "${it}h" }
    val minuteItems = (0..11).map { "${(it * 5).toString().padStart(2, '0')}m" }

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
                .padding(horizontal = 28.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text       = title,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = MulishFamily,
                color      = TextPrimary,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text       = "Drag to set duration",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color      = TextMuted,
            )

            Spacer(Modifier.height(20.dp))

            // ── Two wheels side by side ────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                WheelPickerColumn(
                    items            = hourItems,
                    selectedIndex    = selHours,
                    onSelectedChange = { selHours = it },
                    modifier         = Modifier.weight(1f),
                )

                Text(
                    text       = ":",
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily,
                    color      = TextSecondary,
                    modifier   = Modifier.padding(horizontal = 10.dp),
                )

                WheelPickerColumn(
                    items            = minuteItems,
                    selectedIndex    = selMinsIdx,
                    onSelectedChange = { selMinsIdx = it },
                    modifier         = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Confirm ────────────────────────────────────────────────
            val totalMinutes = (selHours * 60 + selMinsIdx * 5).coerceAtLeast(1)
            val displayStr   = buildString {
                if (selHours > 0) append("${selHours}h ")
                append("${(selMinsIdx * 5).toString().padStart(2, '0')}m")
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Primary)
                    .clickable { onConfirm(totalMinutes) }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text          = "Set  $displayStr",
                    fontSize      = 16.sp,
                    fontWeight    = FontWeight.Black,
                    fontFamily    = MulishFamily,
                    color         = Color.White,
                    letterSpacing = 0.5.sp,
                )
            }
        }
    }
}
