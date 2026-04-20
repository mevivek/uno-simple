# UNO Simple — Claude Code context

This file is loaded automatically in every Claude session. Keep it current; it's the single source of truth for "how this project works."

## What this is

**UNO Simple** is a Kotlin Multiplatform + Compose Multiplatform card game — a hobby project, not funded, **not a work project**. Personal use only (UNO is trademarked; app is never going to the App Store under this name). Targets: **Android, iOS, Desktop (JVM), Web (Wasm)**. Dev loop primarily uses Desktop preview (fast) and Web (Wasm) preview in Chrome.

## Hard separation from work

This project must stay fully separated from the user's day job. **Do not call any work-related MCP tools** — Jira, Confluence, Gmail, Couchbase, BigQuery, internal source-search connectors — from this project. Denied in `.claude/settings.json`. See [.claude/memory/feedback_no_work_connections.md](.claude/memory/feedback_no_work_connections.md) for the full rule.

## Persistent project memory

Every session must read [.claude/memory/MEMORY.md](.claude/memory/MEMORY.md) first. It's the index of persistent rules, decisions, and user preferences that travel with the repo (not stored at the user level). When you learn anything worth remembering across sessions, save it there — never at the user's `~/.claude/` path.

## Core rules (MUST follow)

1. **Engine is pure Kotlin Multiplatform `commonMain`.** Code under `shared/src/commonMain/kotlin/com/vivek/unosimple/engine/` imports nothing from Compose, Android, iOS, or any platform package. Zero exceptions. Makes it testable, portable, and ready for server-side use if we add online play later.
2. **No external services in the standalone phases.** No Firebase, Supabase, Google Sign-In, analytics, push, remote config — nothing that needs a third-party account/API key/network call to a non-owned endpoint. Deferred to Phase 5+.
3. **Everything testable.** Target **95%+ coverage on `shared/src/commonMain/.../engine/`**. Every rule change ships with tests. UI tests best-effort (Compose UI tests for key flows, screenshot tests for critical layouts).
4. **Latest and modern.** Latest stable Kotlin, AGP, Compose Multiplatform, Gradle. Modern Kotlin idioms — sealed interfaces, data classes with copy, context receivers where useful, coroutines + Flow, `kotlinx.serialization`. Material 3 in Compose. No deprecated APIs.
5. **Deterministic RNG.** All randomness goes through a seeded `Random` injected into the engine. Tests inject a fixed seed; production uses `Random.Default` or a time-based seed.
6. **Serializable state.** Every engine type is JSON round-trippable via `kotlinx.serialization` (`@Serializable`). Future-proofs online sync without coupling to a backend now.
7. **Session-continuity hygiene.** Every session ends with: green tests, clean git working tree, updated `docs/SESSION_LOG.md`, updated `TODO.md`.

## Tech stack

| Concern | Choice |
|---|---|
| Framework | Kotlin Multiplatform + Compose Multiplatform (latest stable) |
| Language | Kotlin 2.1+ with modern idioms (sealed, data classes, coroutines, Flow) |
| Targets | Android, iOS, Desktop (JVM), Web (Wasm) |
| Build | Gradle 8.11+ with Kotlin DSL + version catalog (`gradle/libs.versions.toml`) |
| UI | Compose Multiplatform (Material 3) |
| State | `ViewModel` (multiplatform-lifecycle) + `StateFlow` / `MutableStateFlow` |
| Navigation | `androidx.navigation.compose` (multiplatform) |
| Models | Kotlin `data class` / `sealed interface` + `kotlinx.serialization` |
| Async | Kotlin Coroutines + Flow |
| Unit tests | `kotlin.test` (multiplatform, runs on all targets) |
| UI tests | Compose UI test (`runComposeUiTest`), screenshot tests (Paparazzi later) |
| Integration tests | Android instrumented via `connectedAndroidTest`, desktop integration via JVM test |
| Browser automation | `Claude_Preview` / `Claude_in_Chrome` MCP tools for web (Wasm) verification |

## Architecture (one-line version)

```
Compose UI (composeApp) ── observes ──> StateFlow<GameState> (ViewModel)
                                              │
                                              ▼
                                      engine.applyAction(state, action) ── returns ──> new GameState
                                              (pure Kotlin, shared module, commonMain)
```

Full detail in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Folder layout

```
uno-simple/
├── settings.gradle.kts              # module declarations
├── build.gradle.kts                 # root plugins
├── gradle.properties                # JVM + Kotlin + Compose settings
├── gradle/libs.versions.toml        # version catalog (single source of truth for versions)
├── gradle/wrapper/                  # gradle-wrapper.jar, .properties
├── gradlew / gradlew.bat            # wrapper scripts
├── shared/                          # PURE KOTLIN — engine + domain
│   └── src/
│       ├── commonMain/kotlin/com/vivek/unosimple/engine/
│       │   ├── models/              # Card, Deck, Player, GameState, GameAction
│       │   ├── rules/               # applyAction, validation, scoring
│       │   └── ai/                  # Bot strategies
│       ├── commonTest/kotlin/...    # engine unit tests
│       └── {android,ios,desktop,wasmJs}Main/  # platform stubs if needed
├── composeApp/                      # Compose Multiplatform UI + platform entry points
│   └── src/
│       ├── commonMain/kotlin/com/vivek/unosimple/
│       │   └── App.kt               # root @Composable
│       ├── commonTest/              # Compose UI tests
│       ├── androidMain/             # MainActivity, AndroidManifest
│       ├── iosMain/                 # MainViewController (Objective-C bridge)
│       ├── desktopMain/             # main() + Window
│       └── wasmJsMain/              # main() + ComposeViewport + index.html
└── docs/                            # ROADMAP, ARCHITECTURE, DECISIONS, SESSION_LOG, KMP_SETUP
```

## Current phase

See [docs/ROADMAP.md](docs/ROADMAP.md). Active session notes in [docs/SESSION_LOG.md](docs/SESSION_LOG.md). Short-term next-up items in [TODO.md](TODO.md).

## Decisions log

Every architectural decision goes in [docs/DECISIONS.md](docs/DECISIONS.md) as a dated ADR. Don't re-litigate silently; if we change an ADR, write a new one that supersedes the old.

## Dev workflow

- **Desktop preview (primary during dev):** `./gradlew :composeApp:run`
- **Web (Wasm) preview in Chrome:** `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` (serves at `http://localhost:8080`)
- **Android build + install on emulator/device:** `./gradlew :composeApp:installDebug`
- **Unit tests (engine):** `./gradlew :shared:desktopTest` (runs fastest)
- **All tests across targets:** `./gradlew :shared:allTests`
- **Compose UI tests:** `./gradlew :composeApp:desktopTest`
- **Android instrumented tests:** `./gradlew :composeApp:connectedAndroidTest` (requires running emulator)
- **Static check:** `./gradlew :shared:compileKotlin :composeApp:compileKotlin` (compile succeeds = passes)
- **Lint (when we add detekt):** TBD
- **Clean:** `./gradlew clean`

## Autonomous debug loop (use this when verifying your own changes)

Claude must verify changes end-to-end without asking the user. The loop:

```
1. ./gradlew :shared:desktopTest                        # engine unit tests (must be green, seconds)
2. ./gradlew :composeApp:assembleDebug                  # Android build sanity
3. ./gradlew :composeApp:wasmJsBrowserDevelopmentRun    # serve Wasm build
4. Claude_Preview / Claude_in_Chrome MCP:
     - navigate to http://localhost:8080
     - screenshot to verify layout
     - read console for runtime errors
     - click/type to exercise flows
5. If broken: read stack trace, fix root cause, go to 1
6. If green and user-visible: commit and update SESSION_LOG.md
```

Only ask the user for things the loop cannot verify: subjective design feel, real-device haptics, App Store behavior, things requiring their credentials.

## Testing expectations per feature

Every new feature (Composable, engine rule, ViewModel) ships with:
- **Unit tests** for pure logic (engine rules, state transitions, bot strategies)
- **Compose UI test** for rendered output / basic interactions
- **Screenshot test** for visual correctness where it matters (added when we adopt Paparazzi or Compose screenshot testing)
- **Integration coverage** where it's part of a full user flow
- **Updated SESSION_LOG.md** with what was added and how to verify

No feature is "done" until all applicable layers above are green.

## Don't do

- Don't import Compose, `androidx.*`, iOS APIs, or any platform code inside `shared/src/commonMain/.../engine/`.
- Don't add a library that requires an external account/API key without explicit approval.
- Don't use legacy state APIs (LiveData, RxJava) — stick to Flow + Compose State.
- Don't use deprecated APIs (old Compose Material, pre-M3 stuff).
- Don't commit without green `./gradlew :shared:desktopTest` and at least one target building.
- Don't pin the exact UNO visual trademark (Mattel red/yellow/green/blue + specific font + card back). Use an original design.
- Don't add features for hypothetical future requirements. YAGNI.

## How future sessions pick up

1. Read this file (automatic).
2. Read [.claude/memory/MEMORY.md](.claude/memory/MEMORY.md) — persistent preferences and rules.
3. Read [docs/SESSION_LOG.md](docs/SESSION_LOG.md) — what happened last time, what's next.
4. Read [TODO.md](TODO.md) — short-term queue.
5. Read [docs/ROADMAP.md](docs/ROADMAP.md) — which phase.
6. `git status && git log --oneline -10` — real state of the tree.
