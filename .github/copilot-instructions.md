# DrinkWatch – Copilot Instructions

## Build, test, and lint

All commands use `gradlew.bat` on Windows (not `gradlew`).

```powershell
# Build
.\gradlew.bat assembleDebug

# Lint
.\gradlew.bat lint
# Report: app/build/reports/lint-results-debug.html

# Unit tests (JVM, no device needed)
.\gradlew.bat testDebugUnitTest

# Run a single unit test class
.\gradlew.bat testDebugUnitTest --tests "com.example.drinkwatch.ExampleUnitTest"

# Instrumented tests (requires connected device/emulator)
.\gradlew.bat connectedDebugAndroidTest
```

Deploy and launch on emulator via the `android` CLI:
```powershell
android emulator start Medium_Phone        # start emulator (wait for "success!")
android run --apks app/build/outputs/apk/debug/app-debug.apk
```

Inspect the running app:
```powershell
android layout --pretty     # UI element tree (preferred for debugging)
android screen capture -o screen.png   # screenshot fallback (e.g. for WebViews)
```

## Architecture

Single-module Jetpack Compose application (`com.example.drinkwatch`).

- **`MainActivity`** — sole entry point; sets up the Compose content host via `setContent`
- **`ui/theme/`** — Material3 theme package:
  - `Color.kt` — all color token definitions
  - `Type.kt` — typography scale
  - `Theme.kt` — `DrinkWatchTheme` composable; supports dynamic color (Android 12+) and dark/light modes
- **No XML layouts** — UI is 100% Jetpack Compose
- **No navigation library, ViewModel, or data layer yet** — project is in early scaffolding state

## Before finishing any request

Always run the following before considering a task done:

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three must pass with no errors and no new lint warnings introduced.

## Key conventions

### Dependencies
All versions are managed through the version catalog at `gradle/libs.versions.toml`. Add new libraries there before referencing them in `build.gradle.kts` via `libs.*` aliases. Do not hardcode versions directly in build files.

### Colors
Define colors in `app/src/main/java/com/example/drinkwatch/ui/theme/Color.kt`, not in `res/values/colors.xml` (that file does not exist — Compose theming is used exclusively). Hex color literals use uppercase letters: `Color(0xFF6650A4)`, not `Color(0xFF6650a4)`.

### `gradlew` on Windows
`gradlew` (no extension) in the project root is a Windows PE executable shim that delegates to `gradlew.bat`. It exists so the `android describe` CLI tool works on Windows. Do not replace it with the standard Unix shell script. Always use `.\gradlew.bat` directly for build commands.

### Theme
`DrinkWatchTheme` defaults to dynamic color on Android 12+ (Material You). The fallback static scheme uses `Purple80`/`Purple40` family tokens defined in `Color.kt`.

### minSdk
The app targets `minSdk = 24`. Guard any API above 24 with `Build.VERSION.SDK_INT` checks (as is already done in `Theme.kt` for dynamic color at API 31+).
