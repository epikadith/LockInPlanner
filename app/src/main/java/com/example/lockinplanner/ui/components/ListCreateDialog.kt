package com.example.lockinplanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.util.UUID

data class ObjectiveUiModel(
    val id: String? = null,
    val text: String
)

@Composable
fun ListCreateDialog(
    initialName: String = "",
    initialObjectives: List<ObjectiveUiModel> = emptyList(),
    onDismissRequest: () -> Unit,
    onSave: (String, List<ObjectiveUiModel>) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    data class EditableObjective(
        val id: String?, // null if new
        val uniqueId: String = UUID.randomUUID().toString(), // local unique ID for keys
        var text: String
    )

    val objectives = remember { mutableStateListOf<EditableObjective>().apply {
        if (initialObjectives.isEmpty()) {
            add(EditableObjective(id = null, text = ""))
        } else {
            addAll(initialObjectives.map { EditableObjective(it.id, text = it.text) })
        }
    } }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (initialName.isBlank()) "New Checklist" else "Edit Checklist", 
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("List Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Objectives", style = MaterialTheme.typography.titleMedium)
                
                val listState = rememberLazyListState()
                var focusOnIndex by remember { mutableIntStateOf(-1) }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f, fill = false).padding(vertical = 8.dp)
                ) {
                    itemsIndexed(objectives, key = { _, item -> item.uniqueId }) { index, objective ->
                        val focusRequester = remember { FocusRequester() }
                        
                        LaunchedEffect(focusOnIndex) {
                            if (index == focusOnIndex) {
                                focusRequester.requestFocus()
                                focusOnIndex = -1 // Reset
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            OutlinedTextField(
                                value = objective.text,
                                onValueChange = { objectives[index] = objective.copy(text = it) },
                                placeholder = { Text("Objective") },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                            )
                            IconButton(onClick = { 
                                objectives.removeAt(index) 
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove")
                            }
                        }
                    }
                }
                
                TextButton(
                    onClick = { 
                        objectives.add(EditableObjective(id = null, text = ""))
                        focusOnIndex = objectives.size - 1
                        // Optional: Trigger scroll to bottom?
                        // Scope required for scroll.
                        // Given we are in onClick callback, we can't launch suspend directly unless we have a scope.
                        // But focus request usually handles "BringIntoView".
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Objective")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalObjectives = objectives.map { ObjectiveUiModel(it.id, it.text) }
                            onSave(name, finalObjectives)
                        },
                        enabled = name.isNotBlank() && objectives.isNotEmpty() && objectives.all { it.text.isNotBlank() }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
