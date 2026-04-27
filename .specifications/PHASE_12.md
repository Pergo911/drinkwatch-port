# Phase 12 — Settings Screen

## Goal

Replace the `SettingsScreen` stub with a fully functional settings screen, and wire the theme
preference into `MainActivity` so it applies immediately to the entire app.

---

## Entry Point

Already wired in `AppNavHost` — no changes needed there:

```kotlin
entry<Settings> {
    SettingsScreen(onBack = dropUnlessResumed { backStack.removeLastOrNull() })
}
```

---

## Screen Layout

```
┌──────────────────────────────────────────────────┐
│ TopAppBar — "← Settings"                         │
├──────────────────────────────────────────────────┤
│                                                  │
│  Theme                                           │
│  ┌─────────┬──────────┬─────────┐               │
│  │  Light  │  System  │  Dark   │               │
│  └─────────┴──────────┴─────────┘               │
│                                                  │
│  ─────────────────────────────────────────────   │
│                                                  │
│  Active Drink Highlight                          │
│  ┌────────────────────────────────────────────┐  │
│  │ 3                                          │  │
│  └────────────────────────────────────────────┘  │
│  Highlight players with ≥ N active drinks        │
│                                                  │
│  ─────────────────────────────────────────────   │
│                                                  │
│  Default Timeout                                 │
│  ┌──────────┐  ┌──────────┐                     │
│  │  Hours   │  │ Minutes  │                     │
│  │    0     │  │    5     │                     │
│  └──────────┘  └──────────┘                     │
│                                                  │
└──────────────────────────────────────────────────┘
```

---

## Modified Files

### `ui/settings/SettingsScreen.kt` (replace stub)

```kotlin
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as DrinkWatchApplication
    val viewModel: SettingsViewModel = viewModel(factory = app.settingsViewModelFactory)
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // Local text state — initialised once from the StateFlow's current value.
    // Plain rememberSaveable (no key) avoids jank from intermediate DataStore emissions
    // that would otherwise overwrite in-progress user input.
    var highlightText by rememberSaveable {
        mutableStateOf(settings.activeDrinkHighlight.toString())
    }
    var timeoutHoursText by rememberSaveable {
        mutableStateOf((settings.defaultTimeoutSeconds / 3600).toString())
    }
    var timeoutMinutesText by rememberSaveable {
        mutableStateOf(((settings.defaultTimeoutSeconds % 3600) / 60).toString())
    }

    val highlightError = highlightText.toIntOrNull()?.let { it < 1 } ?: true

    val timeoutHours    = timeoutHoursText.toIntOrNull() ?: 0
    val timeoutMinutes  = timeoutMinutesText.toIntOrNull() ?: 0
    val timeoutMinError = timeoutMinutes > 59
    val totalTimeoutSeconds = timeoutHours * 3600 + timeoutMinutes * 60

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {

            // ── Theme ──────────────────────────────────────────────────────────
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(Theme.LIGHT to "Light", Theme.SYSTEM to "System", Theme.DARK to "Dark")
                    .forEachIndexed { index, (theme, label) ->
                        SegmentedButton(
                            selected = settings.theme == theme,
                            onClick  = { viewModel.setTheme(theme) },
                            shape    = SegmentedButtonDefaults.itemShape(index, 3),
                        ) {
                            Text(label)
                        }
                    }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Active drink highlight ─────────────────────────────────────────
            Text("Active Drink Highlight", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = highlightText,
                onValueChange = { new ->
                    highlightText = new.filter(Char::isDigit).trimStart('0').ifEmpty { "0" }
                    highlightText.toIntOrNull()?.takeIf { it >= 1 }
                        ?.let { viewModel.setActiveDrinkHighlight(it) }
                },
                label           = { Text("Threshold") },
                supportingText  = {
                    if (highlightError) Text("Must be ≥ 1")
                    else Text("Highlight players with ≥ N active drinks")
                },
                isError         = highlightError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Default timeout ────────────────────────────────────────────────
            Text("Default Timeout", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = timeoutHoursText,
                    onValueChange = { new ->
                        timeoutHoursText = new.filter(Char::isDigit).trimStart('0').ifEmpty { "0" }
                        val h = timeoutHoursText.toIntOrNull() ?: 0
                        val m = timeoutMinutesText.toIntOrNull() ?: 0
                        if (m <= 59) viewModel.setDefaultTimeoutSeconds(h * 3600 + m * 60)
                    },
                    label           = { Text("Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine      = true,
                    modifier        = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = timeoutMinutesText,
                    onValueChange = { new ->
                        timeoutMinutesText = new.filter(Char::isDigit).trimStart('0').ifEmpty { "0" }
                        val h = timeoutHoursText.toIntOrNull() ?: 0
                        val m = timeoutMinutesText.toIntOrNull() ?: 0
                        if (m <= 59) viewModel.setDefaultTimeoutSeconds(h * 3600 + m * 60)
                    },
                    label           = { Text("Minutes") },
                    isError         = timeoutMinError,
                    supportingText  = if (timeoutMinError) { { Text("0–59") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine      = true,
                    modifier        = Modifier.weight(1f),
                )
            }
        }
    }
}
```

**ViewModel import:** obtain from `app.settingsViewModelFactory` (already registered in
`DrinkWatchApplication`).

---

### `MainActivity.kt`

Wire the `Theme` setting to `DrinkWatchTheme.darkTheme` so the preference applies immediately
across the whole app:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as DrinkWatchApplication
            val settings by app.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val darkTheme = when (settings.theme) {
                Theme.LIGHT  -> false
                Theme.DARK   -> true
                Theme.SYSTEM -> isSystemInDarkTheme()
            }
            DrinkWatchTheme(darkTheme = darkTheme) {
                AppNavHost()
            }
        }
    }
}
```

- `settingsRepository.settings` is a plain `Flow<AppSettings>`, so `collectAsStateWithLifecycle(initialValue = …)` is used.
- The `initialValue = AppSettings()` value maps to `Theme.SYSTEM` so the app renders with the
  system theme on the very first frame, which is the correct default.
- Dynamic color (`dynamicColor = true`, the default) is left unchanged.

---

## Key Behavioural Details

| Concern | Decision |
|---|---|
| Theme applies immediately | `MainActivity` drives `DrinkWatchTheme.darkTheme` from the settings flow |
| Theme display order | Light → System → Dark (left to right in segmented button) |
| Active drink highlight validation | ≥ 1; `isError` shown when invalid; save only on valid values |
| Default timeout validation | Minutes 0–59; 0:00 total is allowed (consistent with `TimeoutDialog`) |
| Text field state initialisation | Plain `rememberSaveable` (no key) — initialised once from the StateFlow current value at composition time; avoids jank from intermediate DataStore saves during typing |
| SettingsViewModel scoping | Nav-entry-scoped (default `viewModel()`) |
| Dynamic color | Unchanged — remains `true` (enabled on API 31+) |
| `collectAsStateWithLifecycle` in `MainActivity` | `initialValue = AppSettings()` used for the plain `Flow<AppSettings>` |
| Supporting text for highlight | Always shows either the error or the description text (no empty gap) |

---

## Build Validation

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three must pass before Phase 12 is considered complete.
