package com.example.lockinplanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lockinplanner.data.local.entity.ChecklistWithObjectives
import com.example.lockinplanner.data.local.entity.TaskEntity
import com.example.lockinplanner.data.repository.ChecklistRepository
import com.example.lockinplanner.data.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

sealed class SearchResult {
    data class TaskItem(val task: TaskEntity) : SearchResult()
    data class ChecklistItem(val checklistWithObjectives: ChecklistWithObjectives) : SearchResult()
}

class SearchViewModel(
    private val taskRepository: TaskRepository,
    private val checklistRepository: ChecklistRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _includeTasks = MutableStateFlow(true)
    val includeTasks: StateFlow<Boolean> = _includeTasks.asStateFlow()

    private val _includeLists = MutableStateFlow(true)
    val includeLists: StateFlow<Boolean> = _includeLists.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<SearchResult>> = combine(
        _searchQuery,
        _includeTasks,
        _includeLists
    ) { query, tasks, lists ->
        Triple(query, tasks, lists)
    }.flatMapLatest { (query, tasksEnabled, listsEnabled) ->
        if (query.isBlank()) {
            flowOf(emptyList())
        } else {
            val tasksFlow = if (tasksEnabled) taskRepository.searchTasks(query) else flowOf(emptyList())
            val listsFlow = if (listsEnabled) checklistRepository.searchChecklists(query) else flowOf(emptyList())

            combine(tasksFlow, listsFlow) { tasks, lists ->
                val combined = mutableListOf<SearchResult>()
                combined.addAll(tasks.map { SearchResult.TaskItem(it) })
                combined.addAll(lists.map { SearchResult.ChecklistItem(it) })
                combined.sortedBy { 
                    when (it) {
                        is SearchResult.TaskItem -> it.task.name
                        is SearchResult.ChecklistItem -> it.checklistWithObjectives.checklist.name
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleIncludeTasks(include: Boolean) {
        _includeTasks.value = include
    }

    fun toggleIncludeLists(include: Boolean) {
        _includeLists.value = include
    }
}

class SearchViewModelFactory(
    private val taskRepository: TaskRepository,
    private val checklistRepository: ChecklistRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(taskRepository, checklistRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
