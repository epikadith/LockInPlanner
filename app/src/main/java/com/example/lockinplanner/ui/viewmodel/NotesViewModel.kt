package com.example.lockinplanner.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lockinplanner.data.local.entity.BookEntity
import com.example.lockinplanner.data.local.entity.ChapterEntity
import com.example.lockinplanner.data.local.entity.PageEntity
import com.example.lockinplanner.data.local.entity.ShortEntity
import com.example.lockinplanner.data.repository.NotesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class NotesViewModel(
    private val notesRepository: NotesRepository
) : ViewModel() {

    val allBooks: StateFlow<List<BookEntity>> = notesRepository.allBooks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allShorts: StateFlow<List<ShortEntity>> = notesRepository.allShorts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getChaptersForBook(bookId: String): StateFlow<List<ChapterEntity>> {
        return notesRepository.getChaptersForBook(bookId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun getPagesForChapter(chapterId: String): StateFlow<List<PageEntity>> {
        return notesRepository.getPagesForChapter(chapterId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun getChapterWithPages(chapterId: String) = notesRepository.getChapterWithPages(chapterId)

    fun getPageFlowById(pageId: String) = notesRepository.getPageFlowById(pageId)

    fun getShortFlowById(shortId: String) = notesRepository.getShortFlowById(shortId)

    // --- Caching for complex Undo flows ---
    data class DeletedBookCache(
        val book: BookEntity,
        val chapters: List<ChapterEntity>,
        val pages: List<PageEntity>
    )
    private var lastDeletedBookCache: DeletedBookCache? = null

    data class DeletedChapterCache(
        val chapter: ChapterEntity,
        val pages: List<PageEntity>
    )
    private var lastDeletedChapterCache: DeletedChapterCache? = null

    // --- Book Operations ---
    fun createBook(title: String) {
        viewModelScope.launch {
            notesRepository.insertBook(BookEntity(title = title))
        }
    }

    fun renameBook(book: BookEntity, newTitle: String) {
        viewModelScope.launch {
            notesRepository.updateBook(book.copy(title = newTitle))
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            val chapters = notesRepository.getChaptersForBook(book.id).first()
            val pages = mutableListOf<PageEntity>()
            for (chapter in chapters) {
                pages.addAll(notesRepository.getPagesForChapter(chapter.id).first())
            }
            lastDeletedBookCache = DeletedBookCache(book, chapters, pages)
            
            notesRepository.deleteBook(book)
        }
    }

    fun restoreBook(book: BookEntity) {
        viewModelScope.launch {
            notesRepository.insertBook(book)
            if (lastDeletedBookCache?.book?.id == book.id) {
                lastDeletedBookCache?.chapters?.forEach { notesRepository.insertChapter(it) }
                lastDeletedBookCache?.pages?.forEach { notesRepository.insertPage(it) }
                lastDeletedBookCache = null
            }
        }
    }

    // --- Chapter Operations ---
    fun createChapter(bookId: String, title: String) {
        viewModelScope.launch {
            notesRepository.insertChapter(ChapterEntity(bookId = bookId, title = title))
        }
    }

    fun renameChapter(chapter: ChapterEntity, newTitle: String) {
        viewModelScope.launch {
            notesRepository.updateChapter(chapter.copy(title = newTitle))
        }
    }

    fun deleteChapter(chapter: ChapterEntity) {
        viewModelScope.launch {
            val pages = notesRepository.getPagesForChapter(chapter.id).first()
            lastDeletedChapterCache = DeletedChapterCache(chapter, pages)
            
            notesRepository.deleteChapter(chapter)
        }
    }

    fun restoreChapter(chapter: ChapterEntity) {
        viewModelScope.launch {
            notesRepository.insertChapter(chapter)
            if (lastDeletedChapterCache?.chapter?.id == chapter.id) {
                lastDeletedChapterCache?.pages?.forEach { notesRepository.insertPage(it) }
                lastDeletedChapterCache = null
            }
        }
    }

    // --- Page Operations ---
    fun createPage(chapterId: String, title: String) {
        viewModelScope.launch {
            notesRepository.insertPage(PageEntity(chapterId = chapterId, title = title))
        }
    }

    fun renamePage(page: PageEntity, newTitle: String) {
        viewModelScope.launch {
            notesRepository.updatePage(page.copy(title = newTitle))
        }
    }

    fun updatePageContent(page: PageEntity, newContent: String) {
        viewModelScope.launch {
            notesRepository.updatePage(page.copy(content = newContent))
        }
    }

    fun deletePage(page: PageEntity) {
        viewModelScope.launch {
            notesRepository.deletePage(page)
        }
    }

    fun restorePage(page: PageEntity) {
        viewModelScope.launch {
            notesRepository.insertPage(page)
        }
    }

    // --- Short Operations ---
    fun createShort(title: String, colorArgb: Int) {
        viewModelScope.launch {
            notesRepository.insertShort(ShortEntity(title = title, colorArgb = colorArgb))
        }
    }

    fun renameShort(short: ShortEntity, newTitle: String) {
        viewModelScope.launch {
            notesRepository.updateShort(short.copy(title = newTitle))
        }
    }

    fun updateShortDetails(short: ShortEntity, newTitle: String, newColorArgb: Int) {
        viewModelScope.launch {
            notesRepository.updateShort(short.copy(title = newTitle, colorArgb = newColorArgb))
        }
    }

    fun updateShortContent(short: ShortEntity, newContent: String) {
        viewModelScope.launch {
            notesRepository.updateShort(short.copy(content = newContent))
        }
    }

    fun deleteShort(short: ShortEntity) {
        viewModelScope.launch {
            notesRepository.deleteShort(short)
        }
    }

    fun restoreShort(short: ShortEntity) {
        viewModelScope.launch {
            notesRepository.insertShort(short)
        }
    }
}

class NotesViewModelFactory(
    private val notesRepository: NotesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(notesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
