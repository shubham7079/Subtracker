package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {
    const val CHANNEL_ID = "renewal_reminders_channel"
    const val CHANNEL_NAME = "Subscription Renewal Reminders"
    const val CHANNEL_DESC = "Alerts sent before upcoming subscription renewals and bills"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendRenewalNotification(
        context: Context,
        notificationId: Int,
        subscriptionName: String,
        amountFormatted: String,
        daysRemaining: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val timingText = when (daysRemaining) {
            0 -> "renews TODAY!"
            1 -> "renews tomorrow"
            else -> "renews in $daysRemaining days"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Upcoming Bill: $subscriptionName")
            .setContentText("$subscriptionName ($amountFormatted) $timingText. Tap to review.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your $subscriptionName subscription for $amountFormatted $timingText. Keep track or pause it if you no longer need it.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun sendTestAlert(context: Context) {
        sendRenewalNotification(
            context = context,
            notificationId = 9999,
            subscriptionName = "Netflix Premium",
            amountFormatted = "$15.99",
            daysRemaining = 3
        )
    }
}
