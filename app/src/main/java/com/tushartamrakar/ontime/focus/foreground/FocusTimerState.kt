package com.tushartamrakar.ontime.focus.foreground

import com.tushartamrakar.ontime.focus.data.local.SessionType

/**
 * The complete state of the Pomodoro timer at any moment.
 * Emitted every second by FocusTimerService via a StateFlow.
 * FocusViewModel collects this and exposes it to the UI.
 */
sealed class FocusTimerState {

    /** No session running — timer shows the configured work duration. */
    object Idle : FocusTimerState()

    /**
     * A session is actively counting down.
     * @param secondsLeft      seconds remaining in the current phase
     * @param totalSeconds     total seconds this phase was set for (used to draw the ring)
     * @param phase            current phase: WORK, SHORT_BREAK, or LONG_BREAK
     * @param sessionIndex     which work session we're on today (1, 2, 3...)
     * @param taskLabel        what the user is focusing on (may be empty)
     * @param distractionsBlocked  how many app-open attempts blocked so far this session
     */
    data class Running(
        val secondsLeft: Int,
        val totalSeconds: Int,
        val phase: SessionType,
        val sessionIndex: Int,
        val taskLabel: String,
        val distractionsBlocked: Int = 0,
    ) : FocusTimerState() {
        /** 0.0 → 1.0 — drives the circular progress ring animation. */
        val progress: Float get() =
            if (totalSeconds == 0) 0f
            else 1f - (secondsLeft.toFloat() / totalSeconds.toFloat())
    }

    /**
     * Timer is paused mid-session.
     * UI shows resume + stop buttons instead of the running controls.
     */
    data class Paused(
        val secondsLeft: Int,
        val totalSeconds: Int,
        val phase: SessionType,
        val sessionIndex: Int,
        val taskLabel: String,
        val distractionsBlocked: Int = 0,
    ) : FocusTimerState() {
        val progress: Float get() =
            if (totalSeconds == 0) 0f
            else 1f - (secondsLeft.toFloat() / totalSeconds.toFloat())
    }

    /**
     * A phase just finished (timer hit 0).
     * Service saves the session to DB, then auto-advances to the next phase.
     * UI briefly shows a "Well done!" or "Break time!" message.
     */
    data class PhaseCompleted(
        val completedPhase: SessionType,
        val nextPhase: SessionType,
        val sessionIndex: Int,
    ) : FocusTimerState()
}

/** Convenience extension — true only when a WORK session is actively running. */
val FocusTimerState.isWorkSessionRunning: Boolean
    get() = this is FocusTimerState.Running && this.phase == SessionType.WORK
