package com.example.lockinplanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun LicenseDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 500.dp) // Limit height
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    "Open Source Licenses",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("This app uses the following open source software:")
                    
                    LicenseItem(
                        library = "ColorPicker Compose",
                        author = "skydoves (Jaewoong Eum)",
                        license = "Apache License 2.0"
                    )
                    
                    LicenseItem(
                        library = "Gson",
                        author = "Google Inc.",
                        license = "Apache License 2.0"
                    )
                    
                    LicenseItem(
                        library = "Android Jetpack components",
                        author = "Google",
                        license = "Apache License 2.0"
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun LicenseItem(library: String, author: String, license: String) {
    Column {
        Text(library, style = MaterialTheme.typography.titleMedium)
        Text("Copyright $author", style = MaterialTheme.typography.bodyMedium)
        Text("Licensed under $license", style = MaterialTheme.typography.bodySmall)
    }
}
