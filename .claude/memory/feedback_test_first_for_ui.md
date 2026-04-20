---
name: Write UI tests alongside every UI change
description: Never ship UI code without corresponding Compose UI tests in the same commit — the manual preview isn't a substitute for automated click-through coverage.
type: feedback
---

Every UI change (new screen, new interaction, wired-up ViewModel) must ship with Compose UI tests in the **same commit**. Do not rely on the manual Wasm preview to verify correctness. The preview is for visual QA; automated tests are for behavior coverage.

**Why:** On 2026-04-19 the user pointed out that I had queued "step G: Compose UI tests" but kept shipping UI code without them. Clicking Start Game froze the live preview on Wasm — a bug that a `runComposeUiTest { setContent { App() }; onNodeWithTag("start").performClick(); onNodeWithTag("game_screen").assertIsDisplayed() }` would have caught instantly. When I finally wrote the test, it surfaced that bug **plus** a second one (missing `ViewModelStoreOwner`) within the same run.

**How to apply:**

- Every PR/commit that touches `composeApp/` ships corresponding tests in `composeApp/src/commonTest/`. No exceptions.
- Use stable `Modifier.testTag(TestTags.*)` identifiers on everything an assertion might target — do not couple tests to user-visible copy.
- Canonical test layers, from lightest to heaviest:
  - **Screen-level** (`runComposeUiTest { setContent { HomeScreen(...) } }`) — renders a single composable with stubbed callbacks; asserts layout + lambdas firing.
  - **App-level** (`setContent { App() }`) — click-through smoke test; navigates between screens, verifies no exceptions.
  - **ViewModel-level** (`runTest { ... }` with `Dispatchers.setMain(StandardTestDispatcher())`) — drives the whole VM + engine + coroutine scheduler. For full-game coverage, use `humanPlayerId = null` all-bots mode and `advanceUntilIdle()` until `winnerId` is set.
- Test dispatcher setup: `Dispatchers.setMain(StandardTestDispatcher())` in `@BeforeTest`, `Dispatchers.resetMain()` in `@AfterTest` for any test that constructs `GameViewModel` — `viewModelScope` crashes without a Main dispatcher.
- Run `./gradlew :composeApp:desktopTest` **before** every commit that touches UI. Green tests are the stopping criterion, not a green screenshot.
- If a new bug is only reproducible in the live preview, the right response is to write a test that reproduces it, watch it fail, fix the code, watch it pass.

**Specifically for this project:**

- `composeApp/src/commonTest/kotlin/com/vivek/unosimple/AppNavigationTest.kt` — the baseline smoke test. Don't let it go missing.
- `composeApp/src/commonTest/kotlin/com/vivek/unosimple/viewmodel/GameViewModelBotVsBotTest.kt` — the integration backbone. Lives as a regression guard for the coroutine scheduler, determinism, and win-condition handling.
- Testing Compose Wasm is also valuable long-term (patrol or browser-driver), but desktop test target runs fastest and catches 90% of issues; use it as the default for every PR.
