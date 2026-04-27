# Phase 4 — Navigation (Navigation 3)

## Goal

Wire Jetpack Navigation 3 into the app: define all routes as serializable `NavKey` types, create
`AppNavHost` with a flat back stack, conditional session guard, and a `DialogSceneStrategy` for the
Order dialog, then replace `MainActivity`'s stub with `AppNavHost`.

## Current state

- Phases 1–3 complete: all data models, Room entities/DAOs, repositories, serializer, and unit
  tests are implemented.
- Navigation 3 libraries already declared in `app/build.gradle.kts` (added in Phase 1):
  `navigation3-runtime`, `navigation3-ui`, `lifecycle-viewmodel-navigation3`.
- `kotlinx.serialization` plugin is already applied.
- `MainActivity` currently renders a bare `DrinkWatchTheme {}` Scaffold with a `Greeting` stub.
- No navigation code, no UI screens, and no `ui/navigation/`, `ui/main/`, etc. packages exist yet.

---

## New files

| Path | Description |
|---|---|
| `ui/navigation/Routes.kt` | `AppRoute` sealed interface + all `@Serializable` route keys |
| `ui/navigation/AppNavHost.kt` | `AppNavHost` composable: `NavDisplay`, session guard, entry map |
| `ui/main/MainScreen.kt` | Stub: `Scaffold` + `NavigationBar` with two tabs (Order / Glasses) |
| `ui/session/SessionScreen.kt` | Stub: placeholder composable |
| `ui/settings/SettingsScreen.kt` | Stub: placeholder composable |
| `ui/about/AboutScreen.kt` | Stub: placeholder composable |
| `ui/player/PlayerDetailScreen.kt` | Stub: placeholder composable |
| `ui/dialog/OrderDialog.kt` | Stub: `OrderDialogContent` placeholder composable |

## Modified files

| Path | Change |
|---|---|
| `MainActivity.kt` | Replace `Scaffold` + `Greeting` with `AppNavHost()`; remove unused code |

---

## Tasks

### Task 1 — `Routes.kt`

Create `ui/navigation/Routes.kt`.

Each route implements `NavKey` (required by Nav3's type system) and carries `@Serializable` so the
back stack can survive process death via `rememberNavBackStack`.

> **Naming note:** There is a Kotlin import conflict between the `OrderDialog` route class
> (defined here) and a composable function of the same name that will live in
> `ui/dialog/OrderDialog.kt`. To avoid ambiguity in `AppNavHost.kt`, the composable in that file
> is named `OrderDialogContent` (see Task 2). Phase 9 finalises the full dialog implementation.

```kotlin
package com.example.drinkwatch.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable sealed interface AppRoute : NavKey

@Serializable data object Main          : AppRoute
@Serializable data object Session       : AppRoute
@Serializable data object Settings      : AppRoute
@Serializable data object About         : AppRoute
@Serializable data class  PlayerDetail(val playerId: Long) : AppRoute
@Serializable data class  OrderDialog(val playerId: Long)  : AppRoute
```

---

### Task 2 — Stub screen composables

Create the following minimal placeholders in the correct packages. Each stub accepts all the
navigation callbacks it will need in its final form so that `AppNavHost` can be fully wired now;
only `onBack` (and `onNavigateToMain`) is actively called. Real implementations arrive in
Phases 6–13.

#### `ui/main/MainScreen.kt`

```kotlin
package com.example.drinkwatch.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun MainScreen(
    onNavigateToSession: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPlayerDetail: (Long) -> Unit,
    onNavigateToOrderDialog: (Long) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {},
                    label = { Text("Order") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {},
                    label = { Text("Glasses") },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (selectedTab == 0) "Order tab (stub)" else "Glasses tab (stub)")
        }
    }
}
```

#### `ui/session/SessionScreen.kt`

```kotlin
package com.example.drinkwatch.ui.session

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SessionScreen(
    onNavigateToMain: () -> Unit,
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Session screen (stub)")
    }
}
```

#### `ui/settings/SettingsScreen.kt`

```kotlin
package com.example.drinkwatch.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Settings screen (stub)")
    }
}
```

#### `ui/about/AboutScreen.kt`

```kotlin
package com.example.drinkwatch.ui.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("About screen (stub)")
    }
}
```

#### `ui/player/PlayerDetailScreen.kt`

```kotlin
package com.example.drinkwatch.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun PlayerDetailScreen(
    playerId: Long,
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Player $playerId detail (stub)")
    }
}
```

#### `ui/dialog/OrderDialog.kt`

The composable is named **`OrderDialogContent`** (not `OrderDialog`) to avoid an import collision
with `com.example.drinkwatch.ui.navigation.OrderDialog` in `AppNavHost.kt`.

```kotlin
package com.example.drinkwatch.ui.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun OrderDialogContent(
    playerId: Long,
    onDismiss: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Order dialog for player $playerId (stub)")
    }
}
```

---

### Task 3 — `AppNavHost`

Create `ui/navigation/AppNavHost.kt`.

#### Design decisions

| Decision | Rationale |
|---|---|
| Flat `SnapshotStateList<AppRoute>` back stack | Simple; multi-backstack for bottom-nav tabs is managed as local state inside `MainScreen` and is upgraded in Phases 7–8. **Config-change persistence is deferred to Phase 14** (see note below). |
| `LaunchedEffect(Unit)` session guard | `hasActiveSession()` is `suspend`; calling it in a coroutine on first composition is the natural pattern |
| `Main` is the initial back-stack entry | The guard replaces it with `Session` when no session exists, so back-press from `Session` exits the app (empty stack → system back → `finish()`) |
| `OrderDialog` uses `DialogSceneStrategy` | Full-screen dialog rendered by Nav3 as a platform dialog window |
| `rememberViewModelStoreNavEntryDecorator` included now | Required by Phase 5 ViewModels; decorator order matters: `SaveableStateHolder` before `ViewModelStore` |
| `dropUnlessResumed` on navigation lambdas | Prevents double-push / double-pop on rapid taps while a `NavEntry` is still being removed from composition |

#### Session guard behaviour

- **No session** (`hasActiveSession()` returns `false`): guard runs `backStack.clear()` then
  `backStack.add(Session)`. The resulting stack is `[Session]`. Pressing back from `Session`
  pops it, leaving an empty list; Nav3 releases its `BackHandler`, the system back propagates to
  the `Activity`, and the app exits.
- **Session exists**: initial stack `[Main]` is left unchanged.
- **After creating/importing a session** from `SessionScreen`: call `onNavigateToMain`, which does
  `backStack.clear() + add(Main)`. This covers both the no-session flow (`[Session] → [Main]`) and
  the in-session flow (`[Main, Session] → [Main]`).

```kotlin
package com.example.drinkwatch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.example.drinkwatch.DrinkWatchApplication
import com.example.drinkwatch.ui.about.AboutScreen
import com.example.drinkwatch.ui.dialog.OrderDialogContent
import com.example.drinkwatch.ui.main.MainScreen
import com.example.drinkwatch.ui.player.PlayerDetailScreen
import com.example.drinkwatch.ui.session.SessionScreen
import com.example.drinkwatch.ui.settings.SettingsScreen

@Composable
fun AppNavHost() {
    val app = LocalContext.current.applicationContext as DrinkWatchApplication

    val backStack = remember { mutableStateListOf<AppRoute>(Main) }

    LaunchedEffect(Unit) {
        if (!app.sessionRepository.hasActiveSession()) {
            backStack.clear()
            backStack.add(Session)
        }
    }

    val dialogStrategy = remember { DialogSceneStrategy<AppRoute>() }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategies = listOf(dialogStrategy),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {

            entry<Main> {
                MainScreen(
                    onNavigateToSession  = dropUnlessResumed { backStack.add(Session) },
                    onNavigateToSettings = dropUnlessResumed { backStack.add(Settings) },
                    onNavigateToAbout    = dropUnlessResumed { backStack.add(About) },
                    onNavigateToPlayerDetail = { id -> backStack.add(PlayerDetail(id)) },
                    // dropUnlessResumed wraps () -> Unit only; (Long) -> Unit callbacks are
                    // guarded at the call site when screens are implemented in Phases 6–9.
                    onNavigateToOrderDialog  = { id -> backStack.add(OrderDialog(id)) },
                )
            }

            entry<Session> {
                SessionScreen(
                    onNavigateToMain = dropUnlessResumed {
                        backStack.clear()
                        backStack.add(Main)
                    },
                    onBack = dropUnlessResumed { backStack.removeLastOrNull() },
                )
            }

            entry<Settings> {
                SettingsScreen(onBack = dropUnlessResumed { backStack.removeLastOrNull() })
            }

            entry<About> {
                AboutScreen(onBack = dropUnlessResumed { backStack.removeLastOrNull() })
            }

            entry<PlayerDetail> { key ->
                PlayerDetailScreen(
                    playerId = key.playerId,
                    onBack   = dropUnlessResumed { backStack.removeLastOrNull() },
                )
            }

            entry<OrderDialog>(
                metadata = DialogSceneStrategy.dialog(
                    DialogProperties(usePlatformDefaultWidth = false)
                )
            ) { key ->
                OrderDialogContent(
                    playerId  = key.playerId,
                    onDismiss = dropUnlessResumed { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
```

**Note on process-death restoration:** The back stack uses a plain `mutableStateListOf` which does
not survive process death. Upgrading to `rememberNavBackStack` (which uses
`rememberSerializable`) can be done in Phase 14 as a polish item if needed. It requires all
`AppRoute` subtypes to carry their `@Serializable` annotations, which they already do.

---

### Task 4 — Update `MainActivity`

Replace the entire body of `MainActivity.kt` with the minimal version below. Remove the `Greeting`
composable and its `@Preview` function.

```kotlin
package com.example.drinkwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.drinkwatch.ui.navigation.AppNavHost
import com.example.drinkwatch.ui.theme.DrinkWatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrinkWatchTheme {
                AppNavHost()
            }
        }
    }
}
```

---

## Verification

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three must pass with no errors and no new lint warnings.
