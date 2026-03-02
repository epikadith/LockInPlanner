package com.example.lockinplanner.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class BookWithChapters(
    @Embedded val book: BookEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "bookId"
    )
    val chapters: List<ChapterEntity>
)

data class ChapterWithPages(
    @Embedded val chapter: ChapterEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "chapterId"
    )
    val pages: List<PageEntity>
)
