# Phase 8 — Order Tab

## Goal

Replace the Order-tab stub in `MainScreen.kt` with a fully interactive player list. Each
`PlayerCard` reflects one of four states (normal, under-timeout, queued, disabled). The
`TimeoutDialog` lets the bartender set or cancel a player's timeout. The queue FAB commits all
pending orders at once. The Glasses bottom-nav tab gains a badge showing the count of currently
taken glasses (resolved question Q2 from `DEVELOPMENT_PLAN.md`).

---

## Files to Create

| File | Description |
|---|---|
| `ui/order/OrderTab.kt` | Tab container: `LazyColumn` of `PlayerCard`s, queue `ExtendedFloatingActionButton`, queue-blocked snackbar |
| `ui/order/PlayerCard.kt` | `ElevatedCard` with normal / under-timeout / queued / disabled states |
| `ui/order/QueueOverlay.kt` | Replaces the action-button row when an order is queued; shows drink → glass with `×` cancel |
| `ui/dialog/TimeoutDialog.kt` | `AlertDialog` duration picker (hours + minutes `OutlinedTextField`s); 0:00 allowed |

## Files to Modify

| File | Change |
|---|---|
| `viewmodel/PlayerUiState.kt` | Add `queuedDrinkName: String?` field |
| `viewmodel/MainViewModel.kt` | Add `defaultTimeoutSeconds: StateFlow<Int>`; update `playerUiStates` to 4-flow combine with `observeDrinks` to resolve drink name |
| `ui/main/MainScreen.kt` | Add `SnackbarHostState`; collect `uiEvents`; replace Order stub with `OrderTab`; pass badge count to nav bar |
| `ui/main/BottomNavBar.kt` | Add `takenGlassCount: Int` param; wrap Glasses icon in `BadgedBox` |

---

## Task 1 — Extend ViewModel layer

### `viewmodel/PlayerUiState.kt`

Add `queuedDrinkName: String?` so the resolved drink name travels with the card state — keeping
name-resolution logic in the ViewModel, not in Compose:

```kotlin
data class PlayerUiState(
    val derived: PlayerDerivedState,
    val queuedOrder: QueuedOrder?,
    /** Milliseconds until the timeout expires; null if not under timeout. Ticker-driven. */
    val timeoutMillisRemaining: Long?,
    /** Resolved display name of the queued drink; null when no order is queued. */
    val queuedDrinkName: String?,
) {
    val isUnderTimeout: Boolean get() = timeoutMillisRemaining != null
}
```

### `viewmodel/MainViewModel.kt`

**a) Add `defaultTimeoutSeconds` flow** after the `activeDrinkHighlight` declaration:

```kotlin
val defaultTimeoutSeconds: StateFlow<Int> =
    settingsRepository.settings
        .map { it.defaultTimeoutSeconds }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 300)
```

**b) Update `playerUiStates` to a 4-flow `combine`** that also pulls `observeDrinks` so it can
resolve the queued drink name:

```kotlin
val playerUiStates: StateFlow<List<PlayerUiState>> =
    _currentSession.flatMapLatest { session ->
        if (session == null) return@flatMapLatest flowOf(emptyList())
        combine(
            sessionRepository.observePlayerStates(session.id),
            _queue,
            tickerFlow,
            sessionRepository.observeDrinks(session.id),
        ) { derivedList, queue, nowMs, drinks ->
            val drinkById = drinks.associateBy { it.id }
            derivedList.map { derived ->
                val millisRemaining = derived.timeoutEndsAtMs
                    ?.let { it - nowMs }?.takeIf { it > 0 }
                val queuedOrder = queue.firstOrNull { it.playerId == derived.player.id }
                PlayerUiState(
                    derived = derived,
                    queuedOrder = queuedOrder,
                    timeoutMillisRemaining = millisRemaining,
                    queuedDrinkName = queuedOrder?.let { drinkById[it.drinkId]?.name },
                )
            }
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

The existing import `kotlinx.coroutines.flow.combine` already covers the 4-argument overload.

---

## Task 2 — Update `ui/main/BottomNavBar.kt`

Add `takenGlassCount: Int` parameter and wrap the Glasses `NavigationBarItem` icon in a
`BadgedBox`. Show the count badge when `takenGlassCount > 0`.

```kotlin
package com.example.drinkwatch.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver

enum class MainTab { ORDER, GLASSES }

val MainTabSaver: Saver<MainTab, Int> = Saver(
    save    = { it.ordinal },
    restore = { MainTab.entries[it] },
)

@Composable
fun MainBottomNavBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    takenGlassCount: Int,
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
            icon = {
                BadgedBox(
                    badge = {
                        if (takenGlassCount > 0) {
                            Badge { Text(takenGlassCount.toString()) }
                        }
                    },
                ) {
                    Icon(Icons.Filled.LocalBar, contentDescription = null)
                }
            },
            label = { Text("Glasses") },
        )
    }
}
```

---

## Task 3 — Rewrite `ui/main/MainScreen.kt`

Key changes:

1. Collect `takenGlasses`, `queue`, `playerUiStates`, `activeDrinkHighlight`,
   `defaultTimeoutSeconds` from `MainViewModel`.
2. Add `SnackbarHostState` + `snackbarHost` slot to `Scaffold`.
3. Add `LaunchedEffect` to collect `viewModel.uiEvents` and route them to the snackbar (handles
   `GlassAlreadyTaken` emitted from `addToQueue`/`commitOrderNow`).
4. Replace the Order-tab stub with `OrderTab(…)`; keep the Glasses stub (replaced in Phase 10).
5. Pass `takenGlassCount = takenGlasses.size` to `MainBottomNavBar`.

```kotlin
package com.example.drinkwatch.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drinkwatch.DrinkWatchApplication
import com.example.drinkwatch.ui.order.OrderTab
import com.example.drinkwatch.viewmodel.MainViewModel
import kotlinx.coroutines.launch

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

    val sessionName           by viewModel.sessionName.collectAsStateWithLifecycle()
    val sessionId             by viewModel.sessionId.collectAsStateWithLifecycle()
    val takenGlasses          by viewModel.takenGlasses.collectAsStateWithLifecycle()
    val playerUiStates        by viewModel.playerUiStates.collectAsStateWithLifecycle()
    val activeDrinkHighlight  by viewModel.activeDrinkHighlight.collectAsStateWithLifecycle()
    val defaultTimeoutSeconds by viewModel.defaultTimeoutSeconds.collectAsStateWithLifecycle()
    val queue                 by viewModel.queue.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable(sessionId, stateSaver = MainTabSaver) {
        mutableStateOf(MainTab.ORDER)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is MainViewModel.UiEvent.GlassAlreadyTaken ->
                    snackbarHostState.showSnackbar("That glass is already taken.")
            }
        }
    }

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
                takenGlassCount = takenGlasses.size,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.ORDER ->
                OrderTab(
                    playerUiStates = playerUiStates,
                    activeDrinkHighlight = activeDrinkHighlight,
                    defaultTimeoutSeconds = defaultTimeoutSeconds,
                    onNavigateToOrderDialog = onNavigateToOrderDialog,
                    onNavigateToPlayerDetail = onNavigateToPlayerDetail,
                    onStartTimeout = viewModel::startTimeout,
                    onCancelQueuedOrder = viewModel::cancelQueuedOrder,
                    onCommitQueue = viewModel::commitQueue,
                    queueSize = queue.size,
                    onShowSnackbar = { msg ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            MainTab.GLASSES ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Glasses tab (stub)")
                }
        }
    }
}
```

---

## Task 4 — Create `ui/order/QueueOverlay.kt`

A `Row` that replaces the action-button area when an order is queued. Shows the drink name and
optional glass label (e.g. `"A5"`) with an `×` cancel `IconButton` at the end.

The `Row` carries a `clickable(indication = null)` modifier that consumes any touch events in the
overlay area so they do **not** fall through to the parent `ElevatedCard.onClick`.

```kotlin
package com.example.drinkwatch.ui.order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun QueueOverlay(
    drinkName: String,
    /** e.g. "A5" when a glass is selected, or null when no glass. */
    glassLabel: String?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Consume touch events so they don't reach the parent ElevatedCard onClick.
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {},
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (glassLabel != null) "$drinkName → $glassLabel" else drinkName,
            style = MaterialTheme.typography.bodyMedium,
        )
        IconButton(onClick = onCancel) {
            Icon(Icons.Filled.Close, contentDescription = "Cancel queued order")
        }
    }
}
```

---

## Task 5 — Create `ui/dialog/TimeoutDialog.kt`

A simple `AlertDialog` with two `OutlinedTextField`s for hours and minutes. 0:00 is valid (cancels
the current timeout). Pre-populates from `defaultSeconds`.

Validation: minutes must be 0–59; the confirm button is disabled and the minutes field shows an
error state when the value exceeds 59.

The `onConfirm` callback receives the total seconds: `hours * 3600 + minutes * 60`.

```kotlin
package com.example.drinkwatch.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun TimeoutDialog(
    defaultSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val initialHours   = defaultSeconds / 3600
    val initialMinutes = (defaultSeconds % 3600) / 60

    var hoursText   by remember { mutableStateOf(initialHours.toString()) }
    var minutesText by remember { mutableStateOf(initialMinutes.toString()) }

    val hours   = hoursText.toIntOrNull()   ?: 0
    val minutes = minutesText.toIntOrNull() ?: 0

    val minutesError = minutes > 59
    val totalSeconds = hours * 3600 + minutes * 60

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Timeout") },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { new ->
                        hoursText = new.filter(Char::isDigit).trimStart('0').ifEmpty { "0" }
                    },
                    label = { Text("Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { new ->
                        minutesText = new.filter(Char::isDigit).trimStart('0').ifEmpty { "0" }
                    },
                    label = { Text("Minutes") },
                    isError = minutesError,
                    supportingText = if (minutesError) {
                        { Text("0–59") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(totalSeconds) },
                enabled = !minutesError,
            ) {
                Text("Set Timeout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
```

---

## Task 6 — Create `ui/order/PlayerCard.kt`

`ElevatedCard` supporting four states. State priority (highest → lowest):

1. `player.isDisabled` — greyed out (alpha 0.38f), no action area, non-clickable card (no ripple)
2. `queuedOrder != null` — `QueueOverlay` replaces action buttons; card header still clickable
   (navigates to player detail)
3. `playerUiState.isUnderTimeout` — countdown shown; "Add Drink" disabled, "Timeout" enabled
4. Normal — active drink count shown (red if ≥ threshold); both buttons enabled

The top section always shows the player name. The right side of the name row shows the relevant
status badge (countdown during timeout, drink count otherwise, nothing if disabled).

For disabled players, a non-clickable `ElevatedCard` is used (no ripple). All other states use
`ElevatedCard(onClick = onCardClick)`.

```kotlin
package com.example.drinkwatch.ui.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.example.drinkwatch.util.formatCountdown
import com.example.drinkwatch.viewmodel.PlayerUiState

@Composable
fun PlayerCard(
    playerUiState: PlayerUiState,
    activeDrinkHighlight: Int,
    onAddDrink: () -> Unit,
    onTimeout: () -> Unit,
    onCardClick: () -> Unit,
    onCancelOrder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val derived   = playerUiState.derived
    val player    = derived.player
    val isQueued  = playerUiState.queuedOrder != null
    val isTimeout = playerUiState.isUnderTimeout
    val queued    = playerUiState.queuedOrder

    val cardModifier = modifier
        .fillMaxWidth()
        .alpha(if (player.isDisabled) 0.38f else 1f)

    val cardContent: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(12.dp)) {

            // ── Header: name + status badge ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                when {
                    isTimeout -> Text(
                        text = formatCountdown(playerUiState.timeoutMillisRemaining ?: 0L),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    !player.isDisabled -> {
                        val countColor = if (derived.activeDrinkCount >= activeDrinkHighlight) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                        Text(
                            text = derived.activeDrinkCount.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = countColor,
                        )
                    }
                }
            }

            // ── Action area ───────────────────────────────────────────────
            if (!player.isDisabled) {
                Spacer(Modifier.height(8.dp))
                if (isQueued && queued != null) {
                    val glassLabel = if (queued.glassGroup != null && queued.glassNumber != null) {
                        "${queued.glassGroup}${queued.glassNumber}"
                    } else null
                    QueueOverlay(
                        drinkName  = playerUiState.queuedDrinkName ?: "",
                        glassLabel = glassLabel,
                        onCancel   = onCancelOrder,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAddDrink,
                            enabled = !isTimeout,
                        ) {
                            Text("Add Drink")
                        }
                        OutlinedButton(onClick = onTimeout) {
                            Text("Timeout")
                        }
                    }
                }
            }
        }
    }

    if (player.isDisabled) {
        ElevatedCard(modifier = cardModifier) { cardContent() }
    } else {
        ElevatedCard(onClick = onCardClick, modifier = cardModifier) { cardContent() }
    }
}
```

---

## Task 7 — Create `ui/order/OrderTab.kt`

Manages the full Order-tab experience: player list, per-card actions, `TimeoutDialog` lifecycle,
and the queue FAB.

**Responsibilities:**

- Track `timeoutDialogPlayerId: Long?` in local state — opens when Timeout is tapped.
- `onAddDrink` callback: defensive guard — if the player already has a queued order, show snackbar
  ("Cancel or send the queue first."). Otherwise navigate to the Order dialog. In normal flow this
  guard is never hit (the "Add Drink" button is hidden while queued), but it handles any edge case.
- Queue FAB: `ExtendedFloatingActionButton` visible only when `queueSize > 0`. Label: "Send (N)".
  Add extra bottom `contentPadding` (88 dp) to `LazyColumn` when FAB is visible to prevent overlap.
- `TimeoutDialog` shown when `timeoutDialogPlayerId != null`.
- `drinks` parameter removed — drink name is now embedded in `PlayerUiState.queuedDrinkName`.

```kotlin
package com.example.drinkwatch.ui.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.drinkwatch.ui.dialog.TimeoutDialog
import com.example.drinkwatch.viewmodel.PlayerUiState

@Composable
fun OrderTab(
    playerUiStates: List<PlayerUiState>,
    activeDrinkHighlight: Int,
    defaultTimeoutSeconds: Int,
    onNavigateToOrderDialog: (Long) -> Unit,
    onNavigateToPlayerDetail: (Long) -> Unit,
    onStartTimeout: (Long, Int) -> Unit,
    onCancelQueuedOrder: (Long) -> Unit,
    onCommitQueue: () -> Unit,
    queueSize: Int,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var timeoutDialogPlayerId by remember { mutableStateOf<Long?>(null) }

    Box(modifier = modifier) {
        LazyColumn(
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                top = 8.dp,
                bottom = if (queueSize > 0) 88.dp else 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(playerUiStates, key = { it.derived.player.id }) { uiState ->
                PlayerCard(
                    playerUiState = uiState,
                    activeDrinkHighlight = activeDrinkHighlight,
                    onAddDrink = {
                        if (uiState.queuedOrder != null) {
                            onShowSnackbar("Cancel or send the queue first.")
                        } else {
                            onNavigateToOrderDialog(uiState.derived.player.id)
                        }
                    },
                    onTimeout = { timeoutDialogPlayerId = uiState.derived.player.id },
                    onCardClick = { onNavigateToPlayerDetail(uiState.derived.player.id) },
                    onCancelOrder = { onCancelQueuedOrder(uiState.derived.player.id) },
                )
            }
        }

        if (queueSize > 0) {
            ExtendedFloatingActionButton(
                onClick = onCommitQueue,
                icon = { Icon(Icons.Filled.Send, contentDescription = null) },
                text = { Text("Send ($queueSize)") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
    }

    timeoutDialogPlayerId?.let { playerId ->
        TimeoutDialog(
            defaultSeconds = defaultTimeoutSeconds,
            onDismiss = { timeoutDialogPlayerId = null },
            onConfirm = { seconds ->
                onStartTimeout(playerId, seconds)
                timeoutDialogPlayerId = null
            },
        )
    }
}
```

---

## Verification

Run the full build and check suite:

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three must pass with zero errors and no new lint warnings introduced.
