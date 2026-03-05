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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.ColorUtils

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToAnalytics: () -> Unit = {},
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



        // Custom Theme Options
        if (userPreferences.theme == AppTheme.Custom) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var showPrimaryDialog by remember { mutableStateOf(false) }
                val currentPrimary = userPreferences.customPrimaryColor?.let { androidx.compose.ui.graphics.Color(it.toULong()) } ?: MaterialTheme.colorScheme.background

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Background Colour (Primary)")
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(currentPrimary)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            .clickable { showPrimaryDialog = true }
                    )
                }

                if (showPrimaryDialog) {
                    CustomColorPickerDialog(
                        initialColor = currentPrimary,
                        onColorSelected = { viewModel.updateCustomPrimaryColor(it.value.toLong()) },
                        onDismiss = { showPrimaryDialog = false }
                    )
                }

                var showSecondaryDialog by remember { mutableStateOf(false) }
                val currentSecondary = userPreferences.customSecondaryColor?.let { androidx.compose.ui.graphics.Color(it.toULong()) } ?: MaterialTheme.colorScheme.primary

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Foreground Colour (Secondary)")
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(currentSecondary)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            .clickable { showSecondaryDialog = true }
                    )
                }

                if (showSecondaryDialog) {
                    CustomColorPickerDialog(
                        initialColor = currentSecondary,
                        primaryColorConstraint = currentPrimary,
                        onColorSelected = { viewModel.updateCustomSecondaryColor(it.value.toLong()) },
                        onDismiss = { showSecondaryDialog = false }
                    )
                }
            }
        }

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
        
        OutlinedButton(
            onClick = onNavigateToAnalytics,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text("Open Analytics")
        }
        
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
            onConfirm = { deleteTasks, deleteChecklists, deleteShorts, deleteBooks ->
                viewModel.deleteAllData(deleteTasks, deleteChecklists, deleteShorts, deleteBooks) {
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
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = currentOptions.contains("Shorts"),
                        onCheckedChange = { 
                            val new = if (it) currentOptions + "Shorts" else currentOptions - "Shorts"
                            onOptionsChanged(new)
                        }
                    )
                    Text("Shorts")
                    Spacer(modifier = Modifier.width(16.dp))
                    Checkbox(
                        checked = currentOptions.contains("Books"),
                        onCheckedChange = { 
                            val new = if (it) currentOptions + "Books" else currentOptions - "Books"
                            onOptionsChanged(new)
                        }
                    )
                    Text("Books")
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
    onConfirm: (Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    var deleteTasks by remember { mutableStateOf(false) }
    var deleteChecklists by remember { mutableStateOf(false) }
    var deleteShorts by remember { mutableStateOf(false) }
    var deleteBooks by remember { mutableStateOf(false) }

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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = deleteShorts,
                        onCheckedChange = { deleteShorts = it }
                    )
                    Text("Delete All Shorts")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = deleteBooks,
                        onCheckedChange = { deleteBooks = it }
                    )
                    Text("Delete All Books")
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(deleteTasks, deleteChecklists, deleteShorts, deleteBooks) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = deleteTasks || deleteChecklists || deleteShorts || deleteBooks
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

@Composable
fun CustomColorPickerDialog(
    initialColor: androidx.compose.ui.graphics.Color,
    primaryColorConstraint: androidx.compose.ui.graphics.Color? = null,
    onColorSelected: (androidx.compose.ui.graphics.Color) -> Unit,
    onDismiss: () -> Unit
) {
    var red by remember { mutableStateOf(initialColor.red) }
    var green by remember { mutableStateOf(initialColor.green) }
    var blue by remember { mutableStateOf(initialColor.blue) }

    val currentColor = androidx.compose.ui.graphics.Color(red, green, blue)
    var isValid = true
    var contrastRatio = 0.0

    if (primaryColorConstraint != null) {
        contrastRatio = ColorUtils.calculateContrast(currentColor.toArgb(), primaryColorConstraint.toArgb())
        isValid = contrastRatio >= 3.0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp))
                )

                if (primaryColorConstraint != null) {
                    Text(
                        text = if (isValid) "Contrast OK: ${String.format("%.1f", contrastRatio)}:1" else "Low Contrast: ${String.format("%.1f", contrastRatio)}:1\nMust be >= 3.0",
                        color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                ColorSliderRow("R", red, { red = it })
                ColorSliderRow("G", green, { green = it })
                ColorSliderRow("B", blue, { blue = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorSelected(currentColor)
                    onDismiss()
                },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ColorSliderRow(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = (value * 255).toInt().toString(),
            modifier = Modifier.width(32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
