package com.vivek.unosimple.multiplayer

import com.vivek.unosimple.engine.ActionResult
import com.vivek.unosimple.engine.Engine
import com.vivek.unosimple.engine.models.GameAction
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.engine.newRound
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

/**
 * Authoritative in-memory sync service. Backed by shared [MutableStateFlow]
 * instances so multiple "client" objects created from the same [Shared]
 * observe the same state and can submit actions that everyone sees.
 *
 * Usage for hotseat-style local multiplayer:
 * ```
 * val shared = InProcessSyncService.Shared(random = Random(42))
 * val alice = shared.clientFor("p1")
 * val bob   = shared.clientFor("p2")
 * alice.startRound(listOf(PlayerSeat("p1", "Alice"), PlayerSeat("p2", "Bob")))
 * // Both alice.state and bob.state now observe the same GameState.
 * alice.submit(GameAction.PlayCard(playerId = "p1", card = ...))
 * ```
 *
 * Usage for integration tests: the same `Shared` instance can feed N clients
 * from a test dispatcher and the assertions become "after every client
 * submits, do all clients converge on the same state?"
 */
class InProcessSyncService private constructor(
    private val shared: Shared,
    override val myId: String,
) : GameSyncService {

    override val state: StateFlow<GameState?> = shared.stateInternal.asStateFlow()
    override val players: StateFlow<List<PlayerSeat>> = shared.playersInternal.asStateFlow()
    override val emoteEvents: SharedFlow<EmoteEvent> = shared.emotesInternal.asSharedFlow()

    override suspend fun broadcastEmote(reaction: String) {
        shared.emotesInternal.emit(EmoteEvent(senderId = myId, reaction = reaction))
    }

    override suspend fun joinSeat(seat: PlayerSeat) {
        shared.joinSeat(seat)
    }

    override suspend fun startRound(seats: List<PlayerSeat>, handSize: Int) {
        shared.startRound(seats, handSize)
    }

    override suspend fun submit(action: GameAction): SubmitResult {
        return shared.submit(action, fromClientId = myId)
    }

    override suspend fun leave() {
        shared.removeClient(myId)
    }

    /**
     * Shared backend — the "server" of the in-process sim. Create one, then
     * call [clientFor] per participating client. Every returned client observes
     * the same authoritative state.
     */
    class Shared(
        private val random: Random = Random.Default,
    ) {
        internal val stateInternal = MutableStateFlow<GameState?>(null)
        internal val playersInternal = MutableStateFlow<List<PlayerSeat>>(emptyList())
        /**
         * Transient emote bus. `extraBufferCapacity = 8` so a burst of
         * simultaneous reactions doesn't suspend the sender.
         */
        internal val emotesInternal: MutableSharedFlow<EmoteEvent> =
            MutableSharedFlow(extraBufferCapacity = 8)

        /**
         * Serialize all submits through a mutex so concurrent callers can't
         * interleave actions against stale state. In-process impl detail —
         * a real backend enforces ordering via a relay or transaction.
         */
        private val mutex = Mutex()

        fun clientFor(myId: String): GameSyncService {
            if (playersInternal.value.none { it.id == myId }) {
                // Auto-register with a placeholder name if not present yet;
                // startRound will replace names from the seats list.
                playersInternal.value = playersInternal.value + PlayerSeat(myId, myId)
            }
            return InProcessSyncService(shared = this, myId = myId)
        }

        internal suspend fun joinSeat(seat: PlayerSeat) {
            mutex.withLock {
                val existing = playersInternal.value
                playersInternal.value = if (existing.any { it.id == seat.id }) {
                    existing.map { if (it.id == seat.id) seat else it }
                } else {
                    existing + seat
                }
            }
        }

        internal suspend fun startRound(seats: List<PlayerSeat>, handSize: Int) {
            mutex.withLock {
                playersInternal.value = seats
                stateInternal.value = newRound(
                    seats.map { it.id to it.displayName },
                    random,
                    handSize = handSize,
                )
            }
        }

        internal suspend fun submit(action: GameAction, fromClientId: String): SubmitResult {
            mutex.withLock {
                val current = stateInternal.value ?: return SubmitResult.NotConnected

                // Authority: clients may only submit actions for themselves.
                if (action.playerId != fromClientId) {
                    return SubmitResult.Rejected(
                        "Client $fromClientId cannot submit action for ${action.playerId}",
                    )
                }
                if (current.currentPlayer.id != fromClientId) {
                    return SubmitResult.NotMyTurn
                }

                return when (val r = Engine.applyAction(current, action, random)) {
                    is ActionResult.Success -> {
                        stateInternal.value = r.state
                        SubmitResult.Accepted
                    }
                    is ActionResult.Failure -> SubmitResult.Rejected(r.reason)
                }
            }
        }

        internal fun removeClient(id: String) {
            playersInternal.value = playersInternal.value.filter { it.id != id }
        }
    }
}
