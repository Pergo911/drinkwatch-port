# Phase 1 — Dependencies & Project Setup

## Goal

Wire up all required libraries (Room/KSP, DataStore, kotlinx.serialization, Navigation 3) so the
project compiles cleanly with all Phase 2–14 dependency infrastructure in place. Create the
`DrinkWatchApplication` stub.

## Current state

- Bare Compose scaffold: `MainActivity`, theme files, no features.
- `gradle/libs.versions.toml` contains only AGP, Kotlin, Compose BOM, lifecycle, and test deps.
- `app/build.gradle.kts` applies only `android-application` and `kotlin-compose` plugins.
- No `DrinkWatchApplication` class exists.
- `AndroidManifest.xml` has no `android:name` on `<application>`.

## Library versions

| Library                            | Version    | Notes                                   |
|------------------------------------|------------|-----------------------------------------|
| KSP plugin                         | `2.3.7`    | Decoupled from Kotlin version since 2.3 |
| Room                               | `2.8.4`    | Latest stable; requires KSP             |
| DataStore Preferences              | `1.2.1`    | Latest stable                           |
| kotlinx-serialization-json         | `1.11.0`   | Latest stable                           |
| kotlinx-serialization plugin       | `2.3.21`   | Reuses existing `kotlin` version ref    |
| Navigation 3 runtime               | `1.1.1`    | Latest stable                           |
| Navigation 3 UI                    | `1.1.1`    | Latest stable                           |
| lifecycle-viewmodel-navigation3    | `2.10.0`   | Follows lifecycle versioning, not navigation3 |

> **Navigation 3 note:** The `navigation-3` skill will be invoked in **Phase 4** to scaffold
> `AppNavHost.kt`. Phase 1 adds the Nav3 *library* dependencies only.

---

## Tasks

### Task 1 — Update `gradle/libs.versions.toml`

**New `[versions]` entries:**

```toml
ksp = "2.3.7"
room = "2.7.2"
datastore = "1.2.1"
kotlinxSerializationJson = "1.11.0"
navigation3 = "1.1.1"
```

**New `[libraries]` entries:**

```toml
# Room
room-runtime  = { group = "androidx.room", name = "room-runtime",  version.ref = "room" }
room-ktx      = { group = "androidx.room", name = "room-ktx",      version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# DataStore
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# kotlinx.serialization
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerializationJson" }

# Navigation 3
navigation3-runtime             = { group = "androidx.navigation3", name = "navigation3-runtime",             version.ref = "navigation3" }
navigation3-ui                  = { group = "androidx.navigation3", name = "navigation3-ui",                  version.ref = "navigation3" }
lifecycle-viewmodel-navigation3 = { group = "androidx.lifecycle",   name = "lifecycle-viewmodel-navigation3", version.ref = "navigation3" }
```

**New `[plugins]` entries:**

```toml
ksp                  = { id = "com.google.devtools.ksp",                   version.ref = "ksp"    }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

---

### Task 2 — Update root `build.gradle.kts`

Add two plugin declarations (with `apply false`) alongside the existing ones:

```kotlin
alias(libs.plugins.ksp) apply false
alias(libs.plugins.kotlin.serialization) apply false
```

---

### Task 3 — Update `app/build.gradle.kts`

**Apply new plugins:**

```kotlin
alias(libs.plugins.ksp)
alias(libs.plugins.kotlin.serialization)
```

**Add dependencies:**

```kotlin
// Room
implementation(libs.room.runtime)
implementation(libs.room.ktx)
ksp(libs.room.compiler)

// DataStore
implementation(libs.datastore.preferences)

// kotlinx.serialization
implementation(libs.kotlinx.serialization.json)

// Navigation 3
implementation(libs.navigation3.runtime)
implementation(libs.navigation3.ui)
implementation(libs.lifecycle.viewmodel.navigation3)
```

---

### Task 4 — Create `DrinkWatchApplication.kt`

**Path:** `app/src/main/java/com/example/drinkwatch/DrinkWatchApplication.kt`

```kotlin
package com.example.drinkwatch

import android.app.Application

class DrinkWatchApplication : Application() {
    // Singleton repositories and database will be initialized here in Phases 2–3.
}
```

---

### Task 5 — Register in `AndroidManifest.xml`

Add `android:name=".DrinkWatchApplication"` to the `<application>` element in
`app/src/main/AndroidManifest.xml`.

---

### Task 6 — Verify build

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three must pass with no errors and no new lint warnings.

---

### Task 7 — Update `README.md`

Change Phase 1 status from ⬜ to ✅ and add a brief summary of what was done.
