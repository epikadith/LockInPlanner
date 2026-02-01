package com.example.lockinplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "checklists")
data class ChecklistEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
