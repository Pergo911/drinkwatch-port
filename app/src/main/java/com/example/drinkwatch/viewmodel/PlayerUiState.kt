package com.example.drinkwatch.viewmodel

import com.example.drinkwatch.data.model.PlayerDerivedState
import com.example.drinkwatch.data.model.QueuedOrder

data class PlayerUiState(
    val derived: PlayerDerivedState,
    val queuedOrder: QueuedOrder?,
    /** Milliseconds until the timeout expires; null if not under timeout. Ticker-driven. */
    val timeoutMillisRemaining: Long?,
) {
    /**
     * True while [timeoutMillisRemaining] is non-null and > 0.
     *
     * Driven by the 1-second ticker in MainViewModel, NOT by [derived.isUnderTimeout], which only
     * refreshes when a DB event is emitted and can stay stale after expiry.
     */
    val isUnderTimeout: Boolean get() = timeoutMillisRemaining != null
}
