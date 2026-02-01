package com.example.lockinplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lockinplanner.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSync(): List<TaskEntity>

    // Hybrid Query:
    // 1. Fixed Tasks (isFloating=0): Must overlap the UTC window (:rangeStart, :rangeEnd).
    //    Overlap Logic: taskStart < rangeEnd AND taskEnd > rangeStart.
    // 2. Floating Tasks (isFloating=1): Fetch ALL of them (Daily/Custom). We filter correctness in Kotlin/Repo.
    //    Why? Because filtering 'Custom' bitmasks in SQL is hard, and 'Daily' is always true.
    //    Optimization: We could restrict 'Custom' bitmasks here if we pass the day index.
    @Query("""
        SELECT * FROM tasks 
        WHERE 
           (isFloating = 0 AND startTime < :rangeEnd AND endTime > :rangeStart)
        OR 
           (isFloating = 1)
    """)
    fun getTasksOnDate(rangeStart: Long, rangeEnd: Long): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}
