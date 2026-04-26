package com.example.drinkwatch.data.db.dao

import androidx.room.*
import com.example.drinkwatch.data.db.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: PlayerEntity): Long

    @Update
    suspend fun update(player: PlayerEntity)

    @Delete
    suspend fun delete(player: PlayerEntity)

    @Query("SELECT * FROM players WHERE sessionId = :sessionId ORDER BY name ASC")
    fun getAllBySession(sessionId: Long): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getById(id: Long): PlayerEntity?

    @Query("SELECT * FROM players WHERE id = :id")
    fun observeById(id: Long): Flow<PlayerEntity?>

    @Query("UPDATE players SET isDisabled = :disabled WHERE id = :playerId")
    suspend fun setDisabled(playerId: Long, disabled: Boolean)
}
