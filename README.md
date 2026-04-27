# DrinkWatch

A party / drinking-session management app built with Jetpack Compose and Material 3.

## Technology Stack

| Concern | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Jetpack Navigation 3 |
| Local persistence (data) | Room + KSP |
| Local persistence (settings) | DataStore Preferences |
| JSON import/export | kotlinx.serialization |
| Async / reactive | Kotlin Coroutines + StateFlow/Flow |
| DI | Manual constructor injection via `Application`-scoped singletons |
| Build | AGP 9.2.0 · Kotlin 2.3.21 · compileSdk 37 · minSdk 24 |

## Building

```powershell
.\gradlew.bat assembleDebug lint testDebugUnitTest
```

All three must pass before a task is considered done.
