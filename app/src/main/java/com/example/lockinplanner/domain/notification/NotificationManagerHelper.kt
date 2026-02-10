package com.example.lockinplanner.domain.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.lockinplanner.R

class NotificationManagerHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_HIGH = "task_reminders_high"
        const val CHANNEL_ID_LOW = "task_reminders_low"
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val highChannel = NotificationChannel(
                CHANNEL_ID_HIGH,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled tasks"
                enableVibration(true)
                // Default sound behavior respects Ringer Mode
            }

            val lowChannel = NotificationChannel(
                CHANNEL_ID_LOW,
                "Silent Reminders",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent notifications for tasks"
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(listOf(highChannel, lowChannel))
        }
    }

    fun showNotification(taskId: Int, title: String, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_HIGH)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this resource exists, or use a default android icon if custom one missing
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // .setContentIntent pendingIntent for opening app
        
        // Use NotificationManagerCompat for compatibility
        try {
             // In real app, check permission here if targeted Tiramisu+
             val nm = NotificationManagerCompat.from(context)
             if (nm.areNotificationsEnabled()) {
                nm.notify(taskId, builder.build())
             }
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}
