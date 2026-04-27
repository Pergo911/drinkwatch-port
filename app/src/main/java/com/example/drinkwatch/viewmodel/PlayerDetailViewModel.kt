package com.example.drinkwatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.drinkwatch.data.model.Event
import com.example.drinkwatch.data.model.PlayerDerivedState
import com.example.drinkwatch.data.model.Session
import com.example.drinkwatch.data.repository.SessionRepository
import com.example.drinkwatch.data.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerDetailViewModel(
    private val playerId: Long,
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** 1-second ticker that emits the current epoch-ms to drive the timeout countdown. */
    private val tickerFlow: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000L)
        }
    }

    private val _currentSession: StateFlow<Session?> =
        sessionRepository.observeCurrentSession()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Derived state for this player, or null if the player no longer exists. */
    val derivedState: StateFlow<PlayerDerivedState?> =
        _currentSession.flatMapLatest { session ->
            if (session == null) return@flatMapLatest flowOf(null)
            sessionRepository.observePlayerStates(session.id)
                .map { list -> list.firstOrNull { it.player.id == playerId } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Ticker-driven milliseconds until the timeout expires.
     *
     * This is the source of truth for whether the player is under timeout in the UI —
     * not [PlayerDerivedState.isUnderTimeout], which can stay stale after expiry until
     * the next DB event.
     */
    val timeoutMillisRemaining: StateFlow<Long?> =
        combine(derivedState, tickerFlow) { state, nowMs ->
            state?.timeoutEndsAtMs?.let { endsAt -> (endsAt - nowMs).takeIf { it > 0 } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeDrinkHighlight: StateFlow<Int> =
        settingsRepository.settings
            .map { it.activeDrinkHighlight }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 3)

    /** Order history for this player, newest last. */
    val orderHistory: StateFlow<List<OrderHistoryItem>> =
        _currentSession.flatMapLatest { session ->
            if (session == null) return@flatMapLatest flowOf(emptyList())
            combine(
                sessionRepository.observePlayerHistory(session.id, playerId),
                sessionRepository.observeDrinks(session.id),
            ) { events, drinks ->
                val drinkById = drinks.associateBy { it.id }
                events.filterIsInstance<Event.Order>().map { order ->
                    OrderHistoryItem(
                        timestampMs = order.timestampMs,
                        drinkName = drinkById[order.drinkId]?.name ?: "(deleted drink)",
                        glassGroup = order.glassGroup,
                        glassNumber = order.glassNumber,
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Timeout history for this player (all durations including cancel events), newest last. */
    val timeoutHistory: StateFlow<List<TimeoutHistoryItem>> =
        _currentSession.flatMapLatest { session ->
            if (session == null) return@flatMapLatest flowOf(emptyList())
            sessionRepository.observePlayerHistory(session.id, playerId)
                .map { events ->
                    events.filterIsInstance<Event.Timeout>().map { timeout ->
                        TimeoutHistoryItem(
                            timestampMs = timeout.timestampMs,
                            durationSeconds = timeout.durationSeconds,
                        )
                    }
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    class Factory(
        private val sessionRepository: SessionRepository,
        private val settingsRepository: SettingsRepository,
        private val playerId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            PlayerDetailViewModel(playerId, sessionRepository, settingsRepository) as T
    }
}
