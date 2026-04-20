# Project-local memory index

Portable persistent context that travels with the repo. This replaces the user-level memory directory — same format, different location.

**Future Claude sessions:** read this file, then load whichever entries are relevant. Before acting on a memory, verify it's still current by checking the code/docs.

- [User profile](user_profile.md) — Kotlin/Android engineer (day job), comfortable with Flutter, hobby UNO app.
- [UNO project decisions](project_uno_decisions.md) — Scope, ruleset, backend, auth, visual, and dev-env choices locked 2026-04-19.
- [Always ask via AskUserQuestion](feedback_ask_questions.md) — ALL questions to this user must use the AskUserQuestion tool, never inline text questions.
- [Defer external-service features](feedback_defer_external_services.md) — Any feature depending on an external service (auth, backend, analytics) is deferred until the standalone app is solid.
- [Keep everything latest and modern](feedback_latest_modern.md) — Latest stable Flutter/Dart/packages; modern idioms (records, patterns, Riverpod code-gen, Material 3).
- [Autonomous testing & debugging](feedback_autonomous_testing.md) — Project must support running/debugging end-to-end via tests + browser automation without user intervention.
- [Memory lives in the project](feedback_memory_portable.md) — All persistent memory is stored in `.claude/memory/` in the repo, not at the user level.
- [No work-related MCPs on this project](feedback_no_work_connections.md) — Never call Jira/Confluence/Gmail/Couchbase/BigQuery/work-source-search from UNO project. Enforced via `.claude/settings.json` deny list.
- [Adaptive UI for phone/tablet/desktop](feedback_adaptive_ui.md) — From Phase 3 onward, every screen uses Compose `WindowSizeClass` branching for Compact/Medium/Expanded. Not immediate.
- [Live preview during UI work](feedback_live_preview_during_ui.md) — Keep the Wasm dev preview running via Claude_Preview whenever working on composeApp/ code so the user can watch progress.
- [Test-first for UI](feedback_test_first_for_ui.md) — Every UI change ships with Compose UI tests in the SAME commit. The manual preview isn't a substitute for `./gradlew :composeApp:desktopTest`.
- [Game-first visual aesthetic](feedback_game_aesthetic.md) — UI must feel like a game (dark table surface, vivid cards, bold typography, warm accent) — never the default Material 3 productivity palette.
- [UI polish direction (post-claymorph)](feedback_ui_polish_direction.md) — After the claymorph redesign, "prototype feel" remaining = physicality, personality, typography. Prioritize fly-to-pile animation, custom display font, bot characters.
