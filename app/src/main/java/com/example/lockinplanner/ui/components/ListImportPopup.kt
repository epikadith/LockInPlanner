package com.example.lockinplanner.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.BufferedReader
import java.io.InputStreamReader
import android.provider.OpenableColumns

@Composable
fun ListImportPopup(
    onDismissRequest: () -> Unit,
    onSave: (String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var textContent by remember { mutableStateOf("") }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.readText()
                    
                    var filename = "Imported List"
                    if (name.isNotBlank()) {
                        filename = name
                    } else {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1) {
                                    val displayName = cursor.getString(nameIndex)
                                    filename = displayName.removeSuffix(".txt")
                                }
                            }
                        }
                    }
                    
                    val parsedItems = parseListText(content)
                    
                    if (parsedItems.isEmpty()) {
                        Toast.makeText(context, "No valid list items found in file.", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(filename, parsedItems)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Import Checklist", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("List Name (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { filePickerLauncher.launch("text/plain") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upload .txt File")
                }

                Text("Or paste/type below:", style = MaterialTheme.typography.bodyMedium)

                OutlinedTextField(
                    value = textContent,
                    onValueChange = { textContent = it },
                    label = { Text("List Items (one per line)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsedItems = parseListText(textContent)
                            if (parsedItems.isEmpty()) {
                                Toast.makeText(context, "No valid list items found.", Toast.LENGTH_SHORT).show()
                            } else {
                                val finalName = if (name.isNotBlank()) name else "Imported List"
                                onSave(finalName, parsedItems)
                            }
                        },
                        enabled = textContent.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Parses raw text input into a list of strings.
 * Rules:
 * 1. Split by newlines.
 * 2. Trim whitespace.
 * 3. Ignore entirely empty lines.
 * 4. Strip common leading list indicators: 
 *      - dashes/bullets: '-', '*', '+', '•'
 *      - numbers: '1.', '2)', etc.
 */
fun parseListText(text: String): List<String> {
    return text.split('\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { line ->
            // Regex to match leading bullets or numbers followed by optional whitespace
            // Regex matching dashes, asterisks, pluses, bullets, OR digits followed by dot/parenthesis
            val regex = Regex("""^([-*+•]|\d+[.)])\s*""")
            line.replaceFirst(regex, "").trim()
        }
        .filter { it.isNotEmpty() }
}
