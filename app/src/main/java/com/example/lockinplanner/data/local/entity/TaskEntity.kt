package com.example.lockinplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tasks",
    indices = [
        androidx.room.Index(value = ["startTime"]),
        androidx.room.Index(value = ["endTime"]),
        androidx.room.Index(value = ["repeatability"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String?,
    val color: Long,
    val repeatability: String, // "Single", "Daily", "Custom"
    val customRepeatDays: Int?, // Bitmask
    val isFloating: Boolean, // True = Local Time (Minutes), False = UTC (Timestamp)
    val startTime: Long, // UTC Timestamp or Minutes from Midnight
    val endTime: Long, // UTC Timestamp or Minutes from Midnight
    val reminders: List<Int> = emptyList(), // Minutes offset from start time (-10, 0, 15)
    @androidx.room.ColumnInfo(defaultValue = "0") val isThemeColor: Boolean = false
)
