package com.example.drinkwatch.data.repository

import com.example.drinkwatch.data.db.entity.EventEntity
import com.example.drinkwatch.data.model.DrinkType
import com.example.drinkwatch.data.model.Player
import com.example.drinkwatch.data.model.PlayerDerivedState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DerivedStateTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private val sessionId = 1L
    private val playerId = 10L
    private val drinkAlcoholic = 100L
    private val drinkShot = 101L
    private val drinkSoft = 200L

    private val drinkTypeById: Map<Long, DrinkType> = mapOf(
        drinkAlcoholic to DrinkType.LONG_DRINK,
        drinkShot to DrinkType.SHOT,
        drinkSoft to DrinkType.NON_ALCOHOLIC,
    )

    private var nextId = 1L
    private fun nextId() = nextId++

    private fun orderEvent(
        playerId: Long = this.playerId,
        drinkId: Long = drinkAlcoholic,
        timestampMs: Long = 1000L,
        glassGroup: String? = null,
        glassNumber: Int? = null,
    ) = EventEntity(
        id = nextId(),
        sessionId = sessionId,
        timestampMs = timestampMs,
        type = EventEntity.TYPE_ORDER,
        playerId = playerId,
        drinkId = drinkId,
        glassGroup = glassGroup,
        glassNumber = glassNumber,
    )

    private fun returnEvent(
        glassGroup: String,
        glassNumber: Int,
        timestampMs: Long = 2000L,
    ) = EventEntity(
        id = nextId(),
        sessionId = sessionId,
        timestampMs = timestampMs,
        type = EventEntity.TYPE_RETURN,
        glassGroup = glassGroup,
        glassNumber = glassNumber,
    )

    private fun timeoutEvent(
        playerId: Long = this.playerId,
        durationSeconds: Int,
        timestampMs: Long = 1000L,
    ) = EventEntity(
        id = nextId(),
        sessionId = sessionId,
        timestampMs = timestampMs,
        type = EventEntity.TYPE_TIMEOUT,
        playerId = playerId,
        durationSeconds = durationSeconds,
    )

    private fun player() = Player(
        id = playerId, sessionId = sessionId,
        name = "Alice", phone = "", note = "", isDisabled = false,
    )

    private fun derive(
        events: List<EventEntity>,
        nowMs: Long = 0L,
        takenGlassesByKey: Map<Pair<String, Int>, Long> = computeTakenGlassKeys(events),
    ): PlayerDerivedState = computePlayerDerivedState(
        player = player(),
        events = events,
        drinkTypeById = drinkTypeById,
        takenGlassesByKey = takenGlassesByKey,
        nowMs = nowMs,
    )

    // ── Test 1: No events ─────────────────────────────────────────────────────

    @Test
    fun noEvents_allZero() {
        val state = derive(emptyList())
        assertEquals(0, state.activeDrinkCount)
        assertFalse(state.isUnderTimeout)
        assertNull(state.timeoutEndsAtMs)
        assertEquals(0, state.totalDrinks)
        assertEquals(0, state.totalTimeoutSeconds)
        assertEquals(0, state.unreturnedGlassCount)
    }

    // ── Test 2: One alcoholic order, no timeout ───────────────────────────────

    @Test
    fun oneAlcoholicOrder_activeDrinkCount1() {
        val events = listOf(orderEvent(drinkId = drinkAlcoholic))
        val state = derive(events)
        assertEquals(1, state.activeDrinkCount)
        assertEquals(1, state.totalDrinks)
        assertFalse(state.isUnderTimeout)
    }

    // ── Test 3: One NON_ALCOHOLIC order ──────────────────────────────────────

    @Test
    fun oneNonAlcoholicOrder_activeDrinkCount0() {
        val events = listOf(orderEvent(drinkId = drinkSoft))
        val state = derive(events)
        assertEquals(0, state.activeDrinkCount)
        assertEquals(1, state.totalDrinks)
    }

    // ── Test 4: Two alcoholic orders then timeout ─────────────────────────────

    @Test
    fun twoOrdersThenTimeout_activeDrinkCountReset() {
        val events = listOf(
            orderEvent(drinkId = drinkAlcoholic, timestampMs = 1000L),
            orderEvent(drinkId = drinkAlcoholic, timestampMs = 2000L),
            timeoutEvent(durationSeconds = 300, timestampMs = 3000L),
        )
        val state = derive(events, nowMs = 3500L)
        assertEquals(0, state.activeDrinkCount)
        assertEquals(2, state.totalDrinks)
    }

    // ── Test 5: Timeout with durationSeconds=0 (cancel) ──────────────────────

    @Test
    fun cancelTimeout_notUnderTimeout() {
        val events = listOf(
            timeoutEvent(durationSeconds = 0, timestampMs = 1000L),
        )
        val state = derive(events, nowMs = 1500L)
        assertFalse(state.isUnderTimeout)
        assertNull(state.timeoutEndsAtMs)
    }

    // ── Test 6: Active timeout ────────────────────────────────────────────────

    @Test
    fun activeTimeout_isUnderTimeoutTrue() {
        val startMs = 10_000L
        val durationSec = 300
        val nowMs = startMs + 100L  // well within timeout
        val events = listOf(timeoutEvent(durationSeconds = durationSec, timestampMs = startMs))
        val state = derive(events, nowMs = nowMs)
        assertTrue(state.isUnderTimeout)
        assertNotNull(state.timeoutEndsAtMs)
        assertEquals(startMs + durationSec * 1000L, state.timeoutEndsAtMs)
    }

    // ── Test 7: Expired timeout ───────────────────────────────────────────────

    @Test
    fun expiredTimeout_isUnderTimeoutFalse() {
        val startMs = 10_000L
        val durationSec = 5
        val nowMs = startMs + 6_000L  // expired
        val events = listOf(timeoutEvent(durationSeconds = durationSec, timestampMs = startMs))
        val state = derive(events, nowMs = nowMs)
        assertFalse(state.isUnderTimeout)
        assertNull(state.timeoutEndsAtMs)
    }

    // ── Test 8: Same timestampMs, differ by id ────────────────────────────────

    @Test
    fun sameTimestamp_latestIdWins() {
        val ts = 5_000L
        // Lower id: durationSeconds=300 (active timeout)
        // Higher id: durationSeconds=0  (cancel) → should win
        val timeout1 = EventEntity(
            id = 1L, sessionId = sessionId, timestampMs = ts,
            type = EventEntity.TYPE_TIMEOUT, playerId = playerId, durationSeconds = 300,
        )
        val timeout2 = EventEntity(
            id = 2L, sessionId = sessionId, timestampMs = ts,
            type = EventEntity.TYPE_TIMEOUT, playerId = playerId, durationSeconds = 0,
        )
        val state = derive(listOf(timeout1, timeout2), nowMs = ts + 100L)
        assertFalse("Higher id (cancel) should win", state.isUnderTimeout)
    }

    // ── Test 9: Glass taken ───────────────────────────────────────────────────

    @Test
    fun glassTaken_unreturnedGlassCount1() {
        val events = listOf(orderEvent(glassGroup = "A", glassNumber = 1))
        val state = derive(events)
        assertEquals(1, state.unreturnedGlassCount)
    }

    // ── Test 10: Glass taken then returned ────────────────────────────────────

    @Test
    fun glassTakenThenReturned_unreturnedGlassCount0() {
        val events = listOf(
            orderEvent(glassGroup = "A", glassNumber = 1, timestampMs = 1000L),
            returnEvent(glassGroup = "A", glassNumber = 1, timestampMs = 2000L),
        )
        val state = derive(events)
        assertEquals(0, state.unreturnedGlassCount)
    }

    // ── Test 11: Same glass re-ordered (second order wins) ────────────────────

    @Test
    fun glassReOrdered_onlyOneEntry() {
        val order1 = orderEvent(playerId = playerId, glassGroup = "A", glassNumber = 1, timestampMs = 1000L)
        val returnEv = returnEvent(glassGroup = "A", glassNumber = 1, timestampMs = 2000L)
        val order2 = orderEvent(playerId = playerId, glassGroup = "A", glassNumber = 1, timestampMs = 3000L)
        val events = listOf(order1, returnEv, order2)
        val keys = computeTakenGlassKeys(events)
        assertEquals(1, keys.size)
        assertEquals(playerId, keys[Pair("A", 1)])
    }

    // ── Test 12: totalTimeoutSeconds ─────────────────────────────────────────

    @Test
    fun totalTimeoutSeconds_sumOfNonZeroOnly() {
        val events = listOf(
            timeoutEvent(durationSeconds = 300, timestampMs = 1000L),
            timeoutEvent(durationSeconds = 0, timestampMs = 2000L),  // cancel — not counted
            timeoutEvent(durationSeconds = 120, timestampMs = 3000L),
        )
        val state = derive(events, nowMs = 3500L)
        assertEquals(420, state.totalTimeoutSeconds)
    }

    // ── computeTakenGlassKeys: glasses for different players ──────────────────

    @Test
    fun takenGlassKeys_tracksPlayerIdCorrectly() {
        val otherPlayerId = 99L
        val events = listOf(
            orderEvent(playerId = playerId, glassGroup = "A", glassNumber = 1),
            orderEvent(playerId = otherPlayerId, glassGroup = "B", glassNumber = 2),
        )
        val keys = computeTakenGlassKeys(events)
        assertEquals(2, keys.size)
        assertEquals(playerId, keys[Pair("A", 1)])
        assertEquals(otherPlayerId, keys[Pair("B", 2)])
    }

    // ── activeDrinkCount: zero-duration timeout resets count ──────────────────

    @Test
    fun cancelTimeoutAlsoResetsActiveDrinkCount() {
        val events = listOf(
            orderEvent(drinkId = drinkAlcoholic, timestampMs = 1000L),
            timeoutEvent(durationSeconds = 0, timestampMs = 2000L),  // cancel
        )
        val state = derive(events, nowMs = 2500L)
        assertEquals("Cancel timeout should still reset active drink counter", 0, state.activeDrinkCount)
    }
}
