package com.example.drinkwatch.data.db.dao

import androidx.room.*
import com.example.drinkwatch.data.db.entity.DrinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(drink: DrinkEntity): Long

    @Update
    suspend fun update(drink: DrinkEntity)

    @Delete
    suspend fun delete(drink: DrinkEntity)

    @Query("SELECT * FROM drinks WHERE sessionId = :sessionId ORDER BY name ASC")
    fun getAllBySession(sessionId: Long): Flow<List<DrinkEntity>>

    @Query("SELECT * FROM drinks WHERE id = :id")
    suspend fun getById(id: Long): DrinkEntity?

    @Query("SELECT * FROM drinks WHERE id = :id")
    fun observeById(id: Long): Flow<DrinkEntity?>

    @Query("UPDATE drinks SET isDisabled = :disabled WHERE id = :drinkId")
    suspend fun setDisabled(drinkId: Long, disabled: Boolean)
}
