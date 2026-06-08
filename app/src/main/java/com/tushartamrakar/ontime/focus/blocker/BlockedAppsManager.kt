package com.tushartamrakar.ontime.focus.blocker

import android.content.Context
import android.util.Log
import com.tushartamrakar.ontime.focus.data.local.FocusDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BlockedAppsManager — in-memory cache of blocked apps + blocking modes.
 *
 * THREE blocking modes:
 *
 *   1. alwaysBlockedPackages (ALWAYS state)
 *      → Blocked at ALL times, regardless of session or mode.
 *      → Used for apps the user wants permanently blocked.
 *
 *   2. focusBlockedPackages (FOCUS_ONLY state) + alwaysOnMode = false
 *      → Blocked ONLY when a focus session is actively running.
 *      → Default behavior for "focus-session blocker".
 *
 *   3. focusBlockedPackages (FOCUS_ONLY state) + alwaysOnMode = true
 *      → Blocked ALL the time, even without a session.
 *      → "Always-On Mode" — lets users block distracting apps permanently
 *        without needing to start a timer.
 *
 * LIVE SYNC: FocusViewModel calls addFocusBlock/addAlwaysBlock/remove*
 * methods immediately after every DB write so the AccessibilityService
 * always has up-to-date state without a restart.
 */
@Singleton
class BlockedAppsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREFS_NAME       = "ontime_blocker_prefs"
        private const val KEY_ALWAYS_ON    = "always_on_mode"
        private const val TAG              = "BlockedAppsManager"
    }

    private val prefs  = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope  = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── In-memory sets ────────────────────────────────────────────────────────

    /** Blocked at ALL times. */
    private val alwaysBlockedPackages = HashSet<String>()

    /** Blocked only during focus sessions (or always when alwaysOnMode = true). */
    private val focusBlockedPackages  = HashSet<String>()

    /** True when FocusTimerService has an active WORK session running. */
    @Volatile var isFocusSessionActive: Boolean = false

    // ── Always-On Mode ────────────────────────────────────────────────────────

    /**
     * When true, FOCUS_ONLY apps are also blocked without an active session.
     * This gives users a "permanent blocker" independent of the focus timer.
     * Persisted in SharedPreferences — survives app restarts and service kills.
     */
    var alwaysOnMode: Boolean
        get()  = prefs.getBoolean(KEY_ALWAYS_ON, false)
        set(v) {
            prefs.edit().putBoolean(KEY_ALWAYS_ON, v).apply()
            Log.d(TAG, "Always-On Mode: $v")
        }

    // ── Load from DB ──────────────────────────────────────────────────────────

    fun loadFromDb(context: Context) {
        scope.launch {
            try {
                val db = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    FocusDbEntryPoint::class.java,
                ).focusDatabase()

                val enabledApps = db.focusDao().getEnabledBlockedAppsOnce()
                focusBlockedPackages.clear()
                alwaysBlockedPackages.clear()

                enabledApps.forEach { app ->
                    if (app.blockOnlyDuringFocus) focusBlockedPackages.add(app.packageName)
                    else alwaysBlockedPackages.add(app.packageName)
                }
                Log.d(TAG, "Loaded: ${focusBlockedPackages.size} focus-only + " +
                    "${alwaysBlockedPackages.size} always-blocked | alwaysOnMode=$alwaysOnMode")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load blocked apps: ${e.message}")
            }
        }
    }

    // ── Core lookup ───────────────────────────────────────────────────────────

    /**
     * Returns true if [packageName] should be blocked RIGHT NOW.
     *
     *  ALWAYS apps    → always blocked
     *  FOCUS apps     → blocked when session active OR alwaysOnMode is ON
     */
    fun shouldBlock(packageName: String): Boolean {
        if (alwaysBlockedPackages.contains(packageName)) return true
        if ((isFocusSessionActive || alwaysOnMode) && focusBlockedPackages.contains(packageName)) return true
        return false
    }

    // ── Live updates (called immediately after DB writes) ─────────────────────

    /**
     * Called when an app is set to FOCUS_ONLY.
     * Also removes from alwaysBlockedPackages to avoid cross-contamination.
     */
    fun setFocusBlock(packageName: String) {
        focusBlockedPackages.add(packageName)
        alwaysBlockedPackages.remove(packageName)   // clear other set
    }

    /**
     * Called when an app is set to ALWAYS.
     * Also removes from focusBlockedPackages to avoid cross-contamination.
     */
    fun setAlwaysBlock(packageName: String) {
        alwaysBlockedPackages.add(packageName)
        focusBlockedPackages.remove(packageName)    // clear other set
    }

    /**
     * Called when an app is set to ALLOW.
     * Removes from both sets.
     */
    fun clearBlock(packageName: String) {
        focusBlockedPackages.remove(packageName)
        alwaysBlockedPackages.remove(packageName)
    }

    // Keep old method names for backward compat
    fun addFocusBlock(packageName: String)    = setFocusBlock(packageName)
    fun removeFocusBlock(packageName: String) = focusBlockedPackages.remove(packageName)
    fun addAlwaysBlock(packageName: String)   = setAlwaysBlock(packageName)
    fun removeAlwaysBlock(packageName: String)= alwaysBlockedPackages.remove(packageName)

    fun getBlockedCount(): Int = focusBlockedPackages.size + alwaysBlockedPackages.size
    fun getFocusOnlyCount(): Int = focusBlockedPackages.size
    fun getAlwaysBlockedCount(): Int = alwaysBlockedPackages.size
}

// ─── Hilt EntryPoints ─────────────────────────────────────────────────────────

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FocusDbEntryPoint {
    fun focusDatabase(): FocusDatabase
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BlockedAppsManagerEntryPoint {
    fun blockedAppsManager(): BlockedAppsManager
}
