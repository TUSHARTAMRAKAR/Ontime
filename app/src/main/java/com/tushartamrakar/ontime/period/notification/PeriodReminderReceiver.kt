package com.tushartamrakar.ontime.period.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tushartamrakar.ontime.MainActivity
import com.tushartamrakar.ontime.R
import com.tushartamrakar.ontime.core.navigation.DeepLinkHandler

class PeriodReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID   = "ontime_period_reminders"
        const val CHANNEL_NAME = "Period Reminders"

        const val EXTRA_NOTIF_ID = "period_notif_id"
        const val EXTRA_TITLE    = "period_title"
        const val EXTRA_BODY     = "period_body"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        val title   = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body    = intent.getStringExtra(EXTRA_BODY)  ?: return

        createNotificationChannel(context)
        showNotification(context, notifId, title, body)
    }

    private fun showNotification(context: Context, notifId: Int, title: String, body: String) {
        // Android 13+ requires POST_NOTIFICATIONS permission at runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        // Tap notification → open the app
        // Tap notification → open app directly on Period Tracker screen
        val tapIntent = DeepLinkHandler.buildIntent(context, DeepLinkHandler.ROUTE_PERIOD_TRACKER)
        val tapPendingIntent = PendingIntent.getActivity(
            context, notifId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setColor(0xFFE91E8C.toInt())        // rose accent colour
            .setContentIntent(tapPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Gentle reminders about your menstrual cycle"
            enableVibration(true)
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}
