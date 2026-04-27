package com.example.drinkwatch.data.db.entity

import androidx.room.*
import com.example.drinkwatch.data.model.Event

@Entity(
    tableName = "events",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId"), Index("playerId")],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestampMs: Long,
    val type: String,
    val playerId: Long? = null,
    val drinkId: Long? = null,
    val glassGroup: String? = null,
    val glassNumber: Int? = null,
    val durationSeconds: Int? = null,
) {
    companion object {
        const val TYPE_ORDER = "ORDER"
        const val TYPE_RETURN = "RETURN"
        const val TYPE_TIMEOUT = "TIMEOUT"
        const val TYPE_DISABLE_PLAYER = "DISABLE_PLAYER"
        const val TYPE_DISABLE_DRINK = "DISABLE_DRINK"
        const val TYPE_ENABLE_PLAYER = "ENABLE_PLAYER"
        const val TYPE_ENABLE_DRINK = "ENABLE_DRINK"
    }
}

fun EventEntity.toDomain(): Event = when (type) {
    EventEntity.TYPE_ORDER -> Event.Order(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        playerId = requireNotNull(playerId),
        drinkId = requireNotNull(drinkId),
        glassGroup = glassGroup?.first(),
        glassNumber = glassNumber,
    )
    EventEntity.TYPE_RETURN -> Event.Return(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        glassGroup = requireNotNull(glassGroup).first(),
        glassNumber = requireNotNull(glassNumber),
    )
    EventEntity.TYPE_TIMEOUT -> Event.Timeout(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        playerId = requireNotNull(playerId),
        durationSeconds = requireNotNull(durationSeconds),
    )
    EventEntity.TYPE_DISABLE_PLAYER -> Event.DisablePlayer(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        playerId = requireNotNull(playerId),
    )
    EventEntity.TYPE_DISABLE_DRINK -> Event.DisableDrink(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        drinkId = requireNotNull(drinkId),
    )
    EventEntity.TYPE_ENABLE_PLAYER -> Event.EnablePlayer(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        playerId = requireNotNull(playerId),
    )
    EventEntity.TYPE_ENABLE_DRINK -> Event.EnableDrink(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        drinkId = requireNotNull(drinkId),
    )
    else -> error("Unknown event type: $type")
}

fun Event.toEntity(): EventEntity = when (this) {
    is Event.Order -> EventEntity(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        type = EventEntity.TYPE_ORDER,
        playerId = playerId, drinkId = drinkId,
        glassGroup = glassGroup?.toString(), glassNumber = glassNumber,
    )
    is Event.Return -> EventEntity(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        type = EventEntity.TYPE_RETURN,
        glassGroup = glassGroup.toString(), glassNumber = glassNumber,
    )
    is Event.Timeout -> EventEntity(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        type = EventEntity.TYPE_TIMEOUT,
        playerId = playerId, durationSeconds = durationSeconds,
    )
    is Event.DisablePlayer -> EventEntity(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        type = EventEntity.TYPE_DISABLE_PLAYER,
        playerId = playerId,
    )
    is Event.DisableDrink -> EventEntity(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        type = EventEntity.TYPE_DISABLE_DRINK,
        drinkId = drinkId,
    )
    is Event.EnablePlayer -> EventEntity(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        type = EventEntity.TYPE_ENABLE_PLAYER,
        playerId = playerId,
    )
    is Event.EnableDrink -> EventEntity(
        id = id, sessionId = sessionId, timestampMs = timestampMs,
        type = EventEntity.TYPE_ENABLE_DRINK,
        drinkId = drinkId,
    )
}
