---
name: Game-first visual aesthetic, not productivity Material 3
description: UNO Simple's UI must feel like a game — bold, vivid, playful. Avoid default Material 3 palette / styling that reads as a productivity app.
type: feedback
---

The UI must read as a **game**, not a productivity app. User feedback on 2026-04-19: "material theme, color and components looks so boring to use in a game."

**Why:** UNO is a bright, loud, fun card game. Default Material 3 light scheme (lavender surface + purple primary + soft container tones) feels more like an office tool. For a game, players expect vivid color, high-contrast surfaces, punchy typography, and visible personality.

**How to apply:**

- **Don't use the default `MaterialTheme {}`** without overrides. Supply a custom `ColorScheme` (dark-first; light theme optional later) in `composeApp/src/commonMain/kotlin/com/vivek/unosimple/ui/theme/`.
- **Background**: dark "table felt" — deep charcoal-navy, green-black, or dark teal. Cards should pop off it.
- **Card colors**: keep the traditional Mattel four (red/yellow/green/blue) conceptually, but use **richer, more saturated** versions. Ours, not theirs. Add subtle gradient or inner glow so cards feel 3D rather than flat swatches.
- **Primary / accent**: something **warm and high-energy** — gold, electric red, or cyan — not Material 3 purple. Used on call-to-action buttons (Start game, UNO!, New round).
- **Typography**: heavier weights for titles (Black / ExtraBold), generous tracking for headers. Body text can stay default. Consider a custom display font for the "UNO Simple" title.
- **Custom shapes**: cards should have slight elevation / shadow, not flat `Surface(tonalElevation = 0.dp)`. FilterChip / Button corners can stay rounded but give them a more playful look (extra-rounded pill, subtle gradient fill).
- **No raw Material defaults on interactive elements** — customize button containers, filter chip selected colors, dialog surface.
- **Animation readiness**: color palette should work with phase-4 animations (deal, flip, pulse) without washing out motion.

**Don't over-index on theme**: we still ship everything through `MaterialTheme` so Compose Material 3 components (AlertDialog, FilterChip, Button) continue to work. We just swap the ColorScheme / Typography / Shapes underneath.

**When validating a visual change** — screenshot in the live preview; if the first impression is "this could be a calendar app", start over.
