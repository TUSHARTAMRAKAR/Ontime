package com.tushartamrakar.ontime.calendar.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.material.icons.filled.Check
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.calendar.data.local.CalendarEventEntity
import com.tushartamrakar.ontime.calendar.data.local.LiveHoliday
import com.tushartamrakar.ontime.period.data.local.CyclePhase
import com.tushartamrakar.ontime.calendar.data.local.EventCategoryEntity
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary
import com.tushartamrakar.ontime.navigation.Screen
import com.tushartamrakar.ontime.calendar.domain.toReminderItems
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.tushartamrakar.ontime.calendar.domain.toReminderLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun CalendarScreen(
    navController: NavHostController,
    bottomPadding: Dp = 0.dp,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val currentYearMonth by viewModel.currentYearMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isMonthView by viewModel.isMonthView.collectAsState()
    val allEvents by viewModel.allEvents.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    val holidays by viewModel.holidays.collectAsState()
    val isLoadingHolidays by viewModel.isLoadingHolidays.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val scheduleTotalMonths by viewModel.scheduleTotalMonths.collectAsState()
    val periodPhaseMap by viewModel.periodPhaseMap.collectAsState()
    val eventIdsWithAttendees by viewModel.eventIdsWithAttendees.collectAsState()
    val isRefreshingHolidays by viewModel.isRefreshingHolidays.collectAsState()
    val holidayRefreshProgress by viewModel.holidayRefreshProgress.collectAsState()
    val totalHolidaysLoaded by viewModel.totalHolidaysLoaded.collectAsState()
    val selectedDateEvents = viewModel.getEventsForDate(selectedDate)
    val selectedDateHolidays = holidays
        .filter { it.date == selectedDate.toString() }
        .sortedByDescending { if (it.localName != it.name) 1 else 0 }
        .distinctBy { it.name.lowercase().trim() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scheduleListState = rememberLazyListState()
    var visibleScheduleMonth by remember { mutableStateOf(java.time.YearMonth.now()) }
    var scheduleTodayIndex by remember { mutableStateOf(0) }
    var swipeDelta by remember { mutableStateOf(0f) }

    // ─── Reload holidays ONLY after Google Calendar sync ─────────────────────────
    // Previously this used LaunchedEffect(currentRoute) which fired reloadHolidays()
    // on EVERY navigation back to the calendar — alarms, create event, settings, etc.
    // That wiped the in-memory cache and triggered API calls every single time.
    //
    // Fix: CalendarSyncScreen sets savedStateHandle["google_sync_done"] = true after
    // successful Google connection. We watch ONLY that flag here.
    // LiveHolidayCache is @Singleton — its in-memory cache already survives navigation.
    val googleSyncDone = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("google_sync_done", false)
        ?.collectAsState()
        ?.value ?: false
    androidx.compose.runtime.LaunchedEffect(googleSyncDone) {
        if (googleSyncDone) {
            viewModel.reloadHolidays()
            navController.currentBackStackEntry
                ?.savedStateHandle?.set("google_sync_done", false)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            CalendarDrawer(
                isMonthView = viewMode == "MONTH",
                isScheduleView = viewMode == "SCHEDULE",
                bottomPadding = bottomPadding,
                isRefreshing = isRefreshingHolidays,
                holidayRefreshProgress = holidayRefreshProgress,
                totalHolidaysLoaded = totalHolidaysLoaded,
                onMonthClick = {
                    viewModel.setViewMode("MONTH")
                    scope.launch { drawerState.close() }
                },
                onScheduleClick = {
                    viewModel.setViewMode("SCHEDULE")
                    scope.launch { drawerState.close() }
                },
                onSyncClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.CalendarSync.route)
                },
                onRefreshEvents = { viewModel.refreshHolidaysQuick() },
                onRefreshEventsFull = { viewModel.refreshHolidaysFull() },
                onPeriodTrackerClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.PeriodTracker.route)
                },
            )
        },
        gesturesEnabled = true,
        scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ─── Header ───────────────────────────────────────────────────────
            PremiumCalendarHeader(
                currentYearMonth = if (viewMode == "SCHEDULE") visibleScheduleMonth else currentYearMonth,
                isMonthView = isMonthView,
                isScheduleView = viewMode == "SCHEDULE",
                showSearch = false,
                searchQuery = "",
                onSearchQueryChange = {},
                onSearchToggle = { navController.navigate(Screen.Search.route) },
                onTasksClick = { navController.navigate(Screen.Tasks.route) },
                onTodayClick = {
                    if (viewMode == "SCHEDULE") {
                        scope.launch { scheduleListState.animateScrollToItem(scheduleTodayIndex) }
                    } else {
                        viewModel.goToToday()
                    }
                },
                onToggleView = { viewModel.toggleView() },
                onMenuClick = { scope.launch { drawerState.open() } },
            )

            // ─── Holiday Loading Bar ──────────────────────────────────────────
            HolidayLoadingBar(isLoading = isLoadingHolidays)

            // ─── Calendar Grid ────────────────────────────────────────────────
            if (viewMode == "SCHEDULE") {
                ScheduleView(
                    events = allEvents,
                    holidays = holidays,
                    categories = allCategories,
                    selectedDate = selectedDate,
                    bottomPadding = bottomPadding,
                    listState = scheduleListState,
                    onEventClick = { navController.navigate("event_detail/${it.id}") },
                    onMonthVisible = { ym ->
                        visibleScheduleMonth = ym
                        viewModel.loadHolidaysForScheduleMonth(ym)
                    },
                    totalMonths = scheduleTotalMonths,
                    onLoadMore = { viewModel.loadMoreScheduleMonths() },
                    onTodayIndexFound = { scheduleTodayIndex = it },
                )
            } else {
                Box(
                    modifier = Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (swipeDelta < -80f) viewModel.goToNextMonth()
                                else if (swipeDelta > 80f) viewModel.goToPreviousMonth()
                                swipeDelta = 0f
                            },
                            onDragCancel = { swipeDelta = 0f },
                            onHorizontalDrag = { _, dragAmount -> swipeDelta += dragAmount },
                        )
                    }
                ) {
                    AnimatedContent(
                        targetState = currentYearMonth,
                        transitionSpec = {
                            fadeIn(tween(250)) togetherWith fadeOut(tween(250))
                        },
                        label = "calendar_month_view",
                    ) { ym ->
                        if (isMonthView) {
                            MonthView(
                                yearMonth = ym,
                                selectedDate = selectedDate,
                                events = allEvents,
                                holidays = holidays,
                                categories = allCategories,
                                periodPhaseMap = periodPhaseMap,
                                eventIdsWithAttendees = eventIdsWithAttendees,
                                onDateSelected = { viewModel.selectDate(it) },
                            )
                        } else {
                            WeekView(
                                selectedDate = selectedDate,
                                events = allEvents,
                                categories = allCategories,
                                onDateSelected = { viewModel.selectDate(it) },
                            )
                        }
                    }
                }
            }

            // ─── Divider ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SurfaceHigh),
            )

            // ─── Selected Date Header ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (selectedDate == LocalDate.now()) "Today"
                    else selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextPrimary,
                )
                if (selectedDateEvents.isNotEmpty() || selectedDateHolidays.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Primary.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        val count = selectedDateEvents.size + selectedDateHolidays.size
                        Text(
                            text = "$count item${if (count > 1) "s" else ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = Primary,
                        )
                    }
                }
            }

            // ─── Events + Holidays List ───────────────────────────────────────
            if (selectedDateEvents.isEmpty() && selectedDateHolidays.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = "📭", fontSize = 40.sp)
                        Text(
                            text = "No events today",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MulishFamily,
                            color = TextMuted,
                        )
                        Text(
                            text = "Tap + to add one",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color = TextMuted,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // ─── Holiday cards first ──────────────────────────────────
                    items(selectedDateHolidays, key = { "holiday_${it.date}_${it.name}" }) { holiday ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFF6B35).copy(alpha = 0.1f))
                                .border(1.dp, Color(0xFFFF6B35).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(text = "🎉", fontSize = 22.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = holiday.name,
                                    fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                                    fontFamily = MulishFamily, color = Color(0xFFFF6B35),
                                )
                                if (holiday.localName.isNotBlank() && holiday.localName != holiday.name) {
                                    Text(
                                        text = holiday.localName,
                                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                        fontFamily = MulishFamily,
                                        color = Color(0xFFFF6B35).copy(alpha = 0.7f),
                                    )
                                }
                                Text(
                                    text = "Public Holiday",
                                    fontSize = 11.sp, fontFamily = MulishFamily,
                                    color = Color(0xFFFF6B35).copy(alpha = 0.5f),
                                )
                            }
                        }
                    }

                    // ─── Event cards ──────────────────────────────────────────
                    items(selectedDateEvents, key = { it.id }) { event ->
                        val category = allCategories.find { it.id == event.categoryId }
                        EventCard(
                            event = event,
                            category = category,
                            onClick = {
                                navController.navigate("event_detail/${event.id}")
                            },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(bottomPadding + 80.dp)) }
                }
            }
        }

        // ─── FAB ──────────────────────────────────────────────────────────────
        FloatingActionButton(
            onClick = {
                navController.navigate("create_event/${selectedDate}")
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = bottomPadding + 16.dp),
            containerColor = Primary,
            contentColor = TextPrimary,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Event",
                modifier = Modifier.size(28.dp),
            )
        }
    }
    } // end ModalNavigationDrawer
}

// ─── Holiday Loading Bar ─────────────────────────────────────────────────────
@Composable
fun HolidayLoadingBar(isLoading: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (isLoading) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "loading_alpha",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "loading_shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_offset",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.5.dp)
            .background(Primary.copy(alpha = 0.08f * alpha)),
    ) {
        if (alpha > 0f) {
            // Moving glow bar
            Box(
                modifier = Modifier
                    .height(2.5.dp)
                    .fillMaxWidth(0.35f) // bar is 35% of total width
                    .offset(x = androidx.compose.ui.unit.Dp(shimmerOffset * 400f))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                Primary.copy(alpha = 0.4f * alpha),
                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f * alpha),
                                Primary.copy(alpha = 0.4f * alpha),
                                androidx.compose.ui.graphics.Color.Transparent,
                            )
                        )
                    ),
            )
        }
    }
}

// ─── Premium Calendar Header ─────────────────────────────────────────────────
@Composable
fun PremiumCalendarHeader(
    currentYearMonth: java.time.YearMonth,
    isMonthView: Boolean = true,
    isScheduleView: Boolean = false,
    showSearch: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchToggle: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onTodayClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onToggleView: () -> Unit = {},
) {
    val today = LocalDate.now()
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth().background(Background)) {

        // ─── Row 1: Menu | Month | Today | Week/Month toggle | Search ─────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // ── Hamburger badge ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceHigh)
                    .clickable { onMenuClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu",
                    tint = TextPrimary, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            // ── Month + Year animated ─────────────────────────────────────────
            AnimatedContent(
                targetState = currentYearMonth,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "header_month",
                modifier = Modifier.weight(1f),
            ) { ym ->
                Text(
                    text = ym.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")),
                    fontSize = 19.sp, fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily, color = TextPrimary,
                    letterSpacing = (-0.5).sp,
                )
            }

            // ── Search badge ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (showSearch) Primary.copy(alpha = 0.14f)
                        else SurfaceHigh
                    )
                    .then(
                        if (showSearch) Modifier.border(1.dp, Primary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        else Modifier
                    )
                    .clickable { onSearchToggle() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (showSearch) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = if (showSearch) Primary else TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }

            // ── Tasks badge ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceHigh)
                    .clickable { onTasksClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Assignment,
                    contentDescription = "Tasks",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }

            // ── Today badge (Google Calendar style — premium) ─────────────────
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary.copy(alpha = 0.10f))
                    .border(1.dp, Primary.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                    .clickable { onTodayClick() },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(2.dp),
                ) {
                    // Month strip
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            .background(Primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = today.format(java.time.format.DateTimeFormatter.ofPattern("MMM")).uppercase(),
                            fontSize = 4.5.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily, color = Color.White,
                            letterSpacing = 0.3.sp,
                        )
                    }
                    // Day number
                    Text(
                        text = today.dayOfMonth.toString(),
                        fontSize = 12.sp, fontWeight = FontWeight.Black,
                        fontFamily = MulishFamily, color = Primary,
                        letterSpacing = (-0.5).sp,
                    )
                }
            }

            // ── Month / Week premium gradient pill ────────────────────────────
            if (!isScheduleView) {
                Spacer(modifier = Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Primary.copy(alpha = 0.14f))
                        .border(1.dp, Primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .clickable { onToggleView() }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = if (isMonthView) "Month" else "Week",
                            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily, color = Primary,
                        )
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }

        // ─── Search bar ───────────────────────────────────────────────────────
        AnimatedVisibility(visible = showSearch) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp).padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardBackground)
                    .border(1.dp, Primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Search, contentDescription = null,
                    tint = Primary, modifier = Modifier.size(16.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp,
                        fontFamily = MulishFamily, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(Primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) Text("Search events, holidays...",
                            color = TextMuted, fontSize = 14.sp, fontFamily = MulishFamily)
                        inner()
                    },
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = null,
                            tint = TextMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // ─── Day of week headers ──────────────────────────────────────────────
        if (!isScheduleView && !showSearch) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 4.dp),
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { idx, day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color = if (idx == 6) Color(0xFFFF6B6B).copy(alpha = 0.7f) else TextMuted,
                    )
                }
            }
        }
    }
}

// ─── Month View// ─── Month View ───────────────────────────────────────────────────────────────
@Composable
fun MonthView(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    events: List<CalendarEventEntity>,
    holidays: List<LiveHoliday> = emptyList(),
    categories: List<EventCategoryEntity>,
    periodPhaseMap: Map<String, CyclePhase> = emptyMap(),
    eventIdsWithAttendees: Set<Int> = emptySet(),
    onDateSelected: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val firstDayOfMonth = yearMonth.atDay(1)
    // Start grid from Monday
    val gridStart = firstDayOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        // 6 rows of 7 days
        for (week in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (day in 0 until 7) {
                    val date = gridStart.plusDays((week * 7 + day).toLong())
                    val isCurrentMonth = date.month == yearMonth.month
                    val isToday = date == today
                    val isSelected = date == selectedDate
                    val dayEvents = events.filter { event ->
                        val eventDate = java.time.Instant.ofEpochMilli(event.startTimeMillis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                        eventDate == date
                    }

                    val isSunday = date.dayOfWeek == java.time.DayOfWeek.SUNDAY

                    // Any event on this date that has attendees → show badge
                    val dateHasAttendees = isCurrentMonth &&
                        dayEvents.any { it.id in eventIdsWithAttendees }

                    DayCell(
                        date = date,
                        isCurrentMonth = isCurrentMonth,
                        isToday = isToday,
                        isSelected = isSelected,
                        isSunday = isSunday,
                        events = dayEvents,
                        holidays = holidays.filter { it.date == date.toString() }
                        .sortedByDescending { if (it.localName != it.name) 1 else 0 }
                        .distinctBy { it.name.lowercase().trim() },
                        categories = categories,
                        periodPhase = periodPhaseMap[date.toString()],
                        hasAttendeeBadge = dateHasAttendees,
                        onClick = { onDateSelected(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// ─── Day Cell ─────────────────────────────────────────────────────────────────
@Composable
private fun DayCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    isSunday: Boolean = false,
    events: List<CalendarEventEntity>,
    holidays: List<LiveHoliday> = emptyList(),
    categories: List<EventCategoryEntity>,
    periodPhase: CyclePhase? = null,
    hasAttendeeBadge: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasHoliday  = holidays.isNotEmpty()
    val holidayColor = Color(0xFFFF6B35)

    // Resolve period phase → colour (only for current month; hidden for adjacent months)
    val phaseColor: Color? = if (isCurrentMonth && periodPhase != null && periodPhase != CyclePhase.NONE) {
        when (periodPhase) {
            CyclePhase.MENSTRUATION -> Color(0xFFEF4444)              // red    — period
            CyclePhase.FOLLICULAR   -> Color(0xFFF59E0B)              // amber  — rising energy
            CyclePhase.OVULATION    -> Color(0xFF7C3AED)              // purple — ovulation
            CyclePhase.FERTILE      -> Color(0xFF22C55E)              // green  — fertile window
            CyclePhase.LUTEAL       -> Color(0xFF3B82F6)              // blue   — luteal / PMS
            CyclePhase.PREDICTED    -> Color(0xFFEF4444).copy(alpha = 0.40f)  // pale red — predicted
            else                    -> null
        }
    } else null

    // Box wraps the Column so we can overlay the phase bar at the bottom
    // without touching any of the existing layout logic
    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected                   -> Primary
                    isToday                      -> Primary.copy(alpha = 0.15f)
                    isSunday && isCurrentMonth   -> Color(0xFFFF3D00).copy(alpha = 0.05f)
                    else                         -> Color.Transparent
                }
            )
            .clickable { onClick() },
    ) {
        // ── Main content column ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            // Date number
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 13.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Black else FontWeight.Medium,
                fontFamily = MulishFamily,
                color = when {
                    isSelected                   -> Color.White
                    isToday                      -> Primary
                    isSunday && isCurrentMonth   -> Color(0xFFFF6B6B)
                    isCurrentMonth               -> TextPrimary
                    else                         -> TextMuted.copy(alpha = 0.35f)
                },
            )

            Spacer(modifier = Modifier.height(2.dp))

            // ── Holiday chip ──────────────────────────────────────────────────
            if (hasHoliday) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .padding(horizontal = 1.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.85f)
                            else holidayColor.copy(alpha = 0.9f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "🎉 ${holidays[0].name}",
                        fontSize = 5.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) holidayColor else Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                }
                Spacer(modifier = Modifier.height(1.dp))
            }

            // ── Event blocks (up to 2, or 1 if holiday) ──────────────────────
            val maxEvents = if (hasHoliday) 1 else 2
            events.take(maxEvents).forEach { event ->
                val category   = categories.find { it.id == event.categoryId }
                val colorHex   = category?.colorHex ?: "#5C6BC0"
                val eventColor = parseColor(colorHex)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .padding(horizontal = 1.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.7f) else eventColor
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = event.title,
                        fontSize = 5.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) eventColor else Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                }
                Spacer(modifier = Modifier.height(1.dp))
            }

            // ── Overflow "+N" ─────────────────────────────────────────────────
            val total = events.size + (if (hasHoliday) 1 else 0)
            val shown = maxEvents + (if (hasHoliday) 1 else 0)
            if (total > shown) {
                Text(
                    text = "+${total - shown}",
                    fontSize = 6.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextMuted,
                    fontFamily = MulishFamily,
                )
            }
        }

        // ── People badge — 6dp dot pinned to top-right corner ────────────────
        // Shows when any event on this date has attendees. Uses the same
        // Box-overlay technique as the phase bar — zero layout disruption.
        if (hasAttendeeBadge) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.85f)
                        else Color(0xFF5C6BC0)   // indigo — matches calendar primary
                    ),
            )
        }

        // ── Period phase bar — 3 dp strip pinned to the bottom of the cell ────
        // Overlaid via Box alignment so it never shifts event/holiday chips.
        // Invisible on selected day (white-on-white) — uses a faint white stripe instead.
        if (phaseColor != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.55f)
                        else phaseColor
                    ),
            )
        }
    }
}

// ─── Week View ────────────────────────────────────────────────────────────────
@Composable
fun WeekView(
    selectedDate: LocalDate,
    events: List<CalendarEventEntity>,
    categories: List<EventCategoryEntity>,
    onDateSelected: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    // Start week from Monday
    val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        // Day of week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextMuted,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Single row of 7 days
        Row(modifier = Modifier.fillMaxWidth()) {
            for (day in 0 until 7) {
                val date = weekStart.plusDays(day.toLong())
                val isToday = date == today
                val isSelected = date == selectedDate
                val dayEvents = events.filter { event ->
                    val eventDate = java.time.Instant.ofEpochMilli(event.startTimeMillis)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    eventDate == date
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isSelected -> Primary
                                isToday -> Primary.copy(alpha = 0.15f)
                                else -> Color.Transparent
                            }
                        )
                        .clickable { onDateSelected(date) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 16.sp,
                        fontWeight = if (isToday || isSelected) FontWeight.Black else FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color = when {
                            isSelected -> Color.White
                            isToday -> Primary
                            else -> TextPrimary
                        },
                    )

                    // Event dots
                    if (dayEvents.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.height(6.dp),
                        ) {
                            dayEvents.take(3).forEach { event ->
                                val category = categories.find { it.id == event.categoryId }
                                val colorHex = category?.colorHex ?: "#5C6BC0"
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color.White.copy(alpha = 0.8f)
                                            else parseColor(colorHex)
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

// ─── Event Card ───────────────────────────────────────────────────────────────
@Composable
fun EventCard(
    event: CalendarEventEntity,
    category: EventCategoryEntity?,
    onClick: () -> Unit,
) {
    val colorHex = category?.colorHex ?: "#5C6BC0"
    val eventColor = parseColor(colorHex)

    val startTime = java.time.Instant.ofEpochMilli(event.startTimeMillis)
        .atZone(ZoneId.systemDefault()).toLocalTime()
    val endTime = java.time.Instant.ofEpochMilli(event.endTimeMillis)
        .atZone(ZoneId.systemDefault()).toLocalTime()
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(width = 1.dp, color = Border, shape = RoundedCornerShape(16.dp))
            .clickable { onClick() },
    ) {
        // Color accent bar
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .background(eventColor),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = event.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )

                // Category badge
                if (category != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(eventColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "${category.emoji} ${category.name}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = eventColor,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Description subtitle
            if (event.description.isNotBlank()) {
                Text(
                    text = event.description,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            // Time
            if (!event.isAllDay) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = "${startTime.format(timeFormatter)} → ${endTime.format(timeFormatter)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = MulishFamily,
                        color = TextMuted,
                    )
                }
            } else {
                Text(
                    text = "All Day",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = MulishFamily,
                    color = eventColor,
                )
            }

            // Location
            if (event.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = event.location,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // ─── Reminder badge ───────────────────────────────────────────────
            // Check new multi-reminder JSON first, fallback to legacy single
            val newReminders = event.remindersJson.toReminderItems()
            val hasReminder = newReminders.isNotEmpty() || event.reminderType != "NONE"

            if (hasReminder) {
                Spacer(modifier = Modifier.height(4.dp))
                if (newReminders.isNotEmpty()) {
                    // Show first reminder + count if multiple
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = null,
                            tint = Primary, modifier = Modifier.size(11.dp))
                        Text(
                            text = if (newReminders.size == 1)
                                "${newReminders[0].minutesBefore.toReminderLabel()} · ${if (newReminders[0].type == "ALARM") "Alarm" else "Notification"}"
                            else
                                "${newReminders[0].minutesBefore.toReminderLabel()} · +${newReminders.size - 1} more",
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            fontFamily = MulishFamily, color = Primary,
                        )
                    }
                } else {
                    // Legacy single reminder
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = null,
                            tint = Primary, modifier = Modifier.size(11.dp))
                        Text(
                            text = "${event.reminderMinutesBefore} min before · ${event.reminderType.lowercase().replaceFirstChar { it.uppercase() }}",
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            fontFamily = MulishFamily, color = Primary,
                        )
                    }
                }
            }
        }
    }
}

// ─── Color Parser ─────────────────────────────────────────────────────────────
fun parseColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val r = clean.substring(0, 2).toInt(16) / 255f
        val g = clean.substring(2, 4).toInt(16) / 255f
        val b = clean.substring(4, 6).toInt(16) / 255f
        Color(r, g, b)
    } catch (e: Exception) {
        Primary
    }
}


// ─── Calendar Drawer ──────────────────────────────────────────────────────────
@Composable
fun CalendarDrawer(
    isMonthView: Boolean,
    isScheduleView: Boolean = false,
    bottomPadding: androidx.compose.ui.unit.Dp = 80.dp,
    isRefreshing: Boolean = false,
    holidayRefreshProgress: Pair<Int, Int> = 0 to 0,
    totalHolidaysLoaded: Int = -1,
    onMonthClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onSyncClick: () -> Unit,
    onRefreshEvents: () -> Unit = {},
    onRefreshEventsFull: () -> Unit = {},
    onPeriodTrackerClick: () -> Unit = {},
) {
    ModalDrawerSheet(
        drawerContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        drawerContentColor = TextPrimary,
        modifier = Modifier.padding(bottom = bottomPadding).width(300.dp),
        windowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.Color(0xFF1A1A2E),
                        androidx.compose.ui.graphics.Color(0xFF16213E),
                        androidx.compose.ui.graphics.Color(0xFF0F3460),
                    )
                )
            ),
        ) {
            // ─── Decorative glow circle ───────────────────────────────────────
            Box(
                modifier = Modifier.size(200.dp).offset(x = (-60).dp, y = (-60).dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                Primary.copy(alpha = 0.25f),
                                androidx.compose.ui.graphics.Color.Transparent,
                            )
                        ), shape = CircleShape
                    ),
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // ─── Header ───────────────────────────────────────────────────
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(Primary.copy(alpha = 0.2f))
                            .border(1.dp, Primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "🔔", fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            text = "Ontime",
                            fontSize = 18.sp, fontWeight = FontWeight.Black,
                            fontFamily = MulishFamily, color = TextPrimary,
                            letterSpacing = (-0.3).sp,
                        )
                        Text(
                            text = "Calendar",
                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ─── VIEW label ───────────────────────────────────────────────
                Text(
                    text = "V I E W S",
                    fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f),
                    letterSpacing = 3.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ─── Month View Card ──────────────────────────────────────────
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isMonthView)
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(Primary.copy(alpha = 0.8f), Primary.copy(alpha = 0.4f))
                                )
                            else
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(
                                        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.07f),
                                        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.03f),
                                    )
                                )
                        )
                        .border(
                            width = 1.dp,
                            brush = if (isMonthView)
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(Primary, Primary.copy(alpha = 0.3f))
                                )
                            else
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(
                                        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f),
                                        androidx.compose.ui.graphics.Color.Transparent,
                                    )
                                ),
                            shape = RoundedCornerShape(18.dp),
                        )
                        .clickable { onMonthClick() }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isMonthView) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f)
                                    else Primary.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.GridView, contentDescription = null,
                                tint = if (isMonthView) androidx.compose.ui.graphics.Color.White else Primary,
                                modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Month View",
                                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                                fontFamily = MulishFamily,
                                color = if (isMonthView) androidx.compose.ui.graphics.Color.White else TextPrimary,
                            )
                            Text(
                                text = "Full calendar grid",
                                fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                fontFamily = MulishFamily,
                                color = if (isMonthView) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                                        else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f),
                            )
                        }
                        if (isMonthView) {
                            Box(
                                modifier = Modifier.size(8.dp).clip(CircleShape)
                                    .background(androidx.compose.ui.graphics.Color.White),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ─── Schedule View Card ───────────────────────────────────────
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isScheduleView)
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(Primary.copy(alpha = 0.8f), Primary.copy(alpha = 0.4f))
                                )
                            else
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(
                                        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.07f),
                                        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.03f),
                                    )
                                )
                        )
                        .border(
                            width = 1.dp,
                            brush = if (isScheduleView)
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(Primary, Primary.copy(alpha = 0.3f))
                                )
                            else
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(
                                        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f),
                                        androidx.compose.ui.graphics.Color.Transparent,
                                    )
                                ),
                            shape = RoundedCornerShape(18.dp),
                        )
                        .clickable { onScheduleClick() }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isScheduleView) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f)
                                    else Primary.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.ViewAgenda, contentDescription = null,
                                tint = if (isScheduleView) androidx.compose.ui.graphics.Color.White else Primary,
                                modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Schedule",
                                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                                fontFamily = MulishFamily,
                                color = if (isScheduleView) androidx.compose.ui.graphics.Color.White else TextPrimary,
                            )
                            Text(
                                text = "Agenda list view",
                                fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                fontFamily = MulishFamily,
                                color = if (isScheduleView) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                                        else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f),
                            )
                        }
                        if (isScheduleView) {
                            Box(
                                modifier = Modifier.size(8.dp).clip(CircleShape)
                                    .background(androidx.compose.ui.graphics.Color.White),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // ─── Bottom divider ───────────────────────────────────────────
                Box(
                    modifier = Modifier.fillMaxWidth().height(1.dp).background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f),
                                androidx.compose.ui.graphics.Color.Transparent,
                            )
                        )
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ─── WELLNESS section label ───────────────────────────────────
                Text(
                    "WELLNESS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextMuted.copy(alpha = 0.6f),
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )

                // ─── Period Tracker button ────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(androidx.compose.ui.graphics.Color(0xFFE91E8C).copy(alpha = 0.08f))
                        .border(
                            1.dp,
                            androidx.compose.ui.graphics.Color(0xFFE91E8C).copy(alpha = 0.25f),
                            RoundedCornerShape(14.dp),
                        )
                        .clickable { onPeriodTrackerClick() }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("🌸", fontSize = 16.sp)
                    Text(
                        "Period Tracker",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ─── Refresh Events Button + Premium Progress Bar ────────────────
                // State machine:
                //   Idle      → button, no bar
                //   Loading   → spinning button, animated indigo bar, "X of Y months" text
                //   Complete  → full green bar, "✓ N holidays loaded", fades after 2s
                //   Idle      → clean again

                val refreshInfinite = rememberInfiniteTransition(label = "refresh_spin")
                val refreshSpinAngle by refreshInfinite.animateFloat(
                    initialValue = 0f, targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "refresh_rotation",
                )
                val refreshRotation = if (isRefreshing) refreshSpinAngle else 0f

                // Track "just completed" for the 2-second green celebration
                var justCompleted by remember { mutableStateOf(false) }
                androidx.compose.runtime.LaunchedEffect(isRefreshing) {
                    if (!isRefreshing && totalHolidaysLoaded > 0) {
                        justCompleted = true
                        delay(2000L)
                        justCompleted = false
                    }
                }

                // Animated bar progress fraction
                val progressFraction = if (holidayRefreshProgress.second > 0)
                    holidayRefreshProgress.first.toFloat() / holidayRefreshProgress.second
                else 0f
                val animatedProgress by animateFloatAsState(
                    targetValue = if (justCompleted) 1f else progressFraction,
                    animationSpec = tween(durationMillis = 400, easing = LinearEasing),
                    label = "holiday_progress",
                )
                val barColor = if (justCompleted)
                    androidx.compose.ui.graphics.Color(0xFF4CAF50)  // green on complete
                else Primary                                          // indigo while loading

                // Info dialog state
                var showRefreshInfo by remember { mutableStateOf(false) }
                if (showRefreshInfo) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showRefreshInfo = false },
                        containerColor = androidx.compose.ui.graphics.Color(0xFF1A1A2E),
                        shape = RoundedCornerShape(16.dp),
                        title = {
                            Text(
                                "Refresh Events",
                                fontFamily = MulishFamily, fontWeight = FontWeight.Black,
                                color = androidx.compose.ui.graphics.Color.White, fontSize = 16.sp,
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "Quick refresh loads this month + 2 months ahead. Fast and covers your daily needs.",
                                    fontFamily = MulishFamily, fontWeight = FontWeight.Medium,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f),
                                    fontSize = 13.sp, lineHeight = 20.sp,
                                )
                                androidx.compose.material3.Divider(
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f),
                                )
                                Text(
                                    "Without Google: 27 fixed government holidays.\nWith Google: fixed holidays + exact festival dates (Diwali, Holi, Eid...).",
                                    fontFamily = MulishFamily, fontWeight = FontWeight.Medium,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.55f),
                                    fontSize = 12.sp, lineHeight = 18.sp,
                                )
                                // Full 24-month load option
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Primary.copy(alpha = 0.15f))
                                        .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isRefreshing) {
                                            showRefreshInfo = false
                                            onRefreshEventsFull()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Refresh, contentDescription = null,
                                        tint = Primary, modifier = Modifier.size(16.dp),
                                    )
                                    Column {
                                        Text(
                                            "Load all 2 years",
                                            fontFamily = MulishFamily, fontWeight = FontWeight.ExtraBold,
                                            color = Primary, fontSize = 13.sp,
                                        )
                                        Text(
                                            "Loads 24 months — Jan this year to Dec next year",
                                            fontFamily = MulishFamily, fontWeight = FontWeight.Medium,
                                            color = Primary.copy(alpha = 0.65f), fontSize = 11.sp,
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { showRefreshInfo = false }) {
                                Text("Got it", color = Primary, fontFamily = MulishFamily, fontWeight = FontWeight.Bold)
                            }
                        },
                    )
                }

                // ── Main button row ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = if (isRefreshing || justCompleted) 0.09f else 0.06f))
                        .border(
                            1.dp,
                            if (justCompleted) androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.35f)
                            else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(14.dp),
                        )
                        .clickable(enabled = !isRefreshing && !justCompleted) { onRefreshEvents() }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        if (justCompleted) Icons.Filled.Check else Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = if (justCompleted) androidx.compose.ui.graphics.Color(0xFF4CAF50) else Primary,
                        modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = refreshRotation },
                    )
                    Text(
                        text = when {
                            justCompleted -> "Refresh Events"
                            isRefreshing  -> "Refreshing..."
                            else          -> "Refresh Events"
                        },
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color = if (justCompleted)
                            androidx.compose.ui.graphics.Color(0xFF4CAF50)
                        else
                            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f),
                    )
                    // Small ℹ button
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f))
                            .clickable { showRefreshInfo = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "i", fontSize = 11.sp, fontWeight = FontWeight.Black,
                            fontFamily = MulishFamily,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
                        )
                    }
                }

                // ── Premium progress bar + status text ─────────────────────────
                AnimatedVisibility(
                    visible = isRefreshing || justCompleted,
                    enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                    exit  = fadeOut(tween(400)) + shrinkVertically(tween(400)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        // Thin 3dp animated bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f)
                                ),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(barColor),
                            )
                        }

                        // Status text below the bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(
                                text = when {
                                    justCompleted -> "✓"
                                    isRefreshing  -> "↻"
                                    else          -> ""
                                },
                                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = barColor, fontFamily = MulishFamily,
                            )
                            Text(
                                text = when {
                                    justCompleted ->
                                        "$totalHolidaysLoaded holidays loaded"
                                    isRefreshing && holidayRefreshProgress.second > 0 ->
                                        "${holidayRefreshProgress.first} of ${holidayRefreshProgress.second} months · $totalHolidaysLoaded holidays"
                                    else -> ""
                                },
                                fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                color = if (justCompleted)
                                    androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                else
                                    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.45f),
                                fontFamily = MulishFamily,
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(10.dp))

                // ─── Sync Button ──────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f))
                        .border(
                            1.dp,
                            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(14.dp),
                        )
                        .clickable { onSyncClick() }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Filled.Sync, contentDescription = null,
                        tint = Primary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Google Calendar Sync",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
