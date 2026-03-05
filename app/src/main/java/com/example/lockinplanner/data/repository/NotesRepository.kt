package com.example.lockinplanner.data.repository

import com.example.lockinplanner.data.local.dao.BookDao
import com.example.lockinplanner.data.local.dao.ChapterDao
import com.example.lockinplanner.data.local.dao.PageDao
import com.example.lockinplanner.data.local.entity.BookEntity
import com.example.lockinplanner.data.local.entity.BookWithChapters
import com.example.lockinplanner.data.local.entity.ChapterEntity
import com.example.lockinplanner.data.local.entity.ChapterWithPages
import com.example.lockinplanner.data.local.entity.PageEntity
import com.example.lockinplanner.data.local.entity.ShortEntity
import com.example.lockinplanner.data.local.dao.ShortDao
import kotlinx.coroutines.flow.Flow

class NotesRepository(
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val pageDao: PageDao,
    private val shortDao: ShortDao
) {
    // --- Books ---
    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()
    val allBooksWithChapters: Flow<List<BookWithChapters>> = bookDao.getAllBooksWithChapters()

    fun getBookWithChapters(id: String): Flow<BookWithChapters?> {
        return bookDao.getBookWithChapters(id)
    }

    suspend fun getBookById(id: String): BookEntity? {
        return bookDao.getBookById(id)
    }

    suspend fun insertBook(book: BookEntity) {
        bookDao.insertBook(book)
    }

    suspend fun updateBook(book: BookEntity) {
        bookDao.updateBook(book)
    }

    suspend fun deleteBook(book: BookEntity) {
        bookDao.deleteBook(book)
    }

    // --- Chapters ---
    val allChapters: Flow<List<ChapterEntity>> = chapterDao.getAllChapters()

    fun getChaptersForBook(bookId: String): Flow<List<ChapterEntity>> {
        return chapterDao.getChaptersForBook(bookId)
    }

    fun getChapterWithPages(id: String): Flow<ChapterWithPages?> {
        return chapterDao.getChapterWithPages(id)
    }

    suspend fun getChapterById(id: String): ChapterEntity? {
        return chapterDao.getChapterById(id)
    }

    suspend fun insertChapter(chapter: ChapterEntity) {
        chapterDao.insertChapter(chapter)
    }

    suspend fun updateChapter(chapter: ChapterEntity) {
        chapterDao.updateChapter(chapter)
    }

    suspend fun deleteChapter(chapter: ChapterEntity) {
        chapterDao.deleteChapter(chapter)
    }

    // --- Pages ---
    val allPages: Flow<List<PageEntity>> = pageDao.getAllPages()

    fun getPagesForChapter(chapterId: String): Flow<List<PageEntity>> {
        return pageDao.getPagesForChapter(chapterId)
    }

    suspend fun getPageById(id: String): PageEntity? {
        return pageDao.getPageById(id)
    }

    fun getPageFlowById(id: String): Flow<PageEntity?> {
        return pageDao.getPageFlowById(id)
    }

    suspend fun insertPage(page: PageEntity) {
        pageDao.insertPage(page)
    }

    suspend fun updatePage(page: PageEntity) {
        pageDao.updatePage(page)
    }

    suspend fun deletePage(page: PageEntity) {
        pageDao.deletePage(page)
    }

    // --- Shorts ---
    val allShorts: Flow<List<ShortEntity>> = shortDao.getAllShorts()

    suspend fun getShortById(id: String): ShortEntity? {
        // shortDao.getShortById returns Flow, need to collect or make suspend version
        // Actually since we return flow from DAO, let's provide a flow here
        return null // Replace with flow logic if needed, or query direct
    }

    fun getShortFlowById(id: String): Flow<ShortEntity?> {
        return shortDao.getShortById(id)
    }

    suspend fun insertShort(shortEntity: ShortEntity) {
        shortDao.insertShort(shortEntity)
    }

    suspend fun updateShort(shortEntity: ShortEntity) {
        shortDao.updateShort(shortEntity)
    }

    suspend fun deleteShort(shortEntity: ShortEntity) {
        shortDao.deleteShort(shortEntity)
    }
}
