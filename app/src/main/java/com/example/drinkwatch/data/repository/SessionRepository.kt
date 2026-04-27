package com.example.drinkwatch.data.repository

import com.example.drinkwatch.data.db.AppDatabase
import com.example.drinkwatch.data.db.dao.*
import com.example.drinkwatch.data.db.entity.*
import com.example.drinkwatch.data.model.*
import com.example.drinkwatch.data.serialization.SessionSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction

class SessionRepository(
    private val db: AppDatabase,
    private val sessionDao: SessionDao,
    private val playerDao: PlayerDao,
    private val drinkDao: DrinkDao,
    private val glassGroupDao: GlassGroupDao,
    private val eventDao: EventDao,
) {

    // ── Session ──────────────────────────────────────────────────────────────

    /** Enforces single-session invariant: deletes all existing data first, then inserts. */
    suspend fun createSession(name: String): Long = db.withTransaction {
        sessionDao.deleteAll()
        sessionDao.insert(SessionEntity(name = name))
    }

    suspend fun updateSessionName(sessionId: Long, name: String) {
        val entity = sessionDao.getById(sessionId) ?: return
        sessionDao.update(entity.copy(name = name))
    }

    /** Observes the single active session (first row in the sessions table). */
    fun observeCurrentSession(): Flow<Session?> =
        sessionDao.observeAll().map { it.firstOrNull()?.toDomain() }

    suspend fun hasActiveSession(): Boolean = sessionDao.exists()

    // ── Players ──────────────────────────────────────────────────────────────

    suspend fun addPlayer(
        sessionId: Long,
        name: String,
        phone: String,
        note: String,
    ): Long = playerDao.insert(PlayerEntity(sessionId = sessionId, name = name, phone = phone, note = note))

    suspend fun updatePlayer(player: Player) = playerDao.update(player.toEntity())

    suspend fun deletePlayer(player: Player) = playerDao.delete(player.toEntity())

    suspend fun disablePlayer(
        sessionId: Long,
        playerId: Long,
        nowMs: Long = System.currentTimeMillis(),
    ) = db.withTransaction {
        eventDao.insert(
            EventEntity(
                sessionId = sessionId,
                timestampMs = nowMs,
                type = EventEntity.TYPE_DISABLE_PLAYER,
                playerId = playerId,
            )
        )
        playerDao.setDisabled(playerId, true)
    }

    suspend fun enablePlayer(
        sessionId: Long,
        playerId: Long,
        nowMs: Long = System.currentTimeMillis(),
    ) = db.withTransaction {
        eventDao.insert(
            EventEntity(
                sessionId = sessionId,
                timestampMs = nowMs,
                type = EventEntity.TYPE_ENABLE_PLAYER,
                playerId = playerId,
            )
        )
        playerDao.setDisabled(playerId, false)
    }

    fun observePlayers(sessionId: Long): Flow<List<Player>> =
        playerDao.getAllBySession(sessionId).map { list -> list.map { it.toDomain() } }

    // ── Drinks ───────────────────────────────────────────────────────────────

    suspend fun addDrink(sessionId: Long, name: String, type: DrinkType): Long =
        drinkDao.insert(DrinkEntity(sessionId = sessionId, name = name, type = type))

    suspend fun updateDrink(drink: Drink) = drinkDao.update(drink.toEntity())

    suspend fun deleteDrink(drink: Drink) = drinkDao.delete(drink.toEntity())

    suspend fun disableDrink(
        sessionId: Long,
        drinkId: Long,
        nowMs: Long = System.currentTimeMillis(),
    ) = db.withTransaction {
        eventDao.insert(
            EventEntity(
                sessionId = sessionId,
                timestampMs = nowMs,
                type = EventEntity.TYPE_DISABLE_DRINK,
                drinkId = drinkId,
            )
        )
        drinkDao.setDisabled(drinkId, true)
    }

    suspend fun enableDrink(
        sessionId: Long,
        drinkId: Long,
        nowMs: Long = System.currentTimeMillis(),
    ) = db.withTransaction {
        eventDao.insert(
            EventEntity(
                sessionId = sessionId,
                timestampMs = nowMs,
                type = EventEntity.TYPE_ENABLE_DRINK,
                drinkId = drinkId,
            )
        )
        drinkDao.setDisabled(drinkId, false)
    }

    fun observeDrinks(sessionId: Long): Flow<List<Drink>> =
        drinkDao.getAllBySession(sessionId).map { list -> list.map { it.toDomain() } }

    // ── Glass Groups ─────────────────────────────────────────────────────────

    suspend fun addGlassGroup(sessionId: Long, letter: Char): Long =
        glassGroupDao.insert(GlassGroupEntity(sessionId = sessionId, letter = letter.toString()))

    suspend fun removeGlassGroup(sessionId: Long, letter: Char) {
        val entity = glassGroupDao.getByLetter(sessionId, letter.toString()) ?: return
        glassGroupDao.delete(entity)
    }

    fun observeGlassGroups(sessionId: Long): Flow<List<GlassGroup>> =
        glassGroupDao.getAllBySession(sessionId).map { list -> list.map { it.toDomain() } }

    // ── Derived state ─────────────────────────────────────────────────────────

    fun observePlayerStates(sessionId: Long): Flow<List<PlayerDerivedState>> =
        combine(
            playerDao.getAllBySession(sessionId),
            drinkDao.getAllBySession(sessionId),
            eventDao.getBySession(sessionId),
        ) { players, drinks, events ->
            val drinkTypeById: Map<Long, DrinkType> = drinks.associate { it.id to it.type }
            val takenGlassesByKey: Map<Pair<String, Int>, Long> = computeTakenGlassKeys(events)
            players.map { playerEntity ->
                computePlayerDerivedState(
                    player = playerEntity.toDomain(),
                    events = events,
                    drinkTypeById = drinkTypeById,
                    takenGlassesByKey = takenGlassesByKey,
                    nowMs = System.currentTimeMillis(),
                )
            }
        }

    /**
     * Maps each currently-unreturned glass key (group, number) to the event ID of its ORDER event.
     * Used to determine whether a specific historical order still holds its glass.
     */
    fun observeTakenGlassEventIds(sessionId: Long): Flow<Map<Pair<Char, Int>, Long>> =
        eventDao.getTakenGlasses(sessionId).map { entities ->
            entities.associate { e -> Pair(e.glassGroup!!.first(), e.glassNumber!!) to e.id }
        }

    fun observeTakenGlasses(sessionId: Long): Flow<List<TakenGlass>> =
        eventDao.getTakenGlasses(sessionId).map { entities ->
            entities.map { e ->
                TakenGlass(
                    glassGroup = e.glassGroup!!.first(),
                    glassNumber = e.glassNumber!!,
                    playerId = e.playerId!!,
                    drinkId = e.drinkId!!,
                    takenAtMs = e.timestampMs,
                )
            }
        }

    suspend fun isGlassTaken(sessionId: Long, group: Char, number: Int): Boolean =
        eventDao.isGlassTaken(sessionId, group.toString(), number)

    // ── Player event history ──────────────────────────────────────────────────

    fun observePlayerHistory(sessionId: Long, playerId: Long): Flow<List<Event>> =
        eventDao.getByPlayer(sessionId, playerId)
            .map { entities -> entities.map { it.toDomain() } }

    // ── Timeouts & Returns ────────────────────────────────────────────────────

    suspend fun startTimeout(
        sessionId: Long,
        playerId: Long,
        durationSeconds: Int,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        eventDao.insert(
            EventEntity(
                sessionId = sessionId,
                timestampMs = nowMs,
                type = EventEntity.TYPE_TIMEOUT,
                playerId = playerId,
                durationSeconds = durationSeconds,
            )
        )
    }

    suspend fun returnGlass(
        sessionId: Long,
        glassGroup: Char,
        glassNumber: Int,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        eventDao.insert(
            EventEntity(
                sessionId = sessionId,
                timestampMs = nowMs,
                type = EventEntity.TYPE_RETURN,
                glassGroup = glassGroup.toString(),
                glassNumber = glassNumber,
            )
        )
    }

    // ── Orders ────────────────────────────────────────────────────────────────

    /**
     * Commits a batch of queued orders atomically.
     *
     * Throws [IllegalArgumentException] if the batch contains duplicate (glassGroup, glassNumber)
     * pairs, which would create conflicting glass assignments.
     */
    suspend fun commitOrders(
        sessionId: Long,
        orders: List<QueuedOrder>,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        val glassKeys = orders
            .filter { it.glassGroup != null && it.glassNumber != null }
            .map { it.glassGroup!! to it.glassNumber!! }
        require(glassKeys.size == glassKeys.toSet().size) {
            "Duplicate glass assignments in order batch"
        }

        db.withTransaction {
            for (order in orders) {
                eventDao.insert(
                    EventEntity(
                        sessionId = sessionId,
                        timestampMs = nowMs,
                        type = EventEntity.TYPE_ORDER,
                        playerId = order.playerId,
                        drinkId = order.drinkId,
                        glassGroup = order.glassGroup?.toString(),
                        glassNumber = order.glassNumber,
                    )
                )
            }
        }
    }

    // ── Import ────────────────────────────────────────────────────────────────

    /**
     * Replaces the current session with the given snapshot.
     *
     * Runs entirely in one transaction. Old IDs from the snapshot are remapped to new
     * auto-generated IDs. `isDisabled` flags are re-derived by replaying DISABLE events.
     */
    suspend fun importSession(snapshot: SessionSnapshot) = db.withTransaction {
        sessionDao.deleteAll()

        val newSessionId = sessionDao.insert(SessionEntity(name = snapshot.name))

        // Insert players; build old-id → new-id map.
        val playerIdMap = mutableMapOf<Long, Long>()
        for (p in snapshot.players) {
            val newId = playerDao.insert(
                PlayerEntity(
                    sessionId = newSessionId,
                    name = p.name,
                    phone = p.phone,
                    note = p.note,
                )
            )
            playerIdMap[p.id] = newId
        }

        // Insert drinks; build old-id → new-id map.
        val drinkIdMap = mutableMapOf<Long, Long>()
        for (d in snapshot.drinks) {
            val newId = drinkDao.insert(
                DrinkEntity(
                    sessionId = newSessionId,
                    name = d.name,
                    type = DrinkType.valueOf(d.type),
                )
            )
            drinkIdMap[d.id] = newId
        }

        // Insert glass groups.
        for (letter in snapshot.glassGroups) {
            glassGroupDao.insert(GlassGroupEntity(sessionId = newSessionId, letter = letter))
        }

        // Insert events; remap player/drink IDs.
        for (e in snapshot.events) {
            val newPlayerId = e.playerId?.let { playerIdMap[it] }
            val newDrinkId = e.drinkId?.let { drinkIdMap[it] }

            // Skip events whose referenced entities are not present in the snapshot.
            val needsPlayer = e.type in setOf(
                EventEntity.TYPE_ORDER,
                EventEntity.TYPE_TIMEOUT,
                EventEntity.TYPE_DISABLE_PLAYER,
                EventEntity.TYPE_ENABLE_PLAYER,
            )
            val needsDrink = e.type in setOf(EventEntity.TYPE_ORDER, EventEntity.TYPE_DISABLE_DRINK, EventEntity.TYPE_ENABLE_DRINK)

            if (needsPlayer && newPlayerId == null) continue
            if (needsDrink && newDrinkId == null) continue

            eventDao.insert(
                EventEntity(
                    sessionId = newSessionId,
                    timestampMs = e.timestampMs,
                    type = e.type,
                    playerId = newPlayerId,
                    drinkId = newDrinkId,
                    glassGroup = e.glassGroup,
                    glassNumber = e.glassNumber,
                    durationSeconds = e.durationSeconds,
                )
            )
        }

        // Re-derive isDisabled flags by replaying events in chronological order (last event wins).
        for (e in snapshot.events) {
            when (e.type) {
                EventEntity.TYPE_DISABLE_PLAYER -> {
                    val newId = e.playerId?.let { playerIdMap[it] } ?: continue
                    playerDao.setDisabled(newId, true)
                }
                EventEntity.TYPE_ENABLE_PLAYER -> {
                    val newId = e.playerId?.let { playerIdMap[it] } ?: continue
                    playerDao.setDisabled(newId, false)
                }
                EventEntity.TYPE_DISABLE_DRINK -> {
                    val newId = e.drinkId?.let { drinkIdMap[it] } ?: continue
                    drinkDao.setDisabled(newId, true)
                }
                EventEntity.TYPE_ENABLE_DRINK -> {
                    val newId = e.drinkId?.let { drinkIdMap[it] } ?: continue
                    drinkDao.setDisabled(newId, false)
                }
            }
        }
    }
}

// ── Internal derived-state helpers ─────────────────────────────────────────
// Exposed as internal for unit testing.

/**
 * Returns a map of (glassGroup, glassNumber) → playerId for all currently taken glasses.
 *
 * A glass is "taken" if the event with the highest id for that (group, number) pair is an ORDER.
 */
internal fun computeTakenGlassKeys(events: List<EventEntity>): Map<Pair<String, Int>, Long> {
    val relevant = events.filter {
        (it.type == EventEntity.TYPE_ORDER || it.type == EventEntity.TYPE_RETURN)
            && it.glassGroup != null && it.glassNumber != null
    }
    val latestByKey = mutableMapOf<Pair<String, Int>, EventEntity>()
    for (e in relevant) {
        val key = Pair(e.glassGroup!!, e.glassNumber!!)
        val existing = latestByKey[key]
        if (existing == null || e.id > existing.id) latestByKey[key] = e
    }
    return latestByKey
        .filter { (_, e) -> e.type == EventEntity.TYPE_ORDER }
        .mapValues { (_, e) -> e.playerId!! }
}

/**
 * Computes [PlayerDerivedState] from the full event list for the given player.
 *
 * [nowMs] is accepted explicitly so callers (and tests) can control time.
 */
internal fun computePlayerDerivedState(
    player: Player,
    events: List<EventEntity>,
    drinkTypeById: Map<Long, DrinkType>,
    takenGlassesByKey: Map<Pair<String, Int>, Long>,
    nowMs: Long,
): PlayerDerivedState {
    // Find the latest timeout event for this player using canonical (timestampMs, id) ordering.
    val lastTimeout = events
        .filter { it.type == EventEntity.TYPE_TIMEOUT && it.playerId == player.id }
        .maxWithOrNull(compareBy({ it.timestampMs }, { it.id }))

    val isUnderTimeout = lastTimeout != null
        && lastTimeout.durationSeconds!! > 0
        && (lastTimeout.timestampMs + lastTimeout.durationSeconds * 1000L) > nowMs

    val timeoutEndsAtMs = if (isUnderTimeout)
        lastTimeout.timestampMs + lastTimeout.durationSeconds * 1000L
    else
        null

    // Reset point: active drinks are counted only since the last timeout event of any kind.
    val lastTimeoutTs = lastTimeout?.timestampMs ?: Long.MIN_VALUE

    val activeDrinkCount = events.count { e ->
        e.type == EventEntity.TYPE_ORDER
            && e.playerId == player.id
            && e.timestampMs > lastTimeoutTs
            && drinkTypeById[e.drinkId!!] != DrinkType.NON_ALCOHOLIC
    }

    val totalDrinks = events.count { e ->
        e.type == EventEntity.TYPE_ORDER && e.playerId == player.id
    }

    val totalTimeoutSeconds = events
        .filter {
            it.type == EventEntity.TYPE_TIMEOUT
                && it.playerId == player.id
                && (it.durationSeconds ?: 0) > 0
        }
        .sumOf { it.durationSeconds!! }

    val unreturnedGlassCount = takenGlassesByKey.values.count { it == player.id }

    return PlayerDerivedState(
        player = player,
        activeDrinkCount = activeDrinkCount,
        isUnderTimeout = isUnderTimeout,
        timeoutEndsAtMs = timeoutEndsAtMs,
        totalDrinks = totalDrinks,
        totalTimeoutSeconds = totalTimeoutSeconds,
        unreturnedGlassCount = unreturnedGlassCount,
    )
}
