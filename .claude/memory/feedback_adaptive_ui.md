---
name: UI adapts to phone / tablet / desktop
description: Game UI must be adaptable to different form factors (phone, tablet, desktop). Not immediate; applies from Phase 3 layout work onward.
type: feedback
---

UI must adapt to the form factor — **phone**, **tablet**, and **desktop** (and by extension, web at various viewport sizes). User flagged on 2026-04-19 as not immediate, for a later stage.

**Why:** The app targets Android, iOS, Desktop (JVM), and Web (Wasm). These span very different screen sizes. A single layout designed for a phone will look wasteful on desktop, and a desktop-first layout will cramp on phones. Adaptive design is the standard Material 3 expectation.

**How to apply:**

- Use Compose Multiplatform's `WindowSizeClass` API (`androidx.compose.material3.windowsizeclass`) to branch layouts on **Compact** (< 600 dp width, typical phone), **Medium** (600–840 dp, small tablet / foldable / large phone landscape), and **Expanded** (> 840 dp, tablet / desktop / web).
- Every new screen introduced from Phase 3 onward should consider all three classes up-front, even if initially the Compact and Medium paths are identical — the branching skeleton should exist so we can refine later.
- Typical adaptations for UNO:
  - **Compact (phone):** player's hand at the bottom, opponents as small avatars along the top, draw/discard stacked center. Single-column.
  - **Medium (tablet):** same layout scaled up; more breathing room; opponents arranged around the table.
  - **Expanded (desktop/web):** wider playfield, side panel for game log / score / chat, keyboard shortcuts available.
- Don't hard-code pixel sizes; use `dp` / `Modifier.fillMaxWidth(fraction)` / `BoxWithConstraints`.
- Test at three viewport sizes in the Wasm preview during Phase 3/4 via `Claude_Preview` `preview_resize`.
- Orientation changes on mobile should reflow (portrait vs landscape).

**When to actually do the work:**
- Phase 3 (basic UI): design each screen for Compact-first, but wrap the root in `WindowSizeClass` branching so Medium/Expanded slots exist.
- Phase 4 (polish): refine Medium and Expanded layouts with real opinions, not just scaled-up Compact.
- Before then: don't over-engineer. A minimal centered Phase 0 screen doesn't need adaptive logic.
