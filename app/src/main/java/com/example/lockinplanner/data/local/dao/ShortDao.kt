package com.example.lockinplanner.data.local.dao

import androidx.room.*
import com.example.lockinplanner.data.local.entity.ShortEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortDao {
    @Query("SELECT * FROM shorts ORDER BY createdAt DESC")
    fun getAllShorts(): Flow<List<ShortEntity>>

    @Query("SELECT * FROM shorts WHERE id = :shortId")
    fun getShortById(shortId: String): Flow<ShortEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShort(shortEntity: ShortEntity)

    @Update
    suspend fun updateShort(shortEntity: ShortEntity)

    @Delete
    suspend fun deleteShort(shortEntity: ShortEntity)
}
