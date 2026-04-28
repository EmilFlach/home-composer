# home-composer — Claude Code Instructions

## Project
Kotlin Multiplatform + Compose Multiplatform app.
Targets: Android, iOS, Desktop (JVM), Web (JS + WASM).
Modules: `:composeApp` (UI + platform entries), `:shared` (KMP library), `:server` (Ktor backend), `:dev` (preview dev server).

## Build & Test Strategy

**Rules:** Compile fast targets (JVM) first to catch errors. Never run `:build` (builds all platforms). Always compile → verify → fix.

| Changed | Compile | Speed |
|---------|---------|-------|
| `commonMain/` or `jvmMain/` (UI, shared logic) | `:composeApp:compileKotlinJvm` | 5-30s — **use for 95% of changes** |
| `androidMain/` | `:composeApp:compileDebugKotlin` | 15s-3m — requires device |
| `iosMain/` | `:composeApp:compileKotlinIosSimulatorArm64` | 20-60s |
| `wasmJsMain/` or `jsMain/` | `:composeApp:compileKotlinWasmJs` | 20-80s |
| `:shared` changes | `:shared:compileKotlinJvm` | 5-30s |
| `:server` changes | `:server:compileKotlin` | 5-15s |

## UI Validation

After making any UI changes, always validate using the compose-hot-reload MCP server:

1. Check the app is running: `mcp__compose-hot-reload__status`
2. Take a screenshot to verify the result: `mcp__compose-hot-reload__take_screenshot`
3. If the UI looks wrong, inspect the semantic tree for debugging: `mcp__compose-hot-reload__get_semantic_tree`

Never report a UI change as done without first taking a screenshot and confirming it looks correct.

## Code Structure

**Key paths in `composeApp/src/`:**
- `commonMain/kotlin/org/example/project/App.kt` — root Compose UI, navigation
- `androidMain/` — `MainActivity.kt`
- `jvmMain/` — `main.kt` (Desktop window)
- `iosMain/` — `MainViewController.kt`
- `webMain/` — `main.kt` (Web entry point)

**Key paths in `shared/src/commonMain/`:**
- `Greeting.kt`, `Platform.kt`, `Constants.kt`

## Stack
- Kotlin 2.3.20, Compose Multiplatform 1.10.3
- Ktor 3.4.1, kotlinx-coroutines 1.10.2
- Android: compileSdk 36, minSdk 24, jvmTarget JVM_11

## iOS — What Needs Implementing Twice

When adding a **new screen or entry point**, also update:
1. `composeApp/src/iosMain/.../MainViewController.kt` — Compose iOS bridge
2. `iosApp/iosApp/ContentView.swift` — SwiftUI wrapper
3. `iosApp/iosApp/iOSApp.swift` — if top-level routing changes
