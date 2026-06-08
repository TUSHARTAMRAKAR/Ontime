package com.tushartamrakar.ontime.focus.presentation

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queries PackageManager for all user-installed apps.
 * Used by BlockerScreen to display the app list with toggles.
 *
 * Result is cached in memory after the first load — the list
 * is stable during a session and only changes if the user
 * installs/uninstalls apps (rare).
 */
@Singleton
class InstalledAppsLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class AppInfo(
        val packageName: String,
        val appName: String,
        val icon: Drawable?,
    )

    private var cachedApps: List<AppInfo>? = null

    /** Returns all user-installed apps, sorted by name. Cached after first load. */
    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<AppInfo> {
        if (!forceRefresh && cachedApps != null) return cachedApps!!
        return withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfoList = pm.queryIntentActivities(mainIntent, 0)
            val apps = resolveInfoList
                .mapNotNull { info ->
                    val pkg = info.activityInfo.packageName
                    if (pkg == context.packageName) return@mapNotNull null  // skip ourselves
                    try {
                        AppInfo(
                            packageName = pkg,
                            appName     = info.loadLabel(pm).toString(),
                            icon        = info.loadIcon(pm),
                        )
                    } catch (e: Exception) { null }
                }
                .distinctBy { it.packageName }     // prevents duplicate LazyColumn keys
                .sortedBy { it.appName.lowercase() }

            cachedApps = apps
            apps
        }
    }

    fun clearCache() { cachedApps = null }
}
