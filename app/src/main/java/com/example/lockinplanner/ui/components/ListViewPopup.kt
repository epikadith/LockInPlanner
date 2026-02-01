package com.example.lockinplanner.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.lockinplanner.data.local.entity.ChecklistWithObjectives
import com.example.lockinplanner.data.local.entity.ObjectiveEntity

@Composable
fun ListViewPopup(
    checklistWithObjectives: ChecklistWithObjectives,
    onDismissRequest: () -> Unit,
    onCheckItem: (ObjectiveEntity, Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMove: (ObjectiveEntity, Boolean) -> Unit
) {
    val checklist = checklistWithObjectives.checklist
    val sortedObjectives = remember(checklistWithObjectives.objectives) {
        checklistWithObjectives.objectives.sortedWith(
            Comparator { a, b ->
                val c1 = a.isCompleted.compareTo(b.isCompleted)
                if (c1 != 0) c1 else a.order.compareTo(b.order)
            }
        )
    }

    Dialog(onDismissRequest = onDismissRequest) {
         Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = checklist.name, 
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissRequest) {
                         Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                     items(sortedObjectives, key = { it.id }) { objective ->
                         Row(
                             verticalAlignment = Alignment.CenterVertically,
                             modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                         ) {
                             Checkbox(
                                 checked = objective.isCompleted,
                                 onCheckedChange = { isChecked ->
                                     if (!checklist.isCompleted) {
                                         onCheckItem(objective, isChecked)
                                     }
                                 },
                                 enabled = !checklist.isCompleted
                             )
                             Text(
                                 text = objective.text,
                                 style = if (objective.isCompleted) 
                                     LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough, color = Color.Gray)
                                 else LocalTextStyle.current,
                                 modifier = Modifier.weight(1f).padding(start = 8.dp)
                             )
                             
                             if (!checklist.isCompleted && !objective.isCompleted) {
                                 Column {
                                     IconButton(
                                         onClick = { onMove(objective, true) },
                                         modifier = Modifier.size(24.dp)
                                     ) {
                                         Icon(
                                             imageVector = Icons.Default.KeyboardArrowUp,
                                             contentDescription = "Up"
                                         )
                                     }
                                     IconButton(
                                         onClick = { onMove(objective, false) },
                                         modifier = Modifier.size(24.dp)
                                     ) {
                                         Icon(
                                             imageVector = Icons.Default.KeyboardArrowDown,
                                             contentDescription = "Down"
                                         )
                                     }
                                 }
                             }
                         }
                     }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                    
                    if (!checklist.isCompleted) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }
            }
        }
    }
}
