package com.example.lockinplanner.data.local.dao

import androidx.room.*
import com.example.lockinplanner.data.local.entity.ChecklistEntity
import com.example.lockinplanner.data.local.entity.ObjectiveEntity
import com.example.lockinplanner.data.local.entity.ChecklistWithObjectives
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Transaction
    @Query("SELECT * FROM checklists ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllChecklists(): Flow<List<ChecklistWithObjectives>>

    @Transaction
    @Query("SELECT * FROM checklists WHERE id = :id")
    suspend fun getChecklistWithObjectivesById(id: String): ChecklistWithObjectives

    @Query("SELECT * FROM checklists WHERE id = :id")
    suspend fun getChecklistById(id: String): ChecklistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: ChecklistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObjective(objective: ObjectiveEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObjectives(objectives: List<ObjectiveEntity>)

    @Update
    suspend fun updateChecklist(checklist: ChecklistEntity)

    @Update
    suspend fun updateObjective(objective: ObjectiveEntity)

    @Delete
    suspend fun deleteChecklist(checklist: ChecklistEntity)
    
    @Query("DELETE FROM objectives WHERE checklistId = :checklistId")
    suspend fun deleteObjectivesForChecklist(checklistId: String)

    @Query("DELETE FROM checklists")
    suspend fun deleteAllChecklists()
}
