# Phase 14 — Polish & Edge Cases

**Goal:** Production-ready finishing touches: empty states, session-guard exit, disabled-item
visuals, friendly error messages, and a final lint + test pass.

---

## 14.1 Empty States

Three list composables currently render nothing when their data is empty. Each needs a centred
placeholder with an icon and a short message.

### 14.1.1 `OrderTab.kt`

Wrap the existing `Box` body so that when `playerUiStates.isEmpty()`:

```
Box(contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = CenterHorizontally) {
        Icon(Icons.Filled.People, …, modifier = Modifier.size(48.dp), tint = onSurfaceVariant)
        Spacer(8 dp)
        Text("No players yet.", color = onSurfaceVariant)
        Text("Add players in Session → Players.", color = onSurfaceVariant, bodySmall)
    }
}
```

When the list is non-empty, render the existing `LazyColumn` + FAB as today.

### 14.1.2 `PlayersTab.kt`

Wrap the inner `LazyColumn` in a `Box`. When `players.isEmpty()`:

```
Icon(Icons.Filled.Person, …, 48 dp, onSurfaceVariant)
Text("No players yet.")
```

Keep the FAB visible in both states (user should still be able to add the first player).

### 14.1.3 `DrinksTab.kt`

Same pattern as `PlayersTab.kt`. When `drinks.isEmpty()`:

```
Icon(Icons.Filled.LocalBar, …, 48 dp, onSurfaceVariant)
Text("No drinks yet.")
```

Keep the FAB visible.

### Unchanged (already have empty states)

| Composable | Empty state |
|---|---|
| `GlassesTab.kt` | "No glasses out." (centred `Text`) ✓ |
| `ExpandableEventTable.kt` | "No history." when expanded + empty ✓ |

---

## 14.2 No-Session Guard: Back Navigation Exits the App

### Problem

When the app launches with no session the back stack is:

```
[Session(openedAsGuard = true)]
```

The `NavDisplay`'s `onBack` calls `backStack.removeLastOrNull()`, making the stack empty.
Nav3 with an empty back stack does not call `Activity.finish()` — the user is stranded on a
blank screen.  Similarly, tapping the back-arrow `IconButton` in `SessionScreen`'s `TopAppBar`
calls the `onBack` lambda which has the same effect.

### Fix

**`AppNavHost.kt`** — three changes:

1. Resolve the activity via the existing `findActivity()` helper (`util/ContextUtils.kt`) — do
   **not** cast `LocalContext.current` directly, as the context inside a dialog is a
   `ContextWrapper`, not the Activity itself:

```kotlin
val activity = LocalContext.current.findActivity()
```

2. Define a shared `popOrFinish` helper that keeps the back-stack non-empty invariant in a
   single place:

```kotlin
val popOrFinish: () -> Unit = {
    if (backStack.size <= 1) activity.finish()
    else backStack.removeLastOrNull()
}
```

3. Use `popOrFinish` in both places:

```kotlin
NavDisplay(
    backStack = backStack,
    onBack = popOrFinish,
    …
)

…

entry<Session> { key ->
    SessionScreen(
        openedAsGuard = key.openedAsGuard,
        onNavigateToMain = dropUnlessResumed { … },
        onBack = if (key.openedAsGuard) {
            dropUnlessResumed { activity.finish() }
        } else {
            dropUnlessResumed { backStack.removeLastOrNull() }
        },
    )
}
```

This ensures both the system back gesture and the TopAppBar back-arrow exit the app when there
is no session.  The `popOrFinish` helper also guards every future root-level back gesture.

---

## 14.3 Disabled-Item Visuals in Session Management Lists

`PlayerCard` in the Order tab already applies `Modifier.alpha(0.38f)` for disabled players.  The
session-management list items do not.

### 14.3.1 `PlayersTab.kt` → `PlayerListItem`

Add `Modifier.alpha(if (player.isDisabled) 0.38f else 1f)` to the `Card`.

### 14.3.2 `DrinksTab.kt` → `DrinkListItem`

Add `Modifier.alpha(if (drink.isDisabled) 0.38f else 1f)` to the `Card`.

No other changes needed; `PlayerCard` (Order tab) already handles this correctly.

---

## 14.4 Active Drink Threshold Highlight

**Already fully implemented** — no changes needed.

- `PlayerCard.kt`: uses `MaterialTheme.colorScheme.error` when
  `activeDrinkCount >= activeDrinkHighlight`. ✓
- `PlayerDetailScreen.kt`: same error-colour logic for the stats row. ✓

---

## 14.5 Timeout Countdown Accuracy

**Already fully implemented** — no changes needed.

`MainViewModel` and `PlayerDetailViewModel` both use a `tickerFlow` that emits
`System.currentTimeMillis()` every 1 000 ms. The expression
`(timeoutEndsAtMs - nowMs).takeIf { it > 0 }` returns `null` as soon as the timeout expires,
so `isUnderTimeout` transitions to `false` automatically within one tick (≤ 1 s). ✓

---

## 14.6 File Picker Error Handling

### 14.6.1 User-friendly import error messages

`SessionViewModel.importSession()` currently forwards the raw exception message from
`kotlinx.serialization` (e.g. _"Unexpected JSON token at offset 0 …"_).

Replace with targeted exception mapping:

```kotlin
} catch (e: CancellationException) {
    throw e   // never swallow coroutine cancellation
} catch (e: kotlinx.serialization.SerializationException) {
    _uiError.value = "Import failed: invalid or corrupted file."
} catch (e: Exception) {
    _uiError.value = "Import failed."
}
```

### 14.6.2 Null or throw from `ContentResolver.openInputStream`

`OverviewTab.kt` silently discards the case where `openInputStream(uri)` returns `null` or
throws (e.g. `FileNotFoundException`, `SecurityException`).  Both cases must surface a snackbar.

**`SessionViewModel.kt`** — add:

```kotlin
fun reportError(message: String) {
    _uiError.value = message
}
```

**`OverviewTab.kt`** — replace the current silent `?.let` pattern:

```kotlin
try {
    val stream = context.contentResolver.openInputStream(uri)
    if (stream == null) {
        viewModel.reportError("Could not open the selected file.")
    } else {
        viewModel.importSession(stream)
    }
} catch (e: Exception) {
    viewModel.reportError("Could not open the selected file.")
}
```

The existing `SnackbarHost` in `SessionScreen` will display the error automatically.

---

## 14.7 Final Lint & Test Pass

Run:

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three tasks must pass with **zero errors** and **no new lint warnings** introduced by this
phase.  Address any issues found before considering the phase complete.

---

## File Changelist

| File | Change |
|---|---|
| `ui/order/OrderTab.kt` | Add empty-state branch |
| `ui/session/players/PlayersTab.kt` | Add empty-state branch; add disabled alpha to `PlayerListItem` |
| `ui/session/drinks/DrinksTab.kt` | Add empty-state branch; add disabled alpha to `DrinkListItem` |
| `ui/navigation/AppNavHost.kt` | `popOrFinish` helper; `findActivity()` for activity ref; guard-mode `onBack` for Session entry |
| `viewmodel/SessionViewModel.kt` | `reportError()` method; targeted exception mapping for import errors |
| `ui/session/overview/OverviewTab.kt` | try/catch + null-check on `openInputStream` with `viewModel.reportError` fallback |
