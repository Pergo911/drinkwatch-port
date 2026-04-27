# Phase 13 — About Screen

## Goal

Replace the `AboutScreen` stub with a minimal informational screen displaying the app name,
version string from `BuildConfig.VERSION_NAME`, and a short description.

---

## Entry Point

Already wired in `AppNavHost` — no changes needed:

```kotlin
entry<About> {
    AboutScreen(onBack = dropUnlessResumed { backStack.removeLastOrNull() })
}
```

---

## Screen Layout

```
┌──────────────────────────────────────────────────┐
│ TopAppBar — "← About"                            │
├──────────────────────────────────────────────────┤
│                                                  │
│                                                  │
│              DrinkWatch                          │
│              Version 1.0                         │
│                                                  │
│       Party & drinking-session manager           │
│                                                  │
│                                                  │
└──────────────────────────────────────────────────┘
```

---

## Modified Files

### `app/build.gradle.kts`

Add `buildConfig = true` to `buildFeatures` so `BuildConfig.VERSION_NAME` is generated.
AGP 8.0+ no longer generates `BuildConfig` by default:

```kotlin
buildFeatures {
    compose = true
    buildConfig = true
}
```

### `ui/about/AboutScreen.kt` (replace stub)

```kotlin
package com.example.drinkwatch.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.drinkwatch.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Text(
                text = "DrinkWatch",
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Party & drinking-session manager",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
```

Also add `import androidx.compose.foundation.rememberScrollState` and
`import androidx.compose.foundation.verticalScroll` to the import list.

| Concern | Decision |
|---|---|
| `BuildConfig.VERSION_NAME` access | Requires `buildConfig = true` in `buildFeatures` (AGP 8.0+ disabled by default) |
| Screen layout | Centered column, vertically centred in the content area |
| No ViewModel needed | Static content only; no repository access required |
| Navigation icon | Same back-arrow pattern as `SettingsScreen` and `PlayerDetailScreen` |
| `onBack` parameter | Already supplied by `AppNavHost` — no navigation changes needed |

---

## Build Validation

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three must pass before Phase 13 is considered complete.
