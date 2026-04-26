package com.example.drinkwatch.data.db.entity

import androidx.room.*
import com.example.drinkwatch.data.model.GlassGroup

@Entity(
    tableName = "glass_groups",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [
        Index("sessionId"),
        Index(value = ["sessionId", "letter"], unique = true),
    ],
)
data class GlassGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val letter: String,
)

fun GlassGroupEntity.toDomain() = GlassGroup(id = id, sessionId = sessionId, letter = letter.first())
fun GlassGroup.toEntity() = GlassGroupEntity(id = id, sessionId = sessionId, letter = letter.toString())
