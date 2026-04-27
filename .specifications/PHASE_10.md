# Phase 10 — Glasses Tab

## Goal

Implement the Glasses Tab in `MainScreen`: a scrollable list of all currently-held glasses,
each tappable to trigger a `ReturnGlassDialog` confirmation, after which
`MainViewModel.returnGlass()` is called to persist the return event.

---

## Context

The following infrastructure is already in place from earlier phases:

- `MainViewModel.takenGlasses: StateFlow<List<TakenGlassUiState>>` — collected in `MainScreen.kt`.
- `MainViewModel.returnGlass(glassGroup: Char, glassNumber: Int)` — inserts a `GLASS_RETURN`
  event via `SessionRepository`.
- `TakenGlassUiState(glassGroup, glassNumber, playerName, drinkName, takenAtMs, playerId)`.
- `MainScreen.kt` already collects `takenGlasses` and has a `MainTab.GLASSES` stub branch.
- `ui/dialog/ReturnGlassDialog.kt` — does **not** exist yet.
- `ui/glasses/` directory — does **not** exist yet.

---

## Files to Modify

| File | Change |
|---|---|
| `data/db/dao/EventDao.kt` | Add `ORDER BY` to `getTakenGlasses` |
| `ui/main/MainScreen.kt` | Replace `GLASSES` stub with `GlassesTab` |

---

## New Files

| File | Description |
|---|---|
| `ui/glasses/GlassCard.kt` | `ElevatedCard` showing glass label, drink, player |
| `ui/glasses/GlassesTab.kt` | `LazyColumn` of `GlassCard`s + empty state + dialog state |
| `ui/dialog/ReturnGlassDialog.kt` | Confirmation `AlertDialog` for returning a glass |

---

## Task 1 — Fix `data/db/dao/EventDao.kt`

`getTakenGlasses` has no `ORDER BY` clause; SQLite does not guarantee row order without one.
Add an explicit sort so the list is stable and deterministic (oldest-taken glass first):

```kotlin
@Query("""
    SELECT * FROM events AS o
    WHERE o.type = 'ORDER'
    AND o.glassGroup IS NOT NULL
    AND o.sessionId = :sessionId
    AND o.id = (
        SELECT MAX(e.id) FROM events AS e
        WHERE (e.type = 'ORDER' OR e.type = 'RETURN')
        AND e.glassGroup = o.glassGroup
        AND e.glassNumber = o.glassNumber
        AND e.sessionId = :sessionId
    )
    ORDER BY o.timestampMs ASC, o.id ASC
""")
fun getTakenGlasses(sessionId: Long): Flow<List<EventEntity>>
```

---

## Task 2 — Create `ui/glasses/GlassCard.kt`

```kotlin
package com.example.drinkwatch.ui.glasses

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.drinkwatch.viewmodel.TakenGlassUiState

@Composable
fun GlassCard(
    uiState: TakenGlassUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        ListItem(
            headlineContent = { Text("${uiState.glassGroup}${uiState.glassNumber}") },
            supportingContent = { Text(uiState.drinkName) },
            trailingContent = { Text(uiState.playerName) },
        )
    }
}
```

---

## Task 3 — Create `ui/glasses/GlassesTab.kt`

```kotlin
package com.example.drinkwatch.ui.glasses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.drinkwatch.ui.dialog.ReturnGlassDialog
import com.example.drinkwatch.viewmodel.TakenGlassUiState

@Composable
fun GlassesTab(
    takenGlasses: List<TakenGlassUiState>,
    onReturnGlass: (Char, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var returnTarget by remember { mutableStateOf<TakenGlassUiState?>(null) }

    if (takenGlasses.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No glasses out.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            items(
                items = takenGlasses,
                key = { "${it.glassGroup}${it.glassNumber}" },
            ) { uiState ->
                GlassCard(
                    uiState = uiState,
                    onClick = { returnTarget = uiState },
                )
            }
        }
    }

    // Close over a stable snapshot to avoid stale-reference issues.
    returnTarget?.let { target ->
        ReturnGlassDialog(
            glass = target,
            onConfirm = {
                onReturnGlass(target.glassGroup, target.glassNumber)
                returnTarget = null
            },
            onDismiss = { returnTarget = null },
        )
    }
}
```

---

## Task 4 — Create `ui/dialog/ReturnGlassDialog.kt`

```kotlin
package com.example.drinkwatch.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.drinkwatch.viewmodel.TakenGlassUiState

@Composable
fun ReturnGlassDialog(
    glass: TakenGlassUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Return Glass?") },
        text = {
            Text(
                "Return glass ${glass.glassGroup}${glass.glassNumber} " +
                    "taken by ${glass.playerName}?"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Return") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
```

---

## Task 5 — Update `ui/main/MainScreen.kt`

Replace the `MainTab.GLASSES` stub `Box` with `GlassesTab`:

```kotlin
// Add imports
import com.example.drinkwatch.ui.glasses.GlassesTab

// Replace stub:
MainTab.GLASSES ->
    GlassesTab(
        takenGlasses = takenGlasses,
        onReturnGlass = viewModel::returnGlass,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    )
```

Remove unused `Alignment` and `Box` imports from `MainScreen.kt` if they are no longer needed
after the stub is replaced.

---

## Verification

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three must pass with zero errors and no new lint warnings introduced.
