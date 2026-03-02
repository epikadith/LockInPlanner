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
import com.example.lockinplanner.data.local.entity.ChapterEntity
import com.example.lockinplanner.domain.model.UserPreferences
import com.example.lockinplanner.ui.components.TextInputDialog
import com.example.lockinplanner.ui.components.UndoSnackbar
import com.example.lockinplanner.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookView(
    bookId: String,
    viewModel: NotesViewModel,
    settingsViewModel: com.example.lockinplanner.ui.viewmodel.SettingsViewModel,
    userPreferences: UserPreferences,
    onNavigateBack: () -> Unit,
    onNavigateToChapter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var bookTitle by remember { mutableStateOf("Loading...") }
    val chapters by viewModel.getChaptersForBook(bookId).collectAsStateWithLifecycle()
    
    BackHandler(onBack = onNavigateBack)

    var showCreateChapterDialog by remember { mutableStateOf(false) }

    var deletedChapter by remember { mutableStateOf<ChapterEntity?>(null) }
    var showUndoSnackbar by remember { mutableStateOf(false) }
    var snackbarTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(bookId) {
        // Fetch current book title
        val book = viewModel.allBooks.value.find { it.id == bookId }
        if (book != null) {
            bookTitle = book.title
        }
    }

    if (showCreateChapterDialog) {
        TextInputDialog(
            title = "New Chapter",
            placeholder = "Chapter Name",
            onConfirm = { title ->
                viewModel.createChapter(bookId, title)
                showCreateChapterDialog = false
            },
            onDismiss = { showCreateChapterDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    TextField(
                        value = bookTitle,
                        onValueChange = { 
                            bookTitle = it
                            // Debounce or save immediately
                            val book = viewModel.allBooks.value.find { b -> b.id == bookId }
                            if (book != null) {
                                viewModel.renameBook(book, it)
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
                        settingsViewModel.updateBookViewColumnCount(if (userPreferences.bookViewColumnCount == 1) 2 else 1)
                    }) {
                        Icon(Icons.Default.List, contentDescription = "Change View")
                    }
                    IconButton(onClick = { showCreateChapterDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Chapter")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (chapters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No chapters yet. Click + to create one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(userPreferences.bookViewColumnCount),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(chapters) { chapter ->
                    var showRenameDialog by remember { mutableStateOf(false) }

                    if (showRenameDialog) {
                        TextInputDialog(
                            title = "Rename Chapter",
                            initialText = chapter.title,
                            onConfirm = { newTitle ->
                                viewModel.renameChapter(chapter, newTitle)
                                showRenameDialog = false
                            },
                            onDismiss = { showRenameDialog = false }
                        )
                    }

                    ChapterCard(
                        chapter = chapter,
                        onClick = { onNavigateToChapter(chapter.id) },
                        onRenameClick = { showRenameDialog = true },
                        onDeleteClick = {
                            if (userPreferences.undoEnabled) {
                                deletedChapter = chapter
                                snackbarTrigger++
                                showUndoSnackbar = true
                            }
                            viewModel.deleteChapter(chapter)
                        }
                    )
                }
            }
        }

        // Undo Snackbar
        if (showUndoSnackbar && deletedChapter != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                UndoSnackbar(
                    message = "Deleted chapter",
                    durationSeconds = userPreferences.undoDuration,
                    triggerKey = snackbarTrigger,
                    onUndo = {
                        deletedChapter?.let { viewModel.restoreChapter(it) }
                        deletedChapter = null
                        showUndoSnackbar = false
                    },
                    onDismiss = {
                        deletedChapter = null
                        showUndoSnackbar = false
                    }
                )
            }
        }
    }
}

@Composable
fun ChapterCard(
    chapter: ChapterEntity,
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
            .height(80.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
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
