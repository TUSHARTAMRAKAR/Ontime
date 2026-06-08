package com.tushartamrakar.ontime.focus.accessibility

import android.content.Context
import android.content.SharedPreferences

/**
 * FocusSessionPrefs
 *
 * A thin SharedPreferences bridge that lets FocusTimerService write session state
 * and OntimeFocusAccessibilityService read it — safely, without coupling them.
 *
 * Call [setSessionActive] from FocusTimerService when sessions start/stop.
 * Call [setBlockedPackages] from FocusTimerService with the current blocked set.
 */
object FocusSessionPrefs {

    private const val PREFS_NAME = OntimeFocusAccessibilityService.PREFS_NAME

    fun setSessionActive(context: Context, active: Boolean) {
        prefs(context).edit()
            .putBoolean(OntimeFocusAccessibilityService.KEY_SESSION_ACTIVE, active)
            .apply()
    }

    fun setBlockedPackages(context: Context, packages: Set<String>) {
        prefs(context).edit()
            .putStringSet(OntimeFocusAccessibilityService.KEY_BLOCKED_PACKAGES, packages)
            .apply()
    }

    fun isSessionActive(context: Context): Boolean =
        prefs(context).getBoolean(OntimeFocusAccessibilityService.KEY_SESSION_ACTIVE, false)

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
