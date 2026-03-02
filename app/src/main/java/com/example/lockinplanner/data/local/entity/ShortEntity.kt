package com.example.lockinplanner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "shorts")
data class ShortEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val colorArgb: Int, // Stored as ARGB integer
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
