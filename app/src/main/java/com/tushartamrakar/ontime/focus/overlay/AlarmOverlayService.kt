package com.tushartamrakar.ontime.focus.overlay

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * AlarmOverlayService
 *
 * Shows a full-screen overlay using TYPE_APPLICATION_OVERLAY so that Ontime's
 * alarm / focus reminder appears on top of everything — including the lock screen
 * and any other app the user might have open.
 *
 * Requires SYSTEM_ALERT_WINDOW permission (checked before starting).
 *
 * Start with:
 *   AlarmOverlayService.start(context, title, message)
 *
 * Snooze: reschedules itself via AlarmManager for N minutes later,
 * then dismisses the current overlay. When the alarm fires, the overlay
 * reappears with the same title and message.
 */
class AlarmOverlayService : Service() {

    companion object {
        const val EXTRA_TITLE          = "overlay_title"
        const val EXTRA_MESSAGE        = "overlay_message"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"

        // Default snooze options shown to user
        const val SNOOZE_5_MIN  = 5
        const val SNOOZE_10_MIN = 10

        // Unique request code for the snooze PendingIntent
        private const val SNOOZE_REQUEST_CODE = 9901

        private const val TAG = "AlarmOverlayService"

        fun start(context: Context, title: String, message: String) {
            if (!OverlayPermissionManager.hasPermission(context)) return
            val intent = Intent(context, AlarmOverlayService::class.java).apply {
                putExtra(EXTRA_TITLE,   title)
                putExtra(EXTRA_MESSAGE, message)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmOverlayService::class.java))
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: FrameLayout?     = null

    // Stored so snooze can re-pass them when rescheduling
    private var currentTitle   = "Focus Reminder"
    private var currentMessage = "Time to get back on track."

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentTitle   = intent?.getStringExtra(EXTRA_TITLE)          ?: "Focus Reminder"
        currentMessage = intent?.getStringExtra(EXTRA_MESSAGE)        ?: "Time to get back on track."
        val snoozeMin  = intent?.getIntExtra(EXTRA_SNOOZE_MINUTES, 0) ?: 0

        if (snoozeMin > 0) {
            Log.d(TAG, "Snooze alarm fired after ${snoozeMin}min — showing overlay again")
        }

        showOverlay()
        return START_NOT_STICKY
    }

    // ─── Overlay UI ───────────────────────────────────────────────────────────

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Root container
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#E60A0A0F"))
        }

        // Content layout
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER
            setPadding(80, 0, 80, 0)
        }

        val tvTitle = TextView(this).apply {
            text     = currentTitle
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity  = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 16)
        }

        val tvMessage = TextView(this).apply {
            text     = currentMessage
            textSize = 15f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity  = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        // Dismiss button — clears overlay, no reschedule
        val btnDismiss = Button(this).apply {
            text    = "Dismiss"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#7C3AED"))
            setPadding(64, 28, 64, 28)
            setOnClickListener { dismiss() }
        }

        // Snooze 5 min button
        val btnSnooze5 = Button(this).apply {
            text    = "Snooze 5 min"
            textSize = 13f
            setTextColor(Color.parseColor("#94A3B8"))
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 20, 0, 4)
            setOnClickListener {
                snoozeFor(SNOOZE_5_MIN)
                dismiss()
            }
        }

        // Snooze 10 min button
        val btnSnooze10 = Button(this).apply {
            text    = "Snooze 10 min"
            textSize = 13f
            setTextColor(Color.parseColor("#64748B"))
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 4, 0, 0)
            setOnClickListener {
                snoozeFor(SNOOZE_10_MIN)
                dismiss()
            }
        }

        content.addView(tvTitle)
        content.addView(tvMessage)
        content.addView(btnDismiss)
        content.addView(btnSnooze5)
        content.addView(btnSnooze10)

        root.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER,
            ),
        )

        overlayView = root

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED     or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON       or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON       or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            PixelFormat.TRANSLUCENT,
        )

        windowManager?.addView(root, params)
    }

    // ─── Snooze ───────────────────────────────────────────────────────────────

    /**
     * Schedules [AlarmOverlayService] to restart [minutes] from now with the
     * same title and message, so the overlay reappears after the snooze period.
     *
     * Uses setExactAndAllowWhileIdle — fires even in Doze mode.
     */
    private fun snoozeFor(minutes: Int) {
        val triggerMillis = System.currentTimeMillis() + (minutes * 60 * 1000L)

        val snoozeIntent = Intent(this, AlarmOverlayService::class.java).apply {
            putExtra(EXTRA_TITLE,          currentTitle)
            putExtra(EXTRA_MESSAGE,        currentMessage)
            putExtra(EXTRA_SNOOZE_MINUTES, minutes)
        }

        // getForegroundService requires API 26 — we target modern Android so this is safe
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                this,
                SNOOZE_REQUEST_CODE,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        } else {
            PendingIntent.getService(
                this,
                SNOOZE_REQUEST_CODE,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent,
        )

        Log.d(TAG, "Snoozed for ${minutes}min — overlay will reappear at $triggerMillis")
    }

    // ─── Dismiss ──────────────────────────────────────────────────────────────

    private fun dismiss() {
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
        stopSelf()
        Log.d(TAG, "Overlay dismissed")
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
        }
    }
}
