package com.example.lockinplanner.domain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val title = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        val message = intent.getStringExtra("TASK_MESSAGE") ?: "You have a task coming up!"

        if (taskId != -1) {
            val helper = NotificationManagerHelper(context)
            // Ensure channels exist (safe to call repeatedly)
            helper.createNotificationChannels()
            helper.showNotification(taskId, title, message)
        }
    }
}
