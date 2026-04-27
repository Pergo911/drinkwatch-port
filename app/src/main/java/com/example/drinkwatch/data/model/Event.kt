package com.example.drinkwatch.data.model

sealed class Event {
    abstract val id: Long
    abstract val sessionId: Long
    abstract val timestampMs: Long

    data class Order(
        override val id: Long,
        override val sessionId: Long,
        override val timestampMs: Long,
        val playerId: Long,
        val drinkId: Long,
        val glassGroup: Char?,
        val glassNumber: Int?,
    ) : Event()

    data class Return(
        override val id: Long,
        override val sessionId: Long,
        override val timestampMs: Long,
        val glassGroup: Char,
        val glassNumber: Int,
    ) : Event()

    data class Timeout(
        override val id: Long,
        override val sessionId: Long,
        override val timestampMs: Long,
        val playerId: Long,
        val durationSeconds: Int,   // 0 = cancel current timeout
    ) : Event()

    data class DisablePlayer(
        override val id: Long,
        override val sessionId: Long,
        override val timestampMs: Long,
        val playerId: Long,
    ) : Event()

    data class DisableDrink(
        override val id: Long,
        override val sessionId: Long,
        override val timestampMs: Long,
        val drinkId: Long,
    ) : Event()

    data class EnablePlayer(
        override val id: Long,
        override val sessionId: Long,
        override val timestampMs: Long,
        val playerId: Long,
    ) : Event()

    data class EnableDrink(
        override val id: Long,
        override val sessionId: Long,
        override val timestampMs: Long,
        val drinkId: Long,
    ) : Event()
}
