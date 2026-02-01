package com.example.lockinplanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lockinplanner.data.repository.TaskRepository
import com.example.lockinplanner.domain.model.Task
import com.example.lockinplanner.domain.model.toDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import com.example.lockinplanner.domain.model.toEntity
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar> = _currentMonth

    private val _showDaily = MutableStateFlow(false)
    val showDaily: StateFlow<Boolean> = _showDaily

    // We fetch all tasks and filter them in the UI or here. 
    // For the calendar grid, we need to know which days have tasks.
    val tasks: StateFlow<List<Task>> = repository.allTasks
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun nextMonth() {
        val next = _currentMonth.value.clone() as Calendar
        next.add(Calendar.MONTH, 1)
        _currentMonth.value = next
    }

    fun previousMonth() {
        val prev = _currentMonth.value.clone() as Calendar
        prev.add(Calendar.MONTH, -1)
        _currentMonth.value = prev
    }

    fun toggleShowDaily() {
        _showDaily.value = !_showDaily.value
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

class CalendarViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
