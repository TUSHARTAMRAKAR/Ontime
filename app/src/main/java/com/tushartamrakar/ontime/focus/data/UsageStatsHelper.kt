package com.tushartamrakar.ontime.focus.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

// ─── Model ────────────────────────────────────────────────────────────────────

data class UsageData(
    val usageMinutes: Int = 0,          // total phone screen-on time today
    val focusMinutes: Int = 0,          // focus time tracked in our DB
    val efficiencyPercent: Int = 0,     // focusMinutes / usageMinutes × 100
    val hasPermission: Boolean = false, // PACKAGE_USAGE_STATS granted?
)

// ─── Helper ───────────────────────────────────────────────────────────────────

/**
 * Queries UsageStatsManager for today's total foreground screen time.
 * Requires the PACKAGE_USAGE_STATS permission (user-granted, not install-time).
 */
@Singleton
class UsageStatsHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Returns true if the PACKAGE_USAGE_STATS AppOp is allowed for this app. */
    fun hasPermission(): Boolean = runCatching {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    /**
     * Returns total foreground time for ALL apps today, in minutes.
     * Returns -1 if permission is not granted.
     */
    fun getTodayUsageMinutes(): Int {
        if (!hasPermission()) return -1
        return runCatching {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDay,
                System.currentTimeMillis(),
            )
            val totalMs = stats?.sumOf { it.totalTimeInForeground } ?: 0L
            (totalMs / 60_000L).toInt()
        }.getOrDefault(0)
    }

    /**
     * Builds a [UsageData] snapshot combining phone usage from the system
     * with the focus seconds already tracked in our DB.
     *
     * @param focusSeconds today's total focus seconds from FocusDao.getTodayFocusSeconds()
     */
    fun buildUsageData(focusSeconds: Int): UsageData {
        val hasPerm   = hasPermission()
        val usageMin  = if (hasPerm) getTodayUsageMinutes().coerceAtLeast(0) else 0
        val focusMin  = focusSeconds / 60
        val efficiency = if (usageMin > 0)
            ((focusMin.toFloat() / usageMin.toFloat()) * 100f).toInt().coerceIn(0, 100)
        else 0
        return UsageData(
            usageMinutes      = usageMin,
            focusMinutes      = focusMin,
            efficiencyPercent = efficiency,
            hasPermission     = hasPerm,
        )
    }

    /** Opens system Usage Access settings screen so user can grant permission. */
    fun openPermissionSettings() {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }
}
