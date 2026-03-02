package com.example.lockinplanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lockinplanner.data.local.entity.PageEntity
import com.example.lockinplanner.domain.model.UserPreferences
import com.example.lockinplanner.ui.components.TextInputDialog
import com.example.lockinplanner.ui.components.UndoSnackbar
import com.example.lockinplanner.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterView(
    chapterId: String,
    viewModel: NotesViewModel,
    settingsViewModel: com.example.lockinplanner.ui.viewmodel.SettingsViewModel,
    userPreferences: UserPreferences,
    onNavigateBack: () -> Unit,
    onNavigateToPage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var chapterTitle by remember { mutableStateOf("Loading...") }
    val chapterWithPages by viewModel.getChapterWithPages(chapterId).collectAsStateWithLifecycle(null)
    val pages = chapterWithPages?.pages ?: emptyList()
    
    BackHandler(onBack = onNavigateBack)

    var showCreatePageDialog by remember { mutableStateOf(false) }

    var deletedPage by remember { mutableStateOf<PageEntity?>(null) }
    var showUndoSnackbar by remember { mutableStateOf(false) }
    var snackbarTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(chapterWithPages) {
        if (chapterWithPages != null) {
            chapterTitle = chapterWithPages!!.chapter.title
        }
    }

    if (showCreatePageDialog) {
        TextInputDialog(
            title = "New Page",
            placeholder = "Page Title",
            onConfirm = { title ->
                viewModel.createPage(chapterId, title)
                showCreatePageDialog = false
            },
            onDismiss = { showCreatePageDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    TextField(
                        value = chapterTitle,
                        onValueChange = { 
                            chapterTitle = it
                            chapterWithPages?.let { cWP ->
                                viewModel.renameChapter(cWP.chapter, it)
                            }
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
                },
                actions = {
                    IconButton(onClick = { 
                        settingsViewModel.updateChapterViewColumnCount(if (userPreferences.chapterViewColumnCount == 1) 2 else 1) 
                    }) {
                        Icon(Icons.Default.List, contentDescription = "Change View")
                    }
                    IconButton(onClick = { showCreatePageDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Page")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (pages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No pages yet. Click + to create one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(userPreferences.chapterViewColumnCount),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(pages) { page ->
                    var showRenameDialog by remember { mutableStateOf(false) }

                    if (showRenameDialog) {
                        TextInputDialog(
                            title = "Rename Page",
                            initialText = page.title,
                            onConfirm = { newTitle ->
                                viewModel.renamePage(page, newTitle)
                                showRenameDialog = false
                            },
                            onDismiss = { showRenameDialog = false }
                        )
                    }

                    PageCard(
                        page = page,
                        onClick = { onNavigateToPage(page.id) },
                        onRenameClick = { showRenameDialog = true },
                        onDeleteClick = {
                            if (userPreferences.undoEnabled) {
                                deletedPage = page
                                snackbarTrigger++
                                showUndoSnackbar = true
                            }
                            viewModel.deletePage(page)
                        }
                    )
                }
            }
        }

        // Undo Snackbar
        if (showUndoSnackbar && deletedPage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                UndoSnackbar(
                    message = "Deleted page",
                    durationSeconds = userPreferences.undoDuration,
                    triggerKey = snackbarTrigger,
                    onUndo = {
                        deletedPage?.let { viewModel.restorePage(it) }
                        deletedPage = null
                        showUndoSnackbar = false
                    },
                    onDismiss = {
                        deletedPage = null
                        showUndoSnackbar = false
                    }
                )
            }
        }
    }
}

@Composable
fun PageCard(
    page: PageEntity,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = page.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showMenu = false
                            onRenameClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}
