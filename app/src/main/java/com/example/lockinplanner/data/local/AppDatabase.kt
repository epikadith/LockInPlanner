package com.example.lockinplanner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lockinplanner.data.local.dao.TaskDao
import com.example.lockinplanner.data.local.dao.ChecklistDao
import com.example.lockinplanner.data.local.entity.TaskEntity
import com.example.lockinplanner.data.local.entity.ChecklistEntity
import com.example.lockinplanner.data.local.entity.ObjectiveEntity

@Database(entities = [TaskEntity::class, ChecklistEntity::class, ObjectiveEntity::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun checklistDao(): ChecklistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lockin_planner_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
