package com.tushartamrakar.ontime.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val CIRCULAR_COUNT = 10_000

// ─── Controller ───────────────────────────────────────────────────────────────

/**
 * WheelPickerController
 *
 * Allows a parent composable to programmatically scroll any WheelPicker.
 * Primary use case: clock carry-over — when minutes roll 59→00, the parent
 * calls hourController.scrollTo(nextHourIndex) to tick the hour forward.
 *
 * Usage:
 *   val hourController = rememberWheelPickerController()
 *   WheelPicker(controller = hourController, ...)
 *   // Later:
 *   hourController.scrollTo(5)  // scroll to index 5 (= "6" in hours list)
 */
class WheelPickerController {
    private var scrollFn: ((Int) -> Unit)? = null

    internal fun attach(fn: (Int) -> Unit) {
        scrollFn = fn
    }

    /**
     * Programmatically scroll to the item at [realIndex].
     * Animates smoothly, taking the shortest circular path.
     * Safe to call from any context — does NOT need to be in a coroutine.
     */
    fun scrollTo(realIndex: Int) {
        scrollFn?.invoke(realIndex)
    }
}

@Composable
fun rememberWheelPickerController(): WheelPickerController = remember { WheelPickerController() }

// ─── WheelPicker ─────────────────────────────────────────────────────────────

@Composable
fun WheelPicker(
    items:            List<String>,
    initialIndex:     Int                    = 0,
    itemHeight:       Dp                     = 56.dp,
    visibleItemCount: Int                    = 3,
    circular:         Boolean                = true,
    controller:       WheelPickerController? = null,   // optional external scroll control
    onItemSelected:   (index: Int) -> Unit,
) {
    val itemCount = items.size
    if (itemCount == 0) return

    val halfVisible = visibleItemCount / 2
    val totalItems  = if (circular) CIRCULAR_COUNT else itemCount

    val startIndex = if (circular) {
        (CIRCULAR_COUNT / 2) - (CIRCULAR_COUNT / 2 % itemCount) + initialIndex
    } else {
        initialIndex
    }

    val listState    = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val snapBehavior = rememberSnapFlingBehavior(listState)
    val scope        = rememberCoroutineScope()

    // ── Wire controller to this picker's listState ────────────────────────────
    LaunchedEffect(controller, listState) {
        controller?.attach { targetRealIndex ->
            scope.launch {
                val currentVirtual = listState.firstVisibleItemIndex
                val currentReal    = if (circular) currentVirtual % itemCount
                                     else currentVirtual

                // Take the SHORTEST circular path (e.g. 11→0 = +1 not -11)
                val rawDiff = (targetRealIndex - currentReal + itemCount) % itemCount
                val diff    = if (rawDiff > itemCount / 2) rawDiff - itemCount else rawDiff
                val targetVirtual = (currentVirtual + diff).coerceIn(0, CIRCULAR_COUNT - 1)

                listState.animateScrollToItem(targetVirtual)
            }
        }
    }

    val selectedVirtualIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    // Emit only after snap completes — not on every drag frame
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .map {
                val vi = listState.firstVisibleItemIndex
                if (circular) vi % itemCount else vi.coerceIn(0, itemCount - 1)
            }
            .distinctUntilChanged()
            .collect { onItemSelected(it) }
    }

    LaunchedEffect(Unit) { onItemSelected(initialIndex) }

    val totalHeight = itemHeight * visibleItemCount

    Box(
        modifier         = Modifier.fillMaxWidth().height(totalHeight),
        contentAlignment = Alignment.Center,
    ) {

        // ── iOS-style selection indicator — two thin lines ────────────────────
        Column(modifier = Modifier.fillMaxWidth().height(totalHeight)) {
            Spacer(modifier = Modifier.height(itemHeight * halfVisible))
            Box(
                modifier = Modifier
                    .fillMaxWidth().padding(horizontal = 16.dp)
                    .height(1.dp).background(Primary.copy(alpha = 0.45f))
            )
            Spacer(modifier = Modifier.height(itemHeight - 2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth().padding(horizontal = 16.dp)
                    .height(1.dp).background(Primary.copy(alpha = 0.45f))
            )
        }

        // ── Drum-roll list ────────────────────────────────────────────────────
        LazyColumn(
            state               = listState,
            flingBehavior       = snapBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.fillMaxWidth().height(totalHeight),
        ) {
            repeat(halfVisible) { i ->
                item(key = "top_$i") { Box(Modifier.fillMaxWidth().height(itemHeight)) }
            }

            items(count = totalItems, key = { "item_$it" }) { virtualIndex ->
                val realIndex = if (circular) virtualIndex % itemCount else virtualIndex

                val distanceFromCenter by remember {
                    derivedStateOf { kotlin.math.abs(virtualIndex - selectedVirtualIndex) }
                }
                val isSelected by remember {
                    derivedStateOf { distanceFromCenter == 0 }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .graphicsLayer {
                            val d = distanceFromCenter.coerceAtMost(2)
                            alpha = when (d) { 0 -> 1.00f; 1 -> 0.45f; else -> 0.15f }
                            val s = when (d) { 0 -> 1.00f; 1 -> 0.80f; else -> 0.62f }
                            scaleX = s; scaleY = s
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text          = items[realIndex],
                        fontSize      = 34.sp,
                        fontWeight    = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                        fontFamily    = MulishFamily,
                        color         = if (isSelected) Primary else TextPrimary,
                        textAlign     = TextAlign.Center,
                        maxLines      = 1,
                        letterSpacing = (-1).sp,
                    )
                }
            }

            repeat(halfVisible) { i ->
                item(key = "bot_$i") { Box(Modifier.fillMaxWidth().height(itemHeight)) }
            }
        }
    }
}
