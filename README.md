# DrinkWatch

A party / drinking-session management app built with Jetpack Compose and Material 3.

## Implementation Status

### ✅ Phase 0 — Planning (complete)

Full specification and development plan are written and committed.

- [`APP_SPECIFICATION.md`](.specifications/APP_SPECIFICATION.md) — feature requirements
- [`DEVELOPMENT_PLAN.md`](.specifications/DEVELOPMENT_PLAN.md) — 14-phase implementation plan with
  architecture decisions, package structure, data model, navigation design, and ViewModel contracts

**Current scaffold** (what exists in the repo today):

| File | Description |
|---|---|
| `app/src/main/…/MainActivity.kt` | Sole entry point; renders a bare `DrinkWatchTheme {}` content host |
| `app/src/main/…/ui/theme/Color.kt` | Material 3 color tokens |
| `app/src/main/…/ui/theme/Type.kt` | Typography scale |
| `app/src/main/…/ui/theme/Theme.kt` | `DrinkWatchTheme` composable (dynamic color on API 31+) |

No features are implemented yet.

---

### ✅ Phase 1 — Dependencies & Project Setup

All required libraries added to the version catalog and build files;
`DrinkWatchApplication` stub created and registered.

| Added | Version |
|---|---|
| KSP plugin | `2.3.7` |
| Room (runtime, ktx, compiler) | `2.8.4` |
| DataStore Preferences | `1.2.1` |
| kotlinx-serialization-json | `1.11.0` |
| Navigation 3 (runtime, ui) | `1.1.1` |
| lifecycle-viewmodel-navigation3 | `2.10.0` |

New files: `DrinkWatchApplication.kt`

### ✅ Phase 2 — Data Layer: Models & Room Database

Domain data classes, Room entities with FK cascade deletes, DAOs, TypeConverters, and
`AppDatabase`. Room KSP annotation processing verified.

| New files | Description |
|---|---|
| `data/model/Session.kt` | Domain model |
| `data/model/Player.kt` | Domain model |
| `data/model/Drink.kt` | Domain model + `DrinkType` enum |
| `data/model/GlassGroup.kt` | Domain model |
| `data/model/Event.kt` | Sealed class event hierarchy |
| `data/model/DerivedState.kt` | `PlayerDerivedState`, `TakenGlass` |
| `data/db/entity/SessionEntity.kt` | Room entity + mapper |
| `data/db/entity/PlayerEntity.kt` | Room entity + mapper |
| `data/db/entity/DrinkEntity.kt` | Room entity + mapper |
| `data/db/entity/GlassGroupEntity.kt` | Room entity + mapper (unique index on `sessionId+letter`) |
| `data/db/entity/EventEntity.kt` | Flat-table Room entity + mapper + type constants |
| `data/db/dao/SessionDao.kt` | CRUD + reactive queries |
| `data/db/dao/PlayerDao.kt` | CRUD + reactive queries |
| `data/db/dao/DrinkDao.kt` | CRUD + reactive queries |
| `data/db/dao/GlassGroupDao.kt` | CRUD + reactive queries |
| `data/db/dao/EventDao.kt` | Insert, session/player queries, `getTakenGlasses`, `isGlassTaken` |
| `data/db/AppDatabase.kt` | `@Database` with `DrinkType` TypeConverter |

Updated: `DrinkWatchApplication.kt` — lazy `AppDatabase` initialization.

### ✅ Phase 3 — Repositories & Business Logic

`SessionRepository`, `SettingsRepository`, `SessionSerializer`; unit tests for derived-state logic.

| New / Modified | Description |
|---|---|
| `data/model/QueuedOrder.kt` | Queue-item data class |
| `data/model/AppSettings.kt` | Settings data class + `Theme` enum |
| `data/repository/SessionRepository.kt` | All session, player, drink, glass-group, event ops; derived-state computation; import/export |
| `data/repository/SettingsRepository.kt` | DataStore-backed settings flow |
| `data/serialization/SessionSnapshot.kt` | `@Serializable` flat transfer objects (with `version` field) |
| `data/serialization/SessionSerializer.kt` | JSON export/import (delegates import to repository) |
| `test/…/DerivedStateTest.kt` | 14 pure-Kotlin JUnit 4 tests for derived-state logic |
| `data/db/dao/SessionDao.kt` | Added `exists()` query |
| `data/db/dao/EventDao.kt` | Fixed sort order to `(timestampMs, id)` |
| `data/db/dao/GlassGroupDao.kt` | Added `getByLetter()` query |
| `DrinkWatchApplication.kt` | Wired `sessionRepository`, `settingsRepository`, `sessionSerializer` singletons |

### ✅ Phase 4 — Navigation (Navigation 3)

`AppRoute` sealed interface with 6 `@Serializable` routes, `AppNavHost` with flat back stack,
conditional session guard, `DialogSceneStrategy` for the order dialog, wired into `MainActivity`.

| New files | Description |
|---|---|
| `ui/navigation/Routes.kt` | `AppRoute : NavKey` sealed interface; `Main`, `Session`, `Settings`, `About`, `PlayerDetail(playerId)`, `OrderDialog(playerId)` |
| `ui/navigation/AppNavHost.kt` | `NavDisplay` with session guard, `DialogSceneStrategy`, `dropUnlessResumed` on nav callbacks |
| `ui/main/MainScreen.kt` | Stub: `Scaffold` + `NavigationBar` (Order / Glasses tabs) |
| `ui/session/SessionScreen.kt` | Stub placeholder |
| `ui/settings/SettingsScreen.kt` | Stub placeholder |
| `ui/about/AboutScreen.kt` | Stub placeholder |
| `ui/player/PlayerDetailScreen.kt` | Stub placeholder |
| `ui/dialog/OrderDialog.kt` | Stub `OrderDialogContent` (name avoids import conflict with route class) |

Updated: `MainActivity.kt` — replaces `Greeting` stub with `AppNavHost()`.

### ✅ Phase 5 — ViewModels

`MainViewModel`, `SessionViewModel`, `SettingsViewModel`, and their `ViewModelProvider.Factory`
implementations. UI state models and missing repository methods also added.

| New / Modified | Description |
|---|---|
| `viewmodel/PlayerUiState.kt` | `PlayerDerivedState` + queue overlay + ticker-driven `timeoutMillisRemaining` / `isUnderTimeout` |
| `viewmodel/TakenGlassUiState.kt` | Taken glass with joined player name, drink name |
| `viewmodel/MainViewModel.kt` | Queue state, 1-second ticker, `playerUiStates`/`takenGlasses`/`activeDrinkHighlight` flows, all order/timeout/return actions |
| `viewmodel/SessionViewModel.kt` | Session/player/drink/glassGroup flows, full CRUD, `exportSession`/`importSession` on `Dispatchers.IO` |
| `viewmodel/SettingsViewModel.kt` | Settings flow + setters |
| `util/TimeUtils.kt` | `formatCountdown` (MM:SS) and `formatDuration` (Xh Ym) |
| `data/repository/SessionRepository.kt` | Added `startTimeout(…)` and `returnGlass(…)` |
| `DrinkWatchApplication.kt` | Wired `mainViewModelFactory`, `sessionViewModelFactory`, `settingsViewModelFactory` |

### ✅ Phase 6 — Session Screen

Overview tab (create / import / export), Players tab, Drinks tab, Glass Groups tab.

| New / Modified | Description |
|---|---|
| `ui/session/SessionScreen.kt` | Replaces stub; 4-tab scaffold, ViewModel wiring, snackbar errors, guard auto-nav |
| `ui/session/overview/OverviewTab.kt` | No-session (Import / Create) and has-session (edit name, Export, Import, Start New) states; SAF launchers; confirmation dialogs |
| `ui/session/players/PlayersTab.kt` | Scrollable player list with add FAB |
| `ui/session/players/PlayerDialog.kt` | Full-screen add / edit dialog with Delete confirmation |
| `ui/session/drinks/DrinksTab.kt` | Scrollable drink list with add FAB |
| `ui/session/drinks/DrinkDialog.kt` | Full-screen add / edit dialog; DrinkType segmented button; Delete confirmation |
| `ui/session/glassgroups/GlassGroupsTab.kt` | A–Z `FilterChip` grid backed by `GlassGroup` records |
| `data/serialization/SessionSerializer.kt` | Streams wrapped in `use {}` so serializer owns stream lifecycle |
| `ui/navigation/Routes.kt` | `Session` changed from `data object` to `data class Session(val openedAsGuard: Boolean = false)` |
| `ui/navigation/AppNavHost.kt` | Guard pushes `Session(openedAsGuard = true)`; flag threaded to `SessionScreen` |
| `gradle/libs.versions.toml` | Added `material-icons-core` entry (BOM-managed) |
| `app/build.gradle.kts` | Added `material-icons-core` implementation dependency |

### ✅ Phase 7 — Main Screen Shell

`Scaffold` with `TopAppBar` (session name + hamburger menu → Session / Settings / About) and
`BottomNavBar` (Order / Glasses tabs with icons). Tab state keyed by session ID so it resets
automatically on session change.

| New / Modified | Description |
|---|---|
| `ui/main/AppBar.kt` | `MainTopAppBar`: session-name title, hamburger `IconButton` with `DropdownMenu` |
| `ui/main/BottomNavBar.kt` | `MainTab` enum (ORDER/GLASSES) + `MainTabSaver` + `MainBottomNavBar` |
| `ui/main/MainScreen.kt` | Wires `MainViewModel`, `MainTopAppBar`, `MainBottomNavBar`; tab stubs for Phase 8/10 |
| `viewmodel/MainViewModel.kt` | Added `sessionName: StateFlow<String>` and `sessionId: StateFlow<Long?>` |
| `gradle/libs.versions.toml` | Added `material-icons-extended` entry (BOM-managed) |
| `app/build.gradle.kts` | Added `material-icons-extended` implementation dependency |

### ✅ Phase 8 — Order Tab

`PlayerCard` (normal / timeout / queued / disabled states), `QueueOverlay`, `TimeoutDialog`,
queue FAB, and glasses badge on the bottom nav.

| New / Modified | Description |
|---|---|
| `ui/order/OrderTab.kt` | `LazyColumn` of `PlayerCard`s, queue `ExtendedFloatingActionButton`, queue-blocked snackbar |
| `ui/order/PlayerCard.kt` | `ElevatedCard` with normal / under-timeout / queued / disabled states |
| `ui/order/QueueOverlay.kt` | Replaces action-button row when an order is queued; shows drink → glass with `×` cancel |
| `ui/dialog/TimeoutDialog.kt` | `AlertDialog` duration picker (hours + minutes `OutlinedTextField`s); 0:00 allowed |
| `viewmodel/PlayerUiState.kt` | Added `queuedDrinkName: String?` field for ViewModel-resolved drink name |
| `viewmodel/MainViewModel.kt` | Added `defaultTimeoutSeconds: StateFlow<Int>`; updated `playerUiStates` to 4-flow `combine` resolving drink names |
| `ui/main/MainScreen.kt` | Added `SnackbarHostState`, `uiEvents` collection, replaced Order stub with `OrderTab`, passes badge count to nav bar |
| `ui/main/BottomNavBar.kt` | Added `takenGlassCount: Int` param; Glasses icon wrapped in `BadgedBox` |

### ✅ Phase 9 — Order Full-Screen Dialog

Two-step drink → glass picker; split Queue / Confirm Now actions.

| New / Modified | Description |
|---|---|
| `ui/dialog/OrderDialog.kt` | Replaces stub; full `Scaffold`-based two-step dialog: Step 1 `LazyColumn` drink picker grouped by `DrinkType` (disabled drinks dimmed at 0.38 alpha), Step 2 `FlowRow` glass-group chip selector + `OutlinedTextField` for glass number with inline "already taken" / "invalid" error |
| `ui/main/MainScreen.kt` | Scopes `MainViewModel` to the Activity (`viewModelStoreOwner = activity`) so the dialog shares the same in-memory queue |
| `ui/navigation/AppNavHost.kt` | Guards `onNavigateToOrderDialog` with `backStack.none { it is OrderDialog }` to prevent duplicate stacking |
| `viewmodel/MainViewModel.kt` | Fixed `commitOrderNow` to also check in-memory `_queue` for glass conflicts (was DB-only; now matches `addToQueue` dual-check) |

### ⬜ Phase 10 — Glasses Tab
`GlassCard` list; `ReturnGlassDialog`.

### ⬜ Phase 11 — Player Detail Screen
Stats header, totals row, expandable drink and timeout history tables.

### ⬜ Phase 12 — Settings Screen
Theme selector, active-drink highlight threshold, default timeout duration.

### ⬜ Phase 13 — About Screen
App name and version string from `BuildConfig.VERSION_NAME`.

### ⬜ Phase 14 — Polish & Edge Cases
Empty states, disabled-item visuals, timeout-countdown accuracy, file-picker error handling,
final lint + test pass.

---

## Technology Stack

| Concern | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Jetpack Navigation 3 |
| Local persistence (data) | Room + KSP |
| Local persistence (settings) | DataStore Preferences |
| JSON import/export | kotlinx.serialization |
| Async / reactive | Kotlin Coroutines + StateFlow/Flow |
| DI | Manual constructor injection via `Application`-scoped singletons |
| Build | AGP 9.2.0 · Kotlin 2.3.21 · compileSdk 37 · minSdk 24 |

## Building

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three must pass before a task is considered done.
