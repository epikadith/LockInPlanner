package com.example.lockinplanner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lockinplanner.data.local.AppDatabase
import com.example.lockinplanner.data.local.entity.TaskEntity
import com.example.lockinplanner.data.repository.ChecklistRepository
import com.example.lockinplanner.data.repository.TaskRepository
import com.example.lockinplanner.data.repository.UserPreferencesRepository
import com.example.lockinplanner.domain.model.Task
import com.example.lockinplanner.domain.model.UserPreferences
import com.example.lockinplanner.domain.model.toDomain
import com.example.lockinplanner.domain.notification.AlarmScheduler
import com.example.lockinplanner.ui.components.ListViewPopup
import com.example.lockinplanner.ui.components.PositionedTask
import com.example.lockinplanner.ui.components.TaskBuilder
import com.example.lockinplanner.ui.components.TaskView
import com.example.lockinplanner.ui.viewmodel.ChecklistViewModel
import com.example.lockinplanner.ui.viewmodel.ChecklistViewModelFactory
import com.example.lockinplanner.ui.viewmodel.SearchResult
import com.example.lockinplanner.ui.viewmodel.SearchViewModel
import com.example.lockinplanner.ui.viewmodel.TimelineViewModel
import com.example.lockinplanner.ui.viewmodel.TimelineViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    
    val checklistRepository = remember { ChecklistRepository(database.checklistDao()) }
    val checklistViewModel: ChecklistViewModel = viewModel(
        factory = ChecklistViewModelFactory(checklistRepository)
    )

    val userPreferencesRepository = remember { UserPreferencesRepository(context) }
    val alarmScheduler = remember { AlarmScheduler(context) }
    val taskRepository = remember { TaskRepository(database.taskDao(), alarmScheduler, userPreferencesRepository) }
    val timelineViewModel: TimelineViewModel = viewModel(
        factory = TimelineViewModelFactory(taskRepository)
    )

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val includeTasks by viewModel.includeTasks.collectAsStateWithLifecycle()
    val includeLists by viewModel.includeLists.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val allChecklists by checklistViewModel.checklists.collectAsState()

    // Undo State
    var deletedTask by remember { mutableStateOf<Task?>(null) }
    var deletedChecklist by remember { mutableStateOf<com.example.lockinplanner.data.local.entity.ChecklistWithObjectives?>(null) }
    var showUndoSnackbar by remember { mutableStateOf(false) }
    var undoMessage by remember { mutableStateOf("") }
    
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showTaskBuilder by remember { mutableStateOf(false) }

    var selectedListId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Search") }
        )

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search tasks and lists...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            singleLine = true
        )

        // Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = includeTasks,
                    onCheckedChange = { viewModel.toggleIncludeTasks(it) }
                )
                Text("Tasks", style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = includeLists,
                    onCheckedChange = { viewModel.toggleIncludeLists(it) }
                )
                Text("Lists", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Results List
        if (searchQuery.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Type to start searching", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { result ->
                    SearchResultCard(
                        result = result,
                        onClick = {
                            when (result) {
                                is SearchResult.TaskItem -> selectedTask = result.task.toDomain()
                                is SearchResult.ChecklistItem -> selectedListId = result.checklistWithObjectives.checklist.id
                            }
                        }
                    )
                }
            }
        }
    }

    // Overlays
    selectedTask?.let { task ->
        TaskView(
            task = PositionedTask(task, 0),
            onDismiss = { selectedTask = null },
            onDelete = {
                if (userPreferences.undoEnabled) {
                    deletedTask = task
                    deletedChecklist = null
                    undoMessage = "Deleted task"
                    showUndoSnackbar = true
                }
                timelineViewModel.delete(task)
                selectedTask = null
            },
            onEdit = {
                taskToEdit = task
                selectedTask = null
                showTaskBuilder = true
            },
            is24h = userPreferences.timeFormat24h,
            confirmDeletion = userPreferences.confirmDeletion
        )
    }

    if (showTaskBuilder) {
        TaskBuilder(
            onDismiss = { 
                showTaskBuilder = false 
                taskToEdit = null
            },
            onSave = { newTask ->
                if (taskToEdit != null) {
                    timelineViewModel.update(newTask)
                } else {
                    timelineViewModel.insert(newTask)
                }
                showTaskBuilder = false
                taskToEdit = null
            },
            initialStartTime = null,
            initialEndTime = null,
            dateFormat = userPreferences.dateFormat,
            is24h = userPreferences.timeFormat24h,
            timeZone = com.example.lockinplanner.domain.manager.DateTimeManager.getDisplayTimeZone(userPreferences),
            taskToEdit = taskToEdit,
            hapticsEnabled = userPreferences.hapticsEnabled
        )
    }

    selectedListId?.let { id ->
        val list = allChecklists.find { it.checklist.id == id }
        if (list != null) {
            ListViewPopup(
                checklistWithObjectives = list,
                onDismissRequest = { selectedListId = null },
                onCheckItem = { objective, isChecked ->
                    checklistViewModel.toggleObjective(objective, isChecked)
                },
                onEdit = { 
                     // We would ideally open ListCreateDialog here.
                     // It requires slightly more plumbing. We can dismiss for now, or implement it.
                },
                onDelete = {
                    if (userPreferences.undoEnabled) {
                        deletedChecklist = list
                        deletedTask = null
                        undoMessage = "Deleted checklist"
                        showUndoSnackbar = true
                    }
                    checklistViewModel.deleteChecklist(list.checklist)
                    selectedListId = null
                },
                onMove = { obj, up ->
                    checklistViewModel.moveObjective(obj, up)
                }
            )
        } else {
            selectedListId = null
        }
    }
    
    // Undo Snackbar
    if (showUndoSnackbar && (deletedTask != null || deletedChecklist != null)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            com.example.lockinplanner.ui.components.UndoSnackbar(
                message = undoMessage,
                durationSeconds = userPreferences.undoDuration,
                onUndo = {
                    deletedTask?.let { timelineViewModel.insert(it) }
                    deletedChecklist?.let { checklistViewModel.restoreChecklist(it) }
                    deletedTask = null
                    deletedChecklist = null
                    showUndoSnackbar = false
                },
                onDismiss = {
                    showUndoSnackbar = false
                    deletedTask = null
                    deletedChecklist = null
                }
            )
        }
    }
}

@Composable
fun SearchResultCard(
    result: SearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val name = when (result) {
                is SearchResult.TaskItem -> result.task.name
                is SearchResult.ChecklistItem -> result.checklistWithObjectives.checklist.name
            }
            
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            val badgeText = when (result) {
                is SearchResult.TaskItem -> "T"
                is SearchResult.ChecklistItem -> "L"
            }
            
            val badgeColor = when (result) {
                is SearchResult.TaskItem -> MaterialTheme.colorScheme.primaryContainer
                is SearchResult.ChecklistItem -> MaterialTheme.colorScheme.secondaryContainer
            }
            
            val badgeTextColor = when (result) {
                is SearchResult.TaskItem -> MaterialTheme.colorScheme.onPrimaryContainer
                is SearchResult.ChecklistItem -> MaterialTheme.colorScheme.onSecondaryContainer
            }
            
            Surface(
                shape = MaterialTheme.shapes.small,
                color = badgeColor,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeTextColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
