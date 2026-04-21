package com.vivek.unosimple.multiplayer

import com.vivek.unosimple.engine.ActionResult
import com.vivek.unosimple.engine.Engine
import com.vivek.unosimple.engine.models.GameAction
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.engine.newRound
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.CoroutineScope
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
import kotlin.random.Random

/**
 * Firebase Realtime Database-backed sync service.
 *
 * Client-authoritative for simplicity — every participant runs the engine
 * and writes the resulting state. Races are rare in turn-based games (only
 * the `currentPlayer` is expected to write), and the engine's authority
 * checks reject misbehavior client-side before a write ever happens.
 *
 * ### DB schema
 *
 * ```
 * rooms/
 *   {roomCode}/
 *     seats/           : List<PlayerSeat>   — turn order + display names
 *     state/           : GameState          — full state, rewritten per action
 * ```
 *
 * ### Runtime requirements
 *
 * 1. The Firebase platform SDK must be initialized before constructing this
 *    class. See `docs/FIREBASE_SETUP.md` for the full setup (create project,
 *    download `google-services.json` / `GoogleService-Info.plist` / web
 *    config, enable Realtime Database + anonymous auth).
 * 2. Users should be signed in anonymously (or linked to a real account).
 *    This class doesn't enforce — it assumes auth is handled upstream.
 *
 * ### Offline / reconnect
 *
 * Firebase's client caches writes while offline and replays them on
 * reconnect, so brief network blips are transparent. For long outages, the
 * round state at reconnect is whatever the last successful writer produced;
 * any local action that wasn't sent is lost. For a hobby game that's fine.
 */
class FirebaseSyncService(
    override val myId: String,
    private val roomCode: String,
    private val random: Random = Random.Default,
) : GameSyncService {

    private val database = Firebase.database

    private val roomRef = database.reference("rooms/$roomCode")
    private val stateRef = roomRef.child("state")
    private val seatsRef = roomRef.child("seats")
    private val emoteRef = roomRef.child("latestEmote")

    private val _state = MutableStateFlow<GameState?>(null)
    override val state: StateFlow<GameState?> = _state.asStateFlow()

    private val _players = MutableStateFlow<List<PlayerSeat>>(emptyList())
    override val players: StateFlow<List<PlayerSeat>> = _players.asStateFlow()

    private val _emotes = MutableSharedFlow<EmoteEvent>(
        replay = 0,
        extraBufferCapacity = 8,
    )
    override val emoteEvents: SharedFlow<EmoteEvent> = _emotes.asSharedFlow()

    /**
     * Flips true after the seats snapshot has fired at least once. `joinSeat`
     * waits on this so we don't clobber an existing seat list with a merge
     * against an empty local cache.
     */
    private val seatsLoaded = MutableStateFlow(false)

    private val scope = CoroutineScope(SupervisorJob())

    init {
        // Subscribe to state + seats the moment the service is constructed.
        // New snapshots push into the StateFlows; the UI re-renders from
        // whatever the last snapshot was.
        scope.launch {
            stateRef.valueEvents.collect { snapshot ->
                _state.value = runCatching { snapshot.value<GameState?>() }.getOrNull()
            }
        }
        scope.launch {
            seatsRef.valueEvents.collect { snapshot ->
                _players.value =
                    runCatching { snapshot.value<List<PlayerSeat>?>() ?: emptyList() }
                        .getOrDefault(emptyList())
                seatsLoaded.value = true
            }
        }
        // Emotes: single-slot payload. Sender always writes a random nonce
        // so identical reactions fired back-to-back still trigger a new
        // snapshot on peers.
        scope.launch {
            emoteRef.valueEvents.collect { snapshot ->
                val payload = runCatching {
                    snapshot.value<LatestEmotePayload?>()
                }.getOrNull() ?: return@collect
                _emotes.tryEmit(EmoteEvent(senderId = payload.senderId, reaction = payload.reaction))
            }
        }
    }

    override suspend fun broadcastEmote(reaction: String) {
        val nonce = random.nextInt().toString(36) + random.nextInt().toString(36)
        emoteRef.setValue(LatestEmotePayload(senderId = myId, reaction = reaction, nonce = nonce))
    }

    @kotlinx.serialization.Serializable
    private data class LatestEmotePayload(
        val senderId: String,
        val reaction: String,
        val nonce: String,
    )

    override suspend fun joinSeat(seat: PlayerSeat) {
        // Await the first snapshot so we merge against real DB state rather
        // than an empty local cache (otherwise a fresh guest overwrites the
        // host's entry).
        seatsLoaded.first { it }
        val existing = _players.value
        val next = if (existing.any { it.id == seat.id }) {
            existing.map { if (it.id == seat.id) seat else it }
        } else {
            existing + seat
        }
        seatsRef.setValue(next)
        // Mirror to /users/{id} so the Firebase console has a users view.
        database.reference("users/${seat.id}")
            .setValue(UserProfilePayload(displayName = seat.displayName, avatarId = seat.avatarId))
    }

    @kotlinx.serialization.Serializable
    private data class UserProfilePayload(
        val displayName: String,
        val avatarId: String? = null,
    )

    override suspend fun startRound(seats: List<PlayerSeat>, handSize: Int) {
        seatsRef.setValue(seats)
        val fresh = newRound(
            seats.map { it.id to it.displayName },
            random,
            handSize = handSize,
        )
        stateRef.setValue(fresh)
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
                stateRef.setValue(r.state)
                SubmitResult.Accepted
            }
            is ActionResult.Failure -> SubmitResult.Rejected(r.reason)
        }
    }

    override suspend fun leave() {
        // Remove self from seats; keep state so remaining players can
        // continue playing.
        val remaining = _players.value.filterNot { it.id == myId }
        seatsRef.setValue(remaining)
        scope.cancel()
    }
}
