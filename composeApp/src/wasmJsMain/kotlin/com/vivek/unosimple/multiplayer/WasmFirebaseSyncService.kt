package com.vivek.unosimple.multiplayer

import com.vivek.unosimple.engine.ActionResult
import com.vivek.unosimple.engine.Engine
import com.vivek.unosimple.engine.models.GameAction
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.engine.newRound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

// ---------------------------------------------------------------------------
// JS glue externals. Each @JsFun corresponds to a function pinned on window
// in index.html by the uno-db.js module.
// ---------------------------------------------------------------------------

@JsFun("(path, value) => window.unoDbSet(path, value)")
private external fun unoDbSet(path: String, value: String)

@JsFun("(path, value) => window.unoDbSetObject(path, value)")
private external fun unoDbSetObject(path: String, jsonValue: String)

@JsFun("(path, cb) => window.unoDbSubscribe(path, cb)")
private external fun unoDbSubscribe(path: String, callback: (String) -> Unit): Int

@JsFun("(handle) => window.unoDbUnsubscribe(handle)")
private external fun unoDbUnsubscribe(handle: Int)

/**
 * Kotlin/Wasm-native Firebase sync service.
 *
 * dev.gitlive:firebase-kotlin-sdk doesn't publish Wasm variants (GitLive
 * issue #440, open since Dec 2023 with no maintainer response), so the
 * Wasm target talks to the Firebase Web SDK directly via the uno-db.js
 * glue layer loaded in `index.html`. Same `GameSyncService` contract as
 * the dev.gitlive-backed implementation on other platforms — callers
 * don't care which one they got.
 *
 * Only two operations cross the Kotlin/JS boundary:
 * - `unoDbSet(path, value)` — write a JSON string at path
 * - `unoDbSubscribe(path, cb)` — listen for value changes
 *
 * State + seat lists are serialized to JSON via kotlinx.serialization on
 * the Kotlin side and stored as opaque strings in RTDB. This keeps the JS
 * layer trivially small and avoids dealing with Firebase's object schema
 * rules (which reject empty arrays, no dots in keys, etc.).
 */
class WasmFirebaseSyncService(
    override val myId: String,
    roomCode: String,
    private val random: Random = Random.Default,
) : GameSyncService {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val stateRefPath = "rooms/$roomCode/state"
    private val seatsRefPath = "rooms/$roomCode/seats"
    private val emoteRefPath = "rooms/$roomCode/latestEmote"

    private val _state = MutableStateFlow<GameState?>(null)
    override val state: StateFlow<GameState?> = _state.asStateFlow()

    private val _players = MutableStateFlow<List<PlayerSeat>>(emptyList())
    override val players: StateFlow<List<PlayerSeat>> = _players.asStateFlow()

    // Emote stream — replays last 0, buffers 8, drops oldest on overflow so
    // a burst doesn't block the network callback.
    private val _emotes = MutableSharedFlow<EmoteEvent>(
        replay = 0,
        extraBufferCapacity = 8,
    )
    override val emoteEvents: SharedFlow<EmoteEvent> = _emotes.asSharedFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.Connected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /**
     * Flips true after the seats subscription has fired at least once.
     * `joinSeat` waits on this before writing so we don't clobber the
     * current seat list with a merge against an empty local cache — Firebase
     * push is async, so at construction time `_players` is still empty even
     * if the room already has a host.
     */
    private val seatsLoaded = MutableStateFlow(false)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val stateSubscriptionHandle: Int
    private val seatsSubscriptionHandle: Int
    private val emoteSubscriptionHandle: Int
    private val connectedSubscriptionHandle: Int

    init {
        // Subscribe immediately so state/seats populate as soon as Firebase
        // has a snapshot for this room. Empty string = node absent = null.
        stateSubscriptionHandle = unoDbSubscribe(stateRefPath) { raw ->
            _state.value = runCatching {
                if (raw.isEmpty()) null else json.decodeFromString<GameState>(raw)
            }.getOrNull()
        }
        seatsSubscriptionHandle = unoDbSubscribe(seatsRefPath) { raw ->
            _players.value = runCatching {
                if (raw.isEmpty()) emptyList()
                else json.decodeFromString(ListSerializer(PlayerSeat.serializer()), raw)
            }.getOrDefault(emptyList())
            seatsLoaded.value = true
        }
        // Emotes use a single-slot payload at rooms/{code}/latestEmote. The
        // sender always includes a random nonce so back-to-back identical
        // reactions (same sender, same icon) still produce a distinct JSON
        // string and trigger the onValue callback on every peer.
        emoteSubscriptionHandle = unoDbSubscribe(emoteRefPath) { raw ->
            if (raw.isEmpty()) return@unoDbSubscribe
            val payload = runCatching {
                json.decodeFromString(LatestEmotePayload.serializer(), raw)
            }.getOrNull() ?: return@unoDbSubscribe
            // tryEmit so the onValue callback (called from JS main thread)
            // never suspends waiting for a subscriber.
            _emotes.tryEmit(EmoteEvent(senderId = payload.senderId, reaction = payload.reaction))
        }
        // Firebase's special `.info/connected` path emits a boolean when
        // the SDK's websocket flips online / offline. Lets us drive the
        // ConnectionBadge from the real network state instead of a
        // hardcoded "Connected".
        connectedSubscriptionHandle = unoDbSubscribe(".info/connected") { raw ->
            _connectionState.value = when (raw) {
                "true" -> ConnectionState.Connected
                "false" -> ConnectionState.Reconnecting
                else -> _connectionState.value // unknown / initial — leave as-is
            }
        }
    }

    override suspend fun broadcastEmote(reaction: String) {
        val nonce = random.nextInt().toString(36) + random.nextInt().toString(36)
        val payload = LatestEmotePayload(senderId = myId, reaction = reaction, nonce = nonce)
        unoDbSet(emoteRefPath, json.encodeToString(LatestEmotePayload.serializer(), payload))
    }

    @kotlinx.serialization.Serializable
    private data class LatestEmotePayload(
        val senderId: String,
        val reaction: String,
        val nonce: String,
    )

    override suspend fun joinSeat(seat: PlayerSeat) {
        // Wait for the first snapshot so we merge against the real DB state
        // instead of an empty local cache (otherwise a fresh guest would
        // overwrite the host's seat entry). Simultaneous joins are still
        // racy — last write wins — but that's rare in hobby play.
        seatsLoaded.first { it }
        val existing = _players.value
        val next = if (existing.any { it.id == seat.id }) {
            existing.map { if (it.id == seat.id) seat else it }
        } else {
            existing + seat
        }
        // Write seats as a real Firebase object list (not a string blob) so
        // the console tree shows nested displayName/avatarId fields.
        unoDbSetObject(
            seatsRefPath,
            json.encodeToString(ListSerializer(PlayerSeat.serializer()), next),
        )
        // Mirror the joining user to /users/{id}/... so the Firebase
        // console tree shows each field separately instead of one opaque
        // JSON string. Three simple sub-paths keep the glue layer trivial
        // (everything unoDbSet takes is just a string).
        unoDbSet("users/${seat.id}/displayName", seat.displayName)
        seat.avatarId?.let { unoDbSet("users/${seat.id}/avatarId", it) }
    }

    override suspend fun startRound(seats: List<PlayerSeat>, handSize: Int) {
        unoDbSet(
            seatsRefPath,
            json.encodeToString(ListSerializer(PlayerSeat.serializer()), seats),
        )
        val fresh = newRound(
            seats.map { it.id to it.displayName },
            random,
            handSize = handSize,
        )
        unoDbSet(stateRefPath, json.encodeToString(fresh))
    }

    override suspend fun submit(action: GameAction): SubmitResult {
        val current = _state.value ?: return SubmitResult.NotConnected
        if (action.playerId != myId) {
            return SubmitResult.Rejected("client $myId cannot submit action for ${action.playerId}")
        }
        if (current.currentPlayer.id != myId) {
            return SubmitResult.NotMyTurn
        }
        return when (val r = Engine.applyAction(current, action, random)) {
            is ActionResult.Success -> {
                unoDbSet(stateRefPath, json.encodeToString(r.state))
                SubmitResult.Accepted
            }
            is ActionResult.Failure -> SubmitResult.Rejected(r.reason)
        }
    }

    override suspend fun leave() {
        // Detach listeners + cancel any pending launches. Seats list stays
        // on Firebase; remaining players can still play.
        scope.launch {
            unoDbUnsubscribe(stateSubscriptionHandle)
            unoDbUnsubscribe(seatsSubscriptionHandle)
            unoDbUnsubscribe(emoteSubscriptionHandle)
            unoDbUnsubscribe(connectedSubscriptionHandle)
        }
        scope.cancel()
    }
}
