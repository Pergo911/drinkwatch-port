package com.example.drinkwatch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.drinkwatch.data.model.QueuedOrder
import com.example.drinkwatch.data.model.Session
import com.example.drinkwatch.data.repository.SessionRepository
import com.example.drinkwatch.data.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    // ── One-shot UI events ────────────────────────────────────────────────────

    sealed interface UiEvent {
        data object GlassAlreadyTaken : UiEvent
    }

    private val _uiEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    // ── Internal state ────────────────────────────────────────────────────────

    /** 1-second ticker that emits the current epoch-ms to drive timeout countdowns. */
    private val tickerFlow: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000L)
        }
    }

    /** Source of truth for the current session — shared by all session-scoped flows. */
    private val _currentSession: StateFlow<Session?> =
        sessionRepository.observeCurrentSession()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _queue = MutableStateFlow<List<QueuedOrder>>(emptyList())

    init {
        // Clear in-memory queue when the active session is replaced so stale player/drink IDs
        // cannot be committed into the new session.
        viewModelScope.launch {
            var prevId: Long? = null
            _currentSession.collect { session ->
                if (prevId != null && prevId != session?.id) {
                    _queue.value = emptyList()
                }
                prevId = session?.id
            }
        }
    }

    // ── Exposed state ─────────────────────────────────────────────────────────

    val queue: StateFlow<List<QueuedOrder>> = _queue.asStateFlow()

    val sessionName: StateFlow<String> =
        _currentSession
            .map { it?.name ?: "" }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Exposed so MainScreen can key rememberSaveable by session identity. */
    val sessionId: StateFlow<Long?> =
        _currentSession
            .map { it?.id }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val playerUiStates: StateFlow<List<PlayerUiState>> =
        _currentSession.flatMapLatest { session ->
            if (session == null) return@flatMapLatest flowOf(emptyList())
            combine(
                sessionRepository.observePlayerStates(session.id),
                _queue,
                tickerFlow,
            ) { derivedList, queue, nowMs ->
                derivedList.map { derived ->
                    val millisRemaining = derived.timeoutEndsAtMs
                        ?.let { it - nowMs }?.takeIf { it > 0 }
                    PlayerUiState(
                        derived = derived,
                        queuedOrder = queue.firstOrNull { it.playerId == derived.player.id },
                        timeoutMillisRemaining = millisRemaining,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val takenGlasses: StateFlow<List<TakenGlassUiState>> =
        _currentSession.flatMapLatest { session ->
            if (session == null) return@flatMapLatest flowOf(emptyList())
            combine(
                sessionRepository.observeTakenGlasses(session.id),
                sessionRepository.observePlayers(session.id),
                sessionRepository.observeDrinks(session.id),
            ) { takenList, players, drinks ->
                val playerById = players.associateBy { it.id }
                val drinkById  = drinks.associateBy  { it.id }
                takenList.map { tg ->
                    TakenGlassUiState(
                        glassGroup  = tg.glassGroup,
                        glassNumber = tg.glassNumber,
                        playerName  = playerById[tg.playerId]?.name ?: "",
                        drinkName   = drinkById[tg.drinkId]?.name  ?: "",
                        takenAtMs   = tg.takenAtMs,
                        playerId    = tg.playerId,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeDrinkHighlight: StateFlow<Int> =
        settingsRepository.settings
            .map { it.activeDrinkHighlight }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 3)

    // ── Actions ───────────────────────────────────────────────────────────────

    fun addToQueue(playerId: Long, drinkId: Long, glassGroup: Char?, glassNumber: Int?) {
        viewModelScope.launch {
            val sessionId = _currentSession.value?.id ?: return@launch
            if (glassGroup != null && glassNumber != null) {
                val takenInDb = sessionRepository.isGlassTaken(sessionId, glassGroup, glassNumber)
                // A different player already has this glass queued.
                val takenInQueue = _queue.value.any {
                    it.glassGroup == glassGroup && it.glassNumber == glassNumber
                        && it.playerId != playerId
                }
                if (takenInDb || takenInQueue) {
                    _uiEvents.tryEmit(UiEvent.GlassAlreadyTaken)
                    return@launch
                }
            }
            val order = QueuedOrder(playerId, drinkId, glassGroup, glassNumber)
            _queue.update { queue ->
                val idx = queue.indexOfFirst { it.playerId == playerId }
                if (idx >= 0) queue.toMutableList().also { it[idx] = order } else queue + order
            }
        }
    }

    fun cancelQueuedOrder(playerId: Long) {
        _queue.update { it.filter { o -> o.playerId != playerId } }
    }

    fun commitQueue() {
        viewModelScope.launch {
            val sessionId = _currentSession.value?.id ?: return@launch
            val orders = _queue.value.takeIf { it.isNotEmpty() } ?: return@launch
            sessionRepository.commitOrders(sessionId, orders)
            _queue.value = emptyList()
        }
    }

    fun commitOrderNow(playerId: Long, drinkId: Long, glassGroup: Char?, glassNumber: Int?) {
        viewModelScope.launch {
            val sessionId = _currentSession.value?.id ?: return@launch
            if (glassGroup != null && glassNumber != null) {
                if (sessionRepository.isGlassTaken(sessionId, glassGroup, glassNumber)) {
                    _uiEvents.tryEmit(UiEvent.GlassAlreadyTaken)
                    return@launch
                }
            }
            sessionRepository.commitOrders(
                sessionId,
                listOf(QueuedOrder(playerId, drinkId, glassGroup, glassNumber)),
            )
        }
    }

    fun startTimeout(playerId: Long, durationSeconds: Int) {
        viewModelScope.launch {
            val sessionId = _currentSession.value?.id ?: return@launch
            sessionRepository.startTimeout(sessionId, playerId, durationSeconds)
        }
    }

    fun returnGlass(glassGroup: Char, glassNumber: Int) {
        viewModelScope.launch {
            val sessionId = _currentSession.value?.id ?: return@launch
            sessionRepository.returnGlass(sessionId, glassGroup, glassNumber)
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(
        private val sessionRepository: SessionRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            MainViewModel(sessionRepository, settingsRepository) as T
    }
}
