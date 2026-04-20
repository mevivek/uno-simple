# Multiplayer backends

The `GameSyncService` interface at `composeApp/src/commonMain/kotlin/com/vivek/unosimple/multiplayer/GameSyncService.kt` is the single integration point for any multiplayer backend. Swap the implementation, UI and ViewModel code stay unchanged.

## Today: InProcessSyncService (reference impl)

In-memory, single-process, authoritative. Used for integration tests and a future hotseat UI. Zero external dependencies.

Use case: local-device multiplayer, tests, demos.

## Firebase Realtime Database — easiest path to real-internet play

**External setup required.** Out of scope per ADR-005.

- Firebase project with Realtime Database enabled.
- `google-services.json` (Android) / `GoogleService-Info.plist` (iOS) checked into the repo.
- Auth with anonymous accounts (already the decision in ADR-005 — "anon + optional Google/Apple").
- One room = one DB node; action stream is a child list; clients subscribe to changes.

Implementation shape:
```
class FirebaseSyncService(
    private val database: FirebaseDatabase,
    private val roomCode: String,
) : GameSyncService { ... }
```

Pros: cross-platform SDK, transactional writes, scales for free. Cons: vendor lock-in, free-tier quota, needs Google account.

## Supabase Realtime — open alternative to Firebase

Same shape as Firebase, Postgres + Realtime subscription underneath. Self-hostable. External setup still required.

## Ktor WebSocket relay — full control, you host

**User deploys a tiny server.** Out of scope for this project directly; docs only.

- ~150 lines of Ktor to accept WebSocket connections, route messages by room code.
- Deploy free tier on Fly.io / Render / Railway.
- Client: `ktor-client-websockets` in commonMain — works across all KMP targets.

Implementation shape:
```
class KtorRelaySyncService(
    private val wsUrl: String,    // e.g. "wss://uno-signaling.fly.dev"
    private val roomCode: String,
) : GameSyncService { ... }
```

Pros: no vendor lock-in, tiny server, open source all the way down. Cons: you run the server (one hobby-tier VM is enough).

## WebRTC DataChannels — peer-to-peer game traffic

**User deploys a signaling server (tiny) OR accepts a 3rd-party signaling service.**

After signaling, game state flows peer-to-peer. No game-server relay. Perfect for turn-based traffic.

Per-platform dependencies (all behind the same GameSyncService interface):

| Target | Library |
|---|---|
| Android | `io.getstream:stream-webrtc-android` |
| iOS | `WebRTC.framework` via CocoaPods |
| Desktop (JVM) | `webrtc4j` (beta) or fall back to Ktor WebSocket on desktop |
| Web (Wasm/JS) | browser-native `RTCPeerConnection` via external JS declarations |

Signaling options:
- **PeerJS cloud** (free) — no server setup, public broker. Fine for hobby scale. Web-only wrapper originally, Android/iOS libs available.
- **Trystero** (BitTorrent-tracker signaling) — truly serverless, but JS-only so no native mobile.
- **Self-hosted Ktor signaling** — ~150 lines, you deploy to Fly.io hobby tier. Full control.

Implementation shape:
```
class WebRtcSyncService(
    private val signalingTransport: SignalingTransport,
    private val roomCode: String,
) : GameSyncService { ... }
```

Pros: no game-server relay cost, lowest latency, encryption built in. Cons: per-platform WebRTC wrappers, signaling still needed, NAT traversal edge cases.

## Choosing between them

For the hobby project, if/when online multiplayer is desired:

- **Fastest to first multiplayer game**: Firebase. Sign up, drop the config file, swap the sync service, done.
- **Self-hosted / no vendor lock-in**: Ktor WebSocket relay. Write the ~150-line server once, deploy, done.
- **Truly peer-to-peer**: WebRTC. Most work, most rewarding — flip per-platform wrappers + signaling.

None of these are implemented in the current codebase. The InProcessSyncService is the only concrete `GameSyncService`. Every other row in this document is a future decision.
