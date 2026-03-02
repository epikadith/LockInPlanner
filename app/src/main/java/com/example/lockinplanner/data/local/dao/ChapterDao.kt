package com.example.lockinplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.lockinplanner.data.local.entity.ChapterEntity
import com.example.lockinplanner.data.local.entity.ChapterWithPages
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY createdAt ASC")
    fun getChaptersForBook(bookId: String): Flow<List<ChapterEntity>>

    @Transaction
    @Query("SELECT * FROM chapters WHERE id = :id")
    fun getChapterWithPages(id: String): Flow<ChapterWithPages?>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: String): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)
}
