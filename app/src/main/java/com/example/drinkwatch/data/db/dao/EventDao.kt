package com.example.drinkwatch.data.db.dao

import androidx.room.*
import com.example.drinkwatch.data.db.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: EventEntity): Long

    @Query("SELECT * FROM events WHERE sessionId = :sessionId ORDER BY timestampMs ASC, id ASC")
    fun getBySession(sessionId: Long): Flow<List<EventEntity>>

    @Query("""
        SELECT * FROM events
        WHERE sessionId = :sessionId AND playerId = :playerId
        ORDER BY timestampMs ASC, id ASC
    """)
    fun getByPlayer(sessionId: Long, playerId: Long): Flow<List<EventEntity>>

    @Query("""
        SELECT * FROM events
        WHERE sessionId = :sessionId AND playerId = :playerId AND type = 'TIMEOUT'
        ORDER BY timestampMs DESC LIMIT 1
    """)
    fun getLatestTimeoutForPlayer(sessionId: Long, playerId: Long): Flow<EventEntity?>

    @Query("""
        SELECT * FROM events
        WHERE sessionId = :sessionId AND playerId = :playerId AND type = 'ORDER'
        AND timestampMs > COALESCE(
            (SELECT MAX(timestampMs) FROM events
             WHERE sessionId = :sessionId AND playerId = :playerId AND type = 'TIMEOUT'),
            0
        )
        ORDER BY timestampMs ASC
    """)
    fun getOrdersSinceLastTimeout(sessionId: Long, playerId: Long): Flow<List<EventEntity>>

    /**
     * Returns ORDER events representing currently taken glasses.
     *
     * For each (glassGroup, glassNumber) pair, selects the ORDER whose id equals the maximum id
     * across all ORDER and RETURN events for that pair. Because id is append-only (auto-increment),
     * the maximum id is always the most recent event. If the most recent event is a RETURN, no
     * ORDER event matches and the glass is excluded.
     */
    @Query("""
        SELECT * FROM events AS o
        WHERE o.type = 'ORDER'
        AND o.glassGroup IS NOT NULL
        AND o.sessionId = :sessionId
        AND o.id = (
            SELECT MAX(e.id) FROM events AS e
            WHERE (e.type = 'ORDER' OR e.type = 'RETURN')
            AND e.glassGroup = o.glassGroup
            AND e.glassNumber = o.glassNumber
            AND e.sessionId = :sessionId
        )
        ORDER BY o.timestampMs ASC, o.id ASC
    """)
    fun getTakenGlasses(sessionId: Long): Flow<List<EventEntity>>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM events AS o
            WHERE o.type = 'ORDER'
            AND o.glassGroup = :glassGroup
            AND o.glassNumber = :glassNumber
            AND o.sessionId = :sessionId
            AND o.id = (
                SELECT MAX(e.id) FROM events AS e
                WHERE (e.type = 'ORDER' OR e.type = 'RETURN')
                AND e.glassGroup = :glassGroup
                AND e.glassNumber = :glassNumber
                AND e.sessionId = :sessionId
            )
        )
    """)
    suspend fun isGlassTaken(sessionId: Long, glassGroup: String, glassNumber: Int): Boolean
}
