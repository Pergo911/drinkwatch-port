# Phase 5 — ViewModels

## Goal

Implement all three ViewModels and their `ViewModelProvider.Factory` classes so the UI layers
(Phases 6–14) have a stable, compile-verified API surface to build against. Two `SessionRepository`
helper methods (`startTimeout`, `returnGlass`) are missing from Phase 3 and are added here since
they are first consumed by the ViewModels.

---

## Files to Create

| File | Description |
|---|---|
| `viewmodel/PlayerUiState.kt` | UI-layer wrapper: `PlayerDerivedState` + queue overlay + live countdown |
| `viewmodel/TakenGlassUiState.kt` | UI-layer glass: group/number, joined player name, drink name |
| `viewmodel/MainViewModel.kt` | Order/Glasses tab logic; ticker; queue management |
| `viewmodel/SessionViewModel.kt` | Session CRUD, player/drink/glass-group management, import/export |
| `viewmodel/SettingsViewModel.kt` | Settings read/write wrapper |
| `util/TimeUtils.kt` | Countdown and duration formatting helpers |

## Files to Modify

| File | Change |
|---|---|
| `data/repository/SessionRepository.kt` | Add `startTimeout(…)` and `returnGlass(…)` |
| `DrinkWatchApplication.kt` | Expose three `ViewModelProvider.Factory` lazy properties |

---

## Task 1 — Add missing `SessionRepository` methods

```kotlin
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
```

---

## Task 2 — UI state data classes

### `viewmodel/PlayerUiState.kt`

```kotlin
package com.example.drinkwatch.viewmodel

import com.example.drinkwatch.data.model.PlayerDerivedState
import com.example.drinkwatch.data.model.QueuedOrder

data class PlayerUiState(
    val derived: PlayerDerivedState,
    val queuedOrder: QueuedOrder?,
    /** Milliseconds until the timeout expires; null if not under timeout. Ticker-driven. */
    val timeoutMillisRemaining: Long?,
) {
    /** True while timeoutMillisRemaining is non-null and > 0. Driven by the ticker, NOT by
     *  derived.isUnderTimeout (which only refreshes on DB events). */
    val isUnderTimeout: Boolean get() = timeoutMillisRemaining != null
}
```

### `viewmodel/TakenGlassUiState.kt`

```kotlin
package com.example.drinkwatch.viewmodel

data class TakenGlassUiState(
    val glassGroup: Char,
    val glassNumber: Int,
    val playerName: String,
    val drinkName: String,
    val takenAtMs: Long,
    val playerId: Long,     // retained for return-glass action
)
```

---

## Task 3 — `util/TimeUtils.kt`

```kotlin
package com.example.drinkwatch.util

import kotlin.math.max

/** Formats a millisecond countdown as "MM:SS", clamped to "00:00". */
fun formatCountdown(millisRemaining: Long): String {
    val totalSeconds = max(0L, millisRemaining / 1000L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

/** Formats a total-seconds value as "Xh Ym" (e.g., "1h 23m" or "0h 5m"). */
fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return "${hours}h ${minutes}m"
}
```

---

## Task 4 — `viewmodel/MainViewModel.kt`

**Constructor:** `(sessionRepository: SessionRepository, settingsRepository: SettingsRepository)`

### Internal design

```kotlin
// 1-second ticker — emits System.currentTimeMillis() so timeout countdowns update live
private val tickerFlow: Flow<Long> = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(1_000L)
    }
}

// Source of truth for the current session; used by all flatMapLatest chains and actions.
private val _currentSession: StateFlow<Session?> =
    sessionRepository.observeCurrentSession()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

private val _queue = MutableStateFlow<List<QueuedOrder>>(emptyList())
```

### Session-change queue guard (`init` block)

```kotlin
// Clear the in-memory queue whenever the active session changes, to prevent stale
// playerId/drinkId references from leaking into the new session.
viewModelScope.launch {
    var prevId: Long? = null
    _currentSession.collect { session ->
        if (prevId != null && prevId != session?.id) {
            _queue.value = emptyList()
        }
        prevId = session?.id
    }
}
```

### Exposed StateFlows (all `SharingStarted.WhileSubscribed(5_000)`)

| Property | Type |
|---|---|
| `queue` | `StateFlow<List<QueuedOrder>>` |
| `playerUiStates` | `StateFlow<List<PlayerUiState>>` |
| `takenGlasses` | `StateFlow<List<TakenGlassUiState>>` |
| `activeDrinkHighlight` | `StateFlow<Int>` |

#### `playerUiStates`

```kotlin
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
```

`PlayerUiState.isUnderTimeout` is computed from `timeoutMillisRemaining != null`, so the ticker
governs timeout expiry in the UI — not a potentially stale `derived.isUnderTimeout` that only
refreshes on DB events.

#### `takenGlasses`

```kotlin
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
```

#### `activeDrinkHighlight`

```kotlin
val activeDrinkHighlight: StateFlow<Int> =
    settingsRepository.settings
        .map { it.activeDrinkHighlight }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 3)
```

### UI-event channel

```kotlin
sealed interface UiEvent {
    data object GlassAlreadyTaken : UiEvent
}

private val _uiEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()
```

### Actions

All actions read session ID from `_currentSession.value?.id` (no `.first()` calls).

#### `addToQueue`

```kotlin
fun addToQueue(playerId: Long, drinkId: Long, glassGroup: Char?, glassNumber: Int?) {
    viewModelScope.launch {
        val sessionId = _currentSession.value?.id ?: return@launch
        // Check glass uniqueness against DB state
        if (glassGroup != null && glassNumber != null) {
            val takenInDb = sessionRepository.isGlassTaken(sessionId, glassGroup, glassNumber)
            val takenInQueue = _queue.value.any { it.glassGroup == glassGroup && it.glassNumber == glassNumber && it.playerId != playerId }
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
```

#### Other actions

```kotlin
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
```

### Factory

```kotlin
class Factory(
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        MainViewModel(sessionRepository, settingsRepository) as T
}
```

---

## Task 5 — `viewmodel/SessionViewModel.kt`

**Constructor:** `(sessionRepository: SessionRepository, sessionSerializer: SessionSerializer)`

### Exposed StateFlows (all `SharingStarted.WhileSubscribed(5_000)`)

| Property | Type |
|---|---|
| `currentSession` | `StateFlow<Session?>` |
| `players` | `StateFlow<List<Player>>` |
| `drinks` | `StateFlow<List<Drink>>` |
| `glassGroups` | `StateFlow<List<GlassGroup>>` |
| `uiError` | `StateFlow<String?>` |

`players`, `drinks`, and `glassGroups` each use `flatMapLatest` on `currentSession` so they
automatically switch to the new session's data after `createSession` or `importSession`.

### Actions

| Action | Notes |
|---|---|
| `createSession(name)` | `repo.createSession(name)`; existing data is cleared by the repository |
| `updateSessionName(name)` | Requires active session; no-op otherwise |
| `addPlayer(name, phone, note)` | Requires active session |
| `updatePlayer(player)` | |
| `deletePlayer(player)` | |
| `disablePlayer(player)` | `repo.disablePlayer(sessionId, player.id)` |
| `addDrink(name, type)` | Requires active session |
| `updateDrink(drink)` | |
| `deleteDrink(drink)` | |
| `disableDrink(drink)` | `repo.disableDrink(sessionId, drink.id)` |
| `toggleGlassGroup(letter)` | Adds if absent, removes if present |
| `exportSession(outputStream)` | `withContext(Dispatchers.IO) { serializer.export(sessionId, outputStream) }`; sets `uiError` on failure |
| `importSession(inputStream)` | `withContext(Dispatchers.IO) { serializer.import(inputStream) }`; sets `uiError` on failure |
| `clearError()` | `_uiError.value = null` |

### Factory

Same nested `ViewModelProvider.Factory` pattern as `MainViewModel`.

---

## Task 6 — `viewmodel/SettingsViewModel.kt`

**Constructor:** `(settingsRepository: SettingsRepository)`

**State:**
- `settings: StateFlow<AppSettings>` — `SharingStarted.WhileSubscribed(5_000)`

**Actions:** `setTheme(Theme)`, `setActiveDrinkHighlight(Int)`, `setDefaultTimeoutSeconds(Int)` —
each launches a `viewModelScope` coroutine.

**Factory:** same pattern.

---

## Task 7 — Wire factories in `DrinkWatchApplication`

```kotlin
val mainViewModelFactory: MainViewModel.Factory by lazy {
    MainViewModel.Factory(sessionRepository, settingsRepository)
}
val sessionViewModelFactory: SessionViewModel.Factory by lazy {
    SessionViewModel.Factory(sessionRepository, sessionSerializer)
}
val settingsViewModelFactory: SettingsViewModel.Factory by lazy {
    SettingsViewModel.Factory(settingsRepository)
}
```

---

## Task 8 — Verify

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three must pass with no errors and no new lint warnings.
