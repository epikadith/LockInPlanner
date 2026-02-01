package com.example.lockinplanner.domain.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.lockinplanner.domain.model.Task
import com.example.lockinplanner.domain.model.UserPreferences
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleTaskReminders(task: Task, reminders: List<Int>, userPreferences: UserPreferences) {
        if (alarmManager == null) return
        if (!userPreferences.notificationsEnabled) return
        
        // Filter based on Type
        val shouldNotify = when (task.repeatability) {
            "Daily" -> userPreferences.notifyDaily
            "Custom" -> userPreferences.notifyCustom
            else -> userPreferences.notifySingle
        }
        if (!shouldNotify) return

        reminders.forEachIndexed { index, offsetMinutes ->
            val triggerTime = calculateTriggerTime(task, offsetMinutes)
            if (triggerTime > System.currentTimeMillis()) {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("TASK_ID", task.id)
                    putExtra("TASK_TITLE", task.name)
                    putExtra("TASK_MESSAGE", getMessage(task.name, offsetMinutes))
                    // Use unique RequestCode: taskId * 100 + index
                }
                
                val requestCode = task.id * 100 + index
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            }
        }
    }

    fun cancelTaskReminders(task: Task) {
        // We don't know how many reminders there were.
        // Heuristic: Configure max reminders (e.g. 5) and cancel all potential slots.
        for (i in 0 until 5) {
            val requestCode = task.id * 100 + i
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                requestCode, 
                intent, 
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager?.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun calculateTriggerTime(task: Task, offsetMinutes: Int): Long {
        // Logic depends on Fixed vs Floating.
        // For Single (Fixed): startTime is UTC. Just add offset.
        // For Daily (Floating): startTime is minutes from midnight. Calculate NEXT occurrence.
        
        if (!task.isFloating) {
            // Fixed: startTime is UTC. Add offset (-10 min offset means trigger is 10 min BEFORE start)
            // Wait, offset from UI: "-10" means 10 min before. 
            // trigger = startTime + (-10 * 60 * 1000)
            return task.startTime + (offsetMinutes * 60 * 1000L)
        } else {
            // Floating: startTime is minutes from midnight
            val startMins = task.startTime.toInt()
            val hour = startMins / 60
            val minute = startMins % 60
            
            // Find next occurrence relative to NOW default timezone
            val now = Calendar.getInstance()
            val target = Calendar.getInstance()
            // Reset to today
            target.set(Calendar.HOUR_OF_DAY, hour)
            target.set(Calendar.MINUTE, minute)
            target.set(Calendar.SECOND, 0)
            target.set(Calendar.MILLISECOND, 0)
            
            // Add offset for the *trigger* time, not the *task* time
            target.add(Calendar.MINUTE, offsetMinutes)
            
            // If target (with offset) is in the past, schedule for tomorrow
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }
    }

    private fun getMessage(taskName: String, offset: Int): String {
        return when {
            offset == 0 -> "Task '$taskName' is starting now!"
            offset < 0 -> "Task '$taskName' starts in ${-offset} minutes."
            else -> "Task '$taskName' started ${offset} minutes ago."
        }
    }
}
