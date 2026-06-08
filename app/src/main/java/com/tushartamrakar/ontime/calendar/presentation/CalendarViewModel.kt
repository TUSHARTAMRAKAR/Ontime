package com.tushartamrakar.ontime.calendar.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tushartamrakar.ontime.calendar.data.local.CalendarEventEntity
import com.tushartamrakar.ontime.calendar.data.local.EventCategoryEntity
import com.tushartamrakar.ontime.calendar.data.local.LiveHoliday
import com.tushartamrakar.ontime.calendar.data.local.LiveHolidayCache
import com.tushartamrakar.ontime.tasks.data.repository.TaskRepository
import com.tushartamrakar.ontime.period.data.local.CyclePhase
import com.tushartamrakar.ontime.period.data.repository.PeriodRepository
import com.tushartamrakar.ontime.calendar.data.local.ContactResult
import com.tushartamrakar.ontime.calendar.data.local.EventAttendeeEntity
import com.tushartamrakar.ontime.calendar.data.repository.AttendeeDraftStore
import com.tushartamrakar.ontime.calendar.data.repository.AttendeeRepository
import com.tushartamrakar.ontime.calendar.data.repository.CalendarRepository
import com.tushartamrakar.ontime.calendar.notification.AttendeeNotificationHelper
import com.tushartamrakar.ontime.calendar.domain.RecurrenceType
import com.tushartamrakar.ontime.calendar.domain.ReminderItem
import com.tushartamrakar.ontime.calendar.domain.toJson
import com.tushartamrakar.ontime.calendar.notification.EventReminderScheduler
import androidx.compose.runtime.mutableStateListOf
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    val repository: CalendarRepository,
    val holidayCache: LiveHolidayCache,
    @ApplicationContext private val context: Context,
    private val taskRepository: TaskRepository,
    private val periodRepository: PeriodRepository,
    private val attendeeRepository: AttendeeRepository,
    val attendeeNotificationHelper: AttendeeNotificationHelper,
    private val attendeeDraftStore: AttendeeDraftStore,
) : ViewModel() {

    // ─── Calendar state ───────────────────────────────────────────────────────
    private val _currentYearMonth = MutableStateFlow(YearMonth.now())
    val currentYearMonth: StateFlow<YearMonth> = _currentYearMonth

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _isMonthView = MutableStateFlow(true)
    val isMonthView: StateFlow<Boolean> = _isMonthView

    private val _viewMode = MutableStateFlow("MONTH")
    val viewMode: StateFlow<String> = _viewMode

    // ─── Events & Categories ──────────────────────────────────────────────────
    val allEvents: StateFlow<List<CalendarEventEntity>> = repository.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<EventCategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── Live holidays — accumulated across all loaded months ─────────────────
    // KEY FIX: Map accumulates months; never replaces existing data
    // So switching month view → schedule view keeps ALL months' holidays
    private val _holidaysByMonth = HashMap<String, List<LiveHoliday>>()

    private val _holidays = MutableStateFlow<List<LiveHoliday>>(emptyList())
    val holidays: StateFlow<List<LiveHoliday>> = _holidays

    private val _isLoadingHolidays = MutableStateFlow(false)
    val isLoadingHolidays: StateFlow<Boolean> = _isLoadingHolidays

    // ─── Explicit holiday refresh state (Refresh Events button in drawer) ────
    // Separate from isLoadingHolidays (which covers lazy month-nav loads)
    private val _isRefreshingHolidays = MutableStateFlow(false)
    val isRefreshingHolidays: StateFlow<Boolean> = _isRefreshingHolidays

    // Pair(monthsDone, totalMonths) — e.g. 3 to 24 or 2 to 3
    private val _holidayRefreshProgress = MutableStateFlow(0 to 0)
    val holidayRefreshProgress: StateFlow<Pair<Int, Int>> = _holidayRefreshProgress

    // -1 = never refreshed, 0 = none found, >0 = count
    private val _totalHolidaysLoaded = MutableStateFlow(-1)
    val totalHolidaysLoaded: StateFlow<Int> = _totalHolidaysLoaded

    // ─── Task due-date indicators for month view ──────────────────────────────
    // Maps "YYYY-MM-DD" → priority String so CalendarScreen can draw
    // a colored dot below dates that have tasks due.
    val taskDueDates: StateFlow<Map<String, String>> = taskRepository.getAllTasksWithDueDate()
        .map { tasks ->
            tasks.associate { task ->
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = task.dueDate!! }
                val key = String.format("%04d-%02d-%02d",
                    cal.get(java.util.Calendar.YEAR),
                    cal.get(java.util.Calendar.MONTH) + 1,
                    cal.get(java.util.Calendar.DAY_OF_MONTH))
                key to task.priority
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    // ─── Period phase dots for month view ────────────────────────────────────
    // Re-generates whenever cycle data changes (new logs, completed periods, etc.)
    // Maps "YYYY-MM-DD" → CyclePhase so MonthView can draw a tiny coloured dot.
    // Range: 2 months back → 4 months forward covers all visible calendar months.
    val periodPhaseMap: StateFlow<Map<String, CyclePhase>> =
        periodRepository.getAllCycles()
            .map {
                val start = LocalDate.now().minusMonths(2)
                val end   = LocalDate.now().plusMonths(4)
                periodRepository.getPhaseMapForRange(start, end)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ─── Draft attendees — shared between CreateEventScreen ↔ AddPeopleScreen ─
    // CreateEventScreen writes current list here before navigating.
    // AddPeopleScreen reads and modifies it.
    // On return, CreateEventScreen reads the updated list.
    val draftAttendees get() = attendeeDraftStore.draftAttendees

    // ─── Attendee badge indicators for month view ────────────────────────────
    // Set of eventIds that have at least one attendee — used by DayCell
    // to show the people badge dot. Reacts live when attendees are added/removed.
    val eventIdsWithAttendees: StateFlow<Set<Int>> =
        attendeeRepository.getEventIdsWithAttendees()
            .map { it.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Flatten all loaded months into a single sorted deduplicated list
    private fun buildFlatHolidayList(): List<LiveHoliday> =
        _holidaysByMonth.values
            .flatten()
            .distinctBy { "${it.date}_${it.name.lowercase().trim()}" }
            .sortedBy { it.date }

    // ─── Search holidays (wider range) ────────────────────────────────────────
    private val _allHolidaysForSearch = MutableStateFlow<List<LiveHoliday>>(emptyList())
    val allHolidaysForSearch: StateFlow<List<LiveHoliday>> = _allHolidaysForSearch

    init {
        viewModelScope.launch {
            repository.seedDefaultCategories()
            loadHolidaysForMonth(_currentYearMonth.value)
            // Preload next 2 months in background
            holidayCache.preloadMonths(_currentYearMonth.value, 3)
        }
        // Reload when month changes
        viewModelScope.launch {
            _currentYearMonth.collect { ym ->
                loadHolidaysForMonth(ym)
                holidayCache.preloadMonths(ym, 2)
            }
        }
    }

    // ─── Load holidays for a specific month ───────────────────────────────────
    private fun loadHolidaysForMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            _isLoadingHolidays.value = true
            try {
                val result = holidayCache.getHolidaysForMonth(yearMonth)
                // ACCUMULATE: add this month to map, then rebuild flat list
                val key = "${yearMonth.year}_${yearMonth.monthValue}"
                _holidaysByMonth[key] = result
                _holidays.value = buildFlatHolidayList()
            } catch (e: Exception) {
                // Keep existing data — don't wipe it on error
            } finally {
                _isLoadingHolidays.value = false
            }
        }
    }

    // ─── Called from ScheduleView.onMonthVisible when user scrolls ────────────
    // Lazily loads months that scroll into view; skips already-loaded months
    fun loadHolidaysForScheduleMonth(yearMonth: YearMonth) {
        val key = "${yearMonth.year}_${yearMonth.monthValue}"
        if (_holidaysByMonth.containsKey(key)) return // already loaded, skip
        viewModelScope.launch {
            try {
                val result = holidayCache.getHolidaysForMonth(yearMonth)
                _holidaysByMonth[key] = result
                _holidays.value = buildFlatHolidayList()
            } catch (e: Exception) { /* silent — keep existing */ }
        }
    }

    // ─── Load all holidays for search (5 years back + 5 years forward) ──────────
    fun loadAllHolidaysForSearch() {
        viewModelScope.launch {
            val now = YearMonth.now()
            val results = mutableListOf<LiveHoliday>()

            // 5 years back (-60 months) + 5 years forward (+60 months) = 10 years total
            // Covers Diwali 2021, 2022, 2023, 2024, 2025, 2026, 2027, 2028, 2029, 2030
            for (i in -60..60) {
                val month = now.plusMonths(i.toLong())
                try {
                    results.addAll(holidayCache.getHolidaysForMonth(month))
                } catch (_: Exception) { /* skip failed months silently */ }
            }

            // Dedupe only on exact same date + same name (different dates = different occurrences)
            // DO NOT dedupe by name alone — that would remove the same holiday from different years!
            _allHolidaysForSearch.value = results
                .distinctBy { "${it.date}_${it.name.lowercase().trim()}" }
                .sortedBy { it.date }
        }
    }

    // ─── Reload — clears ViewModel accumulator and re-fetches from LiveHolidayCache ──
    fun reloadHolidays() {
        // Clear BOTH the ViewModel accumulator AND the LiveHolidayCache in-memory cache.
        // Without clearing LiveHolidayCache, getHolidaysForMonth() returns cached data
        // instantly and no API calls fire — button appears to do nothing.
        holidayCache.clearCache()
        _holidaysByMonth.clear()
        _holidays.value = emptyList()
        _scheduleTotalMonths.value = 18 // reset to initial range on reload
        if (_viewMode.value == "SCHEDULE") {
            setViewMode("SCHEDULE")
        } else {
            loadHolidaysForMonth(_currentYearMonth.value)
        }
    }


    // ─── Quick holiday refresh — current month + 2 forward (fast, daily use) ─────
    // Clears cache and loads 3 months with per-month progress tracking.
    // Called by "Refresh Events" button in the drawer on a single tap.
    fun refreshHolidaysQuick() {
        if (_isRefreshingHolidays.value) return
        viewModelScope.launch {
            _isRefreshingHolidays.value = true
            _holidayRefreshProgress.value = 0 to 3
            _totalHolidaysLoaded.value = 0
            holidayCache.clearCache()
            _holidaysByMonth.clear()
            _holidays.value = emptyList()

            var holidayCount = 0
            val now = YearMonth.now()
            for (i in 0..2) {
                val ym = now.plusMonths(i.toLong())
                val key = "${ym.year}_${ym.monthValue}"
                try {
                    val result = holidayCache.getHolidaysForMonth(ym)
                    _holidaysByMonth[key] = result
                    _holidays.value = buildFlatHolidayList()
                    holidayCount += result.size
                } catch (e: Exception) { /* skip silently */ }
                _holidayRefreshProgress.value = (i + 1) to 3
                _totalHolidaysLoaded.value = holidayCount
            }
            _isRefreshingHolidays.value = false
        }
    }

    // ─── Full holiday refresh — all 24 months (Jan this year → Dec next year) ────
    // Triggered from the ℹ info dialog "Load all 2 years" option in the drawer.
    // Loads every month with progress so user can see "12 of 24 months" etc.
    fun refreshHolidaysFull() {
        if (_isRefreshingHolidays.value) return
        viewModelScope.launch {
            val totalMonths = 24
            _isRefreshingHolidays.value = true
            _holidayRefreshProgress.value = 0 to totalMonths
            _totalHolidaysLoaded.value = 0
            holidayCache.clearCache()
            _holidaysByMonth.clear()
            _holidays.value = emptyList()

            var holidayCount = 0
            val startYm = YearMonth.of(LocalDate.now().year, 1)
            for (i in 0 until totalMonths) {
                val ym = startYm.plusMonths(i.toLong())
                val key = "${ym.year}_${ym.monthValue}"
                try {
                    val result = holidayCache.getHolidaysForMonth(ym)
                    _holidaysByMonth[key] = result
                    _holidays.value = buildFlatHolidayList()
                    holidayCount += result.size
                } catch (e: Exception) { /* skip silently */ }
                _holidayRefreshProgress.value = (i + 1) to totalMonths
                _totalHolidaysLoaded.value = holidayCount
            }
            _isRefreshingHolidays.value = false
        }
    }

    // ─── Get holidays for a specific date ─────────────────────────────────────
    fun getHolidaysForDate(date: LocalDate): List<LiveHoliday> =
        _holidays.value.filter { it.date == date.toString() }

    // ─── View controls ────────────────────────────────────────────────────────
    // ─── How many months Schedule View covers from Jan of current year ───────
    // Starts at 18. Grows by 3 each time user nears the bottom → truly infinite
    private val _scheduleTotalMonths = MutableStateFlow(18)
    val scheduleTotalMonths: StateFlow<Int> = _scheduleTotalMonths

    fun setViewMode(mode: String) {
        _viewMode.value = mode
        _isMonthView.value = mode == "MONTH"
        if (mode == "SCHEDULE") {
            viewModelScope.launch {
                _isLoadingHolidays.value = true
                val startYm = YearMonth.of(LocalDate.now().year, 1)
                val total = _scheduleTotalMonths.value
                for (i in 0 until total) {
                    val ym = startYm.plusMonths(i.toLong())
                    val key = "${ym.year}_${ym.monthValue}"
                    if (!_holidaysByMonth.containsKey(key)) {
                        try {
                            val result = holidayCache.getHolidaysForMonth(ym)
                            _holidaysByMonth[key] = result
                            _holidays.value = buildFlatHolidayList()
                        } catch (e: Exception) { /* skip */ }
                    }
                }
                _isLoadingHolidays.value = false
            }
        }
    }

    // ─── Dedicated guard for loadMore only — independent of initial load ────────
    // Prevents stacking multiple loadMore calls on top of each other.
    // Does NOT block when setViewMode initial load is in progress.
    // This fixed the bug where fast scrollers got stuck at the bottom.
    private var isLoadingMoreMonths = false

    // ─── Called when user scrolls near the bottom of Schedule View ───────────
    // Loads 3 more months and extends the visible range → truly infinite scroll
    // Triggers BOTH the top progress bar AND the bottom 3-dot indicator together
    fun loadMoreScheduleMonths() {
        if (isLoadingMoreMonths) return // only block duplicate loadMore calls
        val startYm = YearMonth.of(LocalDate.now().year, 1)
        val currentTotal = _scheduleTotalMonths.value
        viewModelScope.launch {
            isLoadingMoreMonths = true
            _isLoadingHolidays.value = true  // fires top progress bar
            for (i in currentTotal until (currentTotal + 3)) {
                val ym = startYm.plusMonths(i.toLong())
                val key = "${ym.year}_${ym.monthValue}"
                try {
                    val result = holidayCache.getHolidaysForMonth(ym)
                    _holidaysByMonth[key] = result
                    _holidays.value = buildFlatHolidayList()
                } catch (e: Exception) { /* skip */ }
            }
            _scheduleTotalMonths.value = currentTotal + 3
            _isLoadingHolidays.value = false // hides top progress bar
            isLoadingMoreMonths = false
        }
    }

    fun toggleView() { _isMonthView.value = !_isMonthView.value }
    fun selectDate(date: LocalDate) { _selectedDate.value = date }
    fun goToPreviousMonth() { _currentYearMonth.value = _currentYearMonth.value.minusMonths(1) }
    fun goToNextMonth() { _currentYearMonth.value = _currentYearMonth.value.plusMonths(1) }
    fun goToToday() {
        _currentYearMonth.value = YearMonth.now()
        _selectedDate.value = LocalDate.now()
    }

    fun getEventsForDate(date: LocalDate): List<CalendarEventEntity> {
        val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return allEvents.value.filter { it.startTimeMillis in start until end }
    }

    // ─── Event CRUD ───────────────────────────────────────────────────────────
    fun createEvent(
        title: String,
        description: String = "",
        location: String = "",
        categoryId: Int = 1,
        startTimeMillis: Long,
        endTimeMillis: Long,
        isAllDay: Boolean = false,
        recurrenceType: RecurrenceType = RecurrenceType.NONE,
        recurrenceEndMillis: Long? = null,
        reminderType: String = "NONE",
        reminderMinutesBefore: Int = 10,
        reminderSound: String = "alarm_digital_alarm",
        announceLabelOnReminder: Boolean = false,
        reminders: List<ReminderItem> = emptyList(),
        priority: String = "NONE",
    ) {
        viewModelScope.launch {
            val newId = repository.createEvent(
                CalendarEventEntity(
                    title = title, description = description, location = location,
                    categoryId = categoryId, startTimeMillis = startTimeMillis,
                    endTimeMillis = endTimeMillis, isAllDay = isAllDay,
                    recurrenceType = recurrenceType.name, recurrenceEndMillis = recurrenceEndMillis,
                    reminderType = reminderType, reminderMinutesBefore = reminderMinutesBefore,
                    reminderSound = reminderSound, announceLabelOnReminder = announceLabelOnReminder,
                    remindersJson = reminders.toJson(), priority = priority,
                )
            )
            val saved = repository.getEventById(newId.toInt())
            if (saved != null) EventReminderScheduler.schedule(context, saved)
        }
    }

    fun updateEvent(event: CalendarEventEntity) {
        viewModelScope.launch {
            repository.updateEvent(event)
            EventReminderScheduler.cancel(context, event.id)
            EventReminderScheduler.schedule(context, event)
        }
    }

    fun deleteEvent(event: CalendarEventEntity) {
        viewModelScope.launch {
            // Cancel event reminder alarms
            EventReminderScheduler.cancel(context, event.id)
            // Cancel day-of attendee alarms + delete attendee rows (manual cascade)
            attendeeNotificationHelper.cancelDayOfAlarms(context, event.id)
            attendeeRepository.deleteAllForEvent(event.id)
            repository.deleteEvent(event)
        }
    }

    // ─── Attendee functions ───────────────────────────────────────────────────

    /**
     * Create event then immediately save attendees, send instant invites,
     * and schedule day-of alarms — all in one coroutine so eventId is
     * available before attendees are inserted.
     */
    fun createEventWithAttendees(
        title: String,
        description: String = "",
        location: String = "",
        categoryId: Int = 1,
        startTimeMillis: Long,
        endTimeMillis: Long,
        isAllDay: Boolean = false,
        recurrenceType: RecurrenceType = RecurrenceType.NONE,
        recurrenceEndMillis: Long? = null,
        reminderType: String = "NONE",
        reminderMinutesBefore: Int = 10,
        reminderSound: String = "alarm_digital_alarm",
        announceLabelOnReminder: Boolean = false,
        reminders: List<ReminderItem> = emptyList(),
        priority: String = "NONE",
        attendees: List<EventAttendeeEntity> = emptyList(),
    ) {
        viewModelScope.launch {
            // 1. Create the event — get real DB id back
            val newId = repository.createEvent(
                CalendarEventEntity(
                    title = title, description = description, location = location,
                    categoryId = categoryId, startTimeMillis = startTimeMillis,
                    endTimeMillis = endTimeMillis, isAllDay = isAllDay,
                    recurrenceType = recurrenceType.name, recurrenceEndMillis = recurrenceEndMillis,
                    reminderType = reminderType, reminderMinutesBefore = reminderMinutesBefore,
                    reminderSound = reminderSound, announceLabelOnReminder = announceLabelOnReminder,
                    remindersJson = reminders.toJson(), priority = priority,
                )
            ).toInt()

            // 2. Schedule event reminder (existing behaviour)
            val saved = repository.getEventById(newId)
            if (saved != null) EventReminderScheduler.schedule(context, saved)

            // 3. Save attendees with the real eventId
            if (attendees.isNotEmpty()) {
                attendeeRepository.saveAttendees(newId, attendees)

                // 4. Send instant invite SMS + open email compose
                attendeeNotificationHelper.sendInstantInvites(
                    context         = context,
                    attendees       = attendees,
                    eventTitle      = title,
                    startTimeMillis = startTimeMillis,
                )

                // 5. Schedule day-of 9AM alarm
                attendeeNotificationHelper.scheduleDayOfAlarms(
                    context         = context,
                    eventId         = newId,
                    attendees       = attendees,
                    startTimeMillis = startTimeMillis,
                    eventTitle      = title,
                )
            }
        }
    }

    /** Save (replace) attendees for an existing event — called on edit. */
    fun saveAttendees(eventId: Int, attendees: List<EventAttendeeEntity>) {
        viewModelScope.launch {
            attendeeRepository.saveAttendees(eventId, attendees)
            // Reschedule day-of alarms with updated attendee list
            val event = repository.getEventById(eventId) ?: return@launch
            attendeeNotificationHelper.cancelDayOfAlarms(context, eventId)
            attendeeNotificationHelper.scheduleDayOfAlarms(
                context         = context,
                eventId         = eventId,
                attendees       = attendees,
                startTimeMillis = event.startTimeMillis,
                eventTitle      = event.title,
            )
        }
    }

    /**
     * One-shot attendee load — used by CreateEventScreen LaunchedEffect
     * to pre-fill chips when opening in edit mode.
     */
    suspend fun getAttendeesOnce(eventId: Int): List<EventAttendeeEntity> =
        attendeeRepository.getForEventOnce(eventId)

    /**
     * Search device contacts by name or email (debounced 300ms in the UI).
     * Returns up to 10 ContactResult items.
     */
    suspend fun searchContacts(query: String): List<ContactResult> =
        attendeeRepository.searchContacts(query)

    /** Frequently/recently contacted people for AddPeopleScreen suggestions. */
    suspend fun getSuggestedContacts(): List<ContactResult> =
        attendeeRepository.getSuggestedContacts()

    /**
     * Send instant invites to a specific subset of attendees.
     * Called by the renotify dialog (edit mode) for newly added people only.
     */
    fun sendInvitesToAttendees(
        eventId: Int,
        attendees: List<EventAttendeeEntity>,
        context: Context,
    ) {
        viewModelScope.launch {
            val event = repository.getEventById(eventId) ?: return@launch
            attendeeNotificationHelper.sendInstantInvites(
                context         = context,
                attendees       = attendees,
                eventTitle      = event.title,
                startTimeMillis = event.startTimeMillis,
            )
        }
    }

    fun createCategory(name: String, colorHex: String, emoji: String) {
        viewModelScope.launch {
            repository.createCategory(EventCategoryEntity(name = name, colorHex = colorHex, emoji = emoji))
        }
    }

    fun deleteCategory(category: EventCategoryEntity) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    fun getCategoryById(id: Int): EventCategoryEntity? = allCategories.value.find { it.id == id }
}
