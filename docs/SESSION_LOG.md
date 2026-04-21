# Session log

One short entry per working session. Future Claude reads the last 3-5 entries to pick up where we left off. Newest entries go at the top.

Entry template:

```
## YYYY-MM-DD — short title
**Phase:** current phase
**Done:**
- bullet of what shipped
**Next up:**
- bullet of immediate next task
**Notes:** optional context that doesn't belong in a decision or memory
```

---

## 2026-04-20 — Premium arcade UI/UX revamp (builds .01 → .28)
**Phase:** end of the six-phase revamp from [.claude/plans/can-you-plan-a-keen-firefly.md](../.claude/plans/can-you-plan-a-keen-firefly.md). Everything except Medium/Expanded adaptive layouts shipped.

**Summary of the 24 deploys (2026-04-20.05 → .28):**

### Phase 1 — foundation
- Dark arcade theme rewrite (obsidian + midnight + amber + coral + neon + sky + violet) replacing the claymorph/cream palette
- Typography: Bungee (chunky arcade display) + Inter (body), OFL, bundled as Compose Resources
- `ClaySurface` / `ClayButton` repainted dark + rim-highlight; new `GhostButton`
- Motion primitives: `ShakeController`, `FlashController`, `Modifier.noiseBackground`
- `WebAudioService` on Wasm — synthesized SFX (card slap, UNO triad, win arpeggio, draw tick, penalty saw) via `AudioContext` oscillators and `@JsFun` interop. Muted state follows Settings toggle.
- Home + Settings reskinned; persona tiles + amber PLAY slab + ghost mode row

### Phase 2 + 2.5 — game surface
- Red UNO felt (radial orange-red → maroon + subtle noise speckle) + 140sp embossed UNO wordmark behind the discard
- Authentic Mattel-style `CardView`: flat saturated face + tilted white oval + huge italic glyph + mini corner glyphs, red card back with italic UNO wordmark
- `BigTableDirectionArrow` — chunky 260° amber arc with triangle arrowhead
- Screen flash on Wild +4 land (white) / +2 land (coral), opponent-tile shake on penalty stacks
- Denser 140-particle celebration in arcade palette
- `CallUnoDisc` — floating red disc bottom-right, pulses when eligible, OK state when declared
- `EmoteCorner` + Canvas-drawn reaction icons (smile / shock / clap / fire / think / party) replacing broken Unicode emojis
- Hotseat + PassDeviceOverlay polished

### Phase 3 — identity
- `Screen.Splash` / `Onboarding` / `AvatarPicker` added
- 1.4s brand splash: fan of 4 cards + "UNO SIMPLE" type-in
- Onboarding: name + avatar live preview + LET'S PLAY
- Profile + AvatarPicker (3×3 persona grid) linked from Settings / ACCOUNT
- `UserProfile` extended with `avatarId` + `hasSeenTutorial`
- Hidden Geet easter egg — typing "Geet" / "Gitanjali" triggers pink hearts rain overlay + auto-selects the private persona + shows "made with ❤ for you"
- Home also shows the coral "made with ❤ for you" line when the current profile is Geet
- "made with ❤ for geet" quiet credit line in About

### Phase 4 — info screens
- Rules screen: scrollable reference with live `CardView` demos for each action card + scoring section
- Full-screen `PauseOverlay` — Resume / How to play / Settings / Quit; menu icon now opens pause instead of quitting; `GameViewModel.pause()` / `resume()` gate the bot coroutine
- About screen: version, build stamp, tech stack, OFL credits, repo + deploy URLs

### Phase 5 — progress + celebration
- `HistoryRepository` — append-only `RoundRecord` log with per-record delete + `stats(humanId)` aggregate
- `AchievementRepository` + `Achievement` sealed enum (7 badges) in `:shared` — evaluated on every round-end, newly-unlocked set exposed via `GameViewModel.recentUnlocks`
- Stats screen: Canvas win-rate ring, streak / best / big-win chips, opponents-faced panel
- Achievements screen: 2-column grid of Canvas gold star medals (locked = slate)
- Round result fully redesigned: full-screen, staggered row entries, gold/silver/bronze Canvas medals, hand-reveal (losers' remaining cards shown face-up), achievement-unlocked banner, NEXT ROUND + BACK TO HOME CTAs

### Phase 6 — online + operability
- `ConnectionBadge` on `.info/connected` (real websocket state — wasm + non-wasm); "LIVE" / "RECONNECTING" / "OFFLINE" with pulsing amber dot
- Online waiting room: big digit-boxes for room code with tap-to-copy, seat-count header, pulsing "WAITING FOR HOST", self-seat amber-highlighted with (YOU) label
- `EmoteFeed` + broadcasting via `GameSyncService.emoteEvents` + `broadcastEmote()`. Implemented on InProcess (hotseat), Wasm Firebase (single-slot `/rooms/{code}/latestEmote` with random nonce so repeats fire), and non-wasm Firebase.
- `users/{uid}/displayName` + `users/{uid}/avatarId` mirror written on joinSeat — Firebase console now shows a `users/` tree populated by actual play.
- `PlayerSeat` gained optional `avatarId`; `App.kt` threads profile.avatarId into the seat on online join.
- OnlineGameScreen active play gained the full solo overlay stack (red felt + flight + flash + SFX + CALL UNO disc + direction arrow).
- Reusable `EmptyState` / `ErrorPanel` / `LoadingPanel` composables for graceful empty / failure surfaces.

### Out-of-plan niceties that landed
- Back-stack navigation (`App.kt` maintains a `mutableListOf<Screen>` + `pushTo` / `goBack` helpers) — every back returns to the real previous screen instead of a hardcoded target; Settings / Rules / About / Stats / Achievements navigation is context-correct everywhere.
- `/admin` URL deep-links into an admin panel listing users + game sessions + history with per-row + bulk delete; two-tap-to-confirm on every destructive button. Invisible from any menu path.
- Fixed Mattel trademark rule in `CLAUDE.md` (rescinded — app is strictly personal-device).
- Z-order fix on Settings / About / Rules / Stats / Achievements — back button moved to the end of the outer Box so the scrolling Column doesn't swallow pointer taps.
- Reverse card glyph + big table arrow geometry rewrites (the arrowhead was off-by-90° for CCW; fixed).
- GitHub: repo made public at [github.com/mevivek/uno-simple](https://github.com/mevivek/uno-simple), history orphan-commit-rewritten to scrub work-related context from [.claude/memory/](.claude/memory).

**Next up:**
- Adaptive Medium/Expanded layouts (tablet + landscape phone ring-of-opponents) — deferred per user.
- F2 friends (friend codes, add-friend, friend list, invite-to-room).
- Real asset-backed audio on Android/iOS (deferred — needs WAV files).

**Notes:**
- Firebase RTDB rules are still in default test mode (public read/write) — fine for private friend groups, should be hardened before any real public play.
- Wasm Firebase stores seats + state as opaque JSON strings (side effect of `unoDbSet` taking a string) — functionally correct but ugly in the Firebase console. Users node writes as proper sub-paths.
- Every destructive admin action uses two-tap confirmation; there's no hidden "are you sure" modal layer that needs maintenance.

---

## 2026-04-19 (part 8) — Firebase online multiplayer fully wired
**Phases:** 5+. Online multiplayer is now live on Android / iOS / Desktop (Wasm gracefully unsupported for now).
**Done:**

### Firebase project + config (via CLI, no console clicks)
- Installed `firebase-tools` (npm global).
- `firebase login` (one-time OAuth in user's cmd.exe terminal).
- `firebase projects:create uno-simple-5757a3 --display-name "UNO Simple"`.
- `firebase apps:create ANDROID "UNO Simple Android" --package-name com.vivek.unosimple` → app id `1:79384481367:android:f0b8391cb653470b389d12`.
- `firebase apps:create WEB "UNO Simple Web"` → app id `1:79384481367:web:8cc0cedb67c9055b389d12`.
- `firebase apps:sdkconfig ANDROID … --out composeApp/google-services.json`.
- `firebase apps:sdkconfig WEB …` → pasted into `composeApp/src/wasmJsMain/resources/firebase-config.js`.
- User clicked through Realtime Database creation in the console (one step the CLI doesn't automate).

### Gradle + source sets
- `gradle.properties`: `firebase.enabled=true` toggle.
- `composeApp/build.gradle.kts`: google-services plugin applied conditionally on the property.
- `applyDefaultHierarchyTemplate()` added for iosMain intermediate.
- New `nonWasmMain` source set (dependsOn commonMain; androidMain/iosMain/desktopMain dependsOn it) hosts Firebase deps. Wasm excluded because dev.gitlive has no Wasm variant.

### Code
- `commonMain/multiplayer/FirebaseSyncFactory.kt` — `expect fun createFirebaseSyncService(...)` + `expect val firebaseSupported`.
- `nonWasmMain/.../FirebaseSyncFactory.nonWasm.kt` — returns real `FirebaseSyncService`, `firebaseSupported = true`.
- `wasmJsMain/.../FirebaseSyncFactory.wasmJs.kt` — returns null, `firebaseSupported = false`.
- `nonWasmMain/.../FirebaseSyncService.kt` — real `dev.gitlive:firebase-database` implementation.
- `commonMain/viewmodel/OnlineGameViewModel.kt` — thin VM wrapping a `GameSyncService`.
- `commonMain/ui/lobby/OnlineLobbyScreen.kt` — name + create-room (6-char code, no 0/O/1/I/L) + join-room.
- `commonMain/ui/game/OnlineGameScreen.kt` — reuses TopBar / OpponentRow / TableCenter / HumanHand / CelebrationOverlay; adds a pre-round waiting room where the host sees seats populate + a "Start round" CTA.
- `HomeScreen` gets a second outlined button — disabled + labelled "Online (web not supported)" on Wasm, active otherwise.
- `index.html` imports `firebase-config.js` and calls `initializeApp` before `composeApp.js`.
- `Screen.OnlineLobby` + `Screen.Online(displayName, roomCode, isHost)` added to sealed nav; `App.kt` gains `OnlineNotSupportedScreen` fallback.

### Verified
- All three targets compile: Android Debug, Desktop JVM, Wasm JS.
- `131 tests green` (shared + composeApp desktopTest).
- Wasm preview renders HomeScreen with the new disabled Online button correctly.

### Runtime testing (user's todo)
Online multiplayer hasn't been exercised end-to-end against real Firebase yet — that requires two desktop instances (or Android devices). Expected flow:
1. Player A on Desktop: taps Online multiplayer → Create room → gets 6-char code.
2. Player B on Desktop: taps Online multiplayer → Join room → enters A's code.
3. A sees B in the waiting room → taps Start round.
4. State syncs via Firebase RTDB; each plays their turn.

### Notes
- Firebase free tier (Spark plan) covers hobby usage many times over.
- Firebase rules are in default test mode (public read/write) — fine for private friend groups, should be hardened before public release.
- Wasm Firebase support: when dev.gitlive adds it, move FirebaseSyncFactory back to commonMain and delete the wasm/nonWasm split.

---

## 2026-04-19 (part 7) — Phase 4 + 5 + 6: polish, multiplayer architecture, iOS docs
**Phases:** 4 (visual polish) + 5 (multiplayer) + 6 (iOS) — all shipped to the limit of what Claude can do without external infrastructure.
**Done:**

### Phase 4 — visual identity + polish
- **UnoTheme** — dark navy "card-table" Material 3 ColorScheme, warm gold primary, coral secondary, electric cyan tertiary. Replaces default Material 3 at the App() root.
- **CardView** overhaul: richer card palette, tilted inner white ellipse behind glyphs, Canvas-drawn icons for Skip / Reverse / Wild star / direction arrow (font-independent on Skia Wasm).
- **Animations**: staggered card deal (80ms/index, 380ms each), discard pile "land" scale-in (280ms), UNO button pulse (infinite 1.0→1.08x).
- **Rules fix from Mattel PDF**: UNO declaration penalty 2→4 cards. ADR-017 cites the instruction sheet as canonical source.
- **Round-end scoring**: winner collects sum of opponents' remaining hand values. `Player.score` accumulates. Banner shows "+{delta}".
- **Explicit "UNO!" button**: replaces auto-declaration. User taps before playing second-to-last card or incurs the 4-card penalty.
- **SettingsScreen** + **SettingsRepository** (in-memory): sound/haptics toggles, 0.25x–2x animation speed slider.
- **AudioService** + **HapticsService** interfaces with no-op defaults. Real impls deferred until platform assets are ready.
- New rule memory: `feedback_game_aesthetic.md` — UI must feel like a game, not a productivity app.

### Phase 5 — multiplayer architecture + hotseat UI
- **GameSyncService** interface + **PlayerSeat** + **SubmitResult** sealed class. Single integration point for any multiplayer backend.
- **InProcessSyncService**: authoritative in-memory reference implementation with 6 integration tests (state convergence across clients, authority rejection, full-game via sync).
- **FirebaseSyncService** stub: returns NotConnected; each method's KDoc has the exact `dev.gitlive:firebase-database` call to paste once Firebase is configured.
- **HotseatGameViewModel** + **LobbyScreen** + **HotseatGameScreen**: local 2-4 player mode on one device with a pass-device overlay between turns. Works immediately with InProcessSyncService; swap one line for real-internet multiplayer.
- Extracted `TopBar` / `OpponentRow` / `TableCenter` / `HumanHand` / `WildColorPickerDialog` / `DirectionIndicator` from private to `internal` so both solo + multiplayer screens share the same widgets.
- Docs: `MULTIPLAYER_BACKENDS.md` (Firebase / Supabase / Ktor / WebRTC tradeoffs), `FIREBASE_SETUP.md` (step-by-step Firebase console + config files + Gradle wiring).

### Phase 6 — iOS
- Kotlin/Native iOS source set, targets (iosX64 / iosArm64 / iosSimulatorArm64), and `MainViewController.kt` all in place.
- `IOS_SETUP.md`: Mac / Codemagic free tier / TestFlight paths.
- Remaining: `iosApp/iosApp.xcodeproj` Xcode wrapper — must be generated on macOS.

### Tests
- **Engine (shared)**: 94 tests green — unchanged except for updated UNO penalty assertion (4 cards instead of 2) and 2 new scoring tests (+ adjusts EngineTest count 24 → 26).
- **composeApp UI + VM + multiplayer**: 35 tests green — AppNavigation (2), HomeScreen (4), CardView (4), UnoButton (3), SettingsScreen (7), LobbyScreen (3), HotseatGameScreen (2), GameViewModelBotVsBot (4), InProcessSyncService (6).
- **Total: 129 green** across all modules. Android APK builds, Wasm bundles.

**Next up:**
- Real sound assets + platform audio impls (Android `MediaPlayer` / iOS `AVAudioPlayer` / desktop `javazoom`).
- Real haptics via `UIImpactFeedbackGenerator` (iOS) and `VibratorManager` (Android).
- Golden tests — deferred until Compose MP golden infrastructure stabilizes.
- Firebase swap-in: user creates Firebase project → follows FIREBASE_SETUP.md → 1-line change in App.kt to use FirebaseSyncService instead of InProcessSyncService.
- iOS build on Mac / Codemagic.

**Notes:**
- Every multiplayer backend choice (Firebase / Supabase / Ktor relay / WebRTC) has a documented path in MULTIPLAYER_BACKENDS.md. User picked Firebase; FirebaseSyncService template + setup doc ready. The actual Firebase project creation + google-services.json download is the user's one-time setup that I can't do for them.
- The existing GameScreen composable works unchanged with the new internal widgets; no visible regressions.

---

## 2026-04-19 (part 6) — Phase 3 fully complete: polish, tests, and bug fixes
**Phase:** Phase 3 → **done**. Moving to Phase 4 (visual identity + polish).
**Done:**
- **Compose UI test harness wired up** (`compose.uiTest` + `kotlinx-coroutines-test` + `compose.desktop.uiTestJUnit4` on desktopTest). Stable `TestTags` object identifies every widget assertions might target.
- **17 composeApp tests** total:
  - `AppNavigationTest` (2): Home → Game → Menu → Home flow.
  - `HomeScreenTest` (4): renders correctly, start button reports bot count, chip selections flow through.
  - `CardViewTest` (4): every card type renders, enabled taps fire, disabled taps swallow.
  - `UnoButtonTest` (3): button hidden at handSize=7, visible+enabled at handSize=2, disabled after declaring.
  - `GameViewModelBotVsBotTest` (4): full 4-bot and 2-bot games finish with a winner under `advanceUntilIdle()`, determinism confirmed with fixed seeds, `clear()` cancels bot loop.
- **Two Wasm/Desktop production bugs found and fixed by tests**:
  - Missing `ViewModelStoreOwner` at app root (the actual cause of the Start-Game freeze).
  - `viewModelScope` needs `Dispatchers.Main` installed; test harness and production both addressed.
- **Skip/Reverse card icons now drawn with Compose Canvas** — Skia's default Wasm font didn't carry the original Unicode glyphs, so they rendered as placeholder boxes. Font-independent now.
- **Explicit "UNO!" button** replaces the auto-declaration. User taps before playing the second-to-last card or the engine applies the 2-card penalty. State consumed on each play. UI button disables after declaring to prevent double-tap noise.
- **Round-end scoring** — engine awards the winner the sum of losers' remaining hand values (numbers = face value, action cards = 20, wild cards = 50). Cumulative across rounds via `Player.score`. Round-over banner shows "+{delta}" for the round.
- **New memory rule**: [feedback_test_first_for_ui.md](.claude/memory/feedback_test_first_for_ui.md) — UI changes always ship with Compose UI tests in the same commit; manual preview isn't enough.
- **113 total tests green** (96 shared engine + 17 composeApp UI/VM).

**Next up:**
- Phase 4 kickoff (visual identity, animations, sound, haptics, settings). Need to decide starting priority — user may want to use Claude Design for the visual system, or iterate organically in Compose.

**Notes:**
- `GameScreen` signature now takes an optional `vm: GameViewModel` parameter so tests can inject pre-configured state. Production default uses the `GameViewModel.Factory`.
- `GameViewModel.startGame` now accepts `handSize` (default = 7) so tests can start with a 2-card opening for UNO-button coverage.
- `GameViewModel` supports all-bots mode (`humanPlayerId = null`) for the bot-vs-bot integration test and future playback/record tooling.

---

## 2026-04-19 (part 5) — Phase 3 mostly done: playable UI end-to-end
**Phase:** Phase 3 → steps A–F complete. Step G (Compose UI tests) still pending.
**Done:**
- Sealed-class navigation (`Screen.Home` / `Screen.Game(botCount)`) in `composeApp/commonMain/.../ui/Screen.kt`; App.kt wraps everything in `MaterialTheme` + `ProvideWindowSizeClass`.
- **HomeScreen** (`ui/home/`): title, 1/2/3 opponents picker (FilterChips), "Start game" pill button, top-anchored layout (fixed after live-preview review showed centered content left a big empty top).
- **GameScreen** (`ui/game/`): Menu button + turn indicator + direction arrow (↻/↺), opponent tiles with name/card-count/UNO flag (current player highlighted), draw-pile/discard-pile/active-color center table, horizontally-scrolling player hand with legal-move highlighting.
- **CardView**: solid-color card tile with glyph (number, ⊘ skip, ⇄ reverse, +2, ★ wild, +4), white border, clip+round corners, alpha dim when disabled. Phase 4 replaces the colors + typography.
- **GameViewModel** (`viewmodel/`): multiplatform `androidx.lifecycle.ViewModel` + `MutableStateFlow<GameState?>`. Actions: `startGame(botCount)`, `playCard(card, color?)`, `drawCard()`, `clear()`. Registers HeuristicBot; `runBotsUntilHuman()` launches a `viewModelScope` coroutine that loops with 800ms delays between bot moves, re-checking state after each delay so a `clear()` mid-wait bails cleanly.
- **Wild color picker dialog**: AlertDialog triggered when a wild is tapped; four big colored swatches + Cancel. HumanHand surfaces `onCardTap(card)` and GameScreen routes wild taps through the dialog.
- **New-round button**: wired on round-over to `vm.startGame(botCount)`.
- **Adaptive scaffold** (`ui/adaptive/WindowSizeClass.kt`): three-bucket enum + `LocalWindowSizeClass` + `ProvideWindowSizeClass { ... }` root using `BoxWithConstraints` for breakpoint detection. All screens currently render Compact — the branching skeleton is in place for Medium/Expanded additions later, per the adaptive-UI memory rule.
- **index.html fix**: removed the `<div id="root">` wrapper — ComposeViewport attaching to body was creating its canvas as a sibling to `#root`, which pushed the canvas down and produced a huge dark area at the top. Clean now.
- All three platform targets compile green: Desktop JVM + Android Debug + Wasm JS.
- Wasm dev preview at `localhost:8080` is the user's live view of every change. `feedback_live_preview_during_ui.md` memory rule established and honored.

**Next up:**
- Phase 3 step G: Compose UI tests (`composeApp/commonTest/`) and an integration test that drives a full game.
- Phase 4: visual identity + polish (original palette/typography, deal/flip animations, UNO warning pulse, sound, haptics on mobile). Consider [Claude Design](https://claude.ai/design) for generating the visual system.

**Notes:**
- Synthetic canvas clicks via `PointerEvent` don't reliably trigger Compose Multiplatform Wasm's Skiko pointer handlers — interactive verification needs real mouse clicks. `Claude_Preview` is great for screenshot-based layout verification; manual clicks handle navigation paths.
- Bundle size in dev server stayed at 4,103,637 bytes across several rebuilds. Likely a webpack-dev-server caching behavior; the compiled artifacts on disk are current (all commits are green through `:composeApp:compileKotlinWasmJs`). If the browser ever seems stale, stop/start the preview via `Claude_Preview.preview_stop` + `preview_start`.

---

## 2026-04-19 (part 4) — Phase 2 complete: AI bots
**Phase:** Phase 2 → **done**. Moving to Phase 3 (Basic UI / Compose Multiplatform).
**Done:**
- `shared/src/commonMain/kotlin/com/vivek/unosimple/engine/ai/`:
  - `Bot` fun interface (deterministic given `(state, random)`).
  - `RandomBot` (baseline).
  - `HeuristicBot` v1 with scoring-based card selection (saves wilds, weaponizes action cards against vulnerable opponents, prefers dominant-color plays, picks wild colors by hand dominance).
- **94 tests green total.** New: 5 BotLegality + 1 Tournament.
  - BotLegality fuzzes both bots over 100 seeds × up to 1000 steps; asserts never-illegal action + UNO-declaration contract + HeuristicBot's chosen wild color always matches hand dominance.
  - Tournament runs 200 two-player games with seat-swapping; asserts HeuristicBot win rate ≥ 60% and <5% of games hit the step cap.
- Difficulty levels (EASY/NORMAL/HARD) deferred; current HeuristicBot is "normal".

**Next up:**
- Phase 3: Compose Multiplatform UI. Concrete first step:
  1. Wire up `GameViewModel` in `composeApp/commonMain/.../viewmodel/` using `androidx.lifecycle.ViewModel` (multiplatform) + `MutableStateFlow<GameState>`.
  2. Root `WindowSizeClass` branching in `App()` (adaptive layout rule).
  3. Minimal `HomeScreen` + `GameScreen` with hand, discard/draw piles, turn indicator.
  4. AI turns scheduled via `viewModelScope.launch { delay(); dispatch(bot.chooseAction(...)) }`.

**Notes:**
- Bot legality tests intentionally reuse FuzzTest's invariant harness — both test classes drive random actions through the engine and watch for illegal outcomes. Different assertions, same infrastructure.
- HeuristicBot is currently deterministic-given-seed because random tiebreaks go through the injected `Random`. Good for reproducing losses in tests.

---

## 2026-04-19 (part 3) — Phase 1 complete: pure-Kotlin game engine
**Phase:** Phase 1 → **done**. Moving to Phase 2 (AI bots).
**Done:**
- Full classic-UNO rule engine in `shared/src/commonMain/kotlin/com/vivek/unosimple/engine/`.
  - Models: `Card` sealed interface (NumberCard, SkipCard, ReverseCard, DrawTwoCard, WildCard, WildDrawFourCard), `CardColor`, `Deck` (108-card Mattel deck + seeded shuffle), `Player`, `PlayDirection`, `GameState`, `GameAction` (PlayCard + DrawCard).
  - `Engine.applyAction(state, action, random) -> ActionResult`: pure, deterministic. Handles legal-move validation, Skip, Reverse (2-player = Skip), DrawTwo, Wild color choice, WildDrawFour, UNO-declaration penalty, draw-pile reshuffle, win detection.
  - `Rules.isLegalPlay` / `Rules.legalPlaysFor` — shared by engine + future bots.
  - `newRound(seats, random)` factory — deals 7-card hands and picks a NumberCard starter.
  - Every type `@Serializable` via kotlinx.serialization. Polymorphic discriminator tokens (`"number"`, `"skip"`, …) are pinned to guard wire-format API.
- **88 tests green** via `./gradlew :shared:desktopTest`, broken down:
  - 24 EngineTest (rule-by-rule coverage)
  - 4 FuzzTest properties × 50 random seeds each (card-count preservation, index-in-range, top-discard/activeColor invariant, eventual termination)
  - 12 SerializationTest (every type round-trips + discriminator regression guards)
  - 10 NewRoundTest
  - 12 GameStateTest + 9 DeckTest + 8 CardTest + 6 PlayerTest + 3 PlayDirectionTest
- All three platform targets still compile cleanly (Desktop JVM + Android + Wasm).
- Commits this session: `Phase 1: Card …`, `Phase 1: Player …`, `Phase 1: GameState + PlayDirection`, `Phase 1: GameAction, Rules, and Engine.applyAction`, `Phase 1: newRound + JSON round-trip + property-based fuzz`.

**Next up:**
- Phase 2: AI bots (`Bot` interface + `HeuristicBot` v1 in `shared/src/commonMain/kotlin/com/vivek/unosimple/engine/ai/`).
- Wire up `GameViewModel` (composeApp) that drives `Engine.applyAction` and schedules bot turns via coroutines.
- After both, Phase 3 (UI) can start.

**Notes:**
- v1 rule simplifications documented in `Engine.kt` KDoc and in ROADMAP.md Phase 1 "deferred" list.
- Fuzz tests are the backstop for "edge case we didn't think of" — they run quickly and are safe to keep in the default desktopTest suite.
- The engine is **fully playable today** from a CLI harness — Phase 2 and 3 just wire it into bots and Compose.

---

## 2026-04-19 (part 2) — KMP pivot + working scaffold
**Phase:** Phase 0 (setup & scaffolding) — effectively done modulo first commit
**Done:**
- **Pivoted from Flutter to KMP + Compose Multiplatform** (ADR-013). Reason: user's Kotlin/Android day-job stack makes KMP faster to develop with.
- Flutter SDK cloned at `C:\dev\flutter` but no longer used. User can delete it.
- JDK 21 (Android Studio JBR), Android SDK, Git, adb all verified present.
- Full KMP project scaffolded:
  - `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties`, `local.properties`, `gradle/libs.versions.toml` (Kotlin 2.1.0, Compose MP 1.7.3, AGP 8.7.3).
  - Gradle wrapper 8.11.1.
  - `shared` module with Android + Desktop (JVM) + iOS + Wasm targets. Pure-Kotlin engine placeholder + first passing `kotlin.test`.
  - `composeApp` module with the same four targets + minimal `App()` Composable showing "UNO Simple — Phase 0 — scaffold ready." on Material 3.
  - Platform entry points: `MainActivity` (Android), `Main.kt` (Desktop), `Main.kt` + `index.html` (Wasm), `MainViewController.kt` (iOS stub).
- `./gradlew :shared:desktopTest` — BUILD SUCCESSFUL in 1m 40s (first run), engine test passed.
- `./gradlew :composeApp:assembleDebug :composeApp:compileKotlinDesktop :composeApp:compileKotlinWasmJs` — BUILD SUCCESSFUL in 1m 52s. All three targets compile (Android APK built, Desktop JVM compiles, Wasm JS compiles).
- Docs fully rewritten for KMP: CLAUDE.md, ROADMAP.md, ARCHITECTURE.md, DECISIONS.md (ADRs 013–016 record the pivot), README.md, TODO.md, KMP_SETUP.md (replaced FLUTTER_SETUP.md).
- Memory updated: `project_uno_decisions.md`, `feedback_latest_modern.md`, `feedback_autonomous_testing.md`, `user_profile.md`.
- `.claude/settings.json` updated: allowed Gradle/adb commands, still denies all work MCPs.

**Next up:**
- Verify Wasm browser preview end-to-end: start `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` in background, screenshot via `Claude_Preview` at http://localhost:8080.
- `git init` (no history yet), first commit.
- Kick off Phase 1 (engine: `Card`, `Deck`, `GameState`, `GameAction`, `applyAction` + tests).

**Notes:**
- KMP iOS targets cannot build on Windows — expected; hidden via `kotlin.native.ignoreDisabledTargets=true`.
- Minor warnings about deprecated `ExperimentalWasmDsl` location (Kotlin 2.1) — non-blocking, can fix with an import later.
- `C:\dev\flutter` is leftover from the earlier Flutter install — safe to delete, not referenced anywhere.

**Wasm preview verified end-to-end (post-commit):**
- `.claude/launch.json` added with a `wasm-dev` configuration for `Claude_Preview` MCP integration.
- `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` served the app at `http://localhost:8080`.
- First-time Wasm build took ~3 min (npm install + Kotlin/Wasm compile + webpack bundle).
- Claude_Preview screenshot confirms Compose Multiplatform renders "UNO Simple" / "Phase 0 — scaffold ready." in Material 3.
- Autonomous dev loop working: tests, all-target compile, Wasm serve + screenshot + console + network inspection.

**Tooling note:** [Claude Design](https://claude.ai/design) (launched 2026-04-17) recorded in ROADMAP as an option for Phase 4 visual identity. Not used yet.

---

## 2026-04-19 (part 1) — Planning + initial (Flutter) scaffold
**Phase:** Phase 0
**Done:**
- All planning decisions captured via AskUserQuestion (see ADR-001 through ADR-012).
- Initial documentation scaffold written (Flutter-oriented).
- Memory migrated to project-local `.claude/memory/` so it travels across machines.
- `.claude/settings.json` created with project-scoped permissions and explicit `deny` list for all work-related MCPs.
- Flutter SDK cloned to `C:\dev\flutter` (later superseded by KMP pivot).

**Next up:** (superseded by part 2)
