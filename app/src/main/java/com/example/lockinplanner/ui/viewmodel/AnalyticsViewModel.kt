package com.example.lockinplanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lockinplanner.data.repository.ChecklistRepository
import com.example.lockinplanner.data.repository.NotesRepository
import com.example.lockinplanner.data.repository.TaskRepository
import com.example.lockinplanner.domain.model.toDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AnalyticsState(
    // Filters
    val taskTypeFilter: String = "All", // "All", "Single", "Daily", "Custom"
    val taskTagFilter: String? = null, // null means "All"
    val availableTags: List<String> = emptyList(),

    // Task Metrics
    val totalTasks: Int = 0,
    val pastTasks: Int = 0, // Single only
    val upcomingTasks: Int = 0, // Single only
    val avgTaskLengthMins: Int = 0,

    // Checklist Metrics
    val totalChecklists: Int = 0,
    val completedChecklists: Int = 0,
    val avgCompletionPercentage: Float = 0f,

    // Notes Metrics
    val totalShorts: Int = 0,
    val totalBooks: Int = 0,
    val avgChaptersPerBook: Float = 0f,
    val avgPagesPerChapter: Float = 0f,
    val avgPagesPerBook: Float = 0f
)

class AnalyticsViewModel(
    private val taskRepository: TaskRepository,
    private val checklistRepository: ChecklistRepository,
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _taskTypeFilter = MutableStateFlow("All")
    private val _taskTagFilter = MutableStateFlow<String?>(null)

    private data class TaskData(
        val tasksEntities: List<com.example.lockinplanner.data.local.entity.TaskEntity>,
        val uniqueTags: List<String>,
        val typeFilter: String,
        val tagFilter: String?
    )

    private data class NotesData(
        val shorts: List<com.example.lockinplanner.data.local.entity.ShortEntity>,
        val books: List<com.example.lockinplanner.data.local.entity.BookEntity>,
        val chapters: List<com.example.lockinplanner.data.local.entity.ChapterEntity>,
        val pages: List<com.example.lockinplanner.data.local.entity.PageEntity>
    )

    private val taskDataFlow = combine(
        taskRepository.allTasks,
        taskRepository.uniqueTags,
        _taskTypeFilter,
        _taskTagFilter
    ) { tasks, tags, type, tag -> TaskData(tasks, tags, type, tag) }

    private val notesDataFlow = combine(
        notesRepository.allShorts,
        notesRepository.allBooks,
        notesRepository.allChapters,
        notesRepository.allPages
    ) { shorts, books, chapters, pages -> NotesData(shorts, books, chapters, pages) }

    val state: StateFlow<AnalyticsState> = combine(
        taskDataFlow,
        notesDataFlow,
        checklistRepository.allChecklists
    ) { taskData, notesData, checklists ->
        
        val tasks = taskData.tasksEntities.map { it.toDomain() }

        // --- TASKS ---
        val filteredTasks = tasks.filter { task ->
            val matchesType = when (taskData.typeFilter) {
                "All" -> true
                else -> task.repeatability == taskData.typeFilter
            }
            val matchesTag = taskData.tagFilter == null || task.tag == taskData.tagFilter
            matchesType && matchesTag
        }

        val currentTime = System.currentTimeMillis()
        var totalLengthMins = 0L
        var pastCount = 0
        var upcomingCount = 0

        filteredTasks.forEach { task ->
            // Average Length handles all tasks, calculating duration using absolute time or minutes
            if (!task.isFloating) {
                totalLengthMins += (task.endTime - task.startTime) / 60000
            } else {
                // For floating tasks, it's stored conceptually as minutes since midnight. 
                // duration is end - start. Handle overnight floating duration if needed.
                var startMins = task.startTime
                var endMins = task.endTime
                if (endMins < startMins) endMins += (24 * 60)
                totalLengthMins += (endMins - startMins)
            }

            // Past/Upcoming logic focuses only on Single tasks. Repeating tasks are ignored.
            if (task.repeatability == "Single") {
                if (task.endTime < currentTime) {
                    pastCount++
                } else if (task.startTime > currentTime) {
                    upcomingCount++
                }
            }
        }

        val avgTaskLen = if (filteredTasks.isNotEmpty()) (totalLengthMins / filteredTasks.size).toInt() else 0

        // --- CHECKLISTS ---
        var totalItems = 0
        var completedItems = 0
        var fullCompletedCount = 0

        checklists.forEach { wrapper ->
            val items = wrapper.objectives
            if (items.isNotEmpty()) {
                val done = items.count { it.isCompleted }
                totalItems += items.size
                completedItems += done
                if (done == items.size) {
                    fullCompletedCount++
                }
            }
        }
        val avgCompletion = if (totalItems > 0) (completedItems.toFloat() / totalItems.toFloat()) * 100f else 0f

        // --- NOTES ---
        val avgChapters = if (notesData.books.isNotEmpty()) notesData.chapters.size.toFloat() / notesData.books.size.toFloat() else 0f
        val avgPagesPerChap = if (notesData.chapters.isNotEmpty()) notesData.pages.size.toFloat() / notesData.chapters.size.toFloat() else 0f
        val avgPagesBook = if (notesData.books.isNotEmpty()) notesData.pages.size.toFloat() / notesData.books.size.toFloat() else 0f

        AnalyticsState(
            taskTypeFilter = taskData.typeFilter,
            taskTagFilter = taskData.tagFilter,
            availableTags = taskData.uniqueTags,
            totalTasks = filteredTasks.size,
            pastTasks = pastCount,
            upcomingTasks = upcomingCount,
            avgTaskLengthMins = avgTaskLen,
            totalChecklists = checklists.size,
            completedChecklists = fullCompletedCount,
            avgCompletionPercentage = avgCompletion,
            totalShorts = notesData.shorts.size,
            totalBooks = notesData.books.size,
            avgChaptersPerBook = avgChapters,
            avgPagesPerChapter = avgPagesPerChap,
            avgPagesPerBook = avgPagesBook
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsState())

    fun setTaskTypeFilter(type: String) {
        _taskTypeFilter.value = type
    }

    fun setTaskTagFilter(tag: String?) {
        _taskTagFilter.value = tag
    }
}

class AnalyticsViewModelFactory(
    private val taskRepository: TaskRepository,
    private val checklistRepository: ChecklistRepository,
    private val notesRepository: NotesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalyticsViewModel(taskRepository, checklistRepository, notesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
