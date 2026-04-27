# DrinkWatch – Copilot Instructions

Always load skills configured in the repository before processing any requests.

## App overview

DrinkWatch is a party drink-tracking app. The full feature specification is at `@.specifications/APP_SPECIFICATION.md`. Key domain concepts: **Sessions**, **Players**, **Drinks**, **Glass Groups**, **Events** (order, return, timeout, disable-player, disable-drink), **Queue** (batched orders), and **Settings**.

## Architecture

Single-module Jetpack Compose application, package root `com.example.drinkwatch`.

### Entry point

- **`DrinkWatchApplication`** — `Application` subclass; owns all singletons (Room database, repositories, serializer, ViewModel factories). No DI framework; factories are wired manually here.
- **`MainActivity`** — sole Activity; sets up `DrinkWatchTheme` and `AppNavHost` via `setContent`.

### Data layer (`data/`)

- **`data/model/`** — pure Kotlin domain models: `Session`, `Player`, `Drink`, `GlassGroup`, `Event`, `QueuedOrder`, `AppSettings`, `DerivedState` (contains `PlayerDerivedState`, `TakenGlass`).
- **`data/db/`**
  - `AppDatabase` — Room database; entities: `SessionEntity`, `PlayerEntity`, `DrinkEntity`, `GlassGroupEntity`, `EventEntity`. `DrinkType` is persisted via a `@TypeConverter`.
  - `db/entity/` — Room `@Entity` classes (one per table). Always keep in sync with `data/model/` via `.toDomain()` / `.toEntity()` extension functions defined alongside each entity.
  - `db/dao/` — `SessionDao`, `PlayerDao`, `DrinkDao`, `GlassGroupDao`, `EventDao`. Use `Flow` returns for reactive queries.
- **`data/repository/`**
  - `SessionRepository` — single source of truth for all session data. Multi-step mutations use `db.withTransaction { }`. Derived state (`observePlayerStates`, `observeTakenGlasses`) is computed inline by `combine`-ing raw DAO flows and calling internal helpers `computePlayerDerivedState` / `computeTakenGlassKeys` (also unit-tested).
  - `SettingsRepository` — reads/writes `AppSettings` via DataStore Preferences.
- **`data/serialization/`** — `SessionSnapshot` (kotlinx.serialization `@Serializable` data class) and `SessionSerializer` for JSON import/export.

### ViewModel layer (`viewmodel/`)

| Class | Scope |
|---|---|
| `MainViewModel` | Main screen + queue management |
| `SessionViewModel` | Session manager screen |
| `PlayerDetailViewModel` | Player detail screen (per-player) |
| `SettingsViewModel` | Settings screen |

Each ViewModel exposes `StateFlow`s for UI state and has a companion `Factory` that receives repository dependencies. Factories are created in `DrinkWatchApplication` and passed through `AppNavHost`.

UI state helpers in `viewmodel/`: `PlayerUiState`, `TakenGlassUiState`, `OrderHistoryItem`, `TimeoutHistoryItem`.

### Navigation (`ui/navigation/`)

Uses **Jetpack Navigation 3** (`androidx.navigation3`).

- `Routes.kt` — sealed interface `AppRoute : NavKey` with `@Serializable` destinations: `Main`, `Session(openedAsGuard)`, `Settings`, `PlayerDetail(playerId)`, `OrderDialog(playerId)`.
- `AppNavHost.kt` — single `NavDisplay` that handles all back-stack navigation. When no session exists, navigation auto-redirects to `Session(openedAsGuard = true)`.

### UI layer (`ui/`)

```
ui/
  theme/          Color.kt, Type.kt, Theme.kt
  main/           MainScreen.kt, AppBar.kt, BottomNavBar.kt
  order/          OrderTab.kt, PlayerCard.kt, QueueOverlay.kt
  glasses/        GlassesTab.kt, GlassCard.kt
  player/         PlayerDetailScreen.kt, ExpandableEventTable.kt
  session/
    overview/     OverviewTab.kt
    players/      PlayersTab.kt, PlayerDialog.kt
    drinks/       DrinksTab.kt, DrinkDialog.kt
    glassgroups/  GlassGroupsTab.kt
  dialog/         OrderDialog.kt, ReturnGlassDialog.kt, TimeoutDialog.kt
  settings/       SettingsScreen.kt
```

- No XML layouts. UI is 100% Jetpack Compose with Material3.
- `OrderDialog` is a two-step full-screen dialog (step 1: drink selection; step 2: optional glass assignment). The confirm button is split: default action queues the order; secondary action commits immediately.
- `QueueOverlay` is a FAB-style overlay visible when the order queue is non-empty.

### Utilities (`util/`)

- `TimeUtils.kt` — countdown / duration formatting.
- `ContextUtils.kt` — Android `Context` helpers (e.g., URI opening for import/export).

## Before finishing any request

Always run the following before considering a task done:

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three must pass with no errors and no new lint warnings introduced.

## Key conventions

### Dependencies

All versions are managed through the version catalog at `gradle/libs.versions.toml`. Add new libraries there before referencing them in `build.gradle.kts` via `libs.*` aliases. Do not hardcode versions directly in build files.

Current key versions: AGP 9.2.0, Kotlin 2.3.21, Compose BOM 2026.04.01, compileSdk/targetSdk 37, Room 2.8.4, Navigation3 1.1.1, DataStore 1.2.1, kotlinx-serialization-json 1.11.0, KSP 2.3.7.

### Domain model ↔ entity mapping

Each `*Entity` class must have `.toDomain()` and the domain class must have `.toEntity()`. These are the only places that translate between the database and application layers.

### Single-session invariant

The app stores exactly one active session. Any operation that creates or imports a session must first call `sessionDao.deleteAll()` inside a transaction (as done in `SessionRepository`).

### Event sourcing

`isDisabled` flags on `Player` and `Drink` entities are the cached read-side; the event log (DISABLE_PLAYER / DISABLE_DRINK events) is the source of truth. On import, flags are re-derived by replaying events.

### Colors

Define colors in `app/src/main/java/com/example/drinkwatch/ui/theme/Color.kt`. No `res/values/colors.xml` — Compose theming is used exclusively. Hex literals use uppercase: `Color(0xFF6650A4)`, not `Color(0xFF6650a4)`.

### Theme

`DrinkWatchTheme` defaults to dynamic color on Android 12+ (Material You). The fallback static scheme uses `Purple80`/`Purple40` family tokens defined in `Color.kt`.

### `gradlew` on Windows

`gradlew` is the POSIX shell wrapper for Linux/macOS/CI. On Windows, always use `.\gradlew.bat`. Keep both in git.

### minSdk

`minSdk = 24`. Guard any API above 24 with `Build.VERSION.SDK_INT` checks (e.g., dynamic color at API 31+ in `Theme.kt`).

### GitHub Actions CI

`.github/workflows/ci.yml` runs on pull requests and pushes to `master`. Uses JDK 17 (Temurin), `gradle/actions/setup-gradle@v4`, and executes `assembleDebug`, `lint`, and `testDebugUnitTest`. Uploads the debug lint HTML report as an artifact.
