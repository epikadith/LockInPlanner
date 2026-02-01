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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lockinplanner.domain.model.Task
import com.example.lockinplanner.domain.model.UserPreferences
import com.example.lockinplanner.ui.components.PositionedTask
import com.example.lockinplanner.ui.components.TaskBuilder
import com.example.lockinplanner.ui.components.TaskView
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
                    endMinute = endMins % 60
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
                        endMinute = endTotalMins % 60
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
            taskToEdit = taskToEdit
        )
    }

    selectedTask?.let {
        TaskView(
            task = PositionedTask(it, 0),
            onDismiss = { selectedTask = null },
            onDelete = {
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
                taskBuilderStartTime = null
                taskBuilderEndTime = null
                showTaskBuilder = true
            }) {
                Text("Add Task")
            }
            Button(onClick = {
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
                            
                            val columnMap = tasksInHour.associateBy { it.columnIndex }

                            for (i in 0 until totalColumns) {
                                Box(modifier = Modifier.width(columnWidth).fillMaxHeight().padding(horizontal = 2.dp)) {
                                    val positionedTask = columnMap[i]
                                    if (positionedTask != null) {
                                        val task = positionedTask.task
                                        // Calculate offsets for this specific hour block
                                        
                                        // Start Fraction within this hour (0.0 to 1.0)
                                        // If task starts before this hour, it starts at 0.0 relative to this hour.
                                        // If task starts in this hour, it starts at minute/60.
                                        val startFraction = if (task.startHour == hour) task.startMinute / 60f else 0f
                                        
                                        // End Fraction within this hour (0.0 to 1.0)
                                        // If task ends after this hour, it ends at 1.0.
                                        // If task ends in this hour, it ends at minute/60.
                                        // Special case: if task ends exactly at hour:00? 
                                        // e.g. 5:00 end means endHour=5, endMinute=0. 
                                        // In loop for hour=4, taskEnd > 4 is true (5 > 4).
                                        // In loop for hour=4, endFraction: task.endHour (5) != hour (4) -> 1f. Correct.
                                        // In loop for hour=5, taskEnd (5) > 5 is FALSE. So it won't render in hour 5. Correct.
                                        // What if end is 4:30?
                                        // Hour 4: endHour(4) == hour(4) -> 30/60 = 0.5. Correct.
                                        val endFraction = if (task.endHour == hour) task.endMinute / 60f else 1f
                                        
                                        val topOffset = 80.dp * startFraction
                                        val height = 80.dp * (endFraction - startFraction)
                                        
                                        val shape = when {
                                            task.startHour == hour && task.endHour == hour && task.startMinute == 0 && task.endMinute == 0 -> RoundedCornerShape(8.dp) // Full hour
                                            task.startHour == hour && (task.endHour > hour || (task.endHour == hour && task.endMinute > 0)) -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                            // Logic for rounded corners is tricky with minute splits. 
                                            // Simplification: Top rounded if it STARTS in this hour (start > hour or start==hour).
                                            // Bottom rounded if it ENDS in this hour.
                                            // Actually: 
                                            // Top rounded if startHour == hour. 
                                            // Bottom rounded if endHour == hour.
                                             else -> RoundedCornerShape(0.dp)
                                        }

                                        // Apply offset and height
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = topOffset)
                                                .height(height)
                                                .clip(RoundedCornerShape(8.dp)) // Just round everything for looks? Or rely on logic.
                                                // User logic: "make it go to the nearest 1/6".
                                                // Visual approximation implied.
                                                // Let's use specific corners if we want continuous look, or just rounded blocks.
                                                // If we stick to continuous:
                                                // .clip(if (startFraction > 0) RoundedCornerShape(topStart=8.dp, topEnd=8.dp) else ...)
                                                // Simpler: Just round all corners for now as discrete blocks usually look okay, 
                                                // OR stick to the original logic:
                                                .clip(
                                                    if (startFraction > 0 && endFraction < 1f) RoundedCornerShape(8.dp)
                                                    else if (startFraction > 0) RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                                    else if (endFraction < 1f) RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                                                    else RoundedCornerShape(0.dp)
                                                )
                                                .background(task.color)
                                                .padding(4.dp)
                                                .clickable { selectedTask = task },
                                        ) {
                                            if (startFraction == 0f || height > 20.dp) { // Show text if enough space or at top
                                                 // Only show text if it's the start block OR significant height?
                                                 // Best: Show if it's the start block.
                                                 if (task.startHour == hour) {
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
    val columns = mutableListOf<Int>()
    val result = mutableListOf<PositionedTask>()

    for (task in sortedTasks) {
        var placed = false
        for (i in columns.indices) {
            // Check if column is free at task start time (minutes)
            // columns[i] stores the end time of the last task in minutes? 
            // Currently it stores 'endHour'. I need to store 'endTotalMinutes'.
            // Wait, logic above was: `columns[i] <= task.startHour`.
            // Now `columns` should store minutes.
            if (columns[i] <= task.startHour * 60 + task.startMinute) {
                columns[i] = task.endHour * 60 + task.endMinute
                result.add(PositionedTask(task, i))
                placed = true
                break
            }
        }
        if (!placed) {
            columns.add(task.endHour * 60 + task.endMinute)
            result.add(PositionedTask(task, columns.lastIndex))
        }
    }
    return result
}
