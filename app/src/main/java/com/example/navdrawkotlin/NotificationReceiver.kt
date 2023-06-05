package com.example.navdrawkotlin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Handle the notification event here
        // You can perform any actions or show a notification to the user

        // Create a notification channel for Android Oreo and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "UpcomingTaskNotificationChannel"
            val channelName = "Upcoming Task Notification"
            val channelDescription = "Channel for displaying upcoming task notifications"

            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = channelDescription
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // Create a notification builder
        val notificationBuilder = NotificationCompat.Builder(context, "UpcomingTaskNotificationChannel")
            .setSmallIcon(R.drawable.timewize)
            .setContentTitle("Upcoming Task")
            .setContentText("You have an upcoming task!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Display the notification
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1001, notificationBuilder.build())
    }
}
