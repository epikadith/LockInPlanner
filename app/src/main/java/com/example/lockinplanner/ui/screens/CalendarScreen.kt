package com.example.lockinplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lockinplanner.domain.model.Task
import com.example.lockinplanner.domain.model.UserPreferences
import com.example.lockinplanner.ui.components.TaskBuilder
import com.example.lockinplanner.ui.components.TaskView
import com.example.lockinplanner.ui.viewmodel.CalendarViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier
) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val showDaily by viewModel.showDaily.collectAsState()
    
    val dateFormatString = userPreferences.dateFormat
    val is24h = userPreferences.timeFormat24h
    
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    val dateFormat = SimpleDateFormat(dateFormatString, Locale.US)

    val timeZone = com.example.lockinplanner.domain.manager.DateTimeManager.getDisplayTimeZone(userPreferences)
    
    // Project tasks into the selected timezone for correct display hours
    val projectedTasks = remember(tasks, timeZone) {
        tasks.map { task ->
            if (task.isFloating) {
                // Formatting for floating tasks is already correct (H:M derived from minutes)
                // Assuming toDomain populated startHour/Minute correctly from minutes.
                val s = task.startTime.toInt()
                val e = task.endTime.toInt()
                task.copy(
                    startHour = s / 60,
                    startMinute = s % 60,
                    endHour = e / 60,
                    endMinute = e % 60,
                    isThemeColor = task.isThemeColor
                )
            } else {
                // Fixed: Convert UTC timestamp to Selected Timezone Hour/Minute
                val cal = Calendar.getInstance(timeZone)
                cal.timeInMillis = task.startTime
                val sH = cal.get(Calendar.HOUR_OF_DAY)
                val sM = cal.get(Calendar.MINUTE)
                
                cal.timeInMillis = task.endTime
                val eH = cal.get(Calendar.HOUR_OF_DAY)
                val eM = cal.get(Calendar.MINUTE)
                
                task.copy(
                    startHour = sH,
                    startMinute = sM,
                    endHour = eH,
                    endMinute = eM,
                    isThemeColor = task.isThemeColor
                )
            }
        }
    }

    val daysInMonth = remember(currentMonth, timeZone) {
        getDaysInMonth(currentMonth, timeZone)
    }

    var showTaskBuilder by remember { mutableStateOf(false) }
    var taskBuilderDate by remember { mutableStateOf<Long?>(null) }
    var selectedDay by remember { mutableStateOf<CalendarDay?>(null) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    
    // Undo State
    var deletedTask by remember { mutableStateOf<Task?>(null) }
    var showUndoSnackbar by remember { mutableStateOf(false) }
    
    // DateFormat should also respect the selected TimeZone for displaying "December 1, 2024" etc.
    // Actually current code uses 'dateFormat' for popup title.
    dateFormat.timeZone = timeZone

    if (showTaskBuilder) {
        TaskBuilder(
            onDismiss = { 
                showTaskBuilder = false 
                taskToEdit = null
            },
            onSave = { task ->
                if (taskToEdit != null) {
                    viewModel.update(task)
                } else {
                    viewModel.insert(task)
                }
                showTaskBuilder = false
                taskToEdit = null
            },
            initialDate = taskBuilderDate,
            dateFormat = dateFormatString,
            is24h = is24h,
            timeZone = timeZone,
            taskToEdit = taskToEdit,
            hapticsEnabled = userPreferences.hapticsEnabled
        )
    }

    selectedDay?.let { day ->
        if (day.date != null) {
            val dateString = dateFormat.format(day.date.time) // Keep for display title
            val dayStartMillis = day.date.timeInMillis 
            
            val checkTime = dayStartMillis // day.date is already normalized to midnight in correct TZ
            
            val dayOfWeek = day.date.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun, 6=Sat

            val dayTasks = projectedTasks.filter { task ->
                if (task.isFloating) {
                    // Floating: Check Repeatability logic locally
                    if (task.repeatability == "Daily") {
                         if (showDaily) return@filter true else return@filter false
                    }
                    if (task.repeatability == "Custom") {
                        val mask = task.customRepeatDays ?: 0
                        (mask and (1 shl dayOfWeek)) != 0
                    } else {
                        false
                    }
                } else {
                    // Fixed: Check overlap with 'checkTime' (Start of Day) to 'checkTime + 24h'
                    // checkTime is 'dayStartMillis' (calculated above).
                    val startOfDay = checkTime
                    val nextDay = day.date.clone() as Calendar
                    nextDay.add(Calendar.DAY_OF_YEAR, 1)
                    val endOfDayCal = nextDay.timeInMillis
                    
                    // Overlap:
                    task.startTime < endOfDayCal && task.endTime > startOfDay
                }
            }.sortedBy { it.startHour }

            DayView(
                date = dateString,
                tasks = dayTasks,
                onDismiss = { selectedDay = null },
                onTaskClick = { selectedTask = it },
                onAddTask = {
                    taskBuilderDate = checkTime // Using the normalized time
                    taskToEdit = null
                    showTaskBuilder = true
                },
                is24h = is24h
            )
        }
    }
    

    selectedTask?.let { task ->
        TaskView(
            task = com.example.lockinplanner.ui.components.PositionedTask(task, 0), // PositionedTask wrapper needed for TaskView
            onDismiss = { selectedTask = null },
            onDelete = {
                if (userPreferences.undoEnabled) {
                    deletedTask = task
                    showUndoSnackbar = true
                }
                viewModel.delete(task)
                selectedTask = null
            },
            onEdit = {
                taskToEdit = task
                selectedTask = null
                showTaskBuilder = true
            },
            is24h = is24h,
            confirmDeletion = userPreferences.confirmDeletion
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // New Task Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = {
                    taskBuilderDate = null
                    taskToEdit = null
                    showTaskBuilder = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "New Task")
                }
            }

            // Month Navigation and Toggle
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }
                    Text(
                        text = monthFormat.format(currentMonth.time),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show Daily", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = showDaily,
                        onCheckedChange = { viewModel.toggleShowDaily() },
                        modifier = Modifier.scale(0.7f)
                    )
                }
            }
        }

        // Days of Week
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Calendar Grid
        val daysInMonth = getDaysInMonth(currentMonth, timeZone)
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize()
        ) {
            items(daysInMonth) { day ->
                val dayTasks = if (day.date != null) {
                    val checkCalendar = day.date.clone() as Calendar
                    checkCalendar.set(Calendar.HOUR_OF_DAY, 0)
                    checkCalendar.set(Calendar.MINUTE, 0)
                    checkCalendar.set(Calendar.SECOND, 0)
                    checkCalendar.set(Calendar.MILLISECOND, 0)
                    val checkTime = checkCalendar.timeInMillis

                    val dayOfWeek = day.date.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun, 6=Sat (to match TaskBuilder)
                    
                    projectedTasks.filter { task ->
                        if (task.isFloating) {
                            // Floating
                            if (task.repeatability == "Daily") {
                                 if (showDaily) return@filter true else return@filter false
                            }
                            if (task.repeatability == "Custom") {
                                val mask = task.customRepeatDays ?: 0
                                (mask and (1 shl dayOfWeek)) != 0
                            } else {
                                false
                            }
                        } else {
                            // Fixed
                            val startOfDay = checkTime
                            val nextDay = day.date.clone() as Calendar
                            nextDay.add(Calendar.DAY_OF_YEAR, 1)
                            val endOfDayCal = nextDay.timeInMillis
                            
                            task.startTime < endOfDayCal && task.endTime > startOfDay
                        }
                    }
                } else {
                    emptyList()
                }
                val isToday = day.date?.let { 
                    val today = Calendar.getInstance(timeZone)
                    it.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    it.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                } ?: false

                DayCell(
                    day = day, 
                    tasks = dayTasks, 
                    isToday = isToday,
                    onClick = { if (day.date != null) selectedDay = day }
                )
            }
        }
    }
}

@Composable
fun DayCell(day: CalendarDay, tasks: List<Task>, isToday: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .background(Color.LightGray.copy(alpha = 0.2f))
            .clickable { onClick() },
    ) {
        if (day.dayOfMonth != -1) {
            Column(
                modifier = Modifier.fillMaxSize().padding(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                // Task Indicators
                tasks.take(4).forEach { task ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(vertical = 1.dp)
                            .background(task.color, RoundedCornerShape(2.dp))
                    )
                }
                if (tasks.size > 4) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(Color.Gray, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun DayView(
    date: String,
    tasks: List<Task>,
    onDismiss: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onAddTask: () -> Unit,
    is24h: Boolean = true
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = date, style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                LazyColumn(
                    modifier = Modifier.height(300.dp), // Limit height
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks) { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, if (task.isThemeColor) MaterialTheme.colorScheme.primary else task.color, RoundedCornerShape(8.dp))
                                .clickable { onTaskClick(task) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = task.name, style = MaterialTheme.typography.bodyLarge)
                            
                            val timeText = if (is24h) {
                                String.format(Locale.US, "%02d:%02d - %02d:%02d", task.startHour, task.startMinute, task.endHour, task.endMinute)
                            } else {
                                val startAmPm = if (task.startHour < 12) "AM" else "PM"
                                val startH = if (task.startHour == 0 || task.startHour == 12) 12 else task.startHour % 12
                                val endAmPm = if (task.endHour < 12) "AM" else "PM"
                                val endH = if (task.endHour == 0 || task.endHour == 12) 12 else task.endHour % 12
                                String.format(Locale.US, "%d:%02d %s - %d:%02d %s", startH, task.startMinute, startAmPm, endH, task.endMinute, endAmPm)
                            }
                            
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    if (tasks.isEmpty()) {
                        item {
                            Text("No tasks for this day.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                }

                Button(
                    onClick = onAddTask,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Add Task")
                }
            }
        }
    }
}

data class CalendarDay(val dayOfMonth: Int, val date: Calendar?)

fun getDaysInMonth(currentMonth: Calendar, timeZone: java.util.TimeZone): List<CalendarDay> {
    val days = mutableListOf<CalendarDay>()
    // create calendar in target timezone
    val calendar = Calendar.getInstance(timeZone)
    calendar.timeInMillis = currentMonth.timeInMillis
    calendar.set(Calendar.DAY_OF_MONTH, 1)

    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed (Sunday is 0)
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Empty slots for previous month
    for (i in 0 until firstDayOfWeek) {
        days.add(CalendarDay(-1, null))
    }

    // Days of current month
    for (i in 1..maxDays) {
        val dayCalendar = Calendar.getInstance(timeZone)
        dayCalendar.timeInMillis = calendar.timeInMillis
        dayCalendar.set(Calendar.DAY_OF_MONTH, i)
        // Normalize to midnight
        dayCalendar.set(Calendar.HOUR_OF_DAY, 0)
        dayCalendar.set(Calendar.MINUTE, 0)
        dayCalendar.set(Calendar.SECOND, 0)
        dayCalendar.set(Calendar.MILLISECOND, 0)
        days.add(CalendarDay(i, dayCalendar))
    }

    return days
}
