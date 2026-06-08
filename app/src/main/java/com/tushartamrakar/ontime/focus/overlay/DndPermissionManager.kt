package com.tushartamrakar.ontime.focus.overlay

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * DndPermissionManager
 *
 * Android has two levels for "ring during DND":
 *
 * Level 1 — Declaring ACCESS_NOTIFICATION_POLICY in the manifest (done ✅)
 *           This allows the app to READ DND state and CREATE channels.
 *
 * Level 2 — User granting "Notification Policy Access" from system settings.
 *           Without this, NotificationChannel.setBypassDnd(true) is silently
 *           ignored and your alarm will still be muted by DND.
 *
 * This manager handles Level 2: checking if it's granted and directing
 * the user to the right settings page if not.
 */
object DndPermissionManager {

    /**
     * True if the user has granted Ontime permission to bypass DND.
     * Until this returns true, alarm channels cannot bypass DND.
     */
    fun hasPermission(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    /**
     * Opens the system "Do Not Disturb access" settings page.
     * The user must toggle Ontime ON manually — Android does not allow
     * programmatic granting of this permission.
     */
    fun openPermissionSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
