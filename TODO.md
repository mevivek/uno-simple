# TODO — short-term queue

Actively-worked items. Longer-term phase tracking lives in [docs/ROADMAP.md](docs/ROADMAP.md). When an item is done, delete it (git log preserves history). Keep this list under ~10 items.

## Shipped (premium arcade revamp, builds 2026-04-20.05 → .28)

The six-phase UI/UX revamp from [.claude/plans/can-you-plan-a-keen-firefly.md](.claude/plans/can-you-plan-a-keen-firefly.md) landed across 24 deploys:

- **Phase 1a/b** — dark arcade theme, Bungee + Inter fonts, Web Audio SFX, new `ClayButton`/`GhostButton`/motion primitives.
- **Phase 2 + 2.5** — red UNO felt, authentic Mattel card faces, big table direction arrow, screen flash on +4/+2, denser celebration, floating CALL UNO disc, emote palette with Canvas-drawn reactions.
- **Phase 3** — Splash → Onboarding → Home flow, Profile + AvatarPicker, hidden Geet easter egg (name entry triggers heart rain + auto-selects the private persona).
- **Phase 4** — Rules reference, in-game Pause overlay, About/Credits.
- **Phase 5** — HistoryRepository + AchievementRepository, Stats dashboard with win-rate ring, Achievements grid, full-screen RoundResult with hand-reveal + medal drop-ins, achievement-unlock banners.
- **Phase 6** — OnlineLobby room-code hero with copy, seat-count header, ConnectionBadge on `.info/connected`, EmoteFeed + broadcast via GameSyncService (wasm + non-wasm Firebase wired), `users/{uid}` mirror in Firebase console, back-stack navigation across all screens, `/admin` URL with users + sessions + history delete controls.

## Still deferred

- [ ] **Adaptive Medium/Expanded layouts** — tablet + landscape phone layouts (ring of opponents, two-column settings/stats). `WindowSizeClass` skeleton is in place; branches render Compact only.
- [ ] **F2: friends** — `UserProfile.uid` doubles as friend code; add-friend flow via `friendRequests/{friendUid}/{myUid}`; mutual accept → `friends/{uid}/{friendUid}`; friend-list UI; "invite to room" shortcut.
- [ ] **Real audio / haptics** — need asset files. `AndroidAudioService` (MediaPlayer / ExoPlayer), `IosAudioService` (AVAudioPlayer), Haptics via `VibratorManager` / `UIImpactFeedbackGenerator`. Wasm already ships synthesized Web Audio SFX.
- [ ] **Online game session resume on reload** — solo persists via `SessionStore`, online doesn't.
- [ ] **Settings persistence** — reset on reload; need a localStorage-backed `SettingsRepository` actual.
- [ ] **Tap-hold to preview a card** — enlarge before commit on hover / long-press.
- [ ] **Wasm `/rooms/seats` cosmetic fix** — currently stored as opaque JSON string blob in Firebase console; needs a new JS helper for object writes.

## Build + deploy workflow reminders

- `./gradlew :composeApp:wasmJsBrowserDistribution && firebase deploy --only hosting` ships to https://uno-simple-5757a3.web.app.
- Push to GitHub at https://github.com/mevivek/uno-simple (public) after each deploy.
- Bump `com.vivek.unosimple.BuildInfo.BUILD_STAMP` on every deploy — shown on Home footer + About screen.
- Last deployed stamp: `2026-04-20.28`.
- Yarn file-locks occasionally fail a wasm build; rerun usually works. If worse, `rm -rf composeApp/build/dist` and retry.
- `/admin` URL (e.g. `uno-simple-5757a3.web.app/admin`) deep-links into the data-management panel — not reachable from any menu.
