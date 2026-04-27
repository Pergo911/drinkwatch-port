# Phase 7 — Main Screen Shell

## Goal

Replace the `MainScreen.kt` stub with a fully functional shell: a `TopAppBar` showing the session
name with a hamburger menu, a `BottomNavBar` with Order and Glasses tabs, and proper `MainViewModel`
wiring. Tab content remains as stubs (filled in Phases 8 and 10).

---

## Files to Create

| File | Description |
|---|---|
| `ui/main/AppBar.kt` | `MainTopAppBar` composable — session-name title + hamburger DropdownMenu |
| `ui/main/BottomNavBar.kt` | `MainBottomNavBar` composable — Order / Glasses tabs with icons + `MainTab` enum |

## Files to Modify

| File | Change |
|---|---|
| `viewmodel/MainViewModel.kt` | Expose `sessionName: StateFlow<String>` |
| `ui/main/MainScreen.kt` | Wire `MainViewModel`, `MainTopAppBar`, `MainBottomNavBar`; keep tab stubs |
| `gradle/libs.versions.toml` | Add `material-icons-extended` entry (BOM-managed, no version needed) |
| `app/build.gradle.kts` | Add `material-icons-extended` implementation dependency |

---

## Task 1 — Add `material-icons-extended` dependency

Phase 7 needs icons for the bottom-nav tabs (e.g. `Icons.Filled.People`,
`Icons.Filled.LocalBar`) that are not in `material-icons-core`. Add the extended set.

### `gradle/libs.versions.toml`

Add to the `[libraries]` block:

```toml
compose-material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }
```

### `app/build.gradle.kts`

Add alongside the existing `material-icons-core` line:

```kotlin
implementation(libs.compose.material.icons.extended)
```

> **Note:** `material-icons-extended` is BOM-managed so no explicit version is needed. The library
> is large (~7 MB of vector assets) but the R8 shrinker removes unused icons in release builds.

---

## Task 2 — Add `sessionName` and `sessionId` to `MainViewModel`

The `AppBar` needs to display the active session name. `MainScreen` also needs the session ID to
key the tab-state `rememberSaveable` so the tab resets whenever the session is replaced.
`MainViewModel` already has a private `_currentSession: StateFlow<Session?>` — expose two derived
flows from it.

### `viewmodel/MainViewModel.kt`

Add the following public flows after the `queue` declaration:

```kotlin
val sessionName: StateFlow<String> =
    _currentSession
        .map { it?.name ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

/** Exposed so MainScreen can key rememberSaveable by session identity. */
val sessionId: StateFlow<Long?> =
    _currentSession
        .map { it?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
```

Import `kotlinx.coroutines.flow.map` is already present.

---

## Task 3 — Create `ui/main/AppBar.kt`

```kotlin
package com.example.drinkwatch.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    sessionName: String,
    onNavigateToSession: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(sessionName.ifEmpty { "DrinkWatch" }) },
        navigationIcon = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Session") },
                    onClick = {
                        menuExpanded = false
                        onNavigateToSession()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        menuExpanded = false
                        onNavigateToSettings()
                    },
                )
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = {
                        menuExpanded = false
                        onNavigateToAbout()
                    },
                )
            }
        },
    )
}
```

---

## Task 4 — Create `ui/main/BottomNavBar.kt`

Defines a `MainTab` enum for type-safe tab references across `MainScreen`, `OrderTab` (Phase 8),
and `GlassesTab` (Phase 10). Avoids a bare `Int` that would be harder to migrate when those phases
add per-tab navigation state.

Use `Icons.Filled.People` for the Order tab (it shows a list of players) and
`Icons.Filled.LocalBar` for the Glasses tab (drinking glasses).

```kotlin
package com.example.drinkwatch.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class MainTab { ORDER, GLASSES }

@Composable
fun MainBottomNavBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == MainTab.ORDER,
            onClick = { onTabSelected(MainTab.ORDER) },
            icon = { Icon(Icons.Filled.People, contentDescription = null) },
            label = { Text("Order") },
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.GLASSES,
            onClick = { onTabSelected(MainTab.GLASSES) },
            icon = { Icon(Icons.Filled.LocalBar, contentDescription = null) },
            label = { Text("Glasses") },
        )
    }
}
```

---

## Task 5 — Rewrite `ui/main/MainScreen.kt`

Replace the existing stub with a properly wired composable.

**Key responsibilities:**
- Obtain `MainViewModel` via `viewModel(factory = app.mainViewModelFactory)`. This is the **single
  owner** of the VM; Phase 8 and 10 tab composables will receive state/actions as parameters — they
  must not create their own `MainViewModel` instances to avoid diverging queue state.
- Collect `sessionName` and the current session's `id` from the VM.
- Render `Scaffold` with `MainTopAppBar` (top) and `MainBottomNavBar` (bottom).
- Navigate via the passed-in callbacks on menu-item selection.
- Preserve selected-tab with `rememberSaveable`, **keyed by the active session ID** so the tab
  resets to `ORDER` whenever the session is replaced (import or new session clears the tab).
- Display stub content in each tab (replaced by Phase 8 / Phase 10).

```kotlin
package com.example.drinkwatch.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drinkwatch.DrinkWatchApplication
import com.example.drinkwatch.viewmodel.MainViewModel

@Composable
fun MainScreen(
    onNavigateToSession: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPlayerDetail: (Long) -> Unit,
    onNavigateToOrderDialog: (Long) -> Unit,
) {
    val app = LocalContext.current.applicationContext as DrinkWatchApplication
    val viewModel: MainViewModel = viewModel(factory = app.mainViewModelFactory)

    val sessionName by viewModel.sessionName.collectAsStateWithLifecycle()
    val sessionId   by viewModel.sessionId.collectAsStateWithLifecycle()

    // Key by sessionId: if the session is replaced the tab resets to ORDER automatically.
    var selectedTab by rememberSaveable(sessionId) { mutableStateOf(MainTab.ORDER) }

    Scaffold(
        topBar = {
            MainTopAppBar(
                sessionName = sessionName,
                onNavigateToSession = onNavigateToSession,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToAbout = onNavigateToAbout,
            )
        },
        bottomBar = {
            MainBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when (selectedTab) {
                    MainTab.ORDER   -> "Order tab (stub)"
                    MainTab.GLASSES -> "Glasses tab (stub)"
                }
            )
        }
    }
}
```

> **Note:** `MainTab` is saved by `rememberSaveable` using `enumValues`-based ordinal mapping via
> a custom `Saver`. Add the following companion object to `MainTab`, or declare a top-level saver:
>
> ```kotlin
> enum class MainTab { ORDER, GLASSES }
>
> private val MainTabSaver = Saver<MainTab, Int>(
>     save    = { it.ordinal },
>     restore = { MainTab.entries[it] },
> )
> ```
>
> In `MainScreen`, use:
> ```kotlin
> var selectedTab by rememberSaveable(sessionId, stateSaver = MainTabSaver) {
>     mutableStateOf(MainTab.ORDER)
> }
> ```

---

---

## Verification

Run the full build and check suite:

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three must pass with zero errors and no new lint warnings.
