package com.example.lockinplanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lockinplanner.data.repository.TaskRepository
import com.example.lockinplanner.domain.model.Task
import com.example.lockinplanner.domain.model.toDomain
import com.example.lockinplanner.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TimelineViewModel(private val repository: TaskRepository) : ViewModel() {

    val allTasks: Flow<List<Task>> = repository.allTasks.map { entities ->
        entities.map { it.toDomain() }
    }

    fun getTasksForDate(date: Long, nextDate: Long, timeZoneId: String): Flow<List<Task>> {
        return repository.getTasksForDate(date, nextDate, timeZoneId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun insert(task: Task) = viewModelScope.launch {
        repository.insert(task.toEntity())
    }

    fun update(task: Task) = viewModelScope.launch {
        repository.update(task.toEntity())
    }

    fun delete(task: Task) = viewModelScope.launch {
        repository.delete(task.toEntity())
    }
}

class TimelineViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimelineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimelineViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
