package com.example.drinkwatch.data.db.entity

import androidx.room.*
import com.example.drinkwatch.data.model.Player

@Entity(
    tableName = "players",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId")],
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val name: String,
    val phone: String,
    val note: String,
    val isDisabled: Boolean = false,
)

fun PlayerEntity.toDomain() = Player(
    id = id, sessionId = sessionId, name = name,
    phone = phone, note = note, isDisabled = isDisabled,
)

fun Player.toEntity() = PlayerEntity(
    id = id, sessionId = sessionId, name = name,
    phone = phone, note = note, isDisabled = isDisabled,
)
