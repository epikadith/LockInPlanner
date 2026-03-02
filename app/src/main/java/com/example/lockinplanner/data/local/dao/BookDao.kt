package com.example.lockinplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.lockinplanner.data.local.entity.BookEntity
import com.example.lockinplanner.data.local.entity.BookWithChapters
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY createdAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Transaction
    @Query("SELECT * FROM books ORDER BY createdAt DESC")
    fun getAllBooksWithChapters(): Flow<List<BookWithChapters>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: String): BookEntity?

    @Transaction
    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookWithChapters(id: String): Flow<BookWithChapters?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)
}
