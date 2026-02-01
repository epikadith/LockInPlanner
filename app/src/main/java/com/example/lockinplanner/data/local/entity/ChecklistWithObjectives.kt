package com.example.lockinplanner.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ChecklistWithObjectives(
    @Embedded val checklist: ChecklistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "checklistId"
    )
    val objectives: List<ObjectiveEntity>
)
