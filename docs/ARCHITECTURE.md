# Architecture

## One-line version

```
Compose UI ── observes ──> StateFlow<GameState> (ViewModel)
                                     │
                                     ▼
                            engine.applyAction(state, action) ── returns ──> new GameState
                           (pure Kotlin, shared/commonMain, no UI deps)
```

The engine is a pure-Kotlin function library inside the `shared` module's `commonMain`. The UI is a Compose Multiplatform app in `composeApp`. The only coupling between them is the `GameState` and `GameAction` data types, which are `@Serializable` so they can travel over a network later.

## Layers

```
┌──────────────────────────────────────────────────────────────────┐
│  composeApp/  (Compose UI — screens, widgets, animations)        │
│                                                                  │
│  Composables observe StateFlow from ViewModels. Dispatch         │
│  actions via ViewModel methods. Never touch engine internals.    │
└──────────────────────┬───────────────────────────────────────────┘
                       │ collect / dispatch
┌──────────────────────▼───────────────────────────────────────────┐
│  ViewModels  (composeApp/commonMain/.../viewmodel/)              │
│                                                                  │
│  GameViewModel holds MutableStateFlow<GameState>. Methods wrap   │
│  engine.applyAction. Coroutine scheduler drives AI turns.        │
└──────────────────────┬───────────────────────────────────────────┘
                       │ calls pure functions
┌──────────────────────▼───────────────────────────────────────────┐
│  shared/commonMain/.../engine/  (PURE KOTLIN)                    │
│                                                                  │
│  Models:  Card, Deck, Player, GameState, GameAction              │
│  Rules:   validateAction, applyAction, scoreRound                │
│  AI:      Bot.chooseAction(state) -> GameAction                  │
│                                                                  │
│  No mutation, no I/O, no randomness except via injected Random.  │
│  Every type @Serializable.                                       │
└──────────────────────────────────────────────────────────────────┘
```

## Why the engine is pure Kotlin commonMain

1. **Testability.** No Compose/Android test harness needed for rule tests. Runs on `desktopTest` in under a second.
2. **Determinism.** Seed the injected `Random` and every game is reproducible.
3. **Portability.** Same code runs in a JVM server, an iOS app via Kotlin/Native, a Wasm browser build, or an Android app.
4. **Future multiplayer.** Server can authoritatively replay `GameAction` streams to validate clients — literally the same code.

## Data flow — one game turn

1. Compose widget detects a tap on a card in the player's hand.
2. Widget calls `viewModel.playCard(card)`.
3. `GameViewModel` constructs a `GameAction.PlayCard(card)` and calls `engine.applyAction(currentState, action)`.
4. The engine validates the action. Returns `Result.failure(Reason)` on invalid, `Result.success(newState)` on valid.
5. `GameViewModel` updates `MutableStateFlow<GameState>`. All composables collecting the flow recompose.
6. If the new current player is a bot, ViewModel launches a coroutine with a short delay, calls `bot.chooseAction(newState)`, and dispatches the result.

## Serialization

Every engine type is annotated `@Serializable` (from `kotlinx.serialization`). This gives us:
- `Json.encodeToString(state)` → wire-ready string
- `Json.decodeFromString<GameState>(json)` → parse back
- Stable schema via `@SerialName` where needed
- Free sealed-class handling for `Card` and `GameAction` subtypes

The moment we add networking, game state is already wire-ready.

## State management

- `MutableStateFlow<GameState>` inside ViewModels — Compose collects via `collectAsStateWithLifecycle()`.
- `ViewModel` from the multiplatform `lifecycle-viewmodel-compose` library (JetBrains port; works on all targets).
- No LiveData, no RxJava, no legacy APIs.

## Randomness

Engine functions that need randomness take a `Random` parameter. Production ViewModels pass `Random(seed)` with a time-based seed; tests inject `Random(fixedSeed)`. No top-level `Random.Default` calls inside `engine/`.

## Error handling

- Engine returns `kotlin.Result<GameState>` for actions that may fail (invalid moves, rule violations). Never throws from inside `engine/`.
- UI translates `Result.failure` into user-visible feedback (toast, card-shake, disabled button).
- Unexpected exceptions at the UI layer go to a root `CoroutineExceptionHandler` that logs and shows a recoverable error screen.

## Platform entry points

| Target | Entry file | Builds on |
|---|---|---|
| Android | `composeApp/src/androidMain/.../MainActivity.kt` | Windows / Mac / Linux |
| Desktop (JVM) | `composeApp/src/desktopMain/.../Main.kt` | Windows / Mac / Linux |
| Web (Wasm) | `composeApp/src/wasmJsMain/.../Main.kt` + `resources/index.html` | Windows / Mac / Linux |
| iOS | `composeApp/src/iosMain/.../MainViewController.kt` | **Mac only** |

The iOS target stub exists but cannot be built from Windows. Deferred to Phase 6.

## Testing strategy

| Layer | Tool | Target |
|---|---|---|
| Engine rules | `kotlin.test` in `commonTest` | 95%+ coverage |
| Engine fuzzing | Property-based tests with random seeds | Invariants hold across 10k+ games |
| ViewModels | `kotlinx.coroutines.test` + `runTest` | All state transitions |
| Composables | Compose UI Test (`runComposeUiTest`) | Every screen renders + basic interactions |
| Visual | Screenshot test (Paparazzi / Roborazzi / Compose Desktop capture) | Every stable UI state |
| Integration | `connectedAndroidTest` / Desktop JVM test | Start → play → end game flow |
| Build sanity | `./gradlew :composeApp:assembleDebug` / `compileKotlinDesktop` / `compileKotlinWasmJs` | All targets compile |

The autonomous debug loop in [../CLAUDE.md](../CLAUDE.md) uses these to self-verify without user intervention.
