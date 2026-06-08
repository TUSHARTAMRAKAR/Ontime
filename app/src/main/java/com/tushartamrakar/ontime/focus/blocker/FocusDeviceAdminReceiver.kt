package com.tushartamrakar.ontime.focus.blocker

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * DeviceAdminReceiver — gives Ontime device-admin level friction.
 *
 * What this does:
 *   - Prevents the user from easily uninstalling Ontime while the adult
 *     content filter is active. To uninstall, they must first go to
 *     Settings → Security → Device Admins → Deactivate Ontime Admin.
 *     This extra step breaks the instant-gratification impulse.
 *
 * What this does NOT do:
 *   - It does NOT prevent uninstall forever. Android doesn't allow that
 *     without root. This is purely a friction layer.
 *   - It does NOT access any device management APIs. We only use the
 *     admin status to protect the filter.
 *
 * Registered in AndroidManifest with:
 *   <meta-data android:name="android.app.device_admin"
 *              android:resource="@xml/device_admin_policies" />
 */
class FocusDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d("FocusDeviceAdmin", "Device admin activated — uninstall protection ON")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d("FocusDeviceAdmin", "Device admin deactivated — uninstall protection OFF")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // Shown to the user when they try to deactivate admin
        return "Disabling device admin will remove your adult content filter protection. Are you sure?"
    }
}
