---
name: UI polish direction (post-claymorph)
description: What the user considers "prototype-y" vs "polished" after we landed the claymorph/Duolingo aesthetic redesign
type: feedback
---

After the U1–U7 claymorph redesign (soft pastels, clay shadows, fanned discard/stacked draw, podium, avatars, reduced-motion toggle, adaptive container) the user reviewed the app and said: **"it still feels like a prototype game."**

**Why:** The remaining prototype-feel issues aren't about color or layout — they're about physicality, personality, and typography.

**How to apply:** When the user asks for polish, prioritize:
1. Physical motion (cards travel across the screen on play, not vanish-and-reappear) — the deferred U2 "fly-to-pile" animation is the single highest-leverage change
2. Custom display font (currently `system-ui` reads as generic web)
3. Character (bots named B1/B2/B3 with initial-on-disc avatars feel like placeholders — give them real names and illustrated faces)

Screen transitions, synthesized Web Audio sfx, and an animated splash are secondary polish. Don't propose backend/auth/scale work unless the user explicitly asks — they confirmed the app is personal-use only and they just want the UI/UX to feel shipped.
