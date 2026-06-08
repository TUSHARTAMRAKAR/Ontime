package com.tushartamrakar.ontime.focus.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * OverlayPermissionManager
 *
 * Handles checking and requesting the SYSTEM_ALERT_WINDOW (display over apps)
 * permission. This lets Ontime show alarm and focus reminder screens over
 * any app — including on the lock screen.
 */
object OverlayPermissionManager {

    /** True if Ontime has been granted "Display Over Apps" by the user. */
    fun hasPermission(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    /**
     * Opens the system settings page for this permission.
     * The user must manually toggle it on — Android does not allow
     * runtime grant for SYSTEM_ALERT_WINDOW.
     */
    fun openPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
