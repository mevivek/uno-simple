---
name: UNO project decisions
description: Scope, stack, ruleset, backend, auth, visual, dev-env, and app-name decisions for the hobby UNO KMP project (as of 2026-04-19).
type: project
---

**Project:** Kotlin Multiplatform + Compose Multiplatform UNO card game. Hobby project, non-funded. Targets: Android, iOS, Desktop (JVM), Web (Wasm).

**Decisions (2026-04-19):**
- **App name:** "UNO Simple" (display) / `uno-simple` (Gradle project) / `com.vivek.unosimple` (package). Personal use only — UNO is trademarked.
- **Stack:** KMP 2.1+ with Compose Multiplatform 1.7+. Initially considered Flutter, pivoted same day because user prefers Kotlin (ADR-013). Scaffold lives at project root (no Flutter residue except `C:\dev\flutter` install which can be deleted later).
- **Opponents:** Online multiplayer is the eventual goal. Starting with offline single-player (AI bots) because backend choice is deferred.
- **Ruleset:** Classic UNO rules only for v1. No stacking +2/+4, no jump-in, draw-one-then-skip. House rules may come later as toggles.
- **Backend:** Deferred. Engine + local/AI play first (fully playable offline); backend chosen later (likely Firebase, Supabase, or a Ktor server).
- **Auth (when added):** Anonymous + optional Google/Apple sign-in.
- **Visual identity:** Original design with our own palette and font — NOT the trademarked Mattel UNO look.
- **Dev environment:** Primary = Compose Desktop (JVM) for fast iteration. Secondary = Wasm browser preview at `localhost:8080`. Android emulator for real-device behavior checks.
- **State management:** `MutableStateFlow<GameState>` inside multiplatform `ViewModel` (androidx.lifecycle KMP). No LiveData/RxJava.
- **Serialization:** `kotlinx.serialization` with `@Serializable` on every engine type.
- **Build:** Gradle 8.11.1, Kotlin DSL, version catalog at `gradle/libs.versions.toml`.

**Why these shape the plan:**
- Deferred backend means `shared/commonMain/.../engine/` must be backend-agnostic pure Kotlin with no platform imports.
- Online goal means game state must be serializable from day one (JSON via kotlinx.serialization).
- High polish target means we invest in animations (Compose animate* APIs), sound, haptics, theming.
- Wasm target means we can use `Claude_Preview` for autonomous UI verification in the browser.

**How to apply:**
- Don't add online-multiplayer code until the engine is solid and the backend is chosen.
- Don't suggest UNO trademark assets.
- Keep all engine types `@Serializable`.
- If any decision above is revisited, write a superseding ADR in `docs/DECISIONS.md` rather than silently changing direction.
- Module paths: engine = `shared/src/commonMain/kotlin/com/vivek/unosimple/engine/`; UI = `composeApp/src/commonMain/kotlin/com/vivek/unosimple/`.
