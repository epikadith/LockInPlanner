package com.example.lockinplanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lockinplanner.data.local.entity.ChecklistEntity
import com.example.lockinplanner.data.local.entity.ObjectiveEntity
import com.example.lockinplanner.ui.components.ObjectiveUiModel
import com.example.lockinplanner.data.repository.ChecklistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChecklistViewModel(private val repository: ChecklistRepository) : ViewModel() {
    val checklists = repository.allChecklists
        .map { list ->
            list.sortedBy { item ->
                // Sort Key: Is Completed?
                // A list is "completed" if:
                // 1. item.checklist.isCompleted is TRUE
                // 2. OR all objectives are completed (and there is at least one objective)
                val isEffectivelyCompleted = item.checklist.isCompleted || 
                    (item.objectives.isNotEmpty() && item.objectives.all { it.isCompleted })
                isEffectivelyCompleted
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createChecklist(name: String, objectivesCodes: List<String>) {
        viewModelScope.launch {
            val checklist = ChecklistEntity(name = name)
            repository.insertChecklist(checklist)
            
            val objectiveEntities = objectivesCodes.mapIndexed { index, text ->
                ObjectiveEntity(checklistId = checklist.id, text = text, order = index)
            }
            repository.insertObjectives(objectiveEntities)
        }
    }
    
    fun updateChecklist(checklistId: String, name: String, objectives: List<ObjectiveUiModel>) {
        viewModelScope.launch {
            val checklist = repository.getChecklistById(checklistId) ?: return@launch
            repository.updateChecklist(checklist.copy(name = name))
            
            val completeData = repository.getChecklistWithObjectivesById(checklistId)
            val existingObjectives = completeData.objectives.associateBy { it.id }
            
            val newObjectiveEntities = objectives.mapIndexed { index, uiObj ->
                if (uiObj.id != null && existingObjectives.containsKey(uiObj.id)) {
                    val old = existingObjectives[uiObj.id]!!
                    old.copy(text = uiObj.text, order = index)
                } else {
                    ObjectiveEntity(checklistId = checklistId, text = uiObj.text, order = index)
                }
            }
            
            repository.deleteObjectivesForChecklist(checklistId)
            repository.insertObjectives(newObjectiveEntities)
        }
    }

    fun toggleObjective(objective: ObjectiveEntity, isCompleted: Boolean) {
        viewModelScope.launch {
             val updatedObjective = objective.copy(isCompleted = isCompleted)
             repository.updateObjective(updatedObjective)
        }
    }
    
    fun moveObjective(objective: ObjectiveEntity, moveUp: Boolean) {
        viewModelScope.launch {
            // Get all objectives for this checklist
            val checklistWithObjectives = repository.getChecklistWithObjectivesById(objective.checklistId)
            
            // We only reorder the UNCOMPLETED items relative to each other.
            // Completed items stay at the bottom (or wherever) essentially ignored by this logic,
            // but we must preserve their 'order' fields or just update uncompleted ones.
            // To be safe, we should re-index uncompleted items to be 0..N, 
            // and maybe leave completed items as is? 
            // Better: Re-index uncompleted items to 0, 1, 2... 
            // This ensures they stay at top and are ordered.
            
            val uncompleted = checklistWithObjectives.objectives
                .filter { !it.isCompleted }
                .sortedWith(compareBy({ it.order }, { it.id })) // Stable sort
                .toMutableList()

            val index = uncompleted.indexOfFirst { it.id == objective.id }
            if (index == -1) return@launch

            var listChanged = false
            if (moveUp && index > 0) {
                java.util.Collections.swap(uncompleted, index, index - 1)
                listChanged = true
            } else if (!moveUp && index < uncompleted.size - 1) {
                java.util.Collections.swap(uncompleted, index, index + 1)
                listChanged = true
            }

            if (listChanged) {
                // Re-assign orders
                val updates = uncompleted.mapIndexed { i, obj ->
                    obj.copy(order = i)
                }
                repository.insertObjectives(updates)
            }
        }
    }

    fun deleteChecklist(checklist: ChecklistEntity) {
        viewModelScope.launch {
            repository.deleteChecklist(checklist)
        }
    }

    fun restoreChecklist(checklistWithObjectives: com.example.lockinplanner.data.local.entity.ChecklistWithObjectives) {
        viewModelScope.launch {
            repository.restoreChecklist(checklistWithObjectives)
        }
    }
}

class ChecklistViewModelFactory(private val repository: ChecklistRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChecklistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChecklistViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
