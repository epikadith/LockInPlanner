package com.example.lockinplanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageView(
    pageId: String,
    viewModel: NotesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // We fetch a single page flow to react to changes.
    // If we wanted to avoid keeping flows open for every single page in a list,
    // we use getPageFlowById.
    val pageFlow = remember(pageId) { viewModel.getPageFlowById(pageId) }
    val page by pageFlow.collectAsStateWithLifecycle(initialValue = null)

    BackHandler(onBack = onNavigateBack)

    if (page == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Safe unwrapping
    val currentPage = page!!

    var pageTitle by remember(currentPage.title) { mutableStateOf(currentPage.title) }
    var pageContent by remember(currentPage.content) { mutableStateOf(currentPage.content) }
    
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy \u2022 hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(currentPage.createdAt) { dateFormatter.format(Date(currentPage.createdAt)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    TextField(
                        value = pageTitle,
                        onValueChange = { 
                            pageTitle = it
                            viewModel.renamePage(currentPage, it)
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
        ) {
            // Timestamp
            Text(
                text = "Created $formattedDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp, start = 16.dp)
            )

            // Seamless text editor
            BasicTextField(
                value = pageContent,
                onValueChange = { 
                    pageContent = it
                    viewModel.updatePageContent(currentPage, it)
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
                        if (pageContent.isEmpty()) {
                            Text(
                                text = "Start typing here...",
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
