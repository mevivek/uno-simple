# UNO Simple

A Kotlin Multiplatform + Compose Multiplatform implementation of UNO — hobby project, personal use, targeting Android / iOS / Desktop (JVM) / Web (Wasm).

> This is a personal-use project. "UNO" is a trademark of Mattel, Inc.; this app uses the name only locally and is not published commercially.

## Status

Early development. See [docs/ROADMAP.md](docs/ROADMAP.md) for phase progress.

## Quick start (dev)

```bash
# Engine unit tests (fastest)
./gradlew :shared:desktopTest

# Compose Desktop preview (opens a native window)
./gradlew :composeApp:run

# Wasm web preview in Chrome at http://localhost:8080
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Android debug build (requires emulator or device)
./gradlew :composeApp:installDebug
```

First-time setup (JDK, Android SDK): see [docs/KMP_SETUP.md](docs/KMP_SETUP.md).

## Tests

```bash
./gradlew :shared:allTests                     # engine tests on all targets
./gradlew :composeApp:desktopTest              # Compose UI tests (JVM)
./gradlew :composeApp:connectedAndroidTest     # Android instrumented tests
```

## Architecture

Pure-Kotlin game engine in `shared/commonMain/` with a Compose Multiplatform UI in `composeApp/`. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## For Claude Code sessions

Read [CLAUDE.md](CLAUDE.md) first. Then [.claude/memory/MEMORY.md](.claude/memory/MEMORY.md), [docs/SESSION_LOG.md](docs/SESSION_LOG.md), and [TODO.md](TODO.md).
