package com.emami.pedometerplayground.main.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.emami.pedometerplayground.main.MainActivity

object NotificationUtil {
    fun createNotification(notificationManager: NotificationManager, context: Context): Notification {
        val notificationChannelId = "step-notification-3"
        val notificationChannelName = "step counter notification v4"
        val pendingRequestCode = 96552
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    notificationChannelId,
                    notificationChannelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    enableVibration(false)
                    setSound(null, null)
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                }
            )
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            pendingRequestCode,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, notificationChannelId)
            .setContentTitle("Step Counting in activated")
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.alert_dark_frame)
            .build()
    }
}