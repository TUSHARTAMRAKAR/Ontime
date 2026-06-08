package com.tushartamrakar.ontime.alarm.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

object LocationHelper {

    private const val PREFS_NAME = "ontime_location_prefs"
    private const val KEY_LAT = "weather_lat"
    private const val KEY_LON = "weather_lon"
    private const val KEY_UPDATED = "weather_last_updated"

    // ─── Save location to SharedPreferences ───────────────────────────────────
    fun saveLocation(context: Context, lat: Double, lon: Double) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_LAT, lat.toFloat())
            .putFloat(KEY_LON, lon.toFloat())
            .putLong(KEY_UPDATED, System.currentTimeMillis())
            .apply()
    }

    // ─── Read saved location from SharedPreferences ───────────────────────────
    fun getSavedLocation(context: Context): Pair<Double, Double>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lat = prefs.getFloat(KEY_LAT, 0f).toDouble()
        val lon = prefs.getFloat(KEY_LON, 0f).toDouble()
        return if (lat != 0.0 && lon != 0.0) Pair(lat, lon) else null
    }

    // ─── Fetch location and save to SharedPreferences ─────────────────────────
    fun fetchAndSaveLocation(
        context: Context,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    ) {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            onFailure()
            return
        }

        Thread {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)

                // ─── Try getCurrentLocation first ─────────────────────────────
                val currentTask = fusedClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY, null
                )
                val startTime = System.currentTimeMillis()
                while (!currentTask.isComplete &&
                    System.currentTimeMillis() - startTime < 5000) {
                    Thread.sleep(100)
                }

                if (currentTask.isComplete && currentTask.result != null) {
                    val loc = currentTask.result
                    saveLocation(context, loc.latitude, loc.longitude)
                    onSuccess()
                    return@Thread
                }

                // ─── Fallback to lastLocation ─────────────────────────────────
                val lastTask = fusedClient.lastLocation
                val startTime2 = System.currentTimeMillis()
                while (!lastTask.isComplete &&
                    System.currentTimeMillis() - startTime2 < 3000) {
                    Thread.sleep(100)
                }

                if (lastTask.isComplete && lastTask.result != null) {
                    val loc = lastTask.result
                    saveLocation(context, loc.latitude, loc.longitude)
                    onSuccess()
                    return@Thread
                }

                // ─── Fallback to system LocationManager ───────────────────────
                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER,
                )
                for (provider in providers) {
                    try {
                        val loc = locationManager.getLastKnownLocation(provider)
                        if (loc != null) {
                            saveLocation(context, loc.latitude, loc.longitude)
                            onSuccess()
                            return@Thread
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // All methods failed
                onFailure()

            } catch (e: Exception) {
                e.printStackTrace()
                onFailure()
            }
        }.start()
    }
}