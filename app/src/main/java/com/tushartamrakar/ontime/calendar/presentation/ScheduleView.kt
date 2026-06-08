package com.tushartamrakar.ontime.calendar.presentation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.calendar.data.local.CalendarEventEntity
import com.tushartamrakar.ontime.calendar.data.local.EventCategoryEntity
import com.tushartamrakar.ontime.calendar.data.local.LiveHoliday
import com.tushartamrakar.ontime.calendar.domain.toReminderItems
import com.tushartamrakar.ontime.calendar.domain.toReminderLabel
import com.tushartamrakar.ontime.core.ui.theme.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.runtime.snapshotFlow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

// ─── Schedule Item types ──────────────────────────────────────────────────────
sealed class ScheduleItem {
    data class MonthHeader(val yearMonth: YearMonth) : ScheduleItem()
    // key is always unique — includes index prefix
    data class WeekLabel(val key: String, val label: String) : ScheduleItem()
    data class DayRow(
        val date: LocalDate,
        val events: List<CalendarEventEntity>,
        val holidays: List<LiveHoliday>,
        val isToday: Boolean,
    ) : ScheduleItem()
}

// ─── Month theme data ─────────────────────────────────────────────────────────
data class MonthTheme(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val drawShapes: DrawScope.() -> Unit,
)

fun monthTheme(month: Int): MonthTheme = when (month) {
    1 -> MonthTheme(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF90CAF9)) {
        // January - deep blue, snowflakes as circles
        drawCircle(Color(0xFF90CAF9).copy(alpha = 0.15f), 120f, Offset(size.width * 0.85f, size.height * 0.2f))
        drawCircle(Color(0xFF42A5F5).copy(alpha = 0.2f), 80f, Offset(size.width * 0.1f, size.height * 0.7f))
        drawCircle(Color(0xFFBBDEFB).copy(alpha = 0.1f), 200f, Offset(size.width * 0.5f, size.height * 1.2f))
        repeat(6) { i ->
            drawCircle(Color.White.copy(alpha = 0.12f), 8f + i * 3f,
                Offset(size.width * (0.15f + i * 0.14f), size.height * (0.3f + (i % 2) * 0.4f)))
        }
    }
    2 -> MonthTheme(Color(0xFF880E4F), Color(0xFFC2185B), Color(0xFFF48FB1)) {
        // February - rose, hearts as ovals
        drawOval(Color(0xFFF48FB1).copy(alpha = 0.2f), Offset(size.width * 0.7f, size.height * 0.1f), Size(160f, 130f))
        drawOval(Color(0xFFE91E63).copy(alpha = 0.15f), Offset(size.width * 0.05f, size.height * 0.5f), Size(100f, 80f))
        repeat(5) { i ->
            drawCircle(Color(0xFFF06292).copy(alpha = 0.15f), 15f + i * 8f,
                Offset(size.width * (0.2f + i * 0.15f), size.height * (0.6f + (i % 3) * 0.15f)))
        }
    }
    3 -> MonthTheme(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFFA5D6A7)) {
        // March - spring green, leaf shapes
        val path = Path().apply {
            moveTo(size.width * 0.7f, 0f)
            cubicTo(size.width, 0f, size.width, size.height, size.width * 0.6f, size.height)
            cubicTo(size.width * 0.3f, size.height, size.width * 0.4f, 0f, size.width * 0.7f, 0f)
        }
        drawPath(path, Color(0xFF66BB6A).copy(alpha = 0.2f))
        drawCircle(Color(0xFF43A047).copy(alpha = 0.15f), 90f, Offset(size.width * 0.15f, size.height * 0.3f))
    }
    4 -> MonthTheme(Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFF90CAF9)) {
        // April - sky blue, rain drops
        repeat(8) { i ->
            val x = size.width * (0.1f + i * 0.11f)
            val y = size.height * (0.1f + (i % 3) * 0.3f)
            drawOval(Color(0xFF64B5F6).copy(alpha = 0.2f), Offset(x, y), Size(12f, 20f))
        }
        drawCircle(Color(0xFF42A5F5).copy(alpha = 0.15f), 100f, Offset(size.width * 0.8f, size.height * 0.5f))
    }
    5 -> MonthTheme(Color(0xFF4A148C), Color(0xFF6A1B9A), Color(0xFFCE93D8)) {
        // May - lavender, flower petals
        val cx = size.width * 0.8f; val cy = size.height * 0.3f
        repeat(6) { i ->
            val angle = i * 60f * (Math.PI / 180f).toFloat()
            drawCircle(Color(0xFFAB47BC).copy(alpha = 0.2f), 35f,
                Offset(cx + 55f * kotlin.math.cos(angle), cy + 55f * kotlin.math.sin(angle)))
        }
        drawCircle(Color(0xFFCE93D8).copy(alpha = 0.25f), 25f, Offset(cx, cy))
        drawCircle(Color(0xFF7B1FA2).copy(alpha = 0.1f), 140f, Offset(size.width * 0.1f, size.height * 0.8f))
    }
    6 -> MonthTheme(Color(0xFFE65100), Color(0xFFF57C00), Color(0xFFFFCC80)) {
        // June - warm orange, sun
        drawCircle(Color(0xFFFFB300).copy(alpha = 0.2f), 80f, Offset(size.width * 0.8f, size.height * 0.3f))
        repeat(8) { i ->
            val angle = i * 45f * (Math.PI / 180f).toFloat()
            drawLine(Color(0xFFFFCA28).copy(alpha = 0.25f), strokeWidth = 4f,
                start = Offset(size.width * 0.8f + 85f * kotlin.math.cos(angle),
                    size.height * 0.3f + 85f * kotlin.math.sin(angle)),
                end = Offset(size.width * 0.8f + 115f * kotlin.math.cos(angle),
                    size.height * 0.3f + 115f * kotlin.math.sin(angle)))
        }
        drawCircle(Color(0xFFFF8F00).copy(alpha = 0.1f), 180f, Offset(size.width * 0.2f, size.height))
    }
    7 -> MonthTheme(Color(0xFFB71C1C), Color(0xFFC62828), Color(0xFFEF9A9A)) {
        // July - deep red, burst shapes
        repeat(5) { i ->
            val angle = i * 72f * (Math.PI / 180f).toFloat()
            val cx = size.width * 0.75f; val cy = size.height * 0.35f
            drawLine(Color(0xFFEF5350).copy(alpha = 0.3f), strokeWidth = 3f,
                start = Offset(cx, cy),
                end = Offset(cx + 80f * kotlin.math.cos(angle), cy + 80f * kotlin.math.sin(angle)))
        }
        drawCircle(Color(0xFFE53935).copy(alpha = 0.2f), 50f, Offset(size.width * 0.75f, size.height * 0.35f))
        drawCircle(Color(0xFFFF8A80).copy(alpha = 0.12f), 120f, Offset(size.width * 0.1f, size.height * 0.7f))
    }
    8 -> MonthTheme(Color(0xFF33691E), Color(0xFF558B2F), Color(0xFFDCEDC8)) {
        // August - lush green
        val path = Path().apply {
            moveTo(0f, size.height * 0.6f)
            cubicTo(size.width * 0.3f, size.height * 0.3f, size.width * 0.6f, size.height * 0.8f, size.width, size.height * 0.4f)
            lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(path, Color(0xFF8BC34A).copy(alpha = 0.2f))
        drawCircle(Color(0xFF558B2F).copy(alpha = 0.15f), 90f, Offset(size.width * 0.8f, size.height * 0.25f))
    }
    9 -> MonthTheme(Color(0xFFBF360C), Color(0xFFD84315), Color(0xFFFFCCBC)) {
        // September - autumn orange
        repeat(6) { i ->
            val path = Path().apply {
                val x = size.width * (0.1f + i * 0.15f); val y = size.height * (0.2f + (i % 2) * 0.5f)
                moveTo(x, y - 25f)
                cubicTo(x + 20f, y - 35f, x + 35f, y - 15f, x + 20f, y + 10f)
                cubicTo(x + 5f, y + 25f, x - 15f, y + 15f, x - 10f, y - 5f)
                close()
            }
            drawPath(path, Color(0xFFFF7043).copy(alpha = 0.2f + i * 0.03f))
        }
    }
    10 -> MonthTheme(Color(0xFF212121), Color(0xFF424242), Color(0xFFFF9800)) {
        // October - dark with warm accents
        drawCircle(Color(0xFFFF9800).copy(alpha = 0.2f), 100f, Offset(size.width * 0.8f, size.height * 0.3f))
        repeat(8) { i ->
            drawCircle(Color(0xFFFFB74D).copy(alpha = 0.08f + i * 0.01f), 8f + i * 5f,
                Offset(size.width * (0.05f + i * 0.12f), size.height * (0.5f + (i % 3) * 0.15f)))
        }
        val path = Path().apply {
            moveTo(size.width * 0.1f, size.height)
            cubicTo(size.width * 0.3f, size.height * 0.4f, size.width * 0.5f, size.height * 0.7f, size.width * 0.7f, size.height * 0.3f)
            cubicTo(size.width * 0.85f, 0f, size.width, size.height * 0.4f, size.width, 0f)
            lineTo(size.width, size.height); close()
        }
        drawPath(path, Color(0xFF616161).copy(alpha = 0.15f))
    }
    11 -> MonthTheme(Color(0xFF3E2723), Color(0xFF4E342E), Color(0xFFBCAAA4)) {
        // November - warm brown
        repeat(10) { i ->
            drawCircle(Color(0xFF8D6E63).copy(alpha = 0.12f), 20f + i * 8f,
                Offset(size.width * (0.05f + i * 0.1f), size.height * (0.3f + (i % 4) * 0.2f)))
        }
    }
    else -> MonthTheme(Color(0xFF0D1B3E), Color(0xFF1A2A4A), Color(0xFF90CAF9)) {
        // December - dark blue with stars
        repeat(15) { i ->
            drawCircle(Color.White.copy(alpha = 0.08f + (i % 3) * 0.05f),
                2f + (i % 4) * 2f,
                Offset(size.width * (0.05f + i * 0.065f), size.height * (0.1f + (i % 5) * 0.18f)))
        }
        drawCircle(Color(0xFF42A5F5).copy(alpha = 0.15f), 110f, Offset(size.width * 0.8f, size.height * 0.4f))
    }
}

// ─── Main ScheduleView ────────────────────────────────────────────────────────
@Composable
fun ScheduleView(
    events: List<CalendarEventEntity>,
    holidays: List<LiveHoliday>,
    categories: List<EventCategoryEntity>,
    selectedDate: LocalDate,
    bottomPadding: Dp = 0.dp,
    listState: LazyListState = rememberLazyListState(),
    onEventClick: (CalendarEventEntity) -> Unit,
    onMonthVisible: (YearMonth) -> Unit = {},
    onTodayIndexFound: (Int) -> Unit = {},
    onLoadMore: () -> Unit = {},
    totalMonths: Int = 36,
) {
    val today = LocalDate.now()
    val weekFields = WeekFields.of(Locale.getDefault())

    // Build schedule items — rebuilds only when inputs change
    val scheduleItems = remember(events, holidays, selectedDate, totalMonths) {
        buildScheduleItems(events, holidays, selectedDate, today, weekFields, totalMonths)
    }

    // ── Loading state for the bottom indicator ────────────────────────────────
    var isLoadingMore by remember { mutableStateOf(false) }

    // ── FIX: Auto-scroll to today only ONCE — not every time list grows ───────
    // IMPORTANT: onTodayIndexFound is called EVERY time scheduleItems changes
    // so the Today button index stays accurate as holidays load and the list grows.
    // Only the automatic scroll itself is guarded by hasAutoScrolled.
    // Without this split, Today button locks to an early stale index (Jan/Feb area)
    // computed before holidays loaded, and never updates.
    var hasAutoScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(scheduleItems) {
        val todayIndex = scheduleItems.indexOfFirst {
            it is ScheduleItem.DayRow && it.isToday
        }
        val scrollTo = (todayIndex - 1).coerceAtLeast(0)
        onTodayIndexFound(scrollTo) // always update — Today button stays correct
        if (!hasAutoScrolled && todayIndex > 0) {
            listState.scrollToItem(scrollTo) // only auto-scroll once on first open
            hasAutoScrolled = true
        }
    }

    // ── When list grows (new months loaded) → hide loading indicator ──────────
    LaunchedEffect(scheduleItems.size) {
        isLoadingMore = false
    }

    // ── FIX: visibleMonth now has scheduleItems as key ────────────────────────
    // Without this key, derivedStateOf captures a stale list reference and
    // returns null for any index beyond the original list size → shows YearMonth.now()
    val visibleMonth = remember(scheduleItems) {
        derivedStateOf {
            val firstVisible = listState.firstVisibleItemIndex
            val item = scheduleItems.getOrNull(firstVisible)
            when (item) {
                is ScheduleItem.MonthHeader -> item.yearMonth
                is ScheduleItem.DayRow -> YearMonth.of(item.date.year, item.date.month)
                is ScheduleItem.WeekLabel -> {
                    var idx = firstVisible - 1
                    while (idx >= 0) {
                        val prev = scheduleItems.getOrNull(idx)
                        if (prev is ScheduleItem.MonthHeader) return@derivedStateOf prev.yearMonth
                        if (prev is ScheduleItem.DayRow) return@derivedStateOf YearMonth.of(prev.date.year, prev.date.month)
                        idx--
                    }
                    YearMonth.now()
                }
                else -> YearMonth.now()
            }
        }
    }

    // ── Infinite scroll: snapshotFlow approach — never misses a trigger ─────────
    // WHY NOT LaunchedEffect(shouldLoadMore):
    //   LaunchedEffect only fires when key changes (false→true).
    //   If shouldLoadMore stays TRUE after list grows, it never re-fires.
    //   User was stuck: had to scroll up then down to re-trigger. ❌
    //
    // WHY snapshotFlow inside LaunchedEffect(listState, scheduleItems.size):
    //   LaunchedEffect RESTARTS every time scheduleItems.size changes.
    //   After each load, effect restarts → immediately re-checks if still
    //   near bottom → triggers next load automatically if needed. ✅
    //   User never has to scroll up to unblock.
    LaunchedEffect(listState, scheduleItems.size) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 40
        }
        .distinctUntilChanged()
        .filter { it }
        .collect {
            isLoadingMore = true
            onLoadMore()
        }
    }

    LaunchedEffect(visibleMonth.value) {
        onMonthVisible(visibleMonth.value)
    }

    // ── Compute which year is being loaded for the indicator text ─────────────
    val nextLoadingYear = remember(scheduleItems) {
        scheduleItems.filterIsInstance<ScheduleItem.MonthHeader>()
            .lastOrNull()?.yearMonth?.year ?: LocalDate.now().year
    }

    if (scheduleItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🗓️", fontSize = 48.sp)
                Text("No upcoming events", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily, color = TextMuted)
                Text("Tap + to create an event", fontSize = 13.sp, fontFamily = MulishFamily, color = TextMuted)
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + 100.dp),
    ) {
        items(scheduleItems, key = { item ->
            when (item) {
                is ScheduleItem.MonthHeader -> "month_${item.yearMonth}"
                is ScheduleItem.WeekLabel -> item.key
                is ScheduleItem.DayRow -> "day_${item.date}"
            }
        }) { item ->
            when (item) {
                is ScheduleItem.MonthHeader -> MonthHeaderBanner(item.yearMonth)
                is ScheduleItem.WeekLabel -> WeekLabelRow(item.label)
                is ScheduleItem.DayRow -> DayRowItem(
                    date = item.date,
                    events = item.events,
                    holidays = item.holidays,
                    isToday = item.isToday,
                    categories = categories,
                    onEventClick = onEventClick,
                )
            }
        }

        // ── Loading indicator — 3 bouncing dots at bottom of list ────────────
        // Shows while new months are being fetched from the API
        // Disappears automatically when new items are appended
        if (isLoadingMore) {
            item(key = "loading_footer") {
                LoadingMoreIndicator(loadingYear = nextLoadingYear)
            }
        }
    }
}

// ─── Build schedule item list ─────────────────────────────────────────────────
private fun buildScheduleItems(
    events: List<CalendarEventEntity>,
    holidays: List<LiveHoliday>,
    selectedDate: LocalDate,
    today: LocalDate,
    weekFields: WeekFields,
    totalMonths: Int = 36,
): List<ScheduleItem> {
    val items = mutableListOf<ScheduleItem>()
    val startDate = LocalDate.of(today.year, 1, 1) // Always start from Jan 1 of current year
    val endDate = startDate.plusMonths(totalMonths.toLong()) // Dynamic — grows as user scrolls

    var currentMonth: YearMonth? = null
    var currentWeekKey: String? = null
    var itemIndex = 0

    var date = startDate
    while (date <= endDate) {
        val ym = YearMonth.of(date.year, date.month)
        val dateHolidays = holidays.filter { it.date == date.toString() }
            .sortedByDescending { if (it.localName != it.name) 1 else 0 }
            .distinctBy { it.name.lowercase().trim() }
        val dateEvents = events.filter { event ->
            Instant.ofEpochMilli(event.startTimeMillis)
                .atZone(ZoneId.systemDefault()).toLocalDate() == date
        }
        val hasContent = dateHolidays.isNotEmpty() || dateEvents.isNotEmpty() || date == today

        // ✅ Always add month header when month changes
        if (ym != currentMonth) {
            items.add(ScheduleItem.MonthHeader(ym))
            currentMonth = ym
            currentWeekKey = null
        }

        if (hasContent) {
            val weekNum = date.get(weekFields.weekOfWeekBasedYear())
            val weekYear = date.get(weekFields.weekBasedYear())
            val weekUniqueKey = "${weekYear}_${weekNum}"

            if (weekUniqueKey != currentWeekKey) {
                val weekStart = date.with(weekFields.dayOfWeek(), 1)
                val weekEnd = weekStart.plusDays(6)
                val fmt = DateTimeFormatter.ofPattern("d MMM")
                val label = "Week $weekNum,  ${weekStart.format(fmt)} – ${weekEnd.format(fmt)}"
                items.add(ScheduleItem.WeekLabel(
                    key = "week_${ym.year}_${weekNum}_${itemIndex}",
                    label = label,
                ))
                currentWeekKey = weekUniqueKey
                itemIndex++
            }

            items.add(ScheduleItem.DayRow(
                date = date,
                events = dateEvents,
                holidays = dateHolidays,
                isToday = date == today,
            ))
        }
        date = date.plusDays(1)
    }
    return items
}

// ─── Month Banner ─────────────────────────────────────────────────────────────
@Composable
fun MonthHeaderBanner(yearMonth: YearMonth) {
    val theme = monthTheme(yearMonth.monthValue)
    val monthName = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    Box(
        modifier = Modifier.fillMaxWidth().height(130.dp),
    ) {
        // Gradient background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = Brush.linearGradient(
                colors = listOf(theme.primary, theme.secondary),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height),
            ))
            // Draw month-specific shapes
            theme.drawShapes(this)
        }

        // Month name overlay
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 16.dp),
        ) {
            Text(
                text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM")),
                fontSize = 28.sp, fontWeight = FontWeight.Black,
                fontFamily = MulishFamily, color = Color.White,
                letterSpacing = (-0.5).sp,
            )
            Text(
                text = yearMonth.year.toString(),
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily, color = Color.White.copy(alpha = 0.7f),
            )
        }

        // Accent circle top right
        Box(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                .size(40.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${yearMonth.lengthOfMonth()}",
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

// ─── Week Label ───────────────────────────────────────────────────────────────
@Composable
fun WeekLabelRow(label: String) {
    // Strip the index prefix (e.g. "0_Week 19, ...") before displaying
    val displayLabel = if (label.contains("_Week")) label.substringAfter("_") else label
    Box(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.width(36.dp).height(1.dp).background(SurfaceHigh))
            Text(
                text = displayLabel,
                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = TextMuted,
                letterSpacing = 0.3.sp,
            )
            Box(modifier = Modifier.weight(1f).height(1.dp).background(SurfaceHigh))
        }
    }
}

// ─── Day Row ──────────────────────────────────────────────────────────────────
@Composable
fun DayRowItem(
    date: LocalDate,
    events: List<CalendarEventEntity>,
    holidays: List<LiveHoliday>,
    isToday: Boolean,
    categories: List<EventCategoryEntity>,
    onEventClick: (CalendarEventEntity) -> Unit,
) {
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val dayName = date.format(DateTimeFormatter.ofPattern("EEE"))
    val hasContent = events.isNotEmpty() || holidays.isNotEmpty()

    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        // ─── Date column ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier.width(52.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    text = dayName.uppercase(),
                    fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = when {
                        isToday -> Primary
                        date.dayOfWeek == java.time.DayOfWeek.SUNDAY -> Color(0xFFFF6B6B)
                        else -> TextMuted
                    },
                    letterSpacing = 0.5.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(
                        when {
                            isToday -> Primary
                            else -> Color.Transparent
                        }
                    ).border(
                        width = if (isToday) 0.dp else 0.dp,
                        color = Color.Transparent,
                        shape = CircleShape,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 20.sp,
                        fontWeight = if (isToday) FontWeight.Black else FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color = when {
                            isToday -> Color.White
                            date.dayOfWeek == DayOfWeek.SUNDAY -> Color(0xFFFF6B6B)
                            else -> TextPrimary
                        },
                    )
                }
            }
        }

        // ─── Content column ───────────────────────────────────────────────────
        Column(
            modifier = Modifier.weight(1f).padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (!hasContent && isToday) {
                // Today with no events
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(SurfaceHigh).padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "No events today",
                        fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily, color = TextMuted,
                    )
                }
            }

            // Holiday chips
            holidays.forEach { holiday ->
                HolidayEventRow(holiday = holiday)
            }

            // Event cards
            events.forEach { event ->
                val category = categories.find { it.id == event.categoryId }
                ScheduleEventRow(
                    event = event,
                    category = category,
                    timeFormatter = timeFormatter,
                    onClick = { onEventClick(event) },
                )
            }
        }
    }

    // Thin divider
    Box(modifier = Modifier.fillMaxWidth().padding(start = 68.dp).height(0.5.dp).background(SurfaceHigh))
}

// ─── Holiday Event Row ────────────────────────────────────────────────────────
@Composable
private fun HolidayEventRow(holiday: LiveHoliday) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFF6B35).copy(alpha = 0.12f))
            .border(width = 1.dp, color = Color(0xFFFF6B35).copy(alpha = 0.25f), shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Color bar
        Box(modifier = Modifier.width(3.dp).height(32.dp).clip(RoundedCornerShape(2.dp))
            .background(Color(0xFFFF6B35)))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = holiday.name,
                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = Color(0xFFFF6B35),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (holiday.localName.isNotBlank() && holiday.localName != holiday.name) {
                Text(
                    text = holiday.localName,
                    fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color = Color(0xFFFF6B35).copy(alpha = 0.7f),
                )
            }
        }

        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFFF6B35).copy(alpha = 0.2f))
            .padding(horizontal = 7.dp, vertical = 3.dp)) {
            Text("🎉", fontSize = 12.sp)
        }
    }
}

// ─── Schedule Event Row ───────────────────────────────────────────────────────
@Composable
private fun ScheduleEventRow(
    event: CalendarEventEntity,
    category: EventCategoryEntity?,
    timeFormatter: DateTimeFormatter,
    onClick: () -> Unit,
) {
    val colorHex = category?.colorHex ?: "#5C6BC0"
    val eventColor = parseColor(colorHex)
    val startTime = Instant.ofEpochMilli(event.startTimeMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    val endTime = Instant.ofEpochMilli(event.endTimeMillis).atZone(ZoneId.systemDefault()).toLocalTime()

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(CardBackground)
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Color bar
        Box(modifier = Modifier.width(3.dp).height(36.dp).clip(RoundedCornerShape(2.dp))
            .background(eventColor))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily, color = TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.AccessTime, null, tint = TextMuted, modifier = Modifier.size(10.dp))
                Text(
                    text = if (event.isAllDay) "All day"
                    else "${startTime.format(timeFormatter)} – ${endTime.format(timeFormatter)}",
                    fontSize = 11.sp, fontFamily = MulishFamily, color = TextMuted,
                )
                if (event.location.isNotBlank()) {
                    Text("·", fontSize = 11.sp, color = TextMuted, fontFamily = MulishFamily)
                    Icon(Icons.Filled.LocationOn, null, tint = TextMuted, modifier = Modifier.size(10.dp))
                    Text(event.location, fontSize = 11.sp, fontFamily = MulishFamily, color = TextMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }

            // Priority
            if (event.priority != "NONE") {
                Text(
                    text = when (event.priority) { "HIGH" -> "🔴 High" "MEDIUM" -> "🟡 Medium" else -> "🟢 Low" },
                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily,
                    color = when (event.priority) {
                        "HIGH" -> Color(0xFFEF5350); "MEDIUM" -> Color(0xFFFFA726); else -> Color(0xFF66BB6A)
                    },
                )
            }
        }

        // Reminder dot
        val reminders = event.remindersJson.toReminderItems()
        if (reminders.isNotEmpty() || event.reminderType != "NONE") {
            Column(horizontalAlignment = Alignment.End) {
                Icon(Icons.Filled.Notifications, null, tint = Primary, modifier = Modifier.size(14.dp))
                Text(
                    text = if (reminders.isNotEmpty()) reminders[0].minutesBefore.toReminderLabel()
                    else "${event.reminderMinutesBefore}m",
                    fontSize = 9.sp, fontFamily = MulishFamily, color = Primary, fontWeight = FontWeight.Bold,
                )
            }
        }

        // Category badge
        if (category != null) {
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                .background(eventColor.copy(alpha = 0.15f))
                .padding(horizontal = 7.dp, vertical = 3.dp)) {
                Text("${category.emoji}", fontSize = 12.sp)
            }
        }
    }
}

// ─── Loading More Indicator — 3 bouncing dots ────────────────────────────────
// Shown at bottom of schedule list while next months are being fetched.
// Each dot bounces up/down with a staggered delay — like WhatsApp typing indicator.
@Composable
private fun LoadingMoreIndicator(loadingYear: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_dots")

    val dot1Y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(0),
        ),
        label = "dot1",
    )
    val dot2Y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(150),
        ),
        label = "dot2",
    )
    val dot3Y by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(300),
        ),
        label = "dot3",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .offset(y = dot1Y.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Primary),
            )
            Box(
                modifier = Modifier
                    .offset(y = dot2Y.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.7f)),
            )
            Box(
                modifier = Modifier
                    .offset(y = dot3Y.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.4f)),
            )
        }
        Text(
            text = "Loading $loadingYear...",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MulishFamily,
            color = TextMuted,
        )
    }
}
