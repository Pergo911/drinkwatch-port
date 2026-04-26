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

### ⬜ Phase 3 — Repositories & Business Logic
`SessionRepository`, `SettingsRepository`, `SessionSerializer`; unit tests for derived-state logic.

### ⬜ Phase 4 — Navigation (Navigation 3)
`AppNavHost`, `AppRoute` sealed interface, conditional session guard, wired into `MainActivity`.

### ⬜ Phase 5 — ViewModels
`MainViewModel` (with 1-second ticker), `SessionViewModel`, `SettingsViewModel`, and their
`ViewModelProvider.Factory` implementations.

### ⬜ Phase 6 — Session Screen
Overview tab (create / import / export), Players tab, Drinks tab, Glass Groups tab.

### ⬜ Phase 7 — Main Screen Shell
`Scaffold` with `AppBar` (hamburger menu) and `BottomNavBar` (Order / Glasses tabs).

### ⬜ Phase 8 — Order Tab
`PlayerCard` (normal / timeout / queued / disabled states), `QueueOverlay`, `TimeoutDialog`,
queue FAB.

### ⬜ Phase 9 — Order Full-Screen Dialog
Two-step drink → glass picker; split Queue / Confirm Now button.

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
