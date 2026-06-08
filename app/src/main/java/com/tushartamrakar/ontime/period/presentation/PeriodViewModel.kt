package com.tushartamrakar.ontime.period.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tushartamrakar.ontime.period.data.local.*
import com.tushartamrakar.ontime.period.data.repository.PeriodRepository
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.tushartamrakar.ontime.period.notification.PeriodNotificationScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PeriodViewModel @Inject constructor(
    private val repository: PeriodRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // ─── Settings ──────────────────────────────────────────────────────────────
    // settingsLoaded: null = loading, false = loaded+not done, true = loaded+done
    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded

    val settings: StateFlow<PeriodSettings> = repository.getSettings()
        .onEach { _settingsLoaded.value = true }   // ← mark loaded once DB emits
        .stateIn(viewModelScope, SharingStarted.Eagerly, PeriodSettings())  // ← Eagerly: never resets

    // ─── Cycles ────────────────────────────────────────────────────────────────
    val recentCycles: StateFlow<List<CycleEntity>> = repository.getRecentCycles(6)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cycleCount: StateFlow<Int> = repository.getCycleCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ─── Predictions (loaded fresh when needed) ────────────────────────────────
    private val _currentPhase = MutableStateFlow(CyclePhase.NONE)
    val currentPhase: StateFlow<CyclePhase> = _currentPhase

    private val _daysUntilPeriod = MutableStateFlow<Int?>(null)
    val daysUntilPeriod: StateFlow<Int?> = _daysUntilPeriod

    private val _currentCycleDay = MutableStateFlow<Int?>(null)
    val currentCycleDay: StateFlow<Int?> = _currentCycleDay

    private val _nextPeriodDate = MutableStateFlow<LocalDate?>(null)
    val nextPeriodDate: StateFlow<LocalDate?> = _nextPeriodDate

    private val _ovulationDate = MutableStateFlow<LocalDate?>(null)
    val ovulationDate: StateFlow<LocalDate?> = _ovulationDate

    private val _fertileWindow = MutableStateFlow<List<LocalDate>>(emptyList())
    val fertileWindow: StateFlow<List<LocalDate>> = _fertileWindow

    private val _averageCycleLength = MutableStateFlow(28)
    val averageCycleLength: StateFlow<Int> = _averageCycleLength

    private val _averagePeriodLength = MutableStateFlow(5)
    val averagePeriodLength: StateFlow<Int> = _averagePeriodLength

    private val _isRegular = MutableStateFlow(true)
    val isRegular: StateFlow<Boolean> = _isRegular

    private val _isLate = MutableStateFlow(false)
    val isLate: StateFlow<Boolean> = _isLate

    private val _daysLate = MutableStateFlow(0)
    val daysLate: StateFlow<Int> = _daysLate

    private val _phaseTip = MutableStateFlow("")
    val phaseTip: StateFlow<String> = _phaseTip

    private val _moodInsight = MutableStateFlow<String?>(null)
    val moodInsight: StateFlow<String?> = _moodInsight

    // ─── Selected date for log sheet ───────────────────────────────────────────
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate

    private val _selectedDateLog = MutableStateFlow<PeriodDailyLog?>(null)
    val selectedDateLog: StateFlow<PeriodDailyLog?> = _selectedDateLog

    // ─── Calendar view month ───────────────────────────────────────────────────
    private val _calendarMonth = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    val calendarMonth: StateFlow<LocalDate> = _calendarMonth

    private val _phaseMap = MutableStateFlow<Map<String, CyclePhase>>(emptyMap())
    val phaseMap: StateFlow<Map<String, CyclePhase>> = _phaseMap

    init {
        viewModelScope.launch {
            repository.ensureSettingsExist()  // ← create default row if first launch
        }
        refreshPredictions()
    }

    fun refreshPredictions() {
        viewModelScope.launch {
            _currentPhase.value = repository.getCurrentPhase()
            _daysUntilPeriod.value = repository.getDaysUntilNextPeriod()
            _currentCycleDay.value = repository.getCurrentCycleDay()
            _nextPeriodDate.value = repository.predictNextPeriodStart()
            _ovulationDate.value = repository.getOvulationDay()
            _fertileWindow.value = repository.getFertileWindow()
            _averageCycleLength.value = repository.getAverageCycleLength()
            _averagePeriodLength.value = repository.getAveragePeriodLength()
            _isRegular.value = repository.isRegularCycle()

            // ── Compute late status BEFORE scheduling so it can be passed in ─────
            _isLate.value  = repository.isLatePeriod()
            _daysLate.value = repository.getDaysLate()

            // ── Schedule / reschedule all period notifications ────────────────
            // Called here so notifications always reflect the latest predictions
            PeriodNotificationScheduler.scheduleAll(
                context           = context,
                nextPeriodDate    = _nextPeriodDate.value,
                ovulationDate     = _ovulationDate.value,
                fertileWindowStart = _fertileWindow.value.firstOrNull(),
                showFertileWindow = settings.value.showFertileWindow,
                remindDaysBefore  = settings.value.remindDaysBefore,
                isLate            = _isLate.value,
                daysLate          = _daysLate.value,
            )
            _phaseTip.value = repository.getPhaseTip()
            _moodInsight.value = repository.getMoodPatternInsight()
            loadPhaseMap()
        }
    }

    private fun loadPhaseMap() {
        viewModelScope.launch {
            val month = _calendarMonth.value
            val start = month.withDayOfMonth(1).minusDays(7)
            val end = month.withDayOfMonth(month.lengthOfMonth()).plusDays(7)
            _phaseMap.value = repository.getPhaseMapForRange(start, end)
        }
    }

    fun navigateMonth(forward: Boolean) {
        _calendarMonth.value = if (forward)
            _calendarMonth.value.plusMonths(1)
        else
            _calendarMonth.value.minusMonths(1)
        loadPhaseMap()
    }

    // ─── Log sheet ─────────────────────────────────────────────────────────────
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        viewModelScope.launch {
            _selectedDateLog.value = repository.getLogForDate(date)
        }
    }

    fun clearSelectedDate() {
        _selectedDate.value = null
        _selectedDateLog.value = null
    }

    fun saveLog(
        date: LocalDate,
        flow: FlowIntensity,
        symptoms: List<String>,
        mood: DailyMood,
        notes: String?,
        temperature: Float? = null,
    ) {
        viewModelScope.launch {
            val existing = repository.getLogForDate(date)

            // ── Determine cycleId AND manage cycle state in one pass ──────────
            // Must happen BEFORE saving the log so cycleId is available.
            // logPeriodStart() returns the new cycle's Room id (Long),
            // which we cast to Int for the FK field.
            val cycleId: Int? = when {

                // Flow logged — link to active cycle or start a new one
                flow != FlowIntensity.NONE -> {
                    val latest = repository.getLastPeriodStart()
                    if (latest == null || latest.plusDays(10).isBefore(date)) {
                        // New cycle: create it first, return its id
                        repository.logPeriodStart(date).toInt()
                    } else {
                        // Continuation: just get the existing active cycle's id
                        repository.getActiveCycleId()
                    }
                }

                // Flow = None after active period — close the period, keep its id
                // Example: flow days 1–4 June → logs None on 5 June
                //   → endDate = 4 June, periodLength = 4 days ✓
                flow == FlowIntensity.NONE && repository.hasActivePeriod() -> {
                    val cycleId = repository.getActiveCycleId()
                    val periodStart = repository.getLastPeriodStart()
                    if (periodStart != null && periodStart.isBefore(date)) {
                        val endDate = date.minusDays(1)
                        if (!endDate.isBefore(periodStart)) {
                            repository.logPeriodEnd(endDate)
                        }
                    }
                    cycleId
                }

                // Flow = None, no active period — still link to most recent cycle
                else -> repository.getActiveCycleId()
            }

            // ── Save log with the correct cycleId ────────────────────────────
            val log = PeriodDailyLog(
                id            = existing?.id ?: 0,
                date          = date.atStartOfDay(java.time.ZoneId.systemDefault())
                                    .toInstant().toEpochMilli(),
                cycleId       = cycleId,
                flowIntensity = flow.name,
                symptoms      = symptoms.joinToString(","),
                mood          = mood.name,
                notes         = notes?.takeIf { it.isNotBlank() },
                temperature   = temperature,
            )
            repository.saveLog(log)

            clearSelectedDate()
            refreshPredictions()
        }
    }

    fun logPeriodStartToday() {
        viewModelScope.launch {
            repository.logPeriodStart(LocalDate.now())
            refreshPredictions()
        }
    }

    // ─── Onboarding ────────────────────────────────────────────────────────────
    fun completeOnboarding(lastPeriodStart: LocalDate, cycleLength: Int) {
        viewModelScope.launch {
            repository.completeOnboarding(lastPeriodStart, cycleLength)
            refreshPredictions()
        }
    }

    // ─── Settings ──────────────────────────────────────────────────────────────
    fun toggleFertileWindow() {
        viewModelScope.launch {
            val current = settings.value
            repository.saveSettings(current.copy(showFertileWindow = !current.showFertileWindow))
            refreshPredictions()   // reschedules notifications with new fertile toggle
        }
    }

    fun updateCycleLength(length: Int) {
        viewModelScope.launch {
            repository.saveSettings(settings.value.copy(estimatedCycleLength = length))
            refreshPredictions()   // reschedules notifications with new cycle length
        }
    }

    fun updateRemindDays(days: Int) {
        viewModelScope.launch {
            repository.saveSettings(settings.value.copy(remindDaysBefore = days))
            refreshPredictions()   // reschedules notifications with new reminder day count
        }
    }

    // ─── Full reset ───────────────────────────────────────────────────────────
    // Wipes everything and cancels all scheduled notifications.
    // The settingsLoaded+onboardingComplete guard in PeriodTrackerScreen will
    // then automatically navigate her back to onboarding.
    fun clearAllData() {
        viewModelScope.launch {
            PeriodNotificationScheduler.cancelAll(context)  // cancel alarms first
            repository.clearAllData()                       // wipe DB
            // No need to navigate manually — the LaunchedEffect(settingsLoaded)
            // in PeriodTrackerScreen will fire when settings resets to
            // onboardingComplete=false and redirect to period_onboarding
        }
    }
}
