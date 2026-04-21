package com.vivek.unosimple.multiplayer

import com.vivek.unosimple.engine.models.GameAction
import com.vivek.unosimple.engine.models.GameState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Authoritative multiplayer sync contract. One instance represents a shared
 * round — all clients in the same room read [state] and submit actions via
 * [submit]; the service is responsible for ordering, applying, and
 * broadcasting the result.
 *
 * ### Implementations
 *
 * - [InProcessSyncService] — authoritative in-memory reference impl. Used
 *   for hotseat-style local multiplayer and for integration tests that
 *   exercise the full multi-client loop without a backend.
 * - `FirebaseSyncService`, `SupabaseSyncService`, `KtorRelaySyncService` —
 *   not implemented. These require external infrastructure (a Firebase
 *   project, a Supabase project, or a deployed Ktor server). Each would
 *   be a concrete `GameSyncService` built on `state` transactions / rows /
 *   WebSockets respectively, with the same external API this interface
 *   defines.
 *
 * ### Authority model
 *
 * Actions are **server-authoritative** — clients submit intent, the service
 * applies it against the current state via
 * [com.vivek.unosimple.engine.Engine.applyAction], and broadcasts the new
 * state. The engine is fully deterministic (given a seeded [Random]) so a
 * relay-style backend can also be implemented client-authoritative (each
 * client runs its own engine, the sync layer just gossips the ordered
 * action log). That's a swap the interface doesn't care about.
 */
interface GameSyncService {
    /** Currently-shared game state. `null` before a round is started. */
    val state: StateFlow<GameState?>

    /** Players currently in the room, in seat order (ids). */
    val players: StateFlow<List<PlayerSeat>>

    /** Identifier the remote peer assigned this client — used to filter actions. */
    val myId: String

    /** Observable connection health. UI renders a badge from this. */
    val connectionState: StateFlow<ConnectionState>
        get() = kotlinx.coroutines.flow.MutableStateFlow(ConnectionState.Connected)

    /**
     * Register [seat] in the room's seat list without dealing a round. Used
     * by the lobby flow — host creates the room and registers themselves;
     * each guest registers on join. The round doesn't deal until [startRound]
     * is explicitly called (typically by the host tapping "Start round"
     * after everyone has arrived).
     *
     * Safe to call repeatedly — if [seat.id] is already present the
     * implementation may update the existing entry but must not duplicate.
     */
    suspend fun joinSeat(seat: PlayerSeat)

    /**
     * Start or restart a round with the given seats. Host-only in backends
     * that care about host/guest; in-process impl lets anyone call it.
     */
    suspend fun startRound(seats: List<PlayerSeat>, handSize: Int = 7)

    /**
     * Submit an action as this client. The service validates authority
     * (only the acting player may submit actions for themselves), applies
     * via the engine, and broadcasts the new state.
     */
    suspend fun submit(action: GameAction): SubmitResult

    /** Leave the room; disconnects from further state updates. */
    suspend fun leave()

    /**
     * Transient emote events (😊 / 🔥 / etc.) broadcast by any client in the
     * room. UI subscribes and fades a bubble at the sender's seat for a
     * couple seconds. Default = a silent SharedFlow for implementations
     * that don't care (e.g. solo-only surfaces).
     */
    val emoteEvents: SharedFlow<EmoteEvent>
        get() = MutableSharedFlow()

    /**
     * Broadcast an emote from this client. Implementations emit an
     * [EmoteEvent] on [emoteEvents] for every connected client (including
     * the sender, for optimistic local echo).
     */
    suspend fun broadcastEmote(reaction: String) { /* default no-op */ }
}

/** Transient emote tag sent from [sender] (a [PlayerSeat.id]). */
@kotlinx.serialization.Serializable
data class EmoteEvent(val senderId: String, val reaction: String)

@kotlinx.serialization.Serializable
data class PlayerSeat(val id: String, val displayName: String)

/** Connection health for the badge on the online game surface. */
enum class ConnectionState { Connected, Reconnecting, Offline }

/** Outcome of a submit call — exposes engine-level failures to the UI. */
sealed interface SubmitResult {
    data object Accepted : SubmitResult
    data class Rejected(val reason: String) : SubmitResult
    data object NotMyTurn : SubmitResult
    data object NotConnected : SubmitResult
}
