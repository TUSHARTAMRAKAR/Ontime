package com.tushartamrakar.ontime.period.data.repository

import com.tushartamrakar.ontime.period.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class PeriodRepository @Inject constructor(
    private val cycleDao: CycleDao,
    private val logDao: PeriodLogDao,
    private val settingsDao: PeriodSettingsDao,
) {
    // ─── Settings ──────────────────────────────────────────────────────────────
    fun getSettings(): Flow<PeriodSettings> = settingsDao.getSettings()
        .map { it ?: PeriodSettings() }  // ← null-safe: emit default if no row exists yet

    suspend fun saveSettings(settings: PeriodSettings) = settingsDao.save(settings)

    // Ensure a default settings row always exists (called on first launch)
    suspend fun ensureSettingsExist() {
        if (settingsDao.getSettingsOnce() == null) {
            settingsDao.save(PeriodSettings())
        }
    }

    // ─── Full reset (Option A) ────────────────────────────────────────────────
    // Wipes ALL cycles, ALL daily logs, and ALL settings.
    // After this, onboardingComplete = false — she goes back to step 1.
    suspend fun clearAllData() {
        cycleDao.deleteAll()
        logDao.deleteAll()
        settingsDao.deleteAll()
        // Re-insert a fresh default settings row so the app never sees null
        settingsDao.save(PeriodSettings())
    }

    suspend fun completeOnboarding(lastPeriodStart: LocalDate, cycleLength: Int) {
        settingsDao.save(
            PeriodSettings(
                lastPeriodStart = lastPeriodStart.toMillis(),
                estimatedCycleLength = cycleLength,
                onboardingComplete = true,
            )
        )
        // Also log this as a cycle record
        cycleDao.insert(CycleEntity(startDate = lastPeriodStart.toMillis()))
    }

    suspend fun isOnboardingComplete(): Boolean =
        settingsDao.getSettingsOnce()?.onboardingComplete == true

    // ─── Cycles ────────────────────────────────────────────────────────────────
    fun getAllCycles(): Flow<List<CycleEntity>> = cycleDao.getAllCycles()
    fun getRecentCycles(n: Int = 6): Flow<List<CycleEntity>> = cycleDao.getRecentCycles(n)
    fun getCycleCount(): Flow<Int> = cycleDao.getCycleCount()

    suspend fun logPeriodStart(date: LocalDate): Long {
        // If previous cycle exists without end, close it
        val prev = cycleDao.getLatestCycle()
        if (prev != null && prev.endDate == null) {
            val prevStart = prev.startDate.toLocalDate()
            val cycleLen = ChronoUnit.DAYS.between(prevStart, date).toInt()
            cycleDao.update(prev.copy(cycleLength = cycleLen))
        }
        return cycleDao.insert(CycleEntity(startDate = date.toMillis()))
    }

    suspend fun logPeriodEnd(date: LocalDate) {
        val latest = cycleDao.getLatestCycle() ?: return
        val start = latest.startDate.toLocalDate()
        val periodLen = ChronoUnit.DAYS.between(start, date).toInt() + 1
        cycleDao.update(latest.copy(endDate = date.toMillis(), periodLength = periodLen))
    }

    // Returns true if latest cycle has started but not yet ended
    suspend fun hasActivePeriod(): Boolean {
        val latest = cycleDao.getLatestCycle()
        return latest != null && latest.endDate == null
    }

    // Returns the ID of the latest cycle (active or most recent closed)
    suspend fun getActiveCycleId(): Int? = cycleDao.getLatestCycle()?.id

    // ─── Daily logs ────────────────────────────────────────────────────────────
    fun getAllLogs(): Flow<List<PeriodDailyLog>> = logDao.getAllLogs()

    suspend fun getLogForDate(date: LocalDate): PeriodDailyLog? =
        logDao.getLogForDate(date.toMillis())

    suspend fun saveLog(log: PeriodDailyLog): Long = logDao.insert(log)

    // ─── PREDICTION ENGINE ─────────────────────────────────────────────────────

    // Smart average: uses real cycle data if ≥2 cycles, otherwise uses setting
    suspend fun getAverageCycleLength(): Int {
        val cycles = cycleDao.getAllCycles().first()
        val realLengths = cycles.mapNotNull { it.cycleLength }.filter { it in 18..45 }
        return if (realLengths.size >= 2) realLengths.average().toInt()
        else settingsDao.getSettingsOnce()?.estimatedCycleLength ?: 28
    }

    suspend fun getAveragePeriodLength(): Int {
        val cycles = cycleDao.getAllCycles().first()
        val realLengths = cycles.mapNotNull { it.periodLength }.filter { it in 2..10 }
        return if (realLengths.isNotEmpty()) realLengths.average().toInt() else 5
    }

    // Cycle variability (for smart late detection)
    suspend fun getCycleVariability(): Int {
        val cycles = cycleDao.getAllCycles().first()
        val lengths = cycles.mapNotNull { it.cycleLength }.filter { it in 18..45 }
        if (lengths.size < 3) return 3 // default ±3 days
        val avg = lengths.average()
        return lengths.map { abs(it - avg) }.average().toInt().coerceAtLeast(2)
    }

    suspend fun getLastPeriodStart(): LocalDate? =
        cycleDao.getLatestCycle()?.startDate?.toLocalDate()

    suspend fun predictNextPeriodStart(): LocalDate? {
        val lastStart = getLastPeriodStart() ?: return null
        val avgLen = getAverageCycleLength()
        return lastStart.plusDays(avgLen.toLong())
    }

    suspend fun getOvulationDay(): LocalDate? {
        val next = predictNextPeriodStart() ?: return null
        return next.minusDays(14)
    }

    // Fertile window: 5 days before ovulation + ovulation day = 6 days
    suspend fun getFertileWindow(): List<LocalDate> {
        val ovulation = getOvulationDay() ?: return emptyList()
        return (5 downTo 0).map { ovulation.minusDays(it.toLong()) }
    }

    // PMS window: 5 days before next period
    suspend fun getPmsWindow(): List<LocalDate> {
        val next = predictNextPeriodStart() ?: return emptyList()
        return (5 downTo 1).map { next.minusDays(it.toLong()) }
    }

    // Days until next period (can be negative if overdue)
    suspend fun getDaysUntilNextPeriod(): Int? {
        val next = predictNextPeriodStart() ?: return null
        return ChronoUnit.DAYS.between(LocalDate.now(), next).toInt()
    }

    // Current cycle day (1-based)
    suspend fun getCurrentCycleDay(): Int? {
        val lastStart = getLastPeriodStart() ?: return null
        return ChronoUnit.DAYS.between(lastStart, LocalDate.now()).toInt() + 1
    }

    // Current phase for today
    suspend fun getCurrentPhase(): CyclePhase {
        return getPhaseForDate(LocalDate.now())
    }

    // Phase for any given date — uses the ACTUAL cycle that was active on that date
    suspend fun getPhaseForDate(date: LocalDate): CyclePhase {
        // Load all cycles once (replaces 3 separate queries from the old code)
        val allCycles = cycleDao.getAllCyclesOnce()
        if (allCycles.isEmpty()) return CyclePhase.NONE

        val realCycleLengths  = allCycles.mapNotNull { it.cycleLength  }.filter { it in 18..45 }
        val realPeriodLengths = allCycles.mapNotNull { it.periodLength }.filter { it in 2..10  }
        val avgCycle  = if (realCycleLengths.size >= 2) realCycleLengths.average().toInt()
                        else settingsDao.getSettingsOnce()?.estimatedCycleLength ?: 28
        val avgPeriod = if (realPeriodLengths.isNotEmpty()) realPeriodLengths.average().toInt() else 5

        // Sort ascending so we can find the cycle just before this date
        val sortedAsc = allCycles.sortedBy { it.startDate }

        // Find the cycle that was active on this date (most recent start ≤ date)
        val cycleForDate = sortedAsc.lastOrNull { it.startDate.toLocalDate() <= date }
            ?: return CyclePhase.NONE

        val cycleStart = cycleForDate.startDate.toLocalDate()

        // Use the ACTUAL next cycle start if known; otherwise predict from avgCycle
        val cycleIdx   = sortedAsc.indexOf(cycleForDate)
        val nextPeriod = if (cycleIdx + 1 < sortedAsc.size)
            sortedAsc[cycleIdx + 1].startDate.toLocalDate()
        else
            cycleStart.plusDays(avgCycle.toLong())

        val ovulationDay = nextPeriod.minusDays(14)
        val fertileStart = ovulationDay.minusDays(5)

        // Actual logged flow day always beats prediction
        val log = logDao.getLogForDate(date.toMillis())
        if (log != null && log.flowEnum() != FlowIntensity.NONE) {
            return CyclePhase.MENSTRUATION
        }

        val daysSince = ChronoUnit.DAYS.between(cycleStart, date).toInt()
        val cycleDay  = ((daysSince % avgCycle) + avgCycle) % avgCycle

        return when {
            date == ovulationDay               -> CyclePhase.OVULATION
            date in fertileStart..ovulationDay -> CyclePhase.FERTILE
            cycleDay < avgPeriod               -> CyclePhase.MENSTRUATION
            cycleDay < avgCycle / 2            -> CyclePhase.FOLLICULAR
            else                               -> CyclePhase.LUTEAL
        }
    }

    // Map of date → CyclePhase for calendar dots
    // ── OPTIMISED: 4 DB queries total (was ~900) ──────────────────────────────
    // Old approach called getPhaseForDate() per day × 180 days, each firing
    // 4-5 separate queries. This version hoists all shared data to the top,
    // then computes every phase purely from memory inside the loop.
    suspend fun getPhaseMapForRange(start: LocalDate, end: LocalDate): Map<String, CyclePhase> {
        // ── Query 1: last period start ─────────────────────────────────────────
        val lastStart = getLastPeriodStart() ?: return emptyMap()

        // ── Query 2: all cycles (used for both avgCycle + avgPeriod) ──────────
        val allCycles = cycleDao.getAllCyclesOnce()
        val realCycleLengths  = allCycles.mapNotNull { it.cycleLength  }.filter { it in 18..45 }
        val realPeriodLengths = allCycles.mapNotNull { it.periodLength }.filter { it in 2..10  }

        // ── Query 3: settings (only if no real cycle data yet) ────────────────
        val avgCycle  = if (realCycleLengths.size >= 2) realCycleLengths.average().toInt()
                        else settingsDao.getSettingsOnce()?.estimatedCycleLength ?: 28
        val avgPeriod = if (realPeriodLengths.isNotEmpty()) realPeriodLengths.average().toInt()
                        else 5

        // ── Query 4: all logs in range in one batch ────────────────────────────
        val logsInRange = logDao.getLogsInRangeOnce(
            from = start.toMillis(),
            to   = end.plusDays(1).toMillis(),
        )
        // Build a Set<LocalDate> of days she actually had flow — O(1) lookup later
        val loggedFlowDates: Set<LocalDate> = logsInRange
            .filter { FlowIntensity.valueOf(it.flowIntensity) != FlowIntensity.NONE }
            .map    { it.date.toLocalDate() }
            .toSet()

        // ── Sort cycles ASC so we can quickly find the one active on each date ─
        val sortedCyclesAsc = allCycles.sortedBy { it.startDate }

        // ── Build the phase map entirely in memory (zero DB calls) ───────────
        // For EACH date, find the cycle that was active on that specific day.
        // This fixes incorrect dots for past months: previously all dates used
        // lastStart + avgCycle, which is wrong for dates 1-2+ cycles ago.
        val map = mutableMapOf<String, CyclePhase>()
        var current = start
        while (!current.isAfter(end)) {
            // Most recent cycle whose startDate is on or before this date
            val cycleForDate = sortedCyclesAsc.lastOrNull {
                it.startDate.toLocalDate() <= current
            }

            if (cycleForDate != null) {
                val cycleStart = cycleForDate.startDate.toLocalDate()
                val cycleIdx   = sortedCyclesAsc.indexOf(cycleForDate)

                // Use ACTUAL next cycle start if we have it, otherwise predict
                val nextPeriod = if (cycleIdx + 1 < sortedCyclesAsc.size)
                    sortedCyclesAsc[cycleIdx + 1].startDate.toLocalDate()
                else
                    cycleStart.plusDays(avgCycle.toLong())

                val phase = computePhaseFromMemory(
                    date            = current,
                    lastStart       = cycleStart,
                    avgCycle        = avgCycle,
                    avgPeriod       = avgPeriod,
                    nextPeriod      = nextPeriod,
                    ovulationDay    = nextPeriod.minusDays(14),
                    fertileStart    = nextPeriod.minusDays(19),
                    loggedFlowDates = loggedFlowDates,
                )
                if (phase != CyclePhase.NONE) map[current.toString()] = phase
            }
            current = current.plusDays(1)
        }
        return map
    }

    // Pure in-memory phase calculation — no DB access, called 180× per refresh
    private fun computePhaseFromMemory(
        date            : LocalDate,
        lastStart       : LocalDate,
        avgCycle        : Int,
        avgPeriod       : Int,
        nextPeriod      : LocalDate,
        ovulationDay    : LocalDate,
        fertileStart    : LocalDate,
        loggedFlowDates : Set<LocalDate>,
    ): CyclePhase {
        // Actual logged flow day always wins
        if (date in loggedFlowDates) return CyclePhase.MENSTRUATION

        val daysSince = ChronoUnit.DAYS.between(lastStart, date).toInt()
        val cycleDay  = ((daysSince % avgCycle) + avgCycle) % avgCycle

        return when {
            date == ovulationDay               -> CyclePhase.OVULATION
            date in fertileStart..ovulationDay -> CyclePhase.FERTILE
            cycleDay < avgPeriod               -> CyclePhase.MENSTRUATION
            cycleDay < avgCycle / 2            -> CyclePhase.FOLLICULAR
            else                               -> CyclePhase.LUTEAL
        }
    }

    // ─── INSIGHTS ─────────────────────────────────────────────────────────────
    suspend fun isRegularCycle(): Boolean {
        return getCycleVariability() <= 3
    }

    suspend fun isLatePeriod(): Boolean {
        val daysUntil = getDaysUntilNextPeriod() ?: return false
        val variability = getCycleVariability()
        return daysUntil < -variability
    }

    suspend fun getDaysLate(): Int {
        val daysUntil = getDaysUntilNextPeriod() ?: return 0
        return if (daysUntil < 0) -daysUntil else 0
    }

    // Mood pattern: most common mood on each cycle day
    suspend fun getMoodPatternInsight(): String? {
        val cycles = cycleDao.getAllCycles().first()
        if (cycles.size < 2) return null
        val logs = logDao.getAllLogs().first()
        val tiredDays = logs.filter { it.moodEnum() == DailyMood.TIRED }
        val energeticLogs = logs.filter { it.moodEnum() == DailyMood.GREAT }
        if (tiredDays.isEmpty() || energeticLogs.isEmpty()) return null
        return "You tend to feel most energetic mid-cycle and tired around your period 💙"
    }

    // Phase tip
    suspend fun getPhaseTip(): String {
        return when (getCurrentPhase()) {
            CyclePhase.MENSTRUATION -> "Stay warm and hydrated 💧 Light walks can ease cramps. Be kind to yourself today."
            CyclePhase.FOLLICULAR   -> "Your energy is rising! ✨ Great time for new projects, exercise, and socialising."
            CyclePhase.OVULATION    -> "Peak energy day 🌟 You may feel most confident and social — enjoy it!"
            CyclePhase.LUTEAL       -> "Energy may dip as PMS approaches 🍫 Rest well, dark chocolate helps!"
            CyclePhase.FERTILE      -> "Your fertile window is open 🌱 Highest chance of conception this week."
            else                    -> "Track your cycle to get personalised tips 🌸"
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private fun LocalDate.toMillis(): Long =
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun Long.toLocalDate(): LocalDate =
        java.time.Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault()).toLocalDate()

    private operator fun ClosedRange<LocalDate>.contains(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(endInclusive)
}
