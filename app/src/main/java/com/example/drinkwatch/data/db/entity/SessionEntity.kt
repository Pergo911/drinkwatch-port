package com.example.drinkwatch.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.drinkwatch.data.model.Session

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

fun SessionEntity.toDomain() = Session(id = id, name = name)
fun Session.toEntity() = SessionEntity(id = id, name = name)
