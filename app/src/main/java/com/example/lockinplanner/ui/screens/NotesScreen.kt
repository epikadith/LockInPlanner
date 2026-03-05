package com.example.lockinplanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
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
import com.example.lockinplanner.data.local.entity.BookEntity
import com.example.lockinplanner.data.local.entity.ShortEntity
import com.example.lockinplanner.domain.model.UserPreferences
import com.example.lockinplanner.ui.components.CreateShortDialog
import com.example.lockinplanner.ui.components.TextInputDialog
import com.example.lockinplanner.ui.components.UndoSnackbar
import com.example.lockinplanner.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.launch

sealed class NotesNavigationState {
    data class Overview(val initialTab: Int = 0) : NotesNavigationState()
    data class ViewingShort(val shortId: String) : NotesNavigationState()
    data class ViewingBook(val bookId: String) : NotesNavigationState()
    data class ViewingChapter(val bookId: String, val chapterId: String) : NotesNavigationState()
    data class ViewingPage(val bookId: String, val chapterId: String, val pageId: String) : NotesNavigationState()
}

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    settingsViewModel: com.example.lockinplanner.ui.viewmodel.SettingsViewModel,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier
) {
    var navigationState by remember { mutableStateOf<NotesNavigationState>(NotesNavigationState.Overview(0)) }

    when (val currentState = navigationState) {
        is NotesNavigationState.Overview -> {
            NotesOverviewScreen(
                initialTab = currentState.initialTab,
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                userPreferences = userPreferences,
                onNavigateToBook = { bookId -> navigationState = NotesNavigationState.ViewingBook(bookId) },
                onNavigateToShort = { shortId -> navigationState = NotesNavigationState.ViewingShort(shortId) },
                modifier = modifier
            )
        }
        is NotesNavigationState.ViewingShort -> {
            ShortWriteView(
                shortId = currentState.shortId,
                viewModel = viewModel,
                onNavigateBack = { navigationState = NotesNavigationState.Overview(0) },
                modifier = modifier
            )
        }
        is NotesNavigationState.ViewingBook -> {
            BookView(
                bookId = currentState.bookId,
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                userPreferences = userPreferences,
                onNavigateBack = { navigationState = NotesNavigationState.Overview(1) },
                onNavigateToChapter = { chapterId -> navigationState = NotesNavigationState.ViewingChapter(currentState.bookId, chapterId) },
                modifier = modifier
            )
        }
        is NotesNavigationState.ViewingChapter -> {
            ChapterView(
                chapterId = currentState.chapterId,
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                userPreferences = userPreferences,
                onNavigateBack = { navigationState = NotesNavigationState.ViewingBook(currentState.bookId) }, 
                onNavigateToPage = { pageId -> navigationState = NotesNavigationState.ViewingPage(currentState.bookId, currentState.chapterId, pageId) },
                modifier = modifier
            )
        }
        is NotesNavigationState.ViewingPage -> {
            PageView(
                pageId = currentState.pageId,
                viewModel = viewModel,
                onNavigateBack = { navigationState = NotesNavigationState.ViewingChapter(currentState.bookId, currentState.chapterId) },
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NotesOverviewScreen(
    initialTab: Int,
    viewModel: NotesViewModel,
    settingsViewModel: com.example.lockinplanner.ui.viewmodel.SettingsViewModel,
    userPreferences: UserPreferences,
    onNavigateToBook: (String) -> Unit,
    onNavigateToShort: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabTitles = listOf("Shorts", "Books")
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialTab,
        pageCount = { tabTitles.size }
    )
    val coroutineScope = rememberCoroutineScope()

    var showCreateBookDialog by remember { mutableStateOf(false) }
    var showCreateShortDialog by remember { mutableStateOf(false) }

    var deletedBook by remember { mutableStateOf<BookEntity?>(null) }
    var deletedShort by remember { mutableStateOf<ShortEntity?>(null) }
    var shortToEdit by remember { mutableStateOf<ShortEntity?>(null) }
    
    var showDeleteBookConfirmDialog by remember { mutableStateOf<BookEntity?>(null) }
    var showDeleteShortConfirmDialog by remember { mutableStateOf<ShortEntity?>(null) }
    
    var showUndoSnackbar by remember { mutableStateOf(false) }
    var snackbarTrigger by remember { mutableIntStateOf(0) }

    if (showCreateShortDialog || shortToEdit != null) {
        CreateShortDialog(
            shortToEdit = shortToEdit,
            onDismiss = { 
                showCreateShortDialog = false 
                shortToEdit = null
            },
            onConfirm = { title, colorArgb ->
                if (shortToEdit != null) {
                    viewModel.updateShortDetails(shortToEdit!!, title, colorArgb)
                } else {
                    viewModel.createShort(title, colorArgb)
                }
                showCreateShortDialog = false
                shortToEdit = null
            }
        )
    }

    if (showCreateBookDialog) {
        TextInputDialog(
            title = "New Book",
            placeholder = "Book Title",
            onConfirm = { title ->
                viewModel.createBook(title)
                showCreateBookDialog = false
            },
            onDismiss = { showCreateBookDialog = false }
        )
    }

    if (showDeleteBookConfirmDialog != null) {
        val bookToDelete = showDeleteBookConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteBookConfirmDialog = null },
            title = { Text("Delete Book") },
            text = { Text("Are you sure you want to delete '${bookToDelete.title}'?") },
            confirmButton = {
                TextButton(onClick = {
                    if (userPreferences.undoEnabled) {
                        deletedBook = bookToDelete
                        snackbarTrigger++
                        showUndoSnackbar = true
                    }
                    viewModel.deleteBook(bookToDelete)
                    showDeleteBookConfirmDialog = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBookConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteShortConfirmDialog != null) {
        val shortToDelete = showDeleteShortConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteShortConfirmDialog = null },
            title = { Text("Delete Short") },
            text = { Text("Are you sure you want to delete '${shortToDelete.title}'?") },
            confirmButton = {
                TextButton(onClick = {
                    if (userPreferences.undoEnabled) {
                        deletedShort = shortToDelete
                        snackbarTrigger++
                        showUndoSnackbar = true
                    }
                    viewModel.deleteShort(shortToDelete)
                    showDeleteShortConfirmDialog = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteShortConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = pagerState.currentPage) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { 
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 1) {
                    // Books List
                val books by viewModel.allBooks.collectAsStateWithLifecycle()
                
                if (books.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.lockinplanner.R.drawable.bookpagedefault),
                            contentDescription = "No books yet",
                            modifier = Modifier.size(400.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(books) { book ->
                            var showRenameDialog by remember { mutableStateOf(false) }

                            if (showRenameDialog) {
                                TextInputDialog(
                                    title = "Rename Book",
                                    initialText = book.title,
                                    onConfirm = { newTitle ->
                                        viewModel.renameBook(book, newTitle)
                                        showRenameDialog = false
                                    },
                                    onDismiss = { showRenameDialog = false }
                                )
                            }

                            BookCard(
                                book = book,
                                onClick = { onNavigateToBook(book.id) },
                                onRenameClick = { showRenameDialog = true },
                                onDeleteClick = {
                                    if (userPreferences.confirmDeletion) {
                                        showDeleteBookConfirmDialog = book
                                    } else {
                                        if (userPreferences.undoEnabled) {
                                            deletedBook = book
                                            snackbarTrigger++
                                            showUndoSnackbar = true
                                        }
                                        viewModel.deleteBook(book)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Shorts List
                val shorts by viewModel.allShorts.collectAsStateWithLifecycle()
                Box(modifier = Modifier.fillMaxSize()) {
                    ShortsList(
                        shorts = shorts,
                        displayMode = userPreferences.shortsDisplayMode,
                        onShortClick = { short -> onNavigateToShort(short.id) },
                        onEditShort = { short -> shortToEdit = short },
                        onDeleteShort = { short -> 
                            if (userPreferences.confirmDeletion) {
                                showDeleteShortConfirmDialog = short
                            } else {
                                if (userPreferences.undoEnabled) {
                                    deletedShort = short
                                    snackbarTrigger++
                                    showUndoSnackbar = true
                                }
                                viewModel.deleteShort(short)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { settingsViewModel.updateShortsDisplayMode((userPreferences.shortsDisplayMode + 1) % 3) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        // We cycle through layout types using a simple icon 
                        // It could be dynamic, but a single layout toggle icon is fine
                        Icon(androidx.compose.material.icons.Icons.Default.List, contentDescription = "Toggle Layout")
                    }
                }
            }
            } // End HorizontalPager

            // Floating Action Button for adding
            FloatingActionButton(
                onClick = {
                    if (pagerState.currentPage == 1) {
                        showCreateBookDialog = true
                    } else {
                        showCreateShortDialog = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }

            // Undo Snackbar
            if (showUndoSnackbar && (deletedBook != null || deletedShort != null)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp), // Padding to sit above FAB
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val message = if (deletedBook != null) "Deleted book" else "Deleted short"
                    UndoSnackbar(
                        message = message,
                        durationSeconds = userPreferences.undoDuration,
                        triggerKey = snackbarTrigger,
                        onUndo = {
                            deletedBook?.let { viewModel.restoreBook(it) }
                            deletedShort?.let { viewModel.restoreShort(it) }
                            deletedBook = null
                            deletedShort = null
                            showUndoSnackbar = false
                        },
                        onDismiss = {
                            deletedBook = null
                            deletedShort = null
                            showUndoSnackbar = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BookCard(
    book: BookEntity,
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
            .height(180.dp)
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
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
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
