package com.tushartamrakar.ontime.focus.blocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.EntryPointAccessors
import com.tushartamrakar.ontime.focus.foreground.FocusTimerService
import com.tushartamrakar.ontime.focus.foreground.FocusTimerState
import com.tushartamrakar.ontime.focus.foreground.isWorkSessionRunning

/**
 * BlockerAccessibilityService — the watchdog.
 *
 * Monitors every app that comes to the foreground using
 * TYPE_WINDOW_STATE_CHANGED accessibility events. This is the ONLY
 * non-root way to reliably detect foreground app changes on Android.
 *
 * What it does:
 *   1. On every foreground change → checks BlockedAppsManager.shouldBlock()
 *   2. If blocked → immediately launches FocusBlockerActivity as an overlay
 *   3. Increments distraction counter on FocusTimerService
 *
 * Always-on for adult content (blockOnlyDuringFocus = false).
 * Focus-blocking only when FocusTimerService has an active WORK session.
 *
 * Configured in res/xml/accessibility_service_config.xml.
 * Registered in AndroidManifest with BIND_ACCESSIBILITY_SERVICE permission.
 */
class BlockerAccessibilityService : AccessibilityService() {

    private val tag = "BlockerAccessibility"

    private lateinit var blockedAppsManager: BlockedAppsManager

    // Track last blocked package to avoid showing overlay on every event
    // for the same app (events fire multiple times per app open)
    private var lastBlockedPackage: String? = null
    private var lastBlockedTime: Long = 0L
    private val BLOCK_COOLDOWN_MS = 2000L    // only show overlay once per 2s per app

    // Our own package — never block ourselves
    private val ownPackage by lazy { packageName }

    // ─── Service lifecycle ────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Get BlockedAppsManager via EntryPoint (AccessibilityService isn't Hilt-injectable)
        blockedAppsManager = EntryPointAccessors.fromApplication(
            applicationContext,
            BlockedAppsManagerEntryPoint::class.java,
        ).blockedAppsManager()

        // Load blocked apps from DB into memory
        blockedAppsManager.loadFromDb(applicationContext)

        // Configure which events we want
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes  = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags        = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                           AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100L
        }
        Log.d(tag, "Accessibility service connected")
    }

    override fun onInterrupt() {
        Log.d(tag, "Accessibility service interrupted")
    }

    // ─── Event handling ───────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Never block ourselves, system UI, or home launchers
        if (shouldSkipPackage(packageName)) return

        // Sync focus session state into BlockedAppsManager
        blockedAppsManager.isFocusSessionActive =
            FocusTimerService.timerState.value.isWorkSessionRunning

        // Check if this app should be blocked
        if (!blockedAppsManager.shouldBlock(packageName)) return

        // Cooldown — avoid spamming overlay for same app
        val now = System.currentTimeMillis()
        if (packageName == lastBlockedPackage && now - lastBlockedTime < BLOCK_COOLDOWN_MS) return

        lastBlockedPackage = packageName
        lastBlockedTime    = now

        // Get friendly app name
        val appName = getAppName(packageName)

        // Determine if this is an adult block or focus block
        val blockReason = if (isAlwaysBlocked(packageName)) "ADULT" else "FOCUS"

        Log.d(tag, "Blocking: $packageName ($appName) reason=$blockReason")

        // Increment distraction counter on the timer service
        val timerState = FocusTimerService.timerState.value
        if (timerState is FocusTimerState.Running) {
            // We can't call the service method directly (no binder here),
            // so we send a broadcast intent to increment the counter
            sendBroadcast(
                Intent(DISTRACTIONS_INCREMENT_ACTION).apply {
                    setPackage(ownPackage)
                }
            )
        }

        // Launch the block overlay
        startActivity(
            FocusBlockerActivity.createIntent(
                context = applicationContext,
                appName = appName,
                reason  = blockReason,
            )
        )
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun shouldSkipPackage(pkg: String): Boolean {
        return pkg == ownPackage ||
            pkg.startsWith("com.android.systemui") ||
            pkg.startsWith("com.android.launcher") ||
            pkg.startsWith("com.google.android.apps.nexuslauncher") ||
            pkg.startsWith("com.sec.android.app.launcher") ||
            pkg == "android" ||
            pkg == "com.android.settings"
    }

    private fun isAlwaysBlocked(packageName: String): Boolean {
        // Check if this package is in the always-blocked set
        // (we delegate to BlockedAppsManager internal state)
        return blockedAppsManager.shouldBlock(packageName) &&
               !FocusTimerService.timerState.value.isWorkSessionRunning
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = applicationContext.packageManager
            pm.getApplicationLabel(
                pm.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".")
        }
    }

    companion object {
        const val DISTRACTIONS_INCREMENT_ACTION =
            "com.tushartamrakar.ontime.focus.INCREMENT_DISTRACTIONS"
    }
}
