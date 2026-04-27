# DrinkWatch – Copilot Instructions

Always load skills configured in the repository before processing any requests.

## Development roadmap

- The original project specification is at @.specifications/APP_SPECIFICATION.md
- The detailed development plan is at @.specifications/DEVELOPMENT_PLAN.md, which breaks down the work into 14 phases with specific tasks and milestones for each phase.
- Each phase is expanded on in detail in their own markdown files at @.specifications/PHASE_1.md, @.specifications/PHASE_2.md, ..., @.specifications/PHASE_14.md
- The current implementation status is tracked in the project README at @README.md, which is updated as work progresses through the phases.

## Architecture

Single-module Jetpack Compose application (`com.example.drinkwatch`).

- **`MainActivity`** — sole entry point; sets up the Compose content host via `setContent`
- **`ui/theme/`** — Material3 theme package:
  - `Color.kt` — all color token definitions
  - `Type.kt` — typography scale
  - `Theme.kt` — `DrinkWatchTheme` composable; supports dynamic color (Android 12+) and dark/light modes
- **No XML layouts** — UI is 100% Jetpack Compose

## Before finishing any request

Always run the following before considering a task done:

Windows:

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

`gradlew` in the project root is the POSIX shell wrapper used on Linux/macOS/CI, and `gradlew.bat` is the Windows entry point. On Windows, use `.\gradlew.bat` for build commands; keep the root `gradlew` executable in git so Linux runners can invoke it.

### GitHub Actions CI

`.github/workflows/ci.yml` runs on pull requests and pushes to `master`. It uses JDK 17 (Temurin), `gradle/actions/setup-gradle@v4`, and executes `assembleDebug`, `lint`, and `testDebugUnitTest`. The workflow also uploads the debug lint HTML report as an artifact.

### Theme

`DrinkWatchTheme` defaults to dynamic color on Android 12+ (Material You). The fallback static scheme uses `Purple80`/`Purple40` family tokens defined in `Color.kt`.

### minSdk

The app targets `minSdk = 24`. Guard any API above 24 with `Build.VERSION.SDK_INT` checks (as is already done in `Theme.kt` for dynamic color at API 31+).
