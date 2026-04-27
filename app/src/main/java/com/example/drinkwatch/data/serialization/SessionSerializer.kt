package com.example.drinkwatch.data.serialization

import com.example.drinkwatch.data.db.dao.*
import com.example.drinkwatch.data.repository.SessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

class SessionSerializer(
    private val repository: SessionRepository,
    private val sessionDao: SessionDao,
    private val playerDao: PlayerDao,
    private val drinkDao: DrinkDao,
    private val glassGroupDao: GlassGroupDao,
    private val eventDao: EventDao,
    private val json: Json = Json { prettyPrint = false },
) {

    /** Serialises the given session to JSON and writes it to [outputStream]. */
    suspend fun export(sessionId: Long, outputStream: OutputStream) {
        val session = sessionDao.getById(sessionId)
            ?: error("Session $sessionId not found")
        val players = playerDao.getAllBySession(sessionId).first()
        val drinks = drinkDao.getAllBySession(sessionId).first()
        val glassGroups = glassGroupDao.getAllBySession(sessionId).first()
        val events = eventDao.getBySession(sessionId).first()

        val snapshot = SessionSnapshot(
            name = session.name,
            players = players.map { p ->
                PlayerSnapshot(
                    id = p.id,
                    name = p.name,
                    phone = p.phone,
                    note = p.note,
                    isDisabled = p.isDisabled,
                )
            },
            drinks = drinks.map { d ->
                DrinkSnapshot(
                    id = d.id,
                    name = d.name,
                    type = d.type.name,
                    isDisabled = d.isDisabled,
                )
            },
            glassGroups = glassGroups.map { it.letter },
            events = events.map { e ->
                EventSnapshot(
                    id = e.id,
                    timestampMs = e.timestampMs,
                    type = e.type,
                    playerId = e.playerId,
                    drinkId = e.drinkId,
                    glassGroup = e.glassGroup,
                    glassNumber = e.glassNumber,
                    durationSeconds = e.durationSeconds,
                )
            },
        )

        val bytes = json.encodeToString(snapshot).toByteArray(Charsets.UTF_8)
        outputStream.use { it.write(bytes) }
    }

    /**
     * Reads a JSON snapshot from [inputStream] and replaces the current session with it.
     *
     * ID remapping and `isDisabled` flag restoration are handled by [SessionRepository.importSession].
     */
    suspend fun import(inputStream: InputStream) {
        val text = inputStream.use { it.readBytes() }.toString(Charsets.UTF_8)
        val snapshot = json.decodeFromString<SessionSnapshot>(text)
        repository.importSession(snapshot)
    }
}
