# DrinkWatch — Development Plan

## Overview

Build the DrinkWatch party management app on top of the existing bare-bones Compose scaffold. The
app manages sessions (parties), players, drinks, glasses, and events such as orders, timeouts, and
glass returns.

---

## Technology Stack

| Concern                          | Library                                                          |
| -------------------------------- | ---------------------------------------------------------------- |
| UI                               | Jetpack Compose + Material3 (already present)                    |
| Navigation                       | Jetpack Navigation 3 (via `navigation-3` skill)                  |
| Local persistence (session data) | Room (via KSP)                                                   |
| Local persistence (settings)     | DataStore Preferences                                            |
| JSON import/export               | kotlinx.serialization                                            |
| Async / reactive                 | Kotlin Coroutines + StateFlow/Flow                               |
| DI (optional, keep simple)       | Manual constructor injection via `Application`-scoped singletons |
| Build                            | AGP 9.2.0 / Kotlin 2.3.21 / compileSdk 37 / minSdk 24 (existing) |

No Hilt. Keep DI simple: singleton repositories constructed in a custom `Application` class and
passed into ViewModels via `ViewModelProvider.Factory`.

---

## Package Structure

```
com.example.drinkwatch/
  DrinkWatchApplication.kt        ← Application subclass; constructs singletons
  data/
    db/
      AppDatabase.kt
      entity/
        SessionEntity.kt
        PlayerEntity.kt
        DrinkEntity.kt
        GlassGroupEntity.kt
        EventEntity.kt            ← single flat table; type discriminator field
      dao/
        SessionDao.kt
        PlayerDao.kt
        DrinkDao.kt
        GlassGroupDao.kt
        EventDao.kt
    model/                        ← pure domain data classes (no Room annotations)
      Session.kt
      Player.kt
      Drink.kt                    ← includes DrinkType enum
      GlassGroup.kt
      Event.kt                    ← sealed class hierarchy
      DerivedState.kt             ← PlayerState, TakenGlass, etc.
    repository/
      SessionRepository.kt
      SettingsRepository.kt
    serialization/
      SessionSerializer.kt        ← JSON import/export (kotlinx.serialization)
      SessionSnapshot.kt          ← @Serializable transfer objects
  ui/
    theme/                        ← existing Color.kt, Type.kt, Theme.kt
    navigation/
      AppNavHost.kt               ← Nav3 NavHost + route definitions
      Routes.kt                   ← sealed class / data object route types
    main/                         ← Main screen shell (Scaffold, bottom nav)
      MainScreen.kt
      AppBar.kt
      BottomNavBar.kt
    order/                        ← Tab 1
      OrderTab.kt
      PlayerCard.kt
      QueueOverlay.kt             ← overlaid "<drink> → <glass>" + ×
    glasses/                      ← Tab 2
      GlassesTab.kt
      GlassCard.kt
    session/                      ← Session management screen
      SessionScreen.kt
      overview/
        OverviewTab.kt
      players/
        PlayersTab.kt
        PlayerDialog.kt           ← add / edit full-screen dialog
      drinks/
        DrinksTab.kt
        DrinkDialog.kt
      glassgroups/
        GlassGroupsTab.kt
    player/                       ← Player detail screen
      PlayerDetailScreen.kt
      ExpandableEventTable.kt
    dialog/
      OrderDialog.kt              ← 2-step full-screen dialog
      TimeoutDialog.kt
      ReturnGlassDialog.kt
    settings/
      SettingsScreen.kt
    about/
      AboutScreen.kt
  viewmodel/
    MainViewModel.kt
    SessionViewModel.kt
    SettingsViewModel.kt
  util/
    TimeUtils.kt                  ← countdown formatting, duration helpers
```

---

## Data Architecture

### 3.1 Domain Models

```kotlin
// model/Session.kt
data class Session(val id: Long, val name: String)

// model/Drink.kt
enum class DrinkType { SHOT, LONG_DRINK, NON_ALCOHOLIC }
data class Drink(val id: Long, val sessionId: Long, val name: String,
                 val type: DrinkType, val isDisabled: Boolean)

// model/Player.kt
data class Player(val id: Long, val sessionId: Long, val name: String,
                  val phone: String, val note: String, val isDisabled: Boolean)

// model/GlassGroup.kt
data class GlassGroup(val id: Long, val sessionId: Long, val letter: Char)

// model/Event.kt
sealed class Event {
    abstract val id: Long
    abstract val sessionId: Long
    abstract val timestampMs: Long

    data class Order(
        override val id: Long, override val sessionId: Long, override val timestampMs: Long,
        val playerId: Long, val drinkId: Long,
        val glassGroup: Char?, val glassNumber: Int?
    ) : Event()

    data class Return(
        override val id: Long, override val sessionId: Long, override val timestampMs: Long,
        val glassGroup: Char, val glassNumber: Int
    ) : Event()

    data class Timeout(
        override val id: Long, override val sessionId: Long, override val timestampMs: Long,
        val playerId: Long,
        val durationSeconds: Int   // 0 = cancel current timeout
    ) : Event()

    data class DisablePlayer(
        override val id: Long, override val sessionId: Long, override val timestampMs: Long,
        val playerId: Long
    ) : Event()

    data class DisableDrink(
        override val id: Long, override val sessionId: Long, override val timestampMs: Long,
        val drinkId: Long
    ) : Event()
}
```

### 3.2 Room Database Schema

`EventEntity` uses a single flat table with a `type` string discriminator and nullable columns. All
events share `id`, `sessionId`, `timestampMs`, `type`. Unused columns per type are `null`.

| Column                           | Used by                       |
| -------------------------------- | ----------------------------- |
| `playerId`                       | Order, Timeout, DisablePlayer |
| `drinkId`                        | Order, DisableDrink           |
| `glassGroup` (TEXT, single char) | Order (optional), Return      |
| `glassNumber` (INT)              | Order (optional), Return      |
| `durationSeconds`                | Timeout                       |

`SessionEntity`, `PlayerEntity`, `DrinkEntity`, `GlassGroupEntity` are straightforward flat tables.
`PlayerEntity` and `DrinkEntity` carry an `isDisabled` boolean (set via `DisablePlayer` /
`DisableDrink` event handlers so we don't have to recompute from events on every read).

### 3.3 Derived State Logic

All derived values are computed from the event stream (or from maintained columns) and exposed as
`Flow`s from the repository. The key computations:

| Derived value                     | Logic                                                                                                                                    |
| --------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| **Player disabled**               | `PlayerEntity.isDisabled` column (updated on DisablePlayer event)                                                                        |
| **Drink disabled**                | `DrinkEntity.isDisabled` column (updated on DisableDrink event)                                                                          |
| **Under timeout**                 | Latest `Timeout` event for player: if `durationSeconds > 0` AND `(timestampMs + durationSeconds*1000) > nowMs` → under timeout           |
| **Timeout time left**             | `(timestampMs + durationSeconds*1000) - nowMs`                                                                                           |
| **Active drink count**            | Count of `Order` events for player with `DrinkType != NON_ALCOHOLIC` since the latest `Timeout` event (any duration, including 0-cancel) |
| **Taken glasses**                 | `Order` events with a glass set, minus matching `Return` events — uniqueness by `(glassGroup, glassNumber)`                              |
| **Total drinks**                  | Count of all `Order` events for player                                                                                                   |
| **Total timeout time**            | Sum of `durationSeconds` for all non-zero `Timeout` events for player                                                                    |
| **Unreturned glasses for player** | Taken glasses filtered to that player                                                                                                    |

Since `isDisabled` flags are maintained on-write, no full event scan is needed for those. Timeout
and drink-count state are computed via SQL queries or in-memory reduction of event streams.

### 3.4 Queue (in-memory)

The queue lives entirely in `MainViewModel` as `StateFlow<List<QueuedOrder>>`:

```kotlin
data class QueuedOrder(
    val playerId: Long,
    val drinkId: Long,
    val glassGroup: Char?,
    val glassNumber: Int?
)
```

There is at most **one queued order per player** (adding a second order for the same player while
one is pending replaces it — or the UI can prevent it; clarify in implementation). When the FAB is
tapped, all queued orders are committed as `Event.Order` entries in a single transaction with the
current timestamp.

---

## Navigation Architecture (Navigation 3)

The `navigation-3` skill will be invoked to install the library and scaffold the nav host.

### Routes

```kotlin
// Routes.kt
sealed interface AppRoute {
    data object Main : AppRoute           // hosts bottom nav
    data object Session : AppRoute
    data object Settings : AppRoute
    data object About : AppRoute
    data class PlayerDetail(val playerId: Long) : AppRoute
    data class OrderDialog(val playerId: Long) : AppRoute
}
```

### Screen hierarchy

```
AppNavHost (Nav3 NavHost)
├── Main  ← default; contains bottom nav
│   ├── OrderTab
│   └── GlassesTab
├── Session
│   ├── OverviewTab
│   ├── PlayersTab
│   ├── DrinksTab
│   └── GlassGroupsTab
├── Settings
├── About
├── PlayerDetail(playerId)
└── OrderDialog(playerId)     ← full-screen dialog destination
```

### Conditional navigation

On `AppNavHost` composition, check `sessionRepository.hasActiveSession()`:

- `false` → immediately navigate to `Session`, replacing `Main` on the back stack so back-press
  does not return to an empty main screen.

The hamburger menu in the top bar pushes `Session`, `Settings`, or `About` onto the back stack.

---

## ViewModel Design

### MainViewModel

- **Inputs:** player list, taken glasses, queue, settings (active drink threshold)
- **Outputs:**
  - `playerStates: StateFlow<List<PlayerUiState>>` — each entry includes `Player`, derived active
    drink count, timeout state/time-left, queue overlay data, `isDisabled`
  - `takenGlasses: StateFlow<List<TakenGlassUiState>>`
  - `queue: StateFlow<List<QueuedOrder>>`
- **Actions:** `addToQueue(playerId, drinkId, glassGroup, glassNumber?)`,
  `cancelQueuedOrder(playerId)`, `commitQueue()`,
  `commitOrderNow(playerId, drinkId, glassGroup, glassNumber?)`,
  `startTimeout(playerId, durationSeconds)`, `returnGlass(glassGroup, glassNumber)`
- **Timer:** a 1-second `ticker` coroutine (`tickerFlow`) that invalidates timeout countdown values
  every second.

### SessionViewModel

- **Outputs:** `session: StateFlow<Session?>`, player/drink/glassGroup lists
- **Actions:** `createSession(name)`, `updateSessionName(name)`,
  `addPlayer(…)`, `editPlayer(…)`, `deletePlayer(playerId)`,
  `addDrink(…)`, `editDrink(…)`, `deleteDrink(drinkId)`,
  `toggleGlassGroup(letter)`,
  `exportSession(): Uri`, `importSession(uri)`, `newSession()`

### SettingsViewModel

- **Outputs:** `settings: StateFlow<AppSettings>`
- **Actions:** `setTheme(Theme)`, `setActivedrinkHighlight(Int)`,
  `setDefaultTimeoutSeconds(Int)`

---

## Development Phases

---

### Phase 1 — Dependencies & Project Setup

**Goal:** Wire up all required libraries so the project compiles.

**Tasks:**

1. Add to `gradle/libs.versions.toml`:
   - `room` (latest stable)
   - `ksp` plugin
   - `datastore`
   - `kotlinx-serialization-json`
   - `kotlinx-serialization` Kotlin plugin
   - Navigation 3 libraries (handled by the `navigation-3` skill)
2. Add `kotlin-serialization` and `ksp` plugins to `app/build.gradle.kts`.
3. Add all new `implementation`/`ksp` dependencies in `app/build.gradle.kts`.
4. Create `DrinkWatchApplication.kt` and register it in `AndroidManifest.xml`.
5. Run `.\gradlew.bat assembleDebug` to verify no compilation errors.

---

### Phase 2 — Data Layer: Models & Room Database

**Goal:** Define the full data model and compile-verified Room schema.

**Tasks:**

1. Create all domain data classes under `data/model/`.
2. Create Room entities under `data/db/entity/` (annotated with `@Entity`).
3. Create DAOs under `data/db/dao/`:
   - `SessionDao`: `insert`, `update`, `getActive`, `deleteAll`
   - `PlayerDao`: `insert`, `update`, `delete`, `getAllBySession`, `setDisabled`
   - `DrinkDao`: `insert`, `update`, `delete`, `getAllBySession`, `setDisabled`
   - `GlassGroupDao`: `insert`, `delete`, `getAllBySession`
   - `EventDao`: `insert`, `getBySession`, `getByPlayer`, `getLatestTimeoutForPlayer`,
     `getOrdersSinceLastTimeout`, `getTakenGlasses`, `isGlassTaken`
4. Create `AppDatabase` with all entities and DAOs; add a `TypeConverter` for `Char`/`DrinkType`.
5. Run `.\gradlew.bat assembleDebug` — Room annotation processing must succeed.

---

### Phase 3 — Repositories & Business Logic

**Goal:** Expose reactive data and mutations to the ViewModel layer.

**Tasks:**

1. **`SessionRepository`:**
   - CRUD wrapping all DAOs.
   - `fun observePlayerStates(sessionId): Flow<List<PlayerDerivedState>>` — joins Player,
     latest Timeout events, and drink counts into a single flow.
   - `fun observeTakenGlasses(sessionId): Flow<List<TakenGlass>>`
   - `fun isGlassTaken(group, number): Boolean` — glass uniqueness check.
   - `suspend fun commitOrders(orders: List<QueuedOrder>)` — single transaction.
2. **`SettingsRepository`:**
   - DataStore-backed `AppSettings` data class.
   - Read/write flows for `theme`, `activeDrinkHighlight`, `defaultTimeoutSeconds`.
3. **`SessionSerializer`:**
   - `@Serializable` snapshot data classes (`SessionSnapshot`).
   - `suspend fun export(sessionId, outputStream)`
   - `suspend fun import(inputStream): SessionSnapshot` then repository call to replace session.
4. Write unit tests for derived-state logic (Room in-memory database).
5. Run `.\gradlew.bat testDebugUnitTest`.

---

### Phase 4 — Navigation (Navigation 3 Skill)

**Goal:** Full navigation graph with all routes and conditional session guard.

**Tasks:**

1. **Invoke `navigation-3` skill** to install Nav3 and scaffold `AppNavHost.kt`.
2. Define `AppRoute` sealed interface in `navigation/Routes.kt`.
3. Implement `AppNavHost`:
   - Bottom nav multi-backstack for `Main` (Order + Glasses tabs).
   - Full-screen destinations: `Session`, `Settings`, `About`, `PlayerDetail`, `OrderDialog`.
   - Conditional start: if no session loaded, start at `Session`; otherwise `Main`.
4. Wire `MainActivity` to host `AppNavHost`.
5. Run `.\gradlew.bat assembleDebug` to verify navigation compiles.

---

### Phase 5 — ViewModels

**Goal:** All business logic wired; UI can be built against stable ViewModel contracts.

**Tasks:**

1. Implement `MainViewModel` with:
   - `playerStates` flow (includes 1-second countdown ticker).
   - `takenGlasses` flow.
   - `queue` state + all queue/order/timeout/return actions.
   - Glass uniqueness enforcement before `addToQueue`.
2. Implement `SessionViewModel` with all CRUD and import/export actions.
3. Implement `SettingsViewModel`.
4. Create `ViewModelFactory` subclasses wired through `DrinkWatchApplication`.
5. Run `.\gradlew.bat testDebugUnitTest`.

---

### Phase 6 — Session Screen

**Goal:** Fully functional session management.

**Tabs:**

**Overview tab:**

- _No session loaded:_ Centered column with two buttons — "Import From File" and "Create New".
  "Import From File" opens the system file picker (SAF `ACTION_OPEN_DOCUMENT`, MIME `application/json`).
  "Create New" opens a simple name-input dialog.
- _Session loaded:_ Editable session name field (auto-saved on focus-loss), "Export to File" button
  (SAF `ACTION_CREATE_DOCUMENT`), "Import From File" button (with overwrite confirmation),
  "Start New Session" button (with confirmation).

**Players tab:**

- `LazyColumn` of player cards (name, phone, note excerpt).
- FAB: opens `PlayerDialog` in "add" mode.
- Long-press (or edit icon): opens `PlayerDialog` in "edit" mode.
- `PlayerDialog` (full-screen): name, phone, note fields; "Delete" button in edit mode with
  confirmation.

**Drinks tab:**

- Same structure as Players tab.
- `DrinkDialog` (full-screen): name field, `DrinkType` segmented button (Shot / Long Drink /
  Non-Alcoholic).

**Glass Groups tab:**

- Scrollable grid (or list) of letters A–Z, each with a toggle/checkbox.
- Toggling a group on/off inserts or removes a `GlassGroup` record.

---

### Phase 7 — Main Screen Shell

**Goal:** App bar, bottom navigation, and tab scaffold.

**Tasks:**

1. `MainScreen.kt`: `Scaffold` with top bar and bottom nav.
2. `AppBar.kt`: title = session name, hamburger `IconButton` that opens a `DropdownMenu` with
   "Session", "Settings", "About" items.
3. `BottomNavBar.kt`: two items — "Order" and "Glasses", each with an icon and label.
4. Handle bottom nav state restoration (Nav3 multiple-backstack).

---

### Phase 8 — Order Tab

**Goal:** Interactive player list with queue overlays.

**`PlayerCard` states:**

| State         | Display                                                                                                |
| ------------- | ------------------------------------------------------------------------------------------------------ |
| Normal        | Name + active drink count (highlighted red if ≥ threshold); "Add Drink" and "Timeout" buttons          |
| Under timeout | Name + countdown timer ("MM:SS left"); "Add Drink" disabled; "Timeout" enabled (to cancel or override) |
| Queued order  | Card body overlaid with `QueueOverlay`; action buttons replaced by the overlay                         |
| Disabled      | Card greyed out; no action buttons                                                                     |

**`QueueOverlay`:**

- Shows `"<DrinkName> → <GlassGroup><GlassNumber>"` (or `"<DrinkName>"` if no glass).
- `×` (cancel) `IconButton` in the top-right corner.
- Tapping the overlay (outside ×) does nothing.
- Tapping "Add Drink" while a queue overlay is active shows a Snackbar: "Cancel or send the queue
  first." The Order dialog does **not** open.

**Timeout dialog (`TimeoutDialog`):**

- Time picker UI: scrollable hours/minutes pickers (or two text fields) with a "00:00" format.
- Pre-populated with the session's default timeout duration.
- 0:00 is allowed (cancels current timeout).
- Buttons: "Cancel" and "Set Timeout".

**Queue FAB:**

- Visible only when `queue.isNotEmpty()`.
- Label: "Send (N)" where N is queue size.
- Tapping calls `MainViewModel.commitQueue()`.

---

### Phase 9 — Order Full-Screen Dialog

**Goal:** Two-step drink-then-glass selection with split commit button.

**Step 1 — Drink picker:**

- `LazyColumn` grouped by `DrinkType` (type header rows + drink item rows).
- Disabled drinks are shown greyed out and non-selectable.
- Tapping a drink selects it (highlighted).
- "Next" `Button` (enabled only when a drink is selected).

**Step 2 — Glass picker:**

- Segmented button row or `LazyRow` of available glass group letters (those toggled on in session).
  Only enabled group letters appear.
- Number input: `OutlinedTextField` (numeric keyboard), validated as a positive integer.
- "None" chip/button to clear any glass selection.
- Glass uniqueness: if the entered `(group, number)` is already taken, show inline error and
  disable confirm.
- **Split confirm button** (Material3 `SplitButton` or custom implementation):
  - Primary segment: **"Queue"** — calls `MainViewModel.addToQueue(…)` then closes dialog.
  - Secondary segment (dropdown arrow): **"Confirm Now"** — calls
    `MainViewModel.commitOrderNow(…)` then closes dialog.

---

### Phase 10 — Glasses Tab

**Goal:** View and return currently-held glasses.

**Tasks:**

1. `GlassesTab.kt`: `LazyColumn` of `GlassCard` composables from `takenGlasses` flow.
2. `GlassCard`: shows `"${group}${number}"`, drink name, player name.
3. Tapping a card opens `ReturnGlassDialog` — simple confirmation: "Return glass B10 taken by
   Alice?" with "Return" and "Cancel".
4. On confirm: `MainViewModel.returnGlass(group, number)`.

---

### Phase 11 — Player Detail Screen

**Goal:** Full stats and event history for a single player.

**Layout (top to bottom):**

1. **Header:** name (large), phone number, notes (body text, wrapping).
2. **Stats row:** "Active drinks: N" (highlighted red if ≥ threshold), "Timeout: 5:32 left" or "—",
   "Glasses out: N".
3. **Totals row:** "Total drinks: N", "Total timeout: H hr M min".
4. **Expandable: Drinks** (collapsed by default) — `LazyColumn` inside an expandable card;
   columns: timestamp, drink name, glass (if any).
5. **Expandable: Timeouts** (collapsed by default) — columns: timestamp, duration.

---

### Phase 12 — Settings Screen

**Goal:** Persistent user preferences.

**Items:**

1. **Theme:** tri-state segmented button — "Light" / "System" / "Dark". Applies immediately.
2. **Active drink highlight:** `OutlinedTextField` with numeric keyboard; label "Highlight players
   with ≥ N active drinks". Validated as positive integer.
3. **Default timeout:** Duration input (same UI as TimeoutDialog picker), pre-populated from
   settings.

---

### Phase 13 — About Screen

**Goal:** Minimal informational screen.

- App name "DrinkWatch".
- Version string from `BuildConfig.VERSION_NAME`.
- (Optional) short description.

---

### Phase 14 — Polish & Edge Cases

**Goal:** Production-ready finishing touches.

1. **Empty states:** Friendly placeholder text + icon for all empty lists (no players, no drinks, no
   glasses taken, no history).
2. **No-session guard:** Ensure back-navigation from `Session` when no session is loaded exits the
   app (does not navigate back to empty `Main`).
3. **Disabled player/drink highlights:** Grey `alpha` applied to disabled cards throughout UI.
4. **Active drink threshold highlight:** `activeDrinkHighlight` setting propagated via
   `CompositionLocalProvider` or passed down; player name/count row uses `MaterialTheme.colorScheme.error`
   when threshold is met.
5. **Timeout countdown accuracy:** `tickerFlow` emits every 1000 ms; players whose timeout expires
   mid-session automatically transition back to normal state.
6. **File picker error handling:** Toast / snackbar for failed JSON parse on import, or empty file.
7. **Lint & tests:** `.\gradlew.bat assembleDebug lint testDebugUnitTest` must pass green.

---

## Specification Notes & Clarifications

| #   | Note                                                                                                                                                                                                                                                            |
| --- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | **Queue overlay (confirmed):** When a player has a queued order, the player card body is overlaid with `"<drink> → <glass>"` and an `×` cancel button. Action buttons are hidden during this state.                                                             |
| 2   | **Disable is permanent:** `DisablePlayer` and `DisableDrink` events are one-way — there is no re-enable mechanism.                                                                                                                                              |
| 3   | **Glass uniqueness enforced:** The same `(group, number)` combination may not be active in two orders simultaneously. The Order dialog shows an inline error if the entered glass is already taken.                                                             |
| 4   | **0-duration timeout resets drink counter:** A cancellation timeout (`durationSeconds = 0`) still counts as a Timeout event for the purposes of resetting the active drink counter. This is intentional.                                                        |
| 5   | **Single active session:** The app holds exactly one session at a time in Room. Importing replaces the current session (with confirmation). "Start New Session" clears all data and creates a fresh session.                                                    |
| 6   | **Glass group management vs. glass instances:** `GlassGroup` records define which letters are _available_ in the Order dialog. Actual glass instances (`glassGroup + glassNumber`) are not stored as entities — they are derived from unmatched `Order` events. |
| 7   | **`DrinkType` and active drinks:** Only `SHOT` and `LONG_DRINK` are considered alcoholic for the active drink counter. `NON_ALCOHOLIC` orders do not increment it.                                                                                              |
| 8   | **Split button:** The "Queue" vs "Confirm Now" split appears only on Step 2 of the Order dialog. Step 1 uses a plain "Next" button.                                                                                                                             |
| 9   | **Player note:** The note field appears only in `PlayerDialog` (edit/add) and `PlayerDetailScreen`. It is not shown on the player card in the Order tab.                                                                                                        |

---

## Resolved Questions

| #   | Question                                                                                    | Decision                                                                                     |
| --- | ------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| Q1  | When a queued order exists for a player and the user taps "Add Drink" again                 | **Block** — show a Snackbar: "Cancel or send the queue first." Do not open the Order dialog. |
| Q2  | Should the bottom nav "Glasses" tab show a taken-glass count badge?                         | **Yes.**                                                                                     |
| Q3  | Is there any limit on the number of simultaneously active players, drinks, or glass groups? | No limit.                                                                                    |
| Q4  | Must a player return their glass before ordering again?                                     | **No constraint.** Players may hold multiple glasses and place multiple orders freely.       |
