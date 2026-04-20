# TODO — short-term queue

Actively-worked items. Longer-term phase tracking lives in [docs/ROADMAP.md](docs/ROADMAP.md). When an item is done, delete it (git log preserves history). Keep this list under ~10 items.

## Next session: UI polish pass ("still feels prototype-y")

User is not happy with the perceived polish of the UI despite the claymorph redesign. They explicitly asked for improvements in this order of priority:

1. **Card fly-to-pile animation** *(highest leverage)* — tapping a card should animate it from its hand position to the discard pile (with a slight rotation + scale + translate tween), not just vanish-and-reappear. Same for opponent plays: a face-down card flies from the opponent's tile to the pile. Needs `onGloballyPositioned` for position capture plus a floating overlay card.

2. **Custom display font** — replace `system-ui` with a chunky rounded display font (Fredoka, Paytone One, or similar) embedded as a Compose font resource. Body copy can stay system-ui.

3. **Bot personalities** — "B1 / B2 / B3" reads as placeholder. Give bots real names (e.g. Rosie, Max, Kira, Juno), distinct avatar illustrations (cute characters, not just colored initial discs), and optional one-liner emotes on key events.

4. (Secondary) Screen transitions (fade+scale crossfade between Home ↔ Game), Web Audio-synthesized sound effects (no asset files needed), animated splash replacing "Dealing…".

## F2: friends

F1 (user profile with persistent UID + display name) is deployed. F2 is next when UI polish is done:
- [ ] Friend code: the existing 12-char `UserProfile.uid` doubles as the friend code
- [ ] Add-friend flow: paste a friend's UID, request sent to RTDB at `friendRequests/{friendUid}/{myUid}`
- [ ] Mutual accept creates `friends/{uid}/{friendUid}` entries both ways
- [ ] Friend list UI with online/offline presence
- [ ] "Invite to room" that sends a room code straight to a friend

## Deferred from earlier phases

- [ ] Curved hand arc (U4 deferred) — cards fan vs. flat row
- [ ] Tap-hold to preview a card enlarged before committing
- [ ] Landscape phone + tablet "ring of opponents" adaptive layouts
- [ ] Online game session resume on reload (currently only solo persists; see `persistence/SessionStore`)
- [ ] Settings persistence (currently reset on reload)
- [ ] Real `AudioService` / `HapticsService` implementations on Android/iOS (files listed below)

## Real audio / haptics (deferred, needs assets)

- [ ] `AndroidAudioService` — Android `MediaPlayer` / `ExoPlayer` with packaged WAV assets for each `SoundEffect`
- [ ] `IosAudioService` — `AVAudioPlayer` (needs Mac)
- [ ] `AndroidHapticsService` — `VibratorManager` / `HapticFeedbackConstants`
- [ ] `IosHapticsService` — `UIImpactFeedbackGenerator` (needs Mac)
- [ ] Actual sound files (generate with sfxr or record). Place under `composeApp/src/commonMain/composeResources/sounds/`

## Build + deploy workflow reminders

- `./gradlew :composeApp:wasmJsBrowserDistribution && firebase deploy --only hosting` ships to https://uno-simple-5757a3.web.app
- Bump `com.vivek.unosimple.BuildInfo.BUILD_STAMP` on every deploy — user watches this to confirm the new build loaded (shown at bottom of Home screen)
- Current stamp at last deploy: `2026-04-19.23`
- Yarn file-locks occasionally fail a wasm build; rerun is usually enough. If worse, `rm -rf composeApp/build/dist` and retry
