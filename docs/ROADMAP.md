# UNO Simple — Roadmap

Phases are ordered by dependency, not deadline (hobby project — ship when ready). Each phase has a clear definition of done.

---

## Phase 0 — Setup & scaffolding *(in progress)*

**Goal:** KMP project builds, docs in place, desktop preview + Wasm preview both run, git initialized.

- [x] Decisions captured (see [DECISIONS.md](DECISIONS.md))
- [x] Documentation scaffold (CLAUDE.md, ROADMAP, ARCHITECTURE, DECISIONS, SESSION_LOG, TODO, README, KMP_SETUP)
- [x] JDK 21 (Android Studio JBR) confirmed
- [x] Android SDK confirmed (`C:\Users\meviv\AppData\Local\Android\Sdk`)
- [x] Gradle wrapper at 8.11.1
- [x] `settings.gradle.kts`, root `build.gradle.kts`, version catalog written
- [x] `shared` module (Android + Desktop/JVM + iOS + Wasm targets) with placeholder engine + first test
- [x] `composeApp` module (same targets) with minimal "UNO Simple — Phase 0" screen
- [x] Platform entry points: `MainActivity` (Android), `Main.kt` (Desktop), `Main.kt` + `index.html` (Wasm), `MainViewController.kt` (iOS stub)
- [ ] First `./gradlew :shared:desktopTest` passes (verifies toolchain + engine test)
- [ ] First `./gradlew :composeApp:assembleDebug` succeeds (Android build sanity)
- [ ] First `./gradlew :composeApp:run` launches the Desktop app
- [ ] First `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` serves the Wasm app in Chrome at localhost:8080
- [ ] `git init`, `.gitignore` (written — unverified until git init), first commit

**Done when:** A Claude in a fresh session can `./gradlew :shared:desktopTest && ./gradlew :composeApp:run` and see the placeholder screen on desktop.

---

## Phase 1 — Game engine (pure Kotlin) *(complete)*

**Goal:** Fully tested UNO rule engine inside `shared/src/commonMain/`, zero Compose/Android/iOS dependency.

- [x] `CardColor` enum (RED, YELLOW, GREEN, BLUE — wild cards carry color=null)
- [x] `Card` sealed interface: `NumberCard`, `SkipCard`, `ReverseCard`, `DrawTwoCard`, `WildCard`, `WildDrawFourCard`
- [x] `Deck.shuffled(random)` — deterministic seeded shuffle of the 108-card deck
- [x] `Player` data class (hand, hasCalledUno, score, derived handScore)
- [x] `GameState` data class (players, drawPile, discardPile, currentPlayerIndex, direction, activeColor, winnerId)
- [x] `GameAction` sealed interface: `PlayCard(chosenColor, declareUno)`, `DrawCard` (v1 simplification — Call-UNO folded into PlayCard, no challenge/stacking actions yet)
- [x] `Engine.applyAction(state, action, random) -> ActionResult` pure function
- [x] Rule enforcement: legal-move validation, color/value matching, skip/reverse/draw-2/wild/wild-draw-4 effects, turn direction (Reverse = Skip with 2 players)
- [x] UNO-declaration penalty (2-card penalty when declareUno = false on the second-to-last card)
- [x] Reshuffling discard → draw pile when the draw pile runs out
- [x] `newRound(seats, random)` factory — deals 7-card hands and a valid NumberCard starter
- [x] All models `@Serializable` with `kotlinx.serialization`; JSON round-trip tests for every type (12 tests)
- [x] Unit tests for every rule path — 24 Engine tests plus per-model tests
- [x] Property-based tests: 4 fuzz properties × 50 random seeds × up to 1000 steps each (card-count preservation, currentPlayerIndex in range, top-discard/activeColor invariant, games eventually terminate)

**Deferred to later phases** (roadmap-tracked v1 simplifications):
- [ ] Wild Draw Four challenge rule (classic: only legal when no matching color)
- [ ] Stacking +2 / +4
- [ ] "Play the card you just drew" — drawing ends your turn in v1
- [ ] Round-end scoring (winner adds opponents' hand values to their score) — fields exist, aggregation not yet computed
- [ ] First-flipped-is-an-action-card starter rules (Skip / Reverse / DrawTwo / Wild on first flip)

**Done when:** `./gradlew :shared:desktopTest` shows 88+ tests green. ✓ (as of 2026-04-19)

---

## Phase 2 — AI bots *(complete)*

**Goal:** Single-player vs 1-3 CPU opponents with credible play.

- [x] `Bot` fun interface: `fun chooseAction(state: GameState, random: Random): GameAction`
- [x] `RandomBot` baseline — uniformly random legal action
- [x] `HeuristicBot` v1: always plays over drawing when possible, saves wilds, weaponizes action cards when next opponent is vulnerable (hand ≤ 2), prefers dominant-color cards, picks wild color by hand dominance, declares UNO correctly
- [ ] Difficulty levels (easy / normal / hard) via strategy tuning — deferred (current HeuristicBot is "normal")
- [x] Unit tests: both bots never return illegal actions (100 seeds × up to 1000 steps)
- [x] Unit tests: HeuristicBot beats RandomBot ≥ 60% in 200 head-to-head two-player games (seat-swap to remove first-player bias)

**Done when:** `./gradlew :shared:desktopTest` still green; tournament asserts HeuristicBot > RandomBot; bot legality fuzz passes. ✓ (as of 2026-04-19)

---

## Phase 3 — Basic UI (Compose Multiplatform) *(complete)*

**Goal:** Functional UI — can play a full game in desktop preview and Wasm browser preview end-to-end. Every screen is adaptive (phone / tablet / desktop) via Compose `WindowSizeClass` — Compact layouts first, Medium/Expanded skeletons wired up even if initially identical to Compact. See [../.claude/memory/feedback_adaptive_ui.md](../.claude/memory/feedback_adaptive_ui.md).

- [x] Root `WindowSizeClass` branching in `App()` (Compact / Medium / Expanded slots) — skeleton in `ui/adaptive/`, all screens currently render Compact
- [x] `HomeScreen`: start game button + 1/2/3 opponents picker + tagline (settings/rules link deferred)
- [x] `GameScreen`: player hand, opponents' card counts, draw pile, discard pile, turn indicator, direction indicator (↻/↺), active-color swatch
- [x] `CardView` composable with color/value rendering — Canvas-drawn icons for Skip/Reverse (font-independent on Wasm), text for number/Draw Two/Draw Four/Wild
- [x] Tap-to-play flow with legal-move highlighting (illegal cards dimmed, legal are clickable)
- [x] Wild color picker dialog (AlertDialog with four colored swatches)
- [x] `GameViewModel` holding `StateFlow<GameState>`, wrapping `Engine.applyAction`
- [x] Bot turn scheduling via coroutines (800ms pacing; re-checks state after each delay)
- [x] Round-end banner with score delta ("{name} won! +{delta}") + "New round" button
- [x] Explicit "UNO!" button UX — user must tap before playing second-to-last card or incur the 2-card penalty
- [x] Round-end score aggregation — winner collects sum of opponents' remaining hand values (engine-enforced)
- [x] Compose UI tests for each screen (17 tests across HomeScreen / AppNavigation / CardView / UnoButton)
- [x] Integration test: full bot-vs-bot game drives entire stack end-to-end (`GameViewModelBotVsBotTest`)
- [ ] Navigation via `androidx.navigation.compose` multiplatform — current simple `remember<Screen>` is enough for 2 screens; upgrade when we add Settings / Rules / Stats

**Done when:** I can click through a full game in desktop + Chrome (Wasm) and Compose UI integration tests pass. ✓ (113 tests green as of 2026-04-19)

---

## Phase 4 — Visual identity + polish *(shipped what Claude can ship without assets)*

**Goal:** Distinctive, high-polish look and feel.

**Tooling note:** [Claude Design](https://claude.ai/design) is still a great option if you want a more bespoke visual system — current theme is hand-coded.

- [x] Original `UnoTheme` — dark navy "card-table" background, warm gold primary, coral secondary, electric-cyan tertiary (UNO button). Replaces default Material 3 ColorScheme at the `App()` root.
- [x] Richer card palette (e.g., `#E63946` red, `#FFC857` yellow, `#38B000` green, `#2A9DF4` blue). Tilted white inner ellipse behind glyphs — classic card motif without copying Mattel.
- [x] Canvas-drawn action icons (Skip circle+slash, Reverse chase arrows, Wild star, direction indicator). Font-independent on Skia's Wasm runtime.
- [x] Staggered card deal animation (80ms stagger, 380ms slide+fade per index).
- [x] Discard-pile "land" animation on every card play (280ms scale-in).
- [x] UNO warning pulse (infinite 1.0 → 1.08x scale) until player taps.
- [x] `SettingsScreen` + `SettingsRepository` — sound toggle, haptics toggle, 0.25x–2x animation speed slider. In-memory for now.
- [x] `AudioService` interface + `SilentAudioService` no-op default.
- [x] `HapticsService` interface + `NoHapticsService` no-op default.
- [ ] Hand reorder animation — deferred; use `animateItemPlacement` when polishing further.
- [ ] Round-end celebration (particles / glow) — deferred.
- [ ] Actual sound effects — requires audio assets (files or runtime synthesis); deferred.
- [ ] Platform audio/haptics implementations — deferred until an Android or iOS device is available for testing.
- [ ] Golden / screenshot tests — deferred; Compose MP golden harness has setup complexity not worth solving alone.

**Done when:** Live preview reads as a game, not a productivity app. ✓

---

## Phase 5 — Online multiplayer *(architecture shipped; backend choice deferred)*

**Goal:** Play vs remote friends.

- [x] `GameSyncService` interface (state StateFlow, players StateFlow, submit / startRound / leave). Sealed `SubmitResult` for engine-level failures.
- [x] `PlayerSeat` data class for seat identity.
- [x] `InProcessSyncService` authoritative in-memory implementation — shared `Shared` backend + per-client observers, serialized with a Mutex. Full 3-player games run end-to-end through it.
- [x] Integration tests (6): state convergence across clients, authority rejection of spoofed / out-of-turn actions, full game via sync produces a winner, all clients observe identical final state.
- [ ] UI wiring: `LobbyScreen` + multiplayer-aware `GameScreen` — deferred until a real backend is chosen (doing UI against the in-process impl would be demo-only).
- [ ] **Real backend implementations** — not started. Each requires external infrastructure the user hasn't stood up:
  - Firebase Realtime Database + anonymous auth (fastest path)
  - Supabase Realtime + Postgres (open-source)
  - Ktor WebSocket relay (self-hosted, ~150 lines)
  - WebRTC DataChannels + signaling (lowest latency, per-platform wrappers)
  - See [MULTIPLAYER_BACKENDS.md](MULTIPLAYER_BACKENDS.md) for full tradeoffs.

**Done when:** Two browsers on different networks can play a round together. Requires backend setup.

---

## Phase 6 — iOS build & device polish *(source ready, needs Mac)*

**Goal:** Run on a real iPhone.

- [x] `iosMain` source set and Kotlin/Native targets (iosX64, iosArm64, iosSimulatorArm64) configured.
- [x] `MainViewController.kt` exposing `ComposeUIViewController { App() }` — the Swift-side entry point.
- [x] `kotlin.native.ignoreDisabledTargets=true` so Windows builds don't fail on the iOS targets.
- [x] [docs/IOS_SETUP.md](IOS_SETUP.md) — covers Option A (Mac), Option B (Codemagic free tier), Option C (TestFlight).
- [ ] `iosApp/iosApp.xcodeproj` — the Xcode wrapper. Must be generated on macOS; mirrors JetBrains' `compose-multiplatform-template/iosApp/`. Not in this repo.
- [ ] Real haptics via `UIImpactFeedbackGenerator` (in a future `IosHapticsService`).
- [ ] Real audio via `AVAudioPlayer` (in a future `IosAudioService`).
- [ ] App icon + launch screen
- [ ] iOS-specific UX tweaks (safe areas, gestures)
- [ ] TestFlight for personal installation (if user wants)
