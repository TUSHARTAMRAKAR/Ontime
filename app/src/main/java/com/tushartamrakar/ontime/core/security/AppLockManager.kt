package com.tushartamrakar.ontime.core.security

import android.content.Context
import android.provider.Settings
import androidx.biometric.BiometricManager

/**
 * AppLockManager
 *
 * Manages the App Lock feature:
 *  - Whether the lock is enabled
 *  - PIN creation, verification, and removal
 *  - Lock timing (app locked after N seconds in background)
 *  - Biometric capability detection
 *
 * PIN is hashed with SHA-256 using ANDROID_ID as a device-unique salt.
 * Never stored in plaintext.
 */
class AppLockManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME              = "ontime_app_lock"
        private const val KEY_ENABLED             = "app_lock_enabled"
        private const val KEY_PIN_HASH            = "pin_hash"
        private const val KEY_LAST_BACKGROUND_MS  = "last_bg_ms"
        private const val KEY_FAIL_COUNT          = "fail_count"

        const val LOCK_TIMEOUT_MS = 30_000L
        const val MAX_ATTEMPTS    = 5

        /**
         * In-memory flag. Lives only while the process is alive.
         * Automatically resets to FALSE when the process is killed
         * (app cleared from recents, phone restarted, etc).
         *
         * This is the key mechanism that makes the lock appear on
         * every fresh app launch — not just background → foreground.
         */
        @Volatile
        private var sessionUnlocked = false
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Enabled state ─────────────────────────────────────────────────────────

    var isEnabled: Boolean
        get()  = prefs.getBoolean(KEY_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_ENABLED, v).apply()

    /** True if a PIN has been set (enabled + hasPinSet = fully configured). */
    val hasPinSet: Boolean get() = prefs.getString(KEY_PIN_HASH, null) != null

    // ── PIN management ────────────────────────────────────────────────────────

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, hash(pin)).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val ok     = hash(pin) == stored
        if (ok) resetFailCount() else incrementFailCount()
        return ok
    }

    fun clearPin() = prefs.edit().remove(KEY_PIN_HASH).apply()

    // ── Lock decision ─────────────────────────────────────────────────────────

    /** Call from lifecycle ON_STOP (app went to background). */
    fun onAppBackground() {
        if (isEnabled && hasPinSet) {
            prefs.edit().putLong(KEY_LAST_BACKGROUND_MS, System.currentTimeMillis()).apply()
        }
    }

    /**
     * THE main lock check. Use this everywhere — initial launch AND
     * every time the app comes to foreground.
     *
     * Returns TRUE (show lock) when:
     *  1. Fresh process start (cleared from recents, phone restart)
     *     → sessionUnlocked == false → ALWAYS lock
     *  2. Already unlocked this session BUT timeout elapsed
     *     → lock again
     *
     * Returns FALSE (don't lock) when:
     *  - Lock not enabled, or no PIN set
     *  - Already unlocked this session AND timeout hasn't elapsed yet
     */
    fun shouldLockOnStart(): Boolean {
        if (!isEnabled || !hasPinSet) return false
        if (!sessionUnlocked) return true   // fresh process — always lock
        return shouldLockAfterBackground()  // session active — check timeout
    }

    private fun shouldLockAfterBackground(): Boolean {
        val lastBg = prefs.getLong(KEY_LAST_BACKGROUND_MS, 0L)
        if (lastBg == 0L) return false
        return System.currentTimeMillis() - lastBg > LOCK_TIMEOUT_MS
    }

    /** Call after successful unlock to mark this session as authenticated. */
    fun onUnlocked() {
        sessionUnlocked = true   // stays true until process is killed
        prefs.edit()
            .putLong(KEY_LAST_BACKGROUND_MS, 0L)
            .putInt(KEY_FAIL_COUNT, 0)
            .apply()
    }

    // ── Failed attempt tracking ───────────────────────────────────────────────

    val failCount: Int get() = prefs.getInt(KEY_FAIL_COUNT, 0)

    private fun incrementFailCount() =
        prefs.edit().putInt(KEY_FAIL_COUNT, failCount + 1).apply()

    private fun resetFailCount() =
        prefs.edit().putInt(KEY_FAIL_COUNT, 0).apply()

    // ── Biometric capability ──────────────────────────────────────────────────

    /** True if the device has usable biometric hardware enrolled. */
    fun canUseBiometrics(): Boolean {
        val bm = BiometricManager.from(context)
        val result = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK,
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    // ── Private: PIN hashing ──────────────────────────────────────────────────

    private fun hash(pin: String): String {
        // Device-unique salt from ANDROID_ID prevents cross-device rainbow attacks
        val salt   = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        ) ?: "ontime_lock_salt"
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes  = digest.digest((salt + pin).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
