package com.tushartamrakar.ontime.focus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── Technique type ────────────────────────────────────────────────────────────
// Stored as String in Room — safe against obfuscation and future renaming.

enum class TechniqueType { POMODORO, STOPWATCH, CUSTOM }

// ─── Custom sub-presets ────────────────────────────────────────────────────────

enum class CustomPreset(val workMin: Int, val breakMin: Int, val displayLabel: String) {
    DEEP_WORK(workMin = 90,  breakMin = 20, displayLabel = "Deep Work"),
    SPRINT(workMin   = 15,  breakMin = 3,  displayLabel = "Sprint"),
    MANUAL(workMin   = 25,  breakMin = 5,  displayLabel = "Manual"),
}

/**
 * Singleton settings row — always id = 1.
 *
 * FocusRepository.getSettings() returns a Flow of this entity.
 * FocusTimerService reads these values when a session starts.
 * FocusViewModel exposes them as a StateFlow so the UI reacts to changes.
 *
 * ── SCHEMA v1 → v2 ────────────────────────────────────────────────────────────
 * Added: techniqueType, lastUsedTechnique, customPreset, strictMode, blockHomeScreen
 *
 * ACTION REQUIRED: bump `version = 1` to `version = 2` in FocusDatabase.kt.
 * fallbackToDestructiveMigration() handles it automatically (dev build OK).
 */
@Entity(tableName = "focus_settings")
data class FocusSettingsEntity(
    @PrimaryKey val id: Int = 1,               // always 1 — singleton

    // ─── Timer durations ──────────────────────────────────────────────────────
    val workMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,      // after N work sessions → long break

    // ─── Sound ────────────────────────────────────────────────────────────────
    val selectedSound: String = AmbientSound.SILENCE.name,

    // ─── Goals ────────────────────────────────────────────────────────────────
    val dailyGoalSessions: Int = 4,

    // ─── Blocking ─────────────────────────────────────────────────────────────
    val adultFilterEnabled: Boolean = false,
    val appBlockingEnabled: Boolean = true,

    // ─── DND ─────────────────────────────────────────────────────────────────
    val enableDndDuringFocus: Boolean = true,

    // ─── Technique — NEW in v2 ────────────────────────────────────────────────
    val techniqueType: String = TechniqueType.POMODORO.name,       // active technique
    val lastUsedTechnique: String = TechniqueType.POMODORO.name,   // pre-selects tab on sheet open
    val customPreset: String = CustomPreset.MANUAL.name,           // active custom sub-preset

    // ─── Deep focus — NEW in v2 ───────────────────────────────────────────────
    val strictMode: Boolean = false,           // when ON, Stop is disabled mid WORK session
    val blockHomeScreen: Boolean = false,      // block Android launcher via AccessibilityService
)
