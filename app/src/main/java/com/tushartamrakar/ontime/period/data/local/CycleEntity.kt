package com.tushartamrakar.ontime.period.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── Cycle phases ──────────────────────────────────────────────────────────────
enum class CyclePhase { MENSTRUATION, FOLLICULAR, OVULATION, LUTEAL, PREDICTED, FERTILE, NONE }

// ─── Flow intensity ────────────────────────────────────────────────────────────
enum class FlowIntensity { NONE, SPOTTING, LIGHT, MEDIUM, HEAVY }

// ─── Mood levels ──────────────────────────────────────────────────────────────
enum class DailyMood { GREAT, OKAY, TIRED, MOODY, SAD, CRAMPS }

// ─── Symptom list (stored as comma-separated string) ─────────────────────────
object Symptoms {
    const val CRAMPS         = "cramps"
    const val HEADACHE       = "headache"
    const val BLOATING       = "bloating"
    const val BACK_PAIN      = "back_pain"
    const val TENDER_BREASTS = "tender_breasts"
    const val FATIGUE        = "fatigue"
    const val FOOD_CRAVINGS  = "food_cravings"
    const val ACNE           = "acne"
    const val NAUSEA         = "nausea"
    const val MOOD_SWINGS    = "mood_swings"

    val all = listOf(
        CRAMPS to "Cramps",
        HEADACHE to "Headache",
        BLOATING to "Bloating",
        BACK_PAIN to "Back Pain",
        TENDER_BREASTS to "Tender Breasts",
        FATIGUE to "Fatigue",
        FOOD_CRAVINGS to "Food Cravings",
        ACNE to "Acne",
        NAUSEA to "Nausea",
        MOOD_SWINGS to "Mood Swings",
    )
}

// ─── One logged menstrual cycle ────────────────────────────────────────────────
@Entity(tableName = "cycles")
data class CycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startDate: Long,                            // period start timestamp
    val endDate: Long? = null,                      // period end timestamp (null = ongoing)
    val cycleLength: Int? = null,                   // days from this start to next start
    val periodLength: Int? = null,                  // actual period duration in days
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

// ─── Daily log entry ──────────────────────────────────────────────────────────
@Entity(tableName = "period_daily_logs")
data class PeriodDailyLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,                                 // date timestamp (day-level)
    val cycleId: Int? = null,                       // FK → cycles.id
    val flowIntensity: String = FlowIntensity.NONE.name,
    val symptoms: String = "",                      // comma-separated symptom keys
    val mood: String = DailyMood.OKAY.name,
    val notes: String? = null,
    val temperature: Float? = null,                 // basal body temperature (optional)
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun flowEnum(): FlowIntensity = FlowIntensity.valueOf(flowIntensity)
    fun moodEnum(): DailyMood = DailyMood.valueOf(mood)
    fun symptomList(): List<String> = if (symptoms.isBlank()) emptyList()
        else symptoms.split(",").map { it.trim() }
}

// ─── Settings (single row, id always = 1) ─────────────────────────────────────
@Entity(tableName = "period_settings")
data class PeriodSettings(
    @PrimaryKey val id: Int = 1,
    val lastPeriodStart: Long? = null,              // from onboarding
    val estimatedCycleLength: Int = 28,             // user's stated average
    val showFertileWindow: Boolean = true,          // toggle for fertility info
    val onboardingComplete: Boolean = false,
    val remindDaysBefore: Int = 3,                  // reminder X days before period
    val remindOverdueDays: Int = 4,                 // alert if this many days late
    val updatedAt: Long = System.currentTimeMillis(),
)
