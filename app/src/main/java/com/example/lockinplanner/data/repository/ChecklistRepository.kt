package com.example.lockinplanner.data.repository

import com.example.lockinplanner.data.local.dao.ChecklistDao
import com.example.lockinplanner.data.local.entity.ChecklistEntity
import com.example.lockinplanner.data.local.entity.ObjectiveEntity
import com.example.lockinplanner.data.local.entity.ChecklistWithObjectives
import kotlinx.coroutines.flow.Flow

class ChecklistRepository(private val checklistDao: ChecklistDao) {
    val allChecklists: Flow<List<ChecklistWithObjectives>> = checklistDao.getAllChecklists()

    suspend fun getChecklistById(id: String): ChecklistEntity? {
        return checklistDao.getChecklistById(id)
    }

    suspend fun getChecklistWithObjectivesById(id: String): ChecklistWithObjectives {
        // We know we can get flow but we want one-shot. 
        // We need to add this to DAO.
        return checklistDao.getChecklistWithObjectivesById(id)
    }

    suspend fun insertChecklist(checklist: ChecklistEntity) {
        checklistDao.insertChecklist(checklist)
    }

    suspend fun insertObjective(objective: ObjectiveEntity) {
        checklistDao.insertObjective(objective)
    }
    
    suspend fun insertObjectives(objectives: List<ObjectiveEntity>) {
        checklistDao.insertObjectives(objectives)
    }

    suspend fun updateChecklist(checklist: ChecklistEntity) {
        checklistDao.updateChecklist(checklist)
    }

    suspend fun updateObjective(objective: ObjectiveEntity) {
        checklistDao.updateObjective(objective)
    }

    suspend fun deleteChecklist(checklist: ChecklistEntity) {
        checklistDao.deleteChecklist(checklist)
    }
    
    suspend fun deleteObjectivesForChecklist(checklistId: String) {
        checklistDao.deleteObjectivesForChecklist(checklistId)
    }

    suspend fun deleteAllChecklists() {
        checklistDao.deleteAllChecklists()
    }
}
