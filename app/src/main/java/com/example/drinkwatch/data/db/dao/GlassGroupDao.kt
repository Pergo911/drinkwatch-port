package com.example.drinkwatch.data.db.dao

import androidx.room.*
import com.example.drinkwatch.data.db.entity.GlassGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GlassGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(glassGroup: GlassGroupEntity): Long

    @Delete
    suspend fun delete(glassGroup: GlassGroupEntity)

    @Query("SELECT * FROM glass_groups WHERE sessionId = :sessionId ORDER BY letter ASC")
    fun getAllBySession(sessionId: Long): Flow<List<GlassGroupEntity>>
}
