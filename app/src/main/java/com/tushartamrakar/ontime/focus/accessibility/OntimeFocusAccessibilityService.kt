package com.tushartamrakar.ontime.focus.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * OntimeFocusAccessibilityService
 *
 * Two powers:
 * 1. REAL-TIME APP BLOCKER — instantly navigates back when a blocked app
 *    opens during an active focus session.
 * 2. POWER MENU DETECTION — detects when the system power menu appears
 *    during a focus session.
 *
 * CRITICAL RULES for accessibility services that stay enabled:
 *
 * Rule 1 — NEVER use lateinit for fields accessed in onAccessibilityEvent.
 *    onAccessibilityEvent CAN fire before onServiceConnected on some OEMs.
 *    lateinit → UninitializedPropertyAccessException → service crash →
 *    Android auto-disables the service.
 *    Fix: use `by lazy` so initialization is deferred to first access safely.
 *
 * Rule 2 — ALWAYS wrap onAccessibilityEvent in try-catch.
 *    Any uncaught exception in onAccessibilityEvent causes Android to
 *    automatically disable the accessibility service — silently, with no
 *    user-visible error. The user sees the toggle flip to OFF.
 *    Fix: catch all exceptions and swallow them (log only).
 *
 * Rule 3 — NEVER show Toasts from an accessibility service.
 *    Toast.makeText() internally calls getSystemService() which can throw
 *    on certain OEM Android builds when called from an accessibility service
 *    context. Use Log instead.
 *
 * Rule 4 — Use applicationContext for SharedPreferences.
 *    The service's own context can be null during restart scenarios.
 *    applicationContext is always safe.
 */
class OntimeFocusAccessibilityService : AccessibilityService() {

    companion object {
        const val PREFS_NAME            = "ontime_focus_prefs"
        const val KEY_SESSION_ACTIVE    = "focus_session_active"
        const val KEY_BLOCKED_PACKAGES  = "blocked_packages"

        private const val TAG           = "OntimeAccessibility"
        private const val SYSTEMUI_PKG  = "com.android.systemui"

        private val POWER_MENU_CLASSES  = setOf(
            "GlobalActionsDialog",
            "GlobalActions",
            "com.android.systemui.globalactions.GlobalActionsDialog",
            "com.android.systemui.power.PowerDialog",
            "GlobalActionsImpl",
        )
    }

    // ── Rule 1: lazy init — safe even if called before onServiceConnected ─────
    private val prefs: SharedPreferences by lazy {
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onServiceConnected() {
        Log.d(TAG, "Accessibility service connected and running")

        // Configure event types here too (belt + suspenders alongside XML config)
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.apply {
            eventTypes   = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                           AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags        = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                           AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // ── Rule 2: try-catch everything — a single uncaught exception ────────
        // will cause Android to silently disable this service. Never let that
        // happen — swallow all errors and log them instead.
        try {
            handleEvent(event)
        } catch (e: Exception) {
            Log.e(TAG, "Swallowed exception in onAccessibilityEvent: ${e.message}")
        }
    }

    private fun handleEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        val className   = event.className?.toString() ?: ""

        // ── 1. Power menu detection ───────────────────────────────────────────
        if (packageName == SYSTEMUI_PKG &&
            POWER_MENU_CLASSES.any { className.contains(it, ignoreCase = true) }
        ) {
            Log.d(TAG, "Power menu detected — focus session is active")
            return
        }

        // ── 2. Blocked app detection ──────────────────────────────────────────
        // Skip if no session is active — avoids unnecessary SharedPrefs reads
        val sessionActive = prefs.getBoolean(KEY_SESSION_ACTIVE, false)
        if (!sessionActive) return

        // Never block Ontime itself
        if (packageName.startsWith("com.tushartamrakar.ontime")) return

        val blockedPackages = prefs.getStringSet(KEY_BLOCKED_PACKAGES, emptySet()) ?: emptySet()
        if (packageName in blockedPackages) {
            Log.d(TAG, "Blocked app intercepted: $packageName — navigating back")
            performGlobalAction(GLOBAL_ACTION_BACK)
            // Rule 3: no Toast here — can crash on OEM builds
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        Log.d(TAG, "Accessibility service destroyed")
        super.onDestroy()
    }
}
