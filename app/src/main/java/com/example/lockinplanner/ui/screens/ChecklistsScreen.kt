package com.example.lockinplanner.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lockinplanner.data.local.AppDatabase
import com.example.lockinplanner.data.local.entity.ChecklistWithObjectives
import com.example.lockinplanner.data.repository.ChecklistRepository
import com.example.lockinplanner.domain.model.UserPreferences
import com.example.lockinplanner.ui.components.ListCreateDialog
import com.example.lockinplanner.ui.components.ListImportPopup
import com.example.lockinplanner.ui.components.ListViewPopup
import com.example.lockinplanner.ui.components.ObjectiveUiModel
import com.example.lockinplanner.ui.viewmodel.ChecklistViewModel
import com.example.lockinplanner.ui.viewmodel.ChecklistViewModelFactory
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.lockinplanner.ui.utils.performLightHapticFeedback
import java.io.InputStreamReader
import java.io.BufferedReader

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChecklistsScreen(
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { ChecklistRepository(database.checklistDao()) }
    val viewModel: ChecklistViewModel = viewModel(
        factory = ChecklistViewModelFactory(repository)
    )

    val checklists by viewModel.checklists.collectAsState()
    
    var columnCount by remember { mutableIntStateOf(2) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedChecklistId by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    // Undo State
    var deletedChecklist by remember { mutableStateOf<ChecklistWithObjectives?>(null) }
    var showUndoSnackbar by remember { mutableStateOf(false) }

    val view = LocalView.current
    
    // Derived state for the selected item to ensure it updates with the Flow
    val selectedChecklist = remember(checklists, selectedChecklistId) {
        checklists.find { it.checklist.id == selectedChecklistId }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this checklist?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedChecklist?.let {
                            if (userPreferences.undoEnabled) {
                                deletedChecklist = it
                                showUndoSnackbar = true
                            }
                            viewModel.deleteChecklist(it.checklist)
                            selectedChecklistId = null
                        }
                        showDeleteConfirmDialog = false
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Checklists", style = MaterialTheme.typography.headlineMedium)
                Row {
                    IconButton(onClick = { 
                        view.performLightHapticFeedback(userPreferences.hapticsEnabled)
                        columnCount = when (columnCount) {
                            1 -> 2
                            2 -> 3
                            else -> 1
                        }
                    }) {
                        Icon(Icons.Default.List, contentDescription = "Change View")
                    }
                    IconButton(onClick = { 
                        view.performLightHapticFeedback(userPreferences.hapticsEnabled)
                        showImportDialog = true 
                    }) {
                        Text("▼")
                    }
                    IconButton(onClick = { 
                        view.performLightHapticFeedback(userPreferences.hapticsEnabled)
                        showCreateDialog = true 
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New Checklist")
                    }
                }
            }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(checklists, key = { it.checklist.id }) { item ->
                    ChecklistCard(
                        checklistWithObjectives = item,
                        isExpandedView = columnCount == 1,
                        onClick = { selectedChecklistId = item.checklist.id }
                    )
                }
            }
        }
        
        if (showCreateDialog) {
            ListCreateDialog(
                initialName = selectedChecklist?.checklist?.name ?: "",
                initialObjectives = selectedChecklist?.objectives?.map { ObjectiveUiModel(it.id, it.text) } ?: emptyList(),
                onDismissRequest = { 
                    showCreateDialog = false 
                },
                onSave = { name, objectives ->
                    if (selectedChecklist != null) {
                        viewModel.updateChecklist(selectedChecklist!!.checklist.id, name, objectives)
                        // Keep selection open? It will update automatically.
                    } else {
                        viewModel.createChecklist(name, objectives.map { it.text })
                    }
                    showCreateDialog = false
                }
            )
        }

        if (showImportDialog) {
            ListImportPopup(
                onDismissRequest = { showImportDialog = false },
                onSave = { name: String, items: List<String> ->
                    viewModel.createChecklist(name, items)
                    showImportDialog = false
                }
            )
        }
        
        // Popup
        if (selectedChecklistId != null) {
            // IF the item was deleted, selectedChecklist becomes null, so we should check that
            if (selectedChecklist != null) {
                 ListViewPopup(
                     checklistWithObjectives = selectedChecklist,
                     onDismissRequest = { selectedChecklistId = null },
                     onCheckItem = { objective, isChecked ->
                         viewModel.toggleObjective(objective, isChecked)
                     },
                     onEdit = { 
                          showCreateDialog = true
                     },
                     onDelete = {
                         if (userPreferences.confirmDeletion) {
                             showDeleteConfirmDialog = true
                         } else {
                             if (userPreferences.undoEnabled) {
                                  deletedChecklist = selectedChecklist
                                  showUndoSnackbar = true
                             }
                             viewModel.deleteChecklist(selectedChecklist.checklist)
                             selectedChecklistId = null
                         }
                     },
                     onMove = { obj, up ->
                         viewModel.moveObjective(obj, up)
                     }
                 )
            } else {
                // Item might have been deleted but ID is still set? 
                // Reset ID if not found and was previously set?
                // SideEffect or LaunchedEffect to clear ID if not found?
                LaunchedEffect(Unit) {
                    selectedChecklistId = null
                }
            }
        }
        
        // Undo Snackbar
        if (showUndoSnackbar && deletedChecklist != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                com.example.lockinplanner.ui.components.UndoSnackbar(
                    message = "Deleted checklist",
                    durationSeconds = userPreferences.undoDuration,
                    onUndo = {
                        deletedChecklist?.let { viewModel.restoreChecklist(it) }
                        deletedChecklist = null
                        showUndoSnackbar = false
                    },
                    onDismiss = {
                        showUndoSnackbar = false
                        deletedChecklist = null
                    }
                )
            }
        }
    }
}

@Composable
fun ChecklistCard(
    checklistWithObjectives: ChecklistWithObjectives,
    isExpandedView: Boolean,
    onClick: () -> Unit
) {
    val checklist = checklistWithObjectives.checklist
    // Check if effectively completed (all objectives done)
    val allObjectivesDone = checklistWithObjectives.objectives.isNotEmpty() && 
                            checklistWithObjectives.objectives.all { it.isCompleted }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        colors = CardDefaults.cardColors(
             containerColor = if (checklist.isCompleted || allObjectivesDone) 
                 MaterialTheme.colorScheme.secondaryContainer 
             else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
         Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
             if (checklist.isCompleted || allObjectivesDone) {
                 Column(
                     modifier = Modifier.align(Alignment.Center),
                     horizontalAlignment = Alignment.CenterHorizontally
                 ) {
                     Icon(
                         Icons.Default.Check, 
                         contentDescription = null, 
                         modifier = Modifier.size(48.dp),
                         tint = MaterialTheme.colorScheme.primary
                     )
                     Text(
                         text = checklist.name,
                         style = MaterialTheme.typography.titleMedium,
                         maxLines = 2,
                         overflow = TextOverflow.Ellipsis
                     )
                 }
             } else {
                 Column {
                     Text(
                         text = checklist.name,
                         style = MaterialTheme.typography.titleMedium,
                         maxLines = 1,
                         overflow = TextOverflow.Ellipsis
                     )
                     Spacer(modifier = Modifier.height(8.dp))
                     
                       val sortedObjectives = remember(checklistWithObjectives.objectives) {
                           checklistWithObjectives.objectives.sortedWith(
                               Comparator { a, b ->
                                   val c1 = a.isCompleted.compareTo(b.isCompleted)
                                   if (c1 != 0) c1 else a.order.compareTo(b.order)
                               }
                           )
                       }
                      
                      val total = sortedObjectives.size
                      val completedCount = sortedObjectives.count { it.isCompleted }
                      val progress = if (total == 0) 0f else completedCount.toFloat() / total
                      
                      LinearProgressIndicator(
                          progress = progress,
                          modifier = Modifier.fillMaxWidth().height(4.dp).padding(vertical = 4.dp),
                          trackColor = MaterialTheme.colorScheme.surfaceVariant,
                          color = MaterialTheme.colorScheme.primary
                      )
                      
                      val maxItems = if (isExpandedView) 10 else 3
                      
                      sortedObjectives.take(maxItems).forEach { obj ->
                          Text(
                              text = "• ${obj.text}",
                              style = MaterialTheme.typography.bodySmall,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis
                          )
                      }
                      if (sortedObjectives.size > maxItems) {
                          Text(
                              text = "+ ${sortedObjectives.size - maxItems} more",
                              style = MaterialTheme.typography.labelSmall,
                              color = Color.Gray
                          )
                      }
                  }
              }
          }
    }
}
