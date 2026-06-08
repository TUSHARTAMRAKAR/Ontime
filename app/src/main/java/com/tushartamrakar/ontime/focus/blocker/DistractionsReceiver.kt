package com.tushartamrakar.ontime.focus.blocker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tushartamrakar.ontime.focus.foreground.FocusTimerService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Receives the INCREMENT_DISTRACTIONS broadcast sent by BlockerAccessibilityService
 * and forwards it to FocusTimerService to increment the distraction counter.
 *
 * WHY THIS EXISTS
 * ────────────────
 * BlockerAccessibilityService cannot bind to FocusTimerService directly since
 * it's not a Hilt-injectable component. A broadcast is the cleanest IPC bridge.
 * This receiver is internal-only (android:exported="false").
 *
 * FocusTimerService handles the "focus.INCREMENT_DISTRACTIONS" action in
 * onStartCommand by calling incrementDistractions().
 */
@AndroidEntryPoint
class DistractionsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == BlockerAccessibilityService.DISTRACTIONS_INCREMENT_ACTION) {
            context.startService(
                Intent(context, FocusTimerService::class.java).apply {
                    action = "focus.INCREMENT_DISTRACTIONS"
                }
            )
        }
    }
}
