package com.example.lockinplanner.data.repository

import kotlinx.coroutines.flow.map
import com.example.lockinplanner.data.local.dao.TaskDao
import com.example.lockinplanner.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

import com.example.lockinplanner.domain.model.toDomain
import com.example.lockinplanner.domain.notification.AlarmScheduler
import kotlinx.coroutines.flow.first

class TaskRepository(
    private val taskDao: TaskDao,
    private val alarmScheduler: AlarmScheduler,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getTasksForDate(date: Long, nextDate: Long, timeZoneId: String): Flow<List<TaskEntity>> {
        return taskDao.getTasksOnDate(date, nextDate).map { list ->
             list.filter { task ->
                 if (task.isFloating) {
                     if (task.repeatability == "Daily") return@filter true
                     val mask = task.customRepeatDays ?: 0
                     if (mask == 0) return@filter false
                     
                     val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone(timeZoneId))
                     calendar.timeInMillis = date
                     val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK) // 1-7
                     val checkIndex = dayOfWeek - 1
                     (mask and (1 shl checkIndex)) != 0
                 } else {
                     true
                 }
             }.sortedBy { it.startTime }
        }
    }

    suspend fun insert(task: TaskEntity) {
        val domainTask = task.toDomain()
        // Always cancel existing alarms for this task ID before updating/inserting
        alarmScheduler.cancelTaskReminders(domainTask)
        
        taskDao.insertTask(task)
        // Schedule Alarm if needed
        if (domainTask.reminders.isNotEmpty()) {
            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            alarmScheduler.scheduleTaskReminders(domainTask, domainTask.reminders, prefs)
        }
    }

    suspend fun update(task: TaskEntity) {
        // Since we use REPLACE strategy, insert acts as update.
        // We also want to reschedule alarms.
        insert(task)
    }

    suspend fun delete(task: TaskEntity) {
        // Cancel Alarm first
        val domainTask = task.toDomain()
        alarmScheduler.cancelTaskReminders(domainTask)
        taskDao.deleteTask(task)
    }

    suspend fun deleteAllTasks() {
        // Fetch all to cancel alarms
        val tasks = taskDao.getAllTasksSync()
        tasks.forEach { entity ->
            alarmScheduler.cancelTaskReminders(entity.toDomain())
        } 
        taskDao.deleteAllTasks()
    }

    suspend fun deletePastTasks(currentTimeMillis: Long) {
        val pastTasks = taskDao.getPastSingleTasksSync(currentTimeMillis)
        pastTasks.forEach { entity ->
            alarmScheduler.cancelTaskReminders(entity.toDomain())
        }
        taskDao.deletePastSingleTasks(currentTimeMillis)
    }

    fun searchTasks(query: String): Flow<List<TaskEntity>> {
        return taskDao.searchTasks(query)
    }
}
