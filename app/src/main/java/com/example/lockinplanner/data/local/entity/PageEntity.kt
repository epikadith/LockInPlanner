package com.example.lockinplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chapterId")]
)
data class PageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val chapterId: String,
    val title: String,
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
