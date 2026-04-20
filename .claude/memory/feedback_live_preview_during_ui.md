---
name: Keep a live preview running during UI work
description: Whenever Claude is working on UI (Phase 3 onward), the Wasm dev preview must be kept running so the user can watch changes live.
type: feedback
---

When working on UI code (any change under `composeApp/` — screens, widgets, theme, animations), keep the Wasm dev server running in the background via `Claude_Preview` so the user can see progress live.

**Why:** User stated on 2026-04-19: "when you start working with UI keep a review runnung so that i can see the progress." Live visual feedback is the point of building a game — if they can't see it unfolding, they can't steer it.

**How to apply:**

- **Start of UI session** — before making UI edits, launch the preview:
  ```
  Claude_Preview.preview_start(name = "wasm-dev")
  ```
  (The `wasm-dev` launch config is already defined in `.claude/launch.json`.)
- **During UI iteration** — save files as normal. Kotlin/Wasm + webpack-dev-server will hot-reload automatically. After significant changes, also:
  - `Claude_Preview.preview_screenshot(serverId)` — confirm render looks right.
  - `Claude_Preview.preview_console_logs(serverId, level = "error")` — catch runtime errors.
- **On compile errors** — webpack-dev-server will show a red overlay in the browser. The user will see it too. Check console logs for details.
- **Adaptive UI testing** — use `preview_resize` to check Compact / Medium / Expanded layouts on the same running preview.
- **End of UI session** — stop the preview explicitly to free resources:
  ```
  Claude_Preview.preview_stop(serverId)
  ```

- **Do NOT start the preview for non-UI work** (engine / rules / bots / docs). It's slow to boot first time and pointless when there's nothing visual changing.

- **If the preview crashes or stalls**: stop it, verify builds green via `./gradlew :composeApp:compileKotlinWasmJs`, then restart the preview.
