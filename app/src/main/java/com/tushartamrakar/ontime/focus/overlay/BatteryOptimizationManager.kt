package com.tushartamrakar.ontime.focus.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * BatteryOptimizationManager
 *
 * Android's Doze mode and battery optimization aggressively kill background
 * processes to save power. This is the #1 cause of missed alarms in
 * productivity apps. When Ontime is battery-optimized:
 *
 *  - AlarmManager alarms may be delayed or silently dropped
 *  - FocusTimerService foreground service may be killed mid-session
 *  - Period tracker reminders may never fire
 *  - BootCompletedReceiver may be delayed or skipped
 *
 * The fix: request exclusion from battery optimization so Android treats
 * Ontime like a system alarm clock — always reliable, always on time.
 *
 * NOTE: Google Play allows this permission for alarm clock, calendar,
 * communication, and productivity apps. Ontime qualifies.
 *
 * OEM-SPECIFIC EXTRA STEPS (user must do these manually):
 *  - Samsung:  Settings → Apps → Ontime → Battery → Unrestricted
 *  - Xiaomi:   Settings → Apps → Manage Apps → Ontime → Battery Saver → No restrictions
 *  - OnePlus:  Settings → Battery → Battery Optimization → Ontime → Don't optimize
 *  - Huawei:   Settings → Apps → Ontime → Battery → Disable Power-intensive prompt
 *
 * The standard Android permission handles stock Android (Pixel, etc.).
 * OEM-specific steps are shown in the UI description.
 */
object BatteryOptimizationManager {

    /**
     * Returns true if Ontime is already excluded from battery optimization.
     * Uses PowerManager.isIgnoringBatteryOptimizations — the official API.
     */
    fun isIgnoring(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Shows the system dialog: "Keep app running in background? Allow / Deny"
     * This is the cleanest UX — one tap and it's done.
     *
     * Requires android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS in manifest.
     * Only shows if not already excluded — check [isIgnoring] first.
     */
    fun requestExclusion(context: Context) {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Fallback: opens the battery optimization list so the user can
     * find Ontime and exclude it manually. Use this if [requestExclusion]
     * is unavailable (some restricted OEM builds).
     */
    fun openBatterySettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
