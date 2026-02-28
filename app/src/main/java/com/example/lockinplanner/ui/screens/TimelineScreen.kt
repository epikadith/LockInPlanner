package com.example.lockinplanner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lockinplanner.domain.model.Task
import com.example.lockinplanner.domain.model.UserPreferences
import com.example.lockinplanner.ui.components.PositionedTask
import com.example.lockinplanner.ui.components.TaskBuilder
import com.example.lockinplanner.ui.components.TaskView
import com.example.lockinplanner.ui.utils.performLightHapticFeedback
import com.example.lockinplanner.ui.viewmodel.TimelineViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier
) {
    var showTaskBuilder by remember { mutableStateOf(false) }
    var taskBuilderStartTime by remember { mutableStateOf<String?>(null) }
    var taskBuilderEndTime by remember { mutableStateOf<String?>(null) }
    
    // Use format from preferences
    val dateFormat = userPreferences.dateFormat
    val is24h = userPreferences.timeFormat24h

    // ... (code omitted: todayDate logic)
    // Use DateTimeManager regarding Timezone
    // Use DateTimeManager regarding Timezone
    val timeZone = com.example.lockinplanner.domain.manager.DateTimeManager.getDisplayTimeZone(userPreferences)
    
    val todayDateParams = remember(userPreferences) {
        val calendar = Calendar.getInstance(timeZone)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val end = calendar.timeInMillis
        
        Pair(start, end)
    }

    // Query DB with Start/End Range (UTC Timestamps) + TimeZone ID (for Floating day check)
    val tasks by viewModel.getTasksForDate(todayDateParams.first, todayDateParams.second, timeZone.id).collectAsState(initial = emptyList())
    
    // Position/Project Tasks
    val positionedTasks = remember(tasks, todayDateParams, timeZone) {
        // Here we project UTC tasks into "Today's Visual Grid" (0-24h).
        val viewStart = todayDateParams.first
        val viewEnd = todayDateParams.second
        
        val projectedTasks = tasks.mapNotNull { task ->
            if (task.isFloating) {
                // Floating: Convert stored "Minutes from Midnight" to Hour/Minute
                val startMins = task.startTime.toInt()
                val endMins = task.endTime.toInt()
                
                task.copy(
                    startHour = startMins / 60,
                    startMinute = startMins % 60,
                    endHour = endMins / 60,
                    endMinute = endMins % 60,
                    isThemeColor = task.isThemeColor
                )
            } else {
                // Fixed: Project UTC -> Local relative to Midnight.
                // We need to find the overlap of [taskStart, taskEnd] with [viewStart, viewEnd].
                // And map that overlap to 0..24h (or 0..1440 mins).
                
                // 1. Calculate Intersection
                val overlapStart = maxOf(viewStart, task.startTime)
                val overlapEnd = minOf(viewEnd, task.endTime)
                
                if (overlapStart < overlapEnd) {
                    // Task is visible today!
                    // Convert overlap timestamps to Minutes from Midnight (0-1440).
                    // offset = (timestamp - viewStart) -> millis from midnight.
                    val startOffsetMillis = overlapStart - viewStart
                    val endOffsetMillis = overlapEnd - viewStart
                    
                    val startTotalMins = (startOffsetMillis / 60000).toInt()
                    val endTotalMins = (endOffsetMillis / 60000).toInt()
                    
                    task.copy(
                        startHour = startTotalMins / 60,
                        startMinute = startTotalMins % 60,
                        endHour = endTotalMins / 60,
                        endMinute = endTotalMins % 60,
                        isThemeColor = task.isThemeColor
                    )
                } else {
                    null // Should have been filtered by SQL, but safe to filter here.
                }
            }
        }
        calculatePositionedTasks(projectedTasks)
    }
    
    var selectedTask by remember { mutableStateOf<Task?>(null) }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val horizontalScrollState = rememberScrollState()

    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    val view = LocalView.current
    
    // Undo State
    var deletedTask by remember { mutableStateOf<Task?>(null) }
    var showUndoSnackbar by remember { mutableStateOf(false) }
    
    if (showTaskBuilder) {
        TaskBuilder(
            onDismiss = { 
                showTaskBuilder = false 
                taskToEdit = null
            },
            onSave = { newTask ->
                if (taskToEdit != null) {
                    viewModel.update(newTask)
                } else {
                    viewModel.insert(newTask)
                }
                showTaskBuilder = false
                taskToEdit = null
            },
            initialStartTime = taskBuilderStartTime,
            initialEndTime = taskBuilderEndTime,
            dateFormat = dateFormat,
            is24h = is24h,
            timeZone = com.example.lockinplanner.domain.manager.DateTimeManager.getDisplayTimeZone(userPreferences),
            taskToEdit = taskToEdit,
            hapticsEnabled = userPreferences.hapticsEnabled
        )
    }

    selectedTask?.let {
        TaskView(
            task = PositionedTask(it, 0),
            onDismiss = { selectedTask = null },
            onDelete = {
                if (userPreferences.undoEnabled) {
                    deletedTask = it
                    showUndoSnackbar = true
                }
                viewModel.delete(it)
                selectedTask = null
            },
            onEdit = {
                taskToEdit = it
                selectedTask = null
                showTaskBuilder = true
            },
            is24h = is24h,
            confirmDeletion = userPreferences.confirmDeletion
        )
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                view.performLightHapticFeedback(userPreferences.hapticsEnabled)
                taskBuilderStartTime = null
                taskBuilderEndTime = null
                showTaskBuilder = true
            }) {
                Text("Add Task")
            }
            Button(onClick = {
                view.performLightHapticFeedback(userPreferences.hapticsEnabled)
                // Determine current hour in the selected timezone
                val calendar = com.example.lockinplanner.domain.manager.DateTimeManager.getCalendar(userPreferences)
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val scrollIndex = (currentHour - 6).coerceAtLeast(0)
                coroutineScope.launch {
                    lazyListState.animateScrollToItem(scrollIndex)
                }
            }) {
                Text("Center")
            }
        }
        LazyColumn(state = lazyListState) {
            items(24) { hour ->
                val timeLabel = com.example.lockinplanner.domain.manager.DateTimeManager.formatTime(hour, 0, userPreferences)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeLabel,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        val columnWidth = this.maxWidth / 3
                        
                        val maxColumns = (positionedTasks.maxOfOrNull { it.columnIndex } ?: -1) + 1
                        val totalColumns = maxOf(3, maxColumns)

                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            // Filter tasks that intersect with this hour
                            // Intersection: taskStart < hour+1 AND taskEnd > hour
                            // Note: A task starting at 4:30 intersects 4:00 hour. 
                            // A task ending at 4:30 intersects 4:00 hour.
                            val tasksInHour = positionedTasks.filter { 
                                val taskStart = it.task.startHour + it.task.startMinute / 60.0
                                val taskEnd = it.task.endHour + it.task.endMinute / 60.0
                                taskStart < hour + 1 && taskEnd > hour
                            }
                            
                            
                            val columnMap = tasksInHour.groupBy { it.columnIndex }
                            val maxColumns = (positionedTasks.maxOfOrNull { it.columnIndex } ?: -1) + 1
                            val totalColumns = maxOf(3, maxColumns)

                            for (i in 0 until totalColumns) {
                                Box(modifier = Modifier.width(columnWidth).fillMaxHeight().padding(horizontal = 2.dp)) {
                                    val tasksInCol = columnMap[i] ?: emptyList()
                                    tasksInCol.forEach { positionedTask ->
                                        val task = positionedTask.task
                                        // Calculate offsets for this specific hour block
                                        
                                        val startFraction = if (task.startHour == hour) task.startMinute / 60f else 0f
                                        val endFraction = if (task.endHour == hour) task.endMinute / 60f else 1f
                                        
                                        val topOffset = 80.dp * startFraction
                                        val height = 80.dp * (endFraction - startFraction)
                                        
                                        // Apply offset and height
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = topOffset)
                                                .height(height)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (task.isThemeColor) androidx.compose.material3.MaterialTheme.colorScheme.primary else task.color)
                                                .padding(4.dp)
                                                .clickable { 
                                                    view.performLightHapticFeedback(userPreferences.hapticsEnabled)
                                                    selectedTask = task 
                                                },
                                        ) {
                                            if (startFraction == 0f || height > 20.dp) { 
                                                 val startBlockHeight = 80f * (1f - (task.startMinute / 60f))
                                                 val startBlockTooSmall = startBlockHeight <= 20f
                                                 val shouldDrawTitle = task.startHour == hour || (startBlockTooSmall && task.startHour + 1 == hour)
                                                 
                                                 if (shouldDrawTitle) {
                                                    Box {
                                                        Text(
                                                            text = task.name,
                                                            fontSize = 12.sp,
                                                            style = LocalTextStyle.current.copy(
                                                                drawStyle = Stroke(width = 2f, join = StrokeJoin.Round)
                                                            ),
                                                            color = Color.Black
                                                        )
                                                        Text(
                                                            text = task.name,
                                                            fontSize = 12.sp,
                                                            color = Color.White
                                                        )
                                                    }
                                                 }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    IconButton(onClick = {
                        taskBuilderStartTime = String.format(Locale.US, "%02d:00", hour)
                        taskBuilderEndTime = String.format(Locale.US, "%02d:00", hour + 1)
                        showTaskBuilder = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add task for $hour:00")
                    }
                }
            }
        }
        
        // Undo Snackbar
        if (showUndoSnackbar && deletedTask != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                com.example.lockinplanner.ui.components.UndoSnackbar(
                    message = "Deleted task",
                    durationSeconds = userPreferences.undoDuration,
                    onUndo = {
                        deletedTask?.let { viewModel.insert(it) }
                        deletedTask = null
                        showUndoSnackbar = false
                    },
                    onDismiss = {
                        showUndoSnackbar = false
                        deletedTask = null
                    }
                )
            }
        }
    }
}

private fun calculatePositionedTasks(tasks: List<Task>): List<PositionedTask> {
    val splitTasks = tasks.flatMap { task ->
        if (task.endHour < task.startHour) {
            listOf(
                task.copy(endHour = 24),
                task.copy(startHour = 0)
            )
        } else {
            listOf(task)
        }
    }

    val sortedTasks = splitTasks.sortedWith(compareBy<Task> { it.startHour * 60 + it.startMinute }
        .thenByDescending { (it.endHour * 60 + it.endMinute) - (it.startHour * 60 + it.startMinute) })
    
    // Columns now store a list of tasks placed in that column to check for overlaps
    val columns = mutableListOf<MutableList<Task>>()
    val result = mutableListOf<PositionedTask>()

    for (task in sortedTasks) {
        var placed = false
        val taskStartMins = task.startHour * 60 + task.startMinute
        val taskEndMins = task.endHour * 60 + task.endMinute

        for (i in columns.indices) {
            // Check if this task overlaps with ANY task in column[i]
            val hasOverlap = columns[i].any { existingTask ->
                val existingStart = existingTask.startHour * 60 + existingTask.startMinute
                val existingEnd = existingTask.endHour * 60 + existingTask.endMinute
                
                // Intersection logic: max(start1, start2) < min(end1, end2)
                kotlin.math.max(taskStartMins, existingStart) < kotlin.math.min(taskEndMins, existingEnd)
            }

            if (!hasOverlap) {
                columns[i].add(task)
                result.add(PositionedTask(task, i))
                placed = true
                break
            }
        }
        
        if (!placed) {
            // Create new column
            columns.add(mutableListOf(task))
            result.add(PositionedTask(task, columns.lastIndex))
        }
    }
    return result
}
