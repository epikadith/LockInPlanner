package com.example.lockinplanner.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lockinplanner.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortWriteView(
    shortId: String,
    viewModel: NotesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shortFlow = remember(shortId) { viewModel.getShortFlowById(shortId) }
    val shortEntity by shortFlow.collectAsStateWithLifecycle(initialValue = null)

    BackHandler(onBack = onNavigateBack)

    if (shortEntity == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentShort = shortEntity!!

    var shortTitle by remember(currentShort.title) { mutableStateOf(currentShort.title) }
    var shortContent by remember(currentShort.content) { mutableStateOf(currentShort.content) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    TextField(
                        value = shortTitle,
                        onValueChange = { 
                            shortTitle = it
                            viewModel.renameShort(currentShort, it)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.surface
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp) // Add top padding since there's no timestamp
        ) {
            // Seamless text editor
            BasicTextField(
                value = shortContent,
                onValueChange = { 
                    shortContent = it
                    viewModel.updateShortContent(currentShort, it)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (shortContent.isEmpty()) {
                            Text(
                                text = "Start typing short...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
