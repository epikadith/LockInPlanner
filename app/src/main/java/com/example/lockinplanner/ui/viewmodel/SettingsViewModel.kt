package com.example.lockinplanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lockinplanner.data.repository.UserPreferencesRepository
import com.example.lockinplanner.domain.model.AppTheme
import com.example.lockinplanner.domain.model.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.first

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val taskRepository: com.example.lockinplanner.data.repository.TaskRepository,
    private val checklistRepository: com.example.lockinplanner.data.repository.ChecklistRepository
) : ViewModel() {

    private val exportManager = com.example.lockinplanner.domain.data.DataExportManager()
    private val importManager = com.example.lockinplanner.domain.data.DataImportManager()

    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val availableTimezones: List<String> = java.util.TimeZone.getAvailableIDs().sorted().toList()

    fun updateTheme(theme: AppTheme) = viewModelScope.launch {
        userPreferencesRepository.updateTheme(theme)
    }

    fun updateDateFormat(format: String) = viewModelScope.launch {
        userPreferencesRepository.updateDateFormat(format)
    }

    fun updateTimeFormat(is24h: Boolean) = viewModelScope.launch {
        userPreferencesRepository.updateTimeFormat(is24h)
    }
    
    fun updateConfirmDeletion(enabled: Boolean) = viewModelScope.launch {
        userPreferencesRepository.updateConfirmDeletion(enabled)
    }

    fun updateTimezoneEnabled(enabled: Boolean) = viewModelScope.launch {
        userPreferencesRepository.updateTimezoneEnabled(enabled)
    }
    
    fun updateMainTimezone(timezoneId: String) = viewModelScope.launch {
        userPreferencesRepository.updateMainTimezone(timezoneId)
    }

    fun addTimezoneProfile(name: String, timezoneId: String) = viewModelScope.launch {
        val currentProfiles = userPreferences.value.timezoneProfiles.toMutableList()
        currentProfiles.add(com.example.lockinplanner.domain.model.TimezoneProfile(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            timezoneId = timezoneId
        ))
        userPreferencesRepository.updateTimezoneProfiles(currentProfiles)
    }

    fun removeTimezoneProfile(profileId: String) = viewModelScope.launch {
        val currentProfiles = userPreferences.value.timezoneProfiles.filter { it.id != profileId }
        userPreferencesRepository.updateTimezoneProfiles(currentProfiles)
        if (userPreferences.value.selectedProfileId == profileId) {
             userPreferencesRepository.updateSelectedProfileId(null)
        }
    }
    
    fun selectTimezoneProfile(profileId: String?) = viewModelScope.launch {
        userPreferencesRepository.updateSelectedProfileId(profileId)
    }

    fun updateNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        userPreferencesRepository.updateNotificationsEnabled(enabled)
    }

    fun updateNotifyDaily(enabled: Boolean) = viewModelScope.launch {
        userPreferencesRepository.updateNotifyDaily(enabled)
    }

    fun updateNotifyCustom(enabled: Boolean) = viewModelScope.launch {
        userPreferencesRepository.updateNotifyCustom(enabled)
    }

    fun updateNotifySingle(enabled: Boolean) = viewModelScope.launch {
        userPreferencesRepository.updateNotifySingle(enabled)
    }

    // --- Export Logic ---
    fun exportData(
        uri: android.net.Uri, 
        contentResolver: android.content.ContentResolver,
        options: Set<String>, // "Tasks", "Checklists"
        format: com.example.lockinplanner.domain.data.ExportFormat,
        onResult: (Boolean, String) -> Unit
    ) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val tasks = if (options.contains("Tasks")) {
                taskRepository.allTasks.first()
            } else emptyList<com.example.lockinplanner.data.local.entity.TaskEntity>()
            
            val checklists = if (options.contains("Checklists")) {
                 checklistRepository.allChecklists.first().map { 
                     com.example.lockinplanner.domain.data.ChecklistWithItems(it.checklist, it.objectives)
                 }
            } else emptyList<com.example.lockinplanner.domain.data.ChecklistWithItems>()
            
            val payload = com.example.lockinplanner.domain.data.ExportData(tasks, checklists)
            val outputString = exportManager.exportData(payload, format)
            
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(outputString.toByteArray())
            }
            launch(kotlinx.coroutines.Dispatchers.Main) { onResult(true, "Export Successful") }
        } catch (e: Exception) {
            launch(kotlinx.coroutines.Dispatchers.Main) { onResult(false, e.message ?: "Unknown Error") }
        }
    }

    // --- Import Logic ---
    fun importData(
        uri: android.net.Uri,
        contentResolver: android.content.ContentResolver,
        format: com.example.lockinplanner.domain.data.ExportFormat,
        onResult: (Boolean, String) -> Unit
    ) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val result = importManager.importData(inputStream, format)
                when (result) {
                    is com.example.lockinplanner.domain.data.DataImportManager.ImportResult.Success -> {
                        // Insert Tasks
                        result.data.tasks.forEach { task ->
                            taskRepository.insert(task.copy(id = 0)) // Force new ID
                        }
                        // Insert Checklists
                        result.data.checklists.forEach { list ->
                             val newChecklistId = java.util.UUID.randomUUID().toString()
                             checklistRepository.insertChecklist(list.checklist.copy(id = newChecklistId))
                             val newItems = list.items.map { item -> 
                                 item.copy(
                                     id = java.util.UUID.randomUUID().toString(),
                                     checklistId = newChecklistId 
                                 )
                             }
                             checklistRepository.insertObjectives(newItems)
                        }
                        
                        launch(kotlinx.coroutines.Dispatchers.Main) { onResult(true, "Import Successful") }
                    }
                    is com.example.lockinplanner.domain.data.DataImportManager.ImportResult.Error -> {
                        launch(kotlinx.coroutines.Dispatchers.Main) { onResult(false, result.message) }
                    }
                }
            }
        } catch (e: Exception) {
            launch(kotlinx.coroutines.Dispatchers.Main) { onResult(false, "Failed to open file: ${e.message}") }
        }
    }

    // --- Delete Logic ---
    fun deleteAllData(deleteTasks: Boolean, deleteChecklists: Boolean, onResult: () -> Unit) = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        if (deleteTasks) {
            taskRepository.deleteAllTasks()
        }
        if (deleteChecklists) {
            checklistRepository.deleteAllChecklists()
        }
        launch(kotlinx.coroutines.Dispatchers.Main) { onResult() }
    }
}

class SettingsViewModelFactory(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val taskRepository: com.example.lockinplanner.data.repository.TaskRepository,
    private val checklistRepository: com.example.lockinplanner.data.repository.ChecklistRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(userPreferencesRepository, taskRepository, checklistRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
