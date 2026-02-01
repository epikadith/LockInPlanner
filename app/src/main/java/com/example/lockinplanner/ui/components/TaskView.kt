package com.example.lockinplanner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.util.Locale
import com.example.lockinplanner.domain.model.Task

data class PositionedTask(val task: Task, val columnIndex: Int)

@Composable
fun TaskView(
    task: PositionedTask,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    is24h: Boolean = true,
    confirmDeletion: Boolean = true
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
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
                    Text(task.task.name, style = MaterialTheme.typography.headlineMedium)
                    Row {
                         IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                val startTime = formatTime(task.task.startHour, task.task.startMinute, is24h)
                val endTime = formatTime(task.task.endHour, task.task.endMinute, is24h)
                Text(text = "$startTime - $endTime")

                task.task.description?.let {
                    Text(text = it)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (confirmDeletion) {
                            showDeleteDialog = true
                        } else {
                            onDelete()
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int, is24h: Boolean): String {
    return if (is24h) {
        String.format(Locale.US, "%02d:%02d", hour, minute)
    } else {
        val amPm = if (hour < 12) "AM" else "PM"
        val h = if (hour == 0 || hour == 12) 12 else hour % 12
        String.format(Locale.US, "%d:%02d %s", h, minute, amPm)
    }
}
