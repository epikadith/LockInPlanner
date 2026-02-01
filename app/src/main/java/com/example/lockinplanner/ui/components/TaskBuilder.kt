package com.example.lockinplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.lockinplanner.domain.model.Task
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import java.text.SimpleDateFormat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.util.TimeZone
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBuilder(
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    initialStartTime: String? = null,
    initialEndTime: String? = null,
    initialDate: Long? = null,
    dateFormat: String = "dd/MM/yyyy",
    is24h: Boolean = true,
    timeZone: TimeZone = TimeZone.getDefault(),
    taskToEdit: Task? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        var taskName by remember { mutableStateOf(taskToEdit?.name ?: "") }
        var taskDescription by remember { mutableStateOf(taskToEdit?.description ?: "") }
        val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta, Color.Cyan)
        var selectedColor by remember { mutableStateOf(colors.find { it.toArgb() == taskToEdit?.color?.toArgb() } ?: colors.first()) }

        // Initialize Time Text
        val initialStartText = taskToEdit?.let { 
             String.format(Locale.US, "%02d:%02d", it.startHour, it.startMinute) 
        } ?: initialStartTime ?: ""
        
        val initialEndText = taskToEdit?.let {
             String.format(Locale.US, "%02d:%02d", it.endHour, it.endMinute)
        } ?: initialEndTime ?: ""

        var startTimeText by remember { mutableStateOf(initialStartText) }
        var endTimeText by remember { mutableStateOf(initialEndText) }

        val startTimePickerState = rememberTimePickerState(
            initialHour = startTimeText.substringBefore(":").toIntOrNull() ?: 0,
            initialMinute = startTimeText.substringAfter(":").toIntOrNull() ?: 0,
            is24Hour = is24h
        )
        val endTimePickerState = rememberTimePickerState(
            initialHour = endTimeText.substringBefore(":").toIntOrNull() ?: 0,
            initialMinute = endTimeText.substringAfter(":").toIntOrNull() ?: 0,
            is24Hour = is24h
        )
        
        var showStartTimePicker by remember { mutableStateOf(false) }
        var showEndTimePicker by remember { mutableStateOf(false) }

        val datePickerState = rememberDatePickerState()
        var showDatePicker by remember { mutableStateOf(false) }
        
        val sdf = remember(dateFormat, timeZone) {
            SimpleDateFormat(dateFormat, Locale.US).apply { this.timeZone = timeZone }
        }
        
        // Initialize Date Text
        val initialDateText = remember(initialDate, timeZone, sdf, taskToEdit) {
             if (taskToEdit != null && !taskToEdit.isFloating) {
                 // Convert task startTime to date string
                 val cal = Calendar.getInstance(timeZone)
                 cal.timeInMillis = taskToEdit.startTime
                 sdf.format(cal.time)
             } else if (initialDate != null) {
                sdf.format(java.util.Date(initialDate))
            } else {
                sdf.format(Calendar.getInstance(timeZone).time)
            }
        }
        
        var dateText by remember { mutableStateOf(initialDateText) }

        var repeatability by remember { mutableStateOf(taskToEdit?.repeatability ?: "Single") }
        var selectedDays by remember { 
            mutableStateOf(
                if (taskToEdit?.repeatability == "Custom" && taskToEdit.customRepeatDays != null) {
                    val days = mutableSetOf<Int>()
                    for (i in 0..6) {
                        if ((taskToEdit.customRepeatDays and (1 shl i)) != 0) {
                            days.add(i)
                        }
                    }
                    days
                } else {
                    emptySet<Int>()
                }
            ) 
        }

        var customColor by remember { mutableStateOf(selectedColor) }
        var customColorHex by remember { mutableStateOf(String.format("#%06X", (0xFFFFFF and selectedColor.toArgb()))) }
        var showCustomColorPicker by remember { mutableStateOf(false) }

        var reminders by remember { mutableStateOf(taskToEdit?.reminders ?: emptyList<Int>()) }
        var showReminderDialog by remember { mutableStateOf(false) }

        // User Requested: Validation updates. 
        // Logic: Start == End is NOT allowed for ANY type.
        // Start < End is OK.
        // Start > End is OK (Overnight / Multi-day).
        val timeError = if (startTimeText.isNotBlank() && endTimeText.isNotBlank()) {
                val start = startTimeText.split(":").map { it.toInt() }
                val end = endTimeText.split(":").map { it.toInt() }
                start[0] == end[0] && start[1] == end[1]
        } else false
        
        val isOvernight = if (!timeError && startTimeText.isNotBlank() && endTimeText.isNotBlank()) {
            val start = startTimeText.split(":").map { it.toInt() }
            val end = endTimeText.split(":").map { it.toInt() }
            end[0] < start[0] || (end[0] == start[0] && end[1] < start[1])
        } else false

        val isSaveEnabled = taskName.isNotBlank() && startTimeText.isNotBlank() && endTimeText.isNotBlank() && !timeError
        
        // Scroll State for overflow
        val scrollState = rememberScrollState()

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Removed duplicate header per user request
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (taskToEdit != null) "Edit Task" else "Create Task", style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                TextField(value = taskName, onValueChange = { taskName = it }, label = { Text("Task Name") }, modifier = Modifier.fillMaxWidth())

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Colour:")
                    Spacer(modifier = Modifier.size(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(colors) { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        2.dp,
                                        if (selectedColor == color) Color.Black else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable {
                                        selectedColor = color
                                        customColor = color
                                        customColorHex = String.format("#%06X", (0xFFFFFF and color.toArgb()))
                                    }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = { showCustomColorPicker = true }) { Text("Custom") }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(customColor)
                                .border(1.dp, Color.DarkGray)
                        )
                        Box {
                            Text(
                                text = customColorHex,
                                style = LocalTextStyle.current.copy(
                                    drawStyle = Stroke(width = 2f, join = StrokeJoin.Round),
                                    color = Color.Black
                                )
                            )
                            Text(
                                text = customColorHex,
                                color = customColor
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(onClick = { showStartTimePicker = true }) { Text("Start Time") }
                        Text(startTimeText)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(onClick = { showEndTimePicker = true }) { Text("End Time") }
                        Text(endTimeText)
                    }
                    if (timeError) {
                        Text(
                            "Start and End time cannot be the same",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { showDatePicker = true },
                            enabled = repeatability != "Daily" && repeatability != "Custom"
                        ) { Text("Select Date") }
                        
                        val displayDate = if (isOvernight && repeatability != "Daily" && repeatability != "Custom") {
                            try {
                                val startD = sdf.parse(dateText)
                                val cal = Calendar.getInstance(timeZone)
                                if (startD != null) {
                                    cal.time = startD
                                    cal.add(Calendar.DAY_OF_YEAR, 1)
                                    "$dateText - ${sdf.format(cal.time)}"
                                } else dateText
                            } catch (e: Exception) {
                                dateText
                            }
                        } else {
                            dateText
                        }
                        Text(displayDate)
                    }
                }

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    TextField(
                        value = repeatability,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Single") }, onClick = { repeatability = "Single"; expanded = false })
                        DropdownMenuItem(text = { Text("Daily") }, onClick = { repeatability = "Daily"; expanded = false })
                        DropdownMenuItem(text = { Text("Custom") }, onClick = { repeatability = "Custom"; expanded = false })
                    }
                }

                if (repeatability == "Custom") {
                    val days = listOf("S", "M", "T", "W", "T", "F", "S")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        days.forEachIndexed { index, day ->
                            val isSelected = selectedDays.contains(index)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray)
                                    .clickable {
                                        selectedDays = if (isSelected) {
                                            selectedDays - index
                                        } else {
                                            selectedDays + index
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Black
                                )
                            }
                        }
                    }
                }

                // Reminders Section
                Text("Reminders:", style = MaterialTheme.typography.bodyMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(reminders) { offset ->
                        val label = when {
                            offset == 0 -> "On Start"
                            offset < 0 -> "${-offset} min before"
                            else -> "$offset min after"
                        }
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                .clickable { reminders = reminders - offset }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { showReminderDialog = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Add", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                TextField(value = taskDescription, onValueChange = { taskDescription = it }, label = { Text("Description (Optional)") }, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = {
                        val isFloating = repeatability == "Daily" || repeatability == "Custom"
                        val startH = startTimeText.substringBefore(":").toInt()
                        val startM = startTimeText.substringAfter(":").toInt()
                        val endH = endTimeText.substringBefore(":").toInt()
                        val endM = endTimeText.substringAfter(":").toInt()
                        
                        // Parse Date in Target Timezone
                        val baseDate = if (!isFloating) {
                            sdf.parse(dateText)?.time ?: 0L
                        } else {
                            0L // Date irrelevant for floating storage (we store minutes)
                        }

                        val startTimeVal: Long
                        val endTimeVal: Long

                        if (isFloating) {
                            // Store as minutes from midnight
                            startTimeVal = (startH * 60 + startM).toLong()
                            endTimeVal = (endH * 60 + endM).toLong()
                        } else {
                            // Store as absolute UTC timestamp
                            // baseDate is already the "Start of Day" in target timezone (parsed via sdf with setTimeZone)
                            // We just add the hour/minute offsets in that timezone.
                            // BUT: sdf.parse returns a UTC timestamp representing that local time.
                            // e.g. "00:00 NY" -> 05:00 UTC.
                            // So we can just add the milliseconds.
                            // Wait, sdf.parse() returns the Millis where that date starts.
                            // Actually `sdf.timeZone = timeZone` means `parse` interprets the string as being IN that timezone.
                            // So `baseDate` is the correct absolute millis for 00:00 in that timezone.
                            // We just need to add the time offset.
                            // Since `timeZone` might have DST shifts during the day, simple addition (h*3600000) is RISKY.
                            // CORRECT WAY: Use Calendar in that Timezone.
                            val cal = Calendar.getInstance(timeZone)
                            cal.timeInMillis = baseDate
                            cal.set(Calendar.HOUR_OF_DAY, startH)
                            cal.set(Calendar.MINUTE, startM)
                            startTimeVal = cal.timeInMillis
                            
                            cal.set(Calendar.HOUR_OF_DAY, endH)
                            cal.set(Calendar.MINUTE, endM)
                            // Handle overnight? Builder validation currently prevents start > end.
                            // If we allow overnight later, we'd add +1 day here.
                            var tempEnd = cal.timeInMillis
                            if (tempEnd < startTimeVal) {
                                cal.add(Calendar.DAY_OF_YEAR, 1) // Assume it wraps to next day if user entered 23:00 -> 01:00
                                tempEnd = cal.timeInMillis
                            }
                            endTimeVal = tempEnd
                        }

                        val task = Task(
                            id = taskToEdit?.id ?: 0,
                            name = taskName,
                            description = taskDescription.takeIf { it.isNotBlank() },
                            color = selectedColor,
                            repeatability = repeatability,
                            customRepeatDays = if (repeatability == "Custom") {
                                selectedDays.fold(0) { acc, dayIndex -> acc or (1 shl dayIndex) }
                            } else null,
                            isFloating = isFloating,
                            startTime = startTimeVal,
                            endTime = endTimeVal,
                            reminders = reminders
                        )
                        onSave(task)
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = isSaveEnabled
                ) {
                    Text("Save")
                }

                if (showStartTimePicker) {
                    AlertDialog(
                        onDismissRequest = { showStartTimePicker = false },
                        title = { Text("Select Start Time") },
                        text = { TimePicker(state = startTimePickerState) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showStartTimePicker = false
                                    // Always store/display as HH:mm internally for simplicity in this variable, 
                                    // OR display formatted based on is24h?
                                    // The Builder logic relies on simple string parsing "HH:mm".
                                    // If we switch to "h:mm aa", the parsing logic breaks: `startTimeText.split(":")`.
                                    // CRITICAL: Keep internal variables `endTimeText` as `HH:mm` (24h) for easy parsing/logic?
                                    // OR update logic to handle 12h.
                                    // User sees `startTimeText`.
                                    // Let's keep `startTimeText` as 24h "HH:mm" for now to avoid breaking validation logic, even if picker is 12h.
                                    // The user asked for "Display times...".
                                    // Ideally, TaskBuilder should show strictly what the user selected.
                                    // If I keep 24h string, but show 12h picker, it's slightly inconsistent but safe.
                                    // If I change `startTimeText` to "02:00 PM", I must update the validation logic and the saving logic.
                                    // Saving logic: `startTimeText.substringBefore(":")`.
                                    // I will keep it 24H for simplicity in this iteration inside the BUILDER text field, 
                                    // but the PICKER will respect the setting.
                                    // Actually, looking at `TaskBuilder`, the text is displayed: `Text(startTimeText)`.
                                    // If I show "14:00" while user selected "2:00 PM", it's fine.
                                    // Let's stick to 24H in the text field for now to minimize logic breakage, 
                                    // but the TimePicker is 12/24 based on setting.
                                    startTimeText = String.format(Locale.US, "%02d:%02d", startTimePickerState.hour, startTimePickerState.minute)
                                }
                            ) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") }
                        }
                    )
                }

                if (showEndTimePicker) {
                    AlertDialog(
                        onDismissRequest = { showEndTimePicker = false },
                        title = { Text("Select End Time") },
                        text = { TimePicker(state = endTimePickerState) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showEndTimePicker = false
                                    endTimeText = String.format(Locale.US, "%02d:%02d", endTimePickerState.hour, endTimePickerState.minute)
                                }
                            ) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") }
                        }
                    )
                }

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDatePicker = false
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        // Picked date is UTC. Convert logic to Target TimeZone.
                                        val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                                        utcCal.timeInMillis = millis
                                        
                                        val targetCal = Calendar.getInstance(timeZone)
                                        targetCal.set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                        targetCal.set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                        targetCal.set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                        targetCal.set(Calendar.HOUR_OF_DAY, 0)
                                        targetCal.set(Calendar.MINUTE, 0)
                                        targetCal.set(Calendar.SECOND, 0)
                                        targetCal.set(Calendar.MILLISECOND, 0)
                                        
                                        dateText = sdf.format(targetCal.time)
                                    }
                                }
                            ) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                if (showCustomColorPicker) {
                    val controller = rememberColorPickerController()
                    AlertDialog(
                        onDismissRequest = { showCustomColorPicker = false },
                        title = { Text("Select Custom Colour") },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                HsvColorPicker(
                                    modifier = Modifier
                                        .size(300.dp)
                                        .padding(16.dp),
                                    controller = controller,
                                    onColorChanged = { colorEnvelope: ColorEnvelope ->
                                        customColor = colorEnvelope.color
                                        customColorHex = String.format("#%06X", (0xFFFFFF and colorEnvelope.color.toArgb()))
                                    }
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    selectedColor = customColor
                                    showCustomColorPicker = false
                                }
                            ) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCustomColorPicker = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }

    if (showReminderDialog) {
        var minutesText by remember { mutableStateOf("10") }
        var type by remember { mutableStateOf("Before") } // Before, After, On Start
        
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Add Reminder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Before", "On Start", "After").forEach { t ->
                            Box(
                                modifier = Modifier
                                    .border(
                                        1.dp, 
                                        if (type == t) MaterialTheme.colorScheme.primary else Color.Gray, 
                                        CircleShape
                                    )
                                    .clickable { type = t }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    t, 
                                    color = if (type == t) MaterialTheme.colorScheme.primary else Color.Gray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    if (type != "On Start") {
                        TextField(
                            value = minutesText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) minutesText = it },
                            label = { Text("Minutes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val mins = minutesText.toIntOrNull() ?: 0
                    val offset = when (type) {
                        "Before" -> -mins
                        "After" -> mins
                        else -> 0
                    }
                    if (!reminders.contains(offset)) {
                        reminders = reminders + offset
                    }
                    showReminderDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDialog = false }) { Text("Cancel") }
            }
        )
    }
}
}
