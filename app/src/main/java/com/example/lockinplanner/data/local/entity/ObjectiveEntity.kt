package com.example.lockinplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "objectives",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("checklistId")]
)
data class ObjectiveEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val checklistId: String,
    val text: String,
    val isCompleted: Boolean = false,
    val order: Int = 0 
)
