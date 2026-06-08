package com.tushartamrakar.ontime.focus.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Manages Ontime's Device Admin state.
 *
 * Usage in Compose:
 *   val manager = remember { DeviceAdminManager(context) }
 *   val launcher = rememberLauncherForActivityResult(...) { ... }
 *   launcher.launch(manager.buildActivationIntent())
 */
class DeviceAdminManager(private val context: Context) {

    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    val adminComponent: ComponentName =
        ComponentName(context, OntimeDeviceAdminReceiver::class.java)

    /** True if Ontime is currently an active device admin (= uninstall-protected). */
    val isAdminActive: Boolean
        get() = devicePolicyManager.isAdminActive(adminComponent)

    /**
     * Returns an Intent that opens the system "Activate device administrator?" dialog.
     * Launch this with [ActivityResultLauncher] from your composable.
     */
    fun buildActivationIntent(): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Activating admin access makes Ontime undeletable so you can't escape " +
                "your focus commitments. You can always remove this from " +
                "Settings → Security → Device Admins.",
            )
        }

    /**
     * Programmatically removes admin access (used for the "Remove protection" button).
     * After this, the app can be uninstalled normally again.
     */
    fun deactivateAdmin() {
        devicePolicyManager.removeActiveAdmin(adminComponent)
    }
}
