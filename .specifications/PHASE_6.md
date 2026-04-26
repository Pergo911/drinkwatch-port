# Phase 6 — Session Screen

## Goal

Replace the `SessionScreen` stub with a fully functional four-tab session management screen.
The screen covers session creation, import, and export, and allows managing players, drinks, and
glass groups.

---

## Files to Create

| File | Description |
|---|---|
| `ui/session/SessionScreen.kt` | Replaces stub; top-level scaffold, tab row, ViewModel wiring |
| `ui/session/overview/OverviewTab.kt` | Two-state tab: no-session (create/import) and has-session (edit/export/import/new) |
| `ui/session/players/PlayersTab.kt` | Player list with FAB |
| `ui/session/players/PlayerDialog.kt` | Add / edit player full-screen dialog |
| `ui/session/drinks/DrinksTab.kt` | Drink list with FAB |
| `ui/session/drinks/DrinkDialog.kt` | Add / edit drink full-screen dialog |
| `ui/session/glassgroups/GlassGroupsTab.kt` | A–Z glass-group toggle grid |

## Files to Modify

| File | Change |
|---|---|
| `data/serialization/SessionSerializer.kt` | Wrap streams in `use { }` so the serializer owns stream lifecycle |
| `ui/navigation/Routes.kt` | Change `Session` from `data object` to `data class Session(val openedAsGuard: Boolean = false)` |
| `ui/navigation/AppNavHost.kt` | Pass `openedAsGuard = true` in the no-session guard; thread the flag through to `SessionScreen` |

---

## Task 1 — Fix stream lifecycle in `SessionSerializer`

The UI opens SAF streams and passes them to `viewModel.importSession(stream)` /
`viewModel.exportSession(stream)`. Both ViewModel functions fire-and-forget into a coroutine and
return immediately. If the caller closes the stream with `.use { }` before the coroutine executes,
the coroutine reads or writes a closed stream. The serializer must close the streams itself.

### `export` — flush and close output stream after writing

```kotlin
suspend fun export(sessionId: Long, outputStream: OutputStream) {
    // … existing snapshot-building code is unchanged …
    val bytes = json.encodeToString(snapshot).toByteArray(Charsets.UTF_8)
    outputStream.use { it.write(bytes) }          // was: outputStream.write(bytes)
}
```

### `import` — close input stream after reading

```kotlin
suspend fun import(inputStream: InputStream) {
    val text = inputStream.use { it.readBytes() }.toString(Charsets.UTF_8)   // was: inputStream.readBytes().toString(…)
    val snapshot = json.decodeFromString<SessionSnapshot>(text)
    repository.importSession(snapshot)
}
```

---

## Task 2 — Update navigation to carry the guard flag

### `ui/navigation/Routes.kt`

Change `Session` from a `data object` to a `data class` so it can carry the `openedAsGuard` flag:

```kotlin
@Serializable data class  Session(val openedAsGuard: Boolean = false) : AppRoute
```

All existing `backStack.add(Session)` call-sites in `AppNavHost` must be updated to
`backStack.add(Session())` or `backStack.add(Session(openedAsGuard = true))`.

### `ui/navigation/AppNavHost.kt`

Three changes:

1. In the guard `LaunchedEffect`, push `Session(openedAsGuard = true)`:
   ```kotlin
   LaunchedEffect(Unit) {
       if (!app.sessionRepository.hasActiveSession()) {
           backStack.clear()
           backStack.add(Session(openedAsGuard = true))  // was: Session
       }
   }
   ```

2. In the hamburger-menu navigation callbacks, use `Session()` (default `openedAsGuard = false`):
   ```kotlin
   onNavigateToSession = dropUnlessResumed { backStack.add(Session()) }  // was: Session
   ```

3. Thread the flag into `SessionScreen`:
   ```kotlin
   entry<Session> { key ->
       SessionScreen(
           openedAsGuard    = key.openedAsGuard,
           onNavigateToMain = dropUnlessResumed { backStack.clear(); backStack.add(Main) },
           onBack           = dropUnlessResumed { backStack.removeLastOrNull() },
       )
   }
   ```

---

## Task 3 — `ui/session/SessionScreen.kt`

Replaces the existing one-liner stub. Obtains the `SessionViewModel`, hosts the
`SnackbarHostState`, and renders a `Scaffold` with a `TopAppBar` and `SecondaryTabRow`.

**Auto-navigation rule:** If the screen was opened because no session existed (app-start guard in
`AppNavHost`), navigate to `Main` as soon as a session first appears in the flow.

```kotlin
package com.example.drinkwatch.ui.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drinkwatch.DrinkWatchApplication
import com.example.drinkwatch.ui.session.drinks.DrinksTab
import com.example.drinkwatch.ui.session.glassgroups.GlassGroupsTab
import com.example.drinkwatch.ui.session.overview.OverviewTab
import com.example.drinkwatch.ui.session.players.PlayersTab
import com.example.drinkwatch.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    openedAsGuard: Boolean,      // true when redirected here because no session existed on launch
    onNavigateToMain: () -> Unit,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as DrinkWatchApplication
    val viewModel: SessionViewModel = viewModel(factory = app.sessionViewModelFactory)

    val session by viewModel.currentSession.collectAsStateWithLifecycle()
    val uiError by viewModel.uiError.collectAsStateWithLifecycle()

    // Navigate to Main once a session is available — but only when this screen was opened as
    // the no-session guard. When opened from the hamburger menu (openedAsGuard = false), the
    // user stays here after creating or importing.
    LaunchedEffect(session) {
        if (openedAsGuard && session != null) {
            onNavigateToMain()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiError) {
        val msg = uiError
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    val tabTitles = listOf("Overview", "Players", "Drinks", "Glass Groups")
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        enabled = index == 0 || session != null,
                        text = { Text(title) },
                    )
                }
            }
            when (selectedTab) {
                0 -> OverviewTab(viewModel = viewModel)
                1 -> if (session != null) PlayersTab(viewModel = viewModel)
                2 -> if (session != null) DrinksTab(viewModel = viewModel)
                3 -> if (session != null) GlassGroupsTab(viewModel = viewModel)
            }
        }
    }
}
```

---

## Task 3 — `ui/session/overview/OverviewTab.kt`

Two-state composable branching on `session == null`. SAF launchers are registered at composable
scope so they survive recomposition.

### File skeleton

```kotlin
package com.example.drinkwatch.ui.session.overview

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drinkwatch.viewmodel.SessionViewModel

@Composable
fun OverviewTab(viewModel: SessionViewModel) {
    val session by viewModel.currentSession.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showNewSessionConfirm by rememberSaveable { mutableStateOf(false) }
    var showImportConfirm by rememberSaveable { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.let { stream ->
                viewModel.importSession(stream)
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { stream ->
                viewModel.exportSession(stream)
            }
        }
    }

    when (val s = session) {
        null -> NoSessionContent(
            onImport = { importLauncher.launch(arrayOf("application/json")) },
            onCreateNew = { showCreateDialog = true },
        )
        else -> SessionLoadedContent(
            session = s,
            onExport = { exportLauncher.launch("${s.name}.json") },
            onImport = { showImportConfirm = true },
            onStartNew = { showNewSessionConfirm = true },
            onNameChange = { viewModel.updateSessionName(it) },
        )
    }

    // ── Dialogs ────────────────────────────────────────────────────────────

    if (showCreateDialog) {
        NameInputDialog(
            title = "Create New Session",
            label = "Session name",
            confirmText = "Create",
            onConfirm = { name ->
                viewModel.createSession(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("Replace Current Session?") },
            text = { Text("Importing will permanently replace the current session and all its data.") },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    importLauncher.launch(arrayOf("application/json"))
                }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showNewSessionConfirm) {
        AlertDialog(
            onDismissRequest = { showNewSessionConfirm = false },
            title = { Text("Start New Session?") },
            text = { Text("All current session data will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showNewSessionConfirm = false
                    showCreateDialog = true
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showNewSessionConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
```

### `NoSessionContent`

```kotlin
@Composable
private fun NoSessionContent(
    onImport: () -> Unit,
    onCreateNew: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text("Import From File")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCreateNew, modifier = Modifier.fillMaxWidth()) {
            Text("Create New")
        }
    }
}
```

### `SessionLoadedContent`

```kotlin
@Composable
private fun SessionLoadedContent(
    session: com.example.drinkwatch.data.model.Session,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onStartNew: () -> Unit,
    onNameChange: (String) -> Unit,
) {
    // rememberSaveable keyed on session.id so the field resets when the session is replaced.
    var name by rememberSaveable(session.id) { mutableStateOf(session.name) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Session Name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        val trimmed = name.trim()
                        when {
                            trimmed.isBlank() -> name = session.name  // revert on empty
                            trimmed != session.name -> onNameChange(trimmed)
                        }
                    }
                },
        )
        Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
            Text("Export to File")
        }
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text("Import From File")
        }
        OutlinedButton(onClick = onStartNew, modifier = Modifier.fillMaxWidth()) {
            Text("Start New Session")
        }
    }
}
```

### `NameInputDialog` (private helper, same file)

```kotlin
@Composable
private fun NameInputDialog(
    title: String,
    label: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank(),
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
```

---

## Task 4 — Players tab

### `ui/session/players/PlayersTab.kt`

```kotlin
package com.example.drinkwatch.ui.session.players

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drinkwatch.data.model.Player
import com.example.drinkwatch.viewmodel.SessionViewModel

@Composable
fun PlayersTab(viewModel: SessionViewModel) {
    val players by viewModel.players.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(players, key = { it.id }) { player ->
                PlayerListItem(
                    player = player,
                    onEdit = { editingPlayer = player },
                )
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Player")
        }
    }

    if (showAddDialog) {
        PlayerDialog(
            player = null,
            onSave = { name, phone, note ->
                viewModel.addPlayer(name, phone, note)
                showAddDialog = false
            },
            onDelete = {},   // not applicable in add mode
            onDismiss = { showAddDialog = false },
        )
    }

    editingPlayer?.let { player ->
        PlayerDialog(
            player = player,
            onSave = { name, phone, note ->
                viewModel.updatePlayer(player.copy(name = name, phone = phone, note = note))
                editingPlayer = null
            },
            onDelete = {
                viewModel.deletePlayer(player)
                editingPlayer = null
            },
            onDismiss = { editingPlayer = null },
        )
    }
}

@Composable
private fun PlayerListItem(
    player: Player,
    onEdit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(player.name, style = MaterialTheme.typography.bodyLarge)
                if (player.phone.isNotBlank()) {
                    Text(player.phone, style = MaterialTheme.typography.bodyMedium)
                }
                if (player.note.isNotBlank()) {
                    Text(
                        player.note,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
            }
        }
    }
}
```

### `ui/session/players/PlayerDialog.kt`

Full-screen dialog implemented as a `Dialog` with `usePlatformDefaultWidth = false`.

```kotlin
package com.example.drinkwatch.ui.session.players

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.drinkwatch.data.model.Player

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDialog(
    player: Player?,         // null = add mode; non-null = edit mode
    onSave: (name: String, phone: String, note: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name  by rememberSaveable { mutableStateOf(player?.name  ?: "") }
    var phone by rememberSaveable { mutableStateOf(player?.phone ?: "") }
    var note  by rememberSaveable { mutableStateOf(player?.note  ?: "") }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(if (player != null) "Edit Player" else "Add Player") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { onSave(name.trim(), phone.trim(), note.trim()) },
                            enabled = name.isNotBlank(),
                        ) { Text("Save") }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (player != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) { Text("Delete Player") }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Player?") },
            text = { Text("This will permanently remove ${player?.name} and their event history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
```

---

## Task 5 — Drinks tab

### `ui/session/drinks/DrinksTab.kt`

Same structure as `PlayersTab` but for `Drink`.

```kotlin
package com.example.drinkwatch.ui.session.drinks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drinkwatch.data.model.Drink
import com.example.drinkwatch.data.model.DrinkType
import com.example.drinkwatch.viewmodel.SessionViewModel

@Composable
fun DrinksTab(viewModel: SessionViewModel) {
    val drinks by viewModel.drinks.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingDrink by remember { mutableStateOf<Drink?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(drinks, key = { it.id }) { drink ->
                DrinkListItem(
                    drink = drink,
                    onEdit = { editingDrink = drink },
                )
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Drink")
        }
    }

    if (showAddDialog) {
        DrinkDialog(
            drink = null,
            onSave = { name, type ->
                viewModel.addDrink(name, type)
                showAddDialog = false
            },
            onDelete = {},
            onDismiss = { showAddDialog = false },
        )
    }

    editingDrink?.let { drink ->
        DrinkDialog(
            drink = drink,
            onSave = { name, type ->
                viewModel.updateDrink(drink.copy(name = name, type = type))
                editingDrink = null
            },
            onDelete = {
                viewModel.deleteDrink(drink)
                editingDrink = null
            },
            onDismiss = { editingDrink = null },
        )
    }
}

@Composable
private fun DrinkListItem(
    drink: Drink,
    onEdit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(drink.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    when (drink.type) {
                        DrinkType.SHOT -> "Shot"
                        DrinkType.LONG_DRINK -> "Long drink"
                        DrinkType.NON_ALCOHOLIC -> "Non-alcoholic"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
            }
        }
    }
}
```

### `ui/session/drinks/DrinkDialog.kt`

```kotlin
package com.example.drinkwatch.ui.session.drinks

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.drinkwatch.data.model.Drink
import com.example.drinkwatch.data.model.DrinkType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrinkDialog(
    drink: Drink?,
    onSave: (name: String, type: DrinkType) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(drink?.name ?: "") }
    var selectedType by rememberSaveable { mutableStateOf(drink?.type ?: DrinkType.LONG_DRINK) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    val drinkTypeOptions = listOf(
        DrinkType.SHOT to "Shot",
        DrinkType.LONG_DRINK to "Long drink",
        DrinkType.NON_ALCOHOLIC to "Non-alcoholic",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(if (drink != null) "Edit Drink" else "Add Drink") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { onSave(name.trim(), selectedType) },
                            enabled = name.isNotBlank(),
                        ) { Text("Save") }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Type", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    drinkTypeOptions.forEachIndexed { index, (type, label) ->
                        SegmentedButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = drinkTypeOptions.size,
                            ),
                        ) { Text(label) }
                    }
                }

                if (drink != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) { Text("Delete Drink") }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Drink?") },
            text = { Text("This will permanently remove ${drink?.name}.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
```

---

## Task 6 — `ui/session/glassgroups/GlassGroupsTab.kt`

Scrollable grid of letters A–Z. Each letter is a `FilterChip` that reflects whether the
corresponding `GlassGroup` record exists for the current session.

```kotlin
package com.example.drinkwatch.ui.session.glassgroups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drinkwatch.viewmodel.SessionViewModel

@Composable
fun GlassGroupsTab(viewModel: SessionViewModel) {
    val glassGroups by viewModel.glassGroups.collectAsStateWithLifecycle()
    val activeLetters = glassGroups.map { it.letter }.toSet()
    val letters = ('A'..'Z').toList()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(72.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(letters) { letter ->
            FilterChip(
                selected = letter in activeLetters,
                onClick = { viewModel.toggleGlassGroup(letter) },
                label = { Text(letter.toString()) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
```

---

## Task 7 — Verify

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three must pass with no errors and no new lint warnings.
