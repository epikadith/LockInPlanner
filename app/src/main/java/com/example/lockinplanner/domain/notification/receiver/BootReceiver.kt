package com.example.lockinplanner.domain.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.lockinplanner.data.local.AppDatabase
import com.example.lockinplanner.data.repository.UserPreferencesRepository
import com.example.lockinplanner.domain.model.toDomain
import com.example.lockinplanner.domain.notification.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.LOCKED_BOOT_COMPLETED") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = AppDatabase.getDatabase(context)
                    val taskDao = database.taskDao()
                    val userPreferencesRepository = UserPreferencesRepository(context)
                    val userPreferences = userPreferencesRepository.userPreferencesFlow.first()
                    
                    // We need ALL recurrent tasks (Daily/Custom) and FUTURE Single tasks.
                    // Simplified: Fetch all tasks and filter in memory (if DB not huge).
                    // Or add a query to Dao for "Active Tasks".
                    // For now, let's fetch all.
                    val tasks = taskDao.getAllTasksSync() // Need to add this method to DAO? Or use Flow.first()
                    // Dao currently returns Flow<List>.
                    // I need a suspend function returning List.
                    
                    val scheduler = AlarmScheduler(context)
                    
                    tasks.forEach { entity ->
                        val task = entity.toDomain()
                        if (task.reminders.isNotEmpty()) {
                             scheduler.scheduleTaskReminders(task, task.reminders, userPreferences)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
