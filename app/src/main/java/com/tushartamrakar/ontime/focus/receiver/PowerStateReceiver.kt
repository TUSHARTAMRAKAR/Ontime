package com.tushartamrakar.ontime.focus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tushartamrakar.ontime.focus.accessibility.FocusSessionPrefs

/**
 * PowerStateReceiver
 *
 * Listens for:
 *  - ACTION_SCREEN_OFF   → screen locked / power button pressed
 *  - ACTION_SHUTDOWN     → device is shutting down
 *
 * During active focus sessions, these events are logged and can be used to:
 *  - Auto-pause the session timer so time isn't wasted while screen is off
 *  - Trigger a "Welcome back" prompt when screen turns back on
 *  - Persist session state before shutdown so it survives a reboot
 *
 * NOTE: This receiver must be registered dynamically (not in manifest)
 * because ACTION_SCREEN_OFF cannot be received by manifest-registered receivers.
 * Register it in FocusTimerService.onCreate() and unregister in onDestroy().
 *
 * Example in FocusTimerService:
 *   private val powerReceiver = PowerStateReceiver()
 *
 *   override fun onCreate() {
 *       val filter = IntentFilter().apply {
 *           addAction(Intent.ACTION_SCREEN_OFF)
 *           addAction(Intent.ACTION_SHUTDOWN)
 *       }
 *       registerReceiver(powerReceiver, filter)
 *   }
 *
 *   override fun onDestroy() {
 *       unregisterReceiver(powerReceiver)
 *   }
 */
class PowerStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PowerStateReceiver"
    }

    /** Called by FocusTimerService to inject a callback for screen off events. */
    var onScreenOff: (() -> Unit)? = null

    /** Called by FocusTimerService to inject a callback for shutdown events. */
    var onShutdown: (() -> Unit)? = null

    override fun onReceive(context: Context, intent: Intent) {
        val sessionActive = FocusSessionPrefs.isSessionActive(context)

        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                Log.d(TAG, "Screen off — session active: $sessionActive")
                if (sessionActive) {
                    onScreenOff?.invoke()
                }
            }

            Intent.ACTION_SHUTDOWN -> {
                Log.d(TAG, "Device shutting down — session active: $sessionActive")
                if (sessionActive) {
                    // Persist any in-flight session state before the device dies
                    onShutdown?.invoke()
                }
            }
        }
    }
}
