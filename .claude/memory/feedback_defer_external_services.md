---
name: Defer features that depend on external services
description: Any feature requiring an external service (auth provider, backend, analytics, etc.) must be deferred until the self-contained app works.
type: feedback
---

Do not add features that depend on external/third-party services until the fully self-contained version of the app is working and polished. Examples to defer:

- Google Sign-In / Apple Sign-In / any OAuth
- Firebase, Supabase, or any cloud backend
- Online multiplayer / netcode
- Analytics (Firebase Analytics, Mixpanel, Sentry)
- Push notifications
- Remote config / feature flags
- App Store / Play Store submission tooling

**Why:** User stated on 2026-04-19 that "any feature that may have dependency on some other service can be implemented later." This is a hobby project — setup friction from external accounts, API keys, billing, SDKs, and platform integrations kills momentum.

**How to apply:**
- Design code so external services can be plugged in later behind interfaces (e.g. an `AuthService` abstraction with a `LocalOnlyAuthService` default implementation).
- Phases 1–4 (engine, AI, UI, polish) must be fully playable with **zero third-party service dependencies**.
- Before adding any package that requires an API key, external account, or network call to a non-owned endpoint, stop and flag it to the user via `AskUserQuestion`.
- Pure Dart/Flutter packages (animations, audio, state management, routing) are fine — those are local libraries, not external services.
