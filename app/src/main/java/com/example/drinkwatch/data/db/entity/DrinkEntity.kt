package com.example.drinkwatch.data.db.entity

import androidx.room.*
import com.example.drinkwatch.data.model.Drink
import com.example.drinkwatch.data.model.DrinkType

@Entity(
    tableName = "drinks",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId")],
)
data class DrinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val name: String,
    val type: DrinkType,
    val isDisabled: Boolean = false,
)

fun DrinkEntity.toDomain() = Drink(
    id = id, sessionId = sessionId, name = name,
    type = type, isDisabled = isDisabled,
)

fun Drink.toEntity() = DrinkEntity(
    id = id, sessionId = sessionId, name = name,
    type = type, isDisabled = isDisabled,
)
