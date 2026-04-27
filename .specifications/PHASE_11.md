# Phase 11 — Player Detail Screen

## Goal

Replace the `PlayerDetailScreen` stub with a fully functional stats and event-history screen for
a single player.

---

## Screen Specification

### Entry point

Navigated to from `AppNavHost` when the user taps a `PlayerCard` in the Order tab:

```kotlin
entry<PlayerDetail> { key ->
    PlayerDetailScreen(
        playerId = key.playerId,
        onBack   = dropUnlessResumed { backStack.removeLastOrNull() },
    )
}
```

### Layout (top to bottom)

```
┌──────────────────────────────────────────────────┐
│ TopAppBar — "← [player name]"                    │
├──────────────────────────────────────────────────┤
│ Header                                           │
│   ‣ Name (headlineLarge typography)              │
│   ‣ Phone  (body, hidden if empty)               │
│   ‣ Notes  (body, wrapping, hidden if empty)     │
├──────────────────────────────────────────────────┤
│ Stats row (e.g. card or surface section)         │
│   ‣ Active drinks: N   (red if ≥ threshold)      │
│   ‣ Timeout: MM:SS left   or   —                 │
│   ‣ Glasses out: N                               │
├──────────────────────────────────────────────────┤
│ Totals row                                       │
│   ‣ Total drinks: N                              │
│   ‣ Total timeout: Xh Ym                         │
├──────────────────────────────────────────────────┤
│ ▸ Drinks  (expandable, collapsed by default)     │
│   Time     │ Drink         │ Glass               │
│   14:32:01 │ Beer          │ B10                 │
│   15:04:17 │ Whiskey       │ —                   │
├──────────────────────────────────────────────────┤
│ ▸ Timeouts  (expandable, collapsed by default)   │
│   Time     │ Duration                            │
│   14:45:00 │ 5h 0m                               │
│   15:30:00 │ Cancelled                           │
└──────────────────────────────────────────────────┘
```

**Null / not-found state:** If the player no longer exists (session replaced, player deleted)
while the screen is open, the body shows a centered `"Player no longer available."` message.

---

## New Files

### `viewmodel/OrderHistoryItem.kt`

```kotlin
data class OrderHistoryItem(
    val timestampMs: Long,
    val drinkName: String,       // "(deleted drink)" if drink was removed
    val glassGroup: Char?,
    val glassNumber: Int?,
)
```

### `viewmodel/TimeoutHistoryItem.kt`

```kotlin
data class TimeoutHistoryItem(
    val timestampMs: Long,
    val durationSeconds: Int,    // 0 = cancel event
)
```

### `viewmodel/PlayerDetailViewModel.kt`

```kotlin
class PlayerDetailViewModel(
    private val playerId: Long,
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    // 1-second ticker
    private val tickerFlow: Flow<Long> = flow {
        while (true) { emit(System.currentTimeMillis()); delay(1_000) }
    }

    // Current session (shared root)
    private val _currentSession: StateFlow<Session?> = ...

    // Derived state filtered to this player
    val derivedState: StateFlow<PlayerDerivedState?> = ...

    // Ticker-driven countdown; source of truth for isUnderTimeout in the UI
    val timeoutMillisRemaining: StateFlow<Long?> = ...

    val activeDrinkHighlight: StateFlow<Int> = ...

    // Order events for this player joined with drink names
    val orderHistory: StateFlow<List<OrderHistoryItem>> = ...

    // Timeout events for this player (all durations, including 0-cancel)
    val timeoutHistory: StateFlow<List<TimeoutHistoryItem>> = ...

    class Factory(
        private val sessionRepository: SessionRepository,
        private val settingsRepository: SettingsRepository,
        private val playerId: Long,
    ) : ViewModelProvider.Factory { ... }
}
```

**Scoping:** Use the default (nav-entry-scoped) `viewModel(factory = ...)` call inside
`PlayerDetailScreen`. Do **not** pass `viewModelStoreOwner = activity`, as that would reuse the
same VM instance for different players.

### `ui/player/ExpandableEventTable.kt`

Generic collapsible table composable:

```kotlin
@Composable
fun ExpandableEventTable(
    title: String,
    columns: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
)
```

Behaviour:
- Starts collapsed (`expanded = false` via `rememberSaveable`).
- Header row: `title` on the left, trailing chevron (`KeyboardArrowDown` / `KeyboardArrowUp`).
  Entire header row is clickable to toggle.
- When expanded:
  - Column names row in `MaterialTheme.colorScheme.onSurfaceVariant` style.
  - Each data row as a horizontal `Row` with evenly weighted cells.
  - "No history." placeholder (centered) when `rows` is empty.

### `ui/player/PlayerDetailScreen.kt` (replace stub)

```kotlin
@Composable
fun PlayerDetailScreen(
    playerId: Long,
    onBack: () -> Unit,
)
```

- Obtains `PlayerDetailViewModel` via `viewModel(factory = app.playerDetailViewModelFactory(playerId))`.
- Layout: `Scaffold` with `TopAppBar` (back `IconButton` + player name as title) and a
  `Column + verticalScroll` body — **never** a `LazyColumn`, to avoid nested scroll conflicts.

---

## Modified Files

### `data/db/dao/EventDao.kt`

Fix `getByPlayer` query for stable deterministic ordering (consistent with the rest of the
codebase which uses `(timestampMs, id)` as canonical order):

```kotlin
// Before:
ORDER BY timestampMs ASC
// After:
ORDER BY timestampMs ASC, id ASC
```

### `data/repository/SessionRepository.kt`

Add:

```kotlin
fun observePlayerHistory(sessionId: Long, playerId: Long): Flow<List<Event>> =
    eventDao.getByPlayer(sessionId, playerId)
        .map { entities -> entities.map { it.toDomain() } }
```

### `util/TimeUtils.kt`

Add:

```kotlin
fun formatTimestamp(timestampMs: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestampMs))
```

### `DrinkWatchApplication.kt`

Add per-player factory method (not a lazy singleton — each call returns a new factory for the
given `playerId`):

```kotlin
fun playerDetailViewModelFactory(playerId: Long): PlayerDetailViewModel.Factory =
    PlayerDetailViewModel.Factory(sessionRepository, settingsRepository, playerId)
```

---

## Key Behavioural Details

| Concern | Decision |
|---|---|
| `isUnderTimeout` in UI | Derived from `timeoutMillisRemaining != null` (ticker-driven), never from `derivedState.isUnderTimeout` |
| Deleted drink in history | Shown as `"(deleted drink)"` — no crash, no omission |
| Cancel-timeout (`durationSeconds == 0`) | Shown in Timeouts history with Duration = `"Cancelled"` |
| Player not found | `"Player no longer available."` full-screen message; no auto-back |
| Scroll container | `Column + verticalScroll` — nested `LazyColumn` not used |
| ViewModel scoping | Nav-entry-scoped (default `viewModel()`) — not activity-scoped |
| Timestamp display | `formatTimestamp()` → `"HH:mm:ss"` using `SimpleDateFormat` |

---

## Build Validation

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three must pass before Phase 11 is considered complete.
