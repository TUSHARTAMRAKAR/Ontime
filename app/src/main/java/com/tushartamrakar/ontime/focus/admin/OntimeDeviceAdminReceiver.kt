package com.tushartamrakar.ontime.focus.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Device Admin Receiver for Ontime.
 *
 * When active, Android prevents the app from being uninstalled.
 * The user must first navigate to Settings → Security → Device Admins,
 * deactivate Ontime, and THEN they can uninstall — making rage-quitting
 * your productivity commitment a deliberate multi-step process.
 */
class OntimeDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(
            context,
            "🔒 Ontime is now protected. Stay locked in!",
            Toast.LENGTH_LONG,
        ).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "Removing admin access will allow Ontime to be uninstalled and your " +
        "focus protection will be disabled. Make sure you've completed your goals first!"

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(
            context,
            "Ontime protection removed.",
            Toast.LENGTH_SHORT,
        ).show()
    }
}
