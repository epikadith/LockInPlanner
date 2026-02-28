package com.example.lockinplanner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lockinplanner.domain.model.AppTheme
import com.example.lockinplanner.ui.viewmodel.SettingsViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val userPreferences by viewModel.userPreferences.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)

        // Preferences Section
        SectionHeader("Preferences")
        
        // Theme
        SettingsDropdown(
            label = "Theme",
            options = AppTheme.values().map { it.name },
            selectedOption = userPreferences.theme.name,
            onOptionSelected = { viewModel.updateTheme(AppTheme.valueOf(it)) }
        )

        // Date Format
        val dateFormats = listOf("dd/MM/yyyy", "MM/dd/yyyy", "yyyy/MM/dd")
        SettingsDropdown(
            label = "Date Format",
            options = dateFormats,
            selectedOption = userPreferences.dateFormat,
            onOptionSelected = { viewModel.updateDateFormat(it) }
        )

        // Time Format
        val timeFormats = listOf("12h", "24h")
        SettingsDropdown(
            label = "Time Format",
            options = timeFormats,
            selectedOption = if (userPreferences.timeFormat24h) "24h" else "12h",
            onOptionSelected = { viewModel.updateTimeFormat(it == "24h") }
        )

        // Deletion Confirmation
        SettingsToggle(
            label = "Deletion Confirmation",
            checked = userPreferences.confirmDeletion,
            onCheckedChange = { viewModel.updateConfirmDeletion(it) }
        )

        // Haptics
        SettingsToggle(
            label = "Haptic Feedback",
            checked = userPreferences.hapticsEnabled,
            onCheckedChange = { viewModel.updateHapticsEnabled(it) }
        )

        // Undo Deletion
        SettingsToggle(
            label = "Undo Delete Snackbar",
            checked = userPreferences.undoEnabled,
            onCheckedChange = { viewModel.updateUndoEnabled(it) }
        )

        if (userPreferences.undoEnabled) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Duration (Seconds)")
                    Text("${userPreferences.undoDuration}s")
                }
                androidx.compose.material3.Slider(
                    value = userPreferences.undoDuration.toFloat(),
                    onValueChange = { viewModel.updateUndoDuration(it.toInt()) },
                    valueRange = 1f..20f,
                    steps = 18
                )
            }
        }

        // Notifications Section
        SectionHeader("Notifications")

        SettingsToggle(
            label = "Enable Notifications",
            checked = userPreferences.notificationsEnabled,
            onCheckedChange = { viewModel.updateNotificationsEnabled(it) }
        )
        
        if (userPreferences.notificationsEnabled) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                SettingsToggle(
                    label = "Daily Tasks",
                    checked = userPreferences.notifyDaily,
                    onCheckedChange = { viewModel.updateNotifyDaily(it) }
                )
                SettingsToggle(
                    label = "Custom Tasks",
                    checked = userPreferences.notifyCustom,
                    onCheckedChange = { viewModel.updateNotifyCustom(it) }
                )
                SettingsToggle(
                    label = "Single Tasks",
                    checked = userPreferences.notifySingle,
                    onCheckedChange = { viewModel.updateNotifySingle(it) }
                )
            }
        }


        // Data Management Section
        SectionHeader("Data Management")
        
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf(com.example.lockinplanner.domain.data.ExportFormat.JSON) }
    var exportOptions by remember { mutableStateOf(setOf("Tasks", "Checklists")) }
    
    // Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            when(exportFormat) {
                com.example.lockinplanner.domain.data.ExportFormat.JSON -> "application/json"
                com.example.lockinplanner.domain.data.ExportFormat.CSV -> "text/csv"
                com.example.lockinplanner.domain.data.ExportFormat.TSV -> "text/tab-separated-values"
                com.example.lockinplanner.domain.data.ExportFormat.TXT -> "text/plain"
            }
        )
    ) { uri ->
        uri?.let {
            viewModel.exportData(it, context.contentResolver, exportOptions, exportFormat) { success, msg ->
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Import Launcher & State
    var pendingImportFormat by remember { mutableStateOf(com.example.lockinplanner.domain.data.ExportFormat.JSON) }
    val importLauncherWithFormat = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importData(it, context.contentResolver, pendingImportFormat) { success, msg ->
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    var showImportFormatDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedButton(onClick = { showExportDialog = true }) {
            Text("Export Backup")
        }
        OutlinedButton(onClick = { showImportFormatDialog = true }) {
            Text("Import Data")
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeletePastDialog by remember { mutableStateOf(false) }
    
    OutlinedButton(
        onClick = { showDeleteDialog = true },
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Delete All Data")
    }

    OutlinedButton(
        onClick = { showDeletePastDialog = true },
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Delete Past Tasks Only")
    }

    if (showDeleteDialog) {
        DeleteDataDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = { deleteTasks, deleteChecklists ->
                viewModel.deleteAllData(deleteTasks, deleteChecklists) {
                    showDeleteDialog = false
                    android.widget.Toast.makeText(context, "Deleted Selected Data", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showDeletePastDialog) {
        DeletePastTasksDialog(
            onDismiss = { showDeletePastDialog = false },
            onConfirm = {
                viewModel.deletePastTasks {
                    showDeletePastDialog = false
                    android.widget.Toast.makeText(context, "Deleted Past Tasks", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // About Section
    SectionHeader("About")
    
    var showLicenseDialog by remember { mutableStateOf(false) }
    
    OutlinedButton(
        onClick = { showLicenseDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Open Source Licenses")
    }

    if (showLicenseDialog) {
        com.example.lockinplanner.ui.components.LicenseDialog(
            onDismiss = { showLicenseDialog = false }
        )
    }

        if (showExportDialog) {
            ExportDialog(
                onDismiss = { showExportDialog = false },
                onOptionsChanged = { exportOptions = it },
                currentOptions = exportOptions,
                onFormatChanged = { exportFormat = it },
                currentFormat = exportFormat,
                onExport = {
                    showExportDialog = false
                    exportLauncher.launch("lockin_backup_${System.currentTimeMillis()}.${exportFormat.name.lowercase()}")
                }
            )
        }
        
        if (showImportFormatDialog) {
             ImportFormatDialog(
                onDismiss = { showImportFormatDialog = false },
                onFormatSelected = { format ->
                    pendingImportFormat = format
                    showImportFormatDialog = false
                    val mime = when(format) {
                        com.example.lockinplanner.domain.data.ExportFormat.JSON -> arrayOf("application/json")
                        com.example.lockinplanner.domain.data.ExportFormat.CSV -> arrayOf("text/csv", "text/comma-separated-values")
                        else -> arrayOf("*/*")
                    }
                    importLauncherWithFormat.launch(mime)
                }
            )
        }
    }
}

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    currentOptions: Set<String>,
    onOptionsChanged: (Set<String>) -> Unit,
    currentFormat: com.example.lockinplanner.domain.data.ExportFormat,
    onFormatChanged: (com.example.lockinplanner.domain.data.ExportFormat) -> Unit,
    onExport: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Export Data", style = MaterialTheme.typography.titleLarge)
                
                Text("Content", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = currentOptions.contains("Tasks"),
                        onCheckedChange = { 
                            val new = if (it) currentOptions + "Tasks" else currentOptions - "Tasks"
                            onOptionsChanged(new)
                        }
                    )
                    Text("Tasks")
                    Spacer(modifier = Modifier.width(16.dp))
                    Checkbox(
                        checked = currentOptions.contains("Checklists"),
                        onCheckedChange = { 
                            val new = if (it) currentOptions + "Checklists" else currentOptions - "Checklists"
                            onOptionsChanged(new)
                        }
                    )
                    Text("Checklists")
                }

                Text("Format", style = MaterialTheme.typography.titleMedium)
                com.example.lockinplanner.domain.data.ExportFormat.values().forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFormatChanged(format) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentFormat == format,
                            onClick = { onFormatChanged(format) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(format.name)
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = onExport,
                        enabled = currentOptions.isNotEmpty()
                    ) { Text("Export") }
                }
            }
        }
    }
}

@Composable
fun ImportFormatDialog(
    onDismiss: () -> Unit,
    onFormatSelected: (com.example.lockinplanner.domain.data.ExportFormat) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
             shape = MaterialTheme.shapes.medium,
             color = MaterialTheme.colorScheme.surface,
             modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
             Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Select Import Format", style = MaterialTheme.typography.titleLarge)
                
                com.example.lockinplanner.domain.data.ExportFormat.values().forEach { format ->
                     OutlinedButton(
                         onClick = { onFormatSelected(format) },
                         modifier = Modifier.fillMaxWidth()
                     ) {
                         Text(format.name)
                     }
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Cancel") }
             }
        }
    }
}


@Composable
fun DeleteDataDialog(
    onDismiss: () -> Unit,
    onConfirm: (Boolean, Boolean) -> Unit
) {
    var deleteTasks by remember { mutableStateOf(false) }
    var deleteChecklists by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Delete All Data", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                Text(
                    "This action cannot be undone. Please select what you want to delete:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = deleteTasks,
                        onCheckedChange = { deleteTasks = it }
                    )
                    Text("Delete All Tasks")
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = deleteChecklists,
                        onCheckedChange = { deleteChecklists = it }
                    )
                    Text("Delete All Checklists")
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(deleteTasks, deleteChecklists) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = deleteTasks || deleteChecklists
                    ) { Text("Confirm Delete") }
                }
            }
        }
    }
}

@Composable
fun DeletePastTasksDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Delete Past Tasks", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                Text(
                    "Are you sure you want to delete all past single tasks? This action cannot be undone. Daily and Custom tasks will not be affected.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Confirm Delete") }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Divider()
    }
}

@Composable
fun SettingsDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        
        Box {
            Row(
                modifier = Modifier
                    .clickable { expanded = true }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedOption, style = MaterialTheme.typography.bodyMedium)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
