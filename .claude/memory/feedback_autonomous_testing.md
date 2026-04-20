---
name: Autonomous testing and debugging
description: UNO project must support running/debugging the app end-to-end without user intervention via comprehensive test coverage and browser automation.
type: feedback
---

The UNO Simple project must be set up so I (Claude) can run, verify, and debug the app end-to-end **without user intervention**. Tests at every level plus browser automation for Wasm UI verification.

**Why:** User stated on 2026-04-19: "incorporate unit testing, integration testing and any other essential testing so that you can run app and debug without my intervention on your own." This means my autonomous verification loop has to work — write code, run tests, run the app, see the result, fix, rerun, all without pinging the user.

**How to apply:**

1. **Unit tests** (`shared/src/commonTest/`): cover every rule in the pure-Kotlin engine. Run via `./gradlew :shared:desktopTest` (JVM, fastest). Target 95%+ coverage.
2. **Multi-target tests** (`./gradlew :shared:allTests`): engine runs on Android + Desktop + Wasm + iOS (where builds are possible). Catches target-specific issues.
3. **Compose UI tests** (`composeApp/src/commonTest/` + `desktopTest`): `runComposeUiTest { ... }` drives composables programmatically. Run via `./gradlew :composeApp:desktopTest`.
4. **Screenshot tests**: add when we adopt Paparazzi / Roborazzi / Compose Desktop capture. Lock in visual correctness.
5. **Android instrumented tests** (`./gradlew :composeApp:connectedAndroidTest`): for flows that need real Android behavior. Requires running emulator.
6. **Build sanity**: `./gradlew :composeApp:assembleDebug :composeApp:compileKotlinDesktop :composeApp:compileKotlinWasmJs` — all three must succeed before a "done" declaration.
7. **Autonomous browser verification**: use the `Claude_Preview` or `Claude_in_Chrome` MCP tools against the Wasm dev server:
   - Start: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` in background
   - Navigate to `http://localhost:8080`
   - Take screenshots for visual verification
   - Read console for runtime errors
   - Click/type to drive game actions

**Autonomous debug loop** (canonical flow to use when verifying changes):

```
1. ./gradlew :shared:desktopTest                         # engine unit tests (seconds)
2. ./gradlew :composeApp:desktopTest                     # Compose UI tests
3. ./gradlew :composeApp:assembleDebug                   # Android build sanity
4. ./gradlew :composeApp:wasmJsBrowserDevelopmentRun     # serve Wasm build (background)
5. Claude_Preview: navigate localhost:8080, screenshot, read console
6. If broken → read stack trace, fix root cause, go to 1
7. If green → commit and update SESSION_LOG.md
```

Never ask the user "can you check this?" for anything verifiable by this loop. Ask only for subjective things (does this design feel right?) or things the loop can't check (real-device haptics, iOS behavior, App Store).
