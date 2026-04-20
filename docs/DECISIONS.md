# Architecture decision records

A superseding log. Each decision has a permanent ID (ADR-NNN). If we change direction, write a new ADR that explicitly supersedes the old one — don't silently edit history.

Format: **ADR-NNN** (date) — title — status — decision — why.

---

## ADR-001 (2026-04-19) — ~~Flutter + Dart~~
**Status:** superseded by ADR-013
**Original decision:** Build the app in Flutter with Dart.
**Why superseded:** User pivoted to KMP + Compose Multiplatform on the same day to leverage their Kotlin/Android experience.

## ADR-002 (2026-04-19) — Pure game engine (language-neutral principle)
**Status:** accepted (carried over to KMP — see ADR-014)
**Decision:** The game engine lives in its own pure module with zero UI dependency. All rules, models, and AI live there.
**Why:** Testability without the UI harness. Deterministic replays. Portable to server-side if we add authoritative multiplayer. Prevents state/rule logic from leaking into widgets/composables.

## ADR-003 (2026-04-19) — ~~Riverpod (Flutter)~~
**Status:** superseded by ADR-015
**Original decision:** Use Riverpod with code-gen for state.
**Why superseded:** Flutter replaced by KMP; Riverpod not applicable. Replacement: StateFlow + ViewModel.

## ADR-004 (2026-04-19) — Classic UNO rules only for v1
**Status:** accepted
**Decision:** v1 implements classic Mattel UNO rules only. No stacking +2/+4, no jump-in, no 7-0 swap, draw-one-then-skip.
**Why:** Smallest rule surface to test exhaustively. House rules add edge-case complexity that's better introduced as opt-in toggles once the core engine is rock-solid.
**Future:** House rules arrive as `RuleSet` config passed into the engine, each independently toggleable.

## ADR-005 (2026-04-19) — Defer all external-service integrations
**Status:** accepted
**Decision:** No Firebase, Supabase, Google Sign-In, analytics, or any third-party service with its own account/API key until phases 1–4 are complete and the standalone app is polished.
**Why:** Setup friction on hobby projects kills momentum. Standalone-first keeps the dev loop fast. Ensures the core game is actually fun before we invest in infrastructure.
**Implication:** Every service dependency (auth, sync, telemetry) is defined as an interface first with a local-only default implementation.

## ADR-006 (2026-04-19) — Deterministic seeded RNG
**Status:** accepted
**Decision:** Engine functions that need randomness accept an injected `Random`. Production wires `Random(seed)`; tests inject `Random(fixedSeed)`.
**Why:** Reproducible games for tests, property-based fuzzing with specific seeds, future replay functionality, deterministic bug reports.
**Enforcement:** No direct `Random.Default` calls in `shared/commonMain/.../engine/` — enforced by review.

## ADR-007 (2026-04-19) — Original visual design (not Mattel UNO)
**Status:** accepted
**Decision:** We design our own palette, typography, card back, and card front layout. We do not reproduce Mattel's trademarked UNO look.
**Why:** Legal safety (UNO trademark), creative fun, builds a distinctive identity.
**Scope:** App name "UNO Simple" stays for personal use; would be renamed if we ever published.

## ADR-008 (2026-04-19) — App name: "UNO Simple" (personal use only)
**Status:** accepted
**Decision:** App display name is "UNO Simple", Gradle project name `uno-simple`, app namespace `com.vivek.unosimple`. Personal-use only — UNO is a Mattel trademark.
**Why:** User preferred the existing folder name for familiarity. Hobby project with no publication plans under this name.
**Supersedes if:** User decides to publish — write a new ADR selecting an original name (candidates: Draw Four, Wild Call, Last Card, Prism, Shed).

## ADR-009 (2026-04-19) — Latest-stable, modern idioms
**Status:** accepted
**Decision:** Always use the latest stable Kotlin, Compose Multiplatform, AGP, and Gradle. Use modern Kotlin features (sealed interfaces, data classes, `Result`, context receivers where useful, coroutines + Flow). Material 3 everywhere.
**Why:** Greenfield hobby project with zero legacy constraints. Reduces technical debt before we have any.
**Non-goal:** Bleeding-edge alpha/beta/EAP. Stable only.

## ADR-010 (2026-04-19) — Memory lives in the project, not at user level
**Status:** accepted
**Decision:** All persistent Claude-session memory lives in `.claude/memory/` inside the repo.
**Why:** Portability — user wants to work on this project from multiple machines without losing context. Git-tracked memory follows the code.

## ADR-011 (2026-04-19) — No work-related MCP connections from this project
**Status:** accepted
**Decision:** `.claude/settings.json` denies all work-related MCP tool namespaces (Jira, Confluence, Gmail, Couchbase, BigQuery, Groundrule source search, etc.).
**Why:** Clean separation of personal and work context. Prevents accidental data leakage.
**Enforcement:** `permissions.deny` patterns in `.claude/settings.json`.

## ADR-012 (2026-04-19) — Autonomous test-driven dev loop
**Status:** accepted
**Decision:** Claude must be able to run, verify, and debug end-to-end without user intervention. Every change goes through the loop in CLAUDE.md.
**Why:** User requested it; dramatically improves iteration speed and reduces false "done" declarations.

## ADR-013 (2026-04-19) — Pivot: Kotlin Multiplatform + Compose Multiplatform
**Status:** accepted (supersedes ADR-001)
**Decision:** Replace Flutter with Kotlin Multiplatform (KMP) + Compose Multiplatform. Targets: Android, iOS, Desktop (JVM), Web (Wasm).
**Why:** User has strong Kotlin/Android background (day job), making KMP faster to write and debug. Compose Multiplatform is production-stable on Android and iOS as of 1.7 (Oct 2024). Wasm web target gives us browser dev preview (slightly slower than Flutter's but workable). Native-feeling output on all platforms.
**Trade-offs accepted:** Less mature web story than Flutter. iOS builds require Mac (same as Flutter). Larger Gradle learning curve for those unfamiliar — but user already uses Gradle at work.

## ADR-014 (2026-04-19) — Engine in `shared/commonMain` (pure Kotlin)
**Status:** accepted (restates ADR-002 in KMP terms)
**Decision:** The engine is pure Kotlin in `shared/src/commonMain/kotlin/com/vivek/unosimple/engine/`. No Compose, no `androidx.*`, no iOS or desktop-specific imports.
**Why:** Same reasons as ADR-002 — testable, portable, deterministic, server-ready.
**Enforcement:** `shared` module has no Compose/Android dependencies in `commonMain` source set. Lint rule added when detekt is wired up.

## ADR-015 (2026-04-19) — State: StateFlow + ViewModel (multiplatform)
**Status:** accepted (supersedes ADR-003)
**Decision:** Use `androidx.lifecycle` multiplatform `ViewModel` holding `MutableStateFlow<GameState>`. Compose collects via `collectAsStateWithLifecycle()`.
**Why:** Modern, idiomatic, multiplatform, testable with `kotlinx.coroutines.test`. Zero legacy APIs. Fits naturally with the `applyAction(state, action) -> newState` pattern.
**Alternatives considered:** Decompose (more ceremony; overkill here), Molecule (nice but adds a library), Jetpack Compose `mutableStateOf` directly (fine for tiny UIs but harder to test off-Compose).

## ADR-017 (2026-04-19) — Mattel instruction sheet 42001pr as the canonical rule source
**Status:** accepted (clarifies ADR-004)
**Decision:** The authoritative reference for classic UNO rules is Mattel's official instruction sheet (PDF at `https://service.mattel.com/instruction_sheets/42001pr.pdf`). When implementations and that document disagree, the PDF wins. A local copy is retained in the Claude session tool results so future sessions can cross-check.
**Why:** The user pointed out on 2026-04-19 that my initial implementation was based on general knowledge rather than the authoritative source. A formal reference prevents future drift.
**Immediate correction prompted by this ADR:** UNO-declaration penalty changed from 2 cards to 4 cards (the classic Mattel value). Named constant `Engine.UNO_PENALTY_CARDS = 4`.
**Known v1 deviations from the Mattel rules** (each tracked in `docs/ROADMAP.md` Phase 1 deferred list):
- No Wild Draw Four challenge rule (Mattel: only legal when no matching-color card; challenger procedure with +2 penalty).
- No "play the card you just drew" (Mattel allows it).
- Starter card is always NumberCard (Mattel specifies behavior for action/wild starters).
- UNO penalty fires automatically rather than requiring another player to "catch" you before the next turn starts.
- No 500-point multi-round game-end check.

## ADR-016 (2026-04-19) — Dev preview primary: Desktop (JVM) + Wasm (browser)
**Status:** accepted
**Decision:** Primary dev loop uses Compose Desktop for quick iteration and Wasm browser for browser-specific verification. Android emulator used for real-device-behavior checks before committing polish.
**Why:** Desktop runs fastest (hot reload, instant window). Wasm lets us verify browser behavior + screenshot via `Claude_Preview`. Android emulator is slower to boot but reflects final mobile experience.
**Windows implication:** iOS preview unavailable on Windows — uses Android emulator or desktop as proxy until iOS phase.
