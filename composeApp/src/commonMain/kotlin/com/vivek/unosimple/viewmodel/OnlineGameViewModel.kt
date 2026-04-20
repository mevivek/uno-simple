package com.vivek.unosimple.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivek.unosimple.engine.HAND_SIZE
import com.vivek.unosimple.engine.models.Card
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.GameAction
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.multiplayer.GameSyncService
import com.vivek.unosimple.multiplayer.PlayerSeat
import com.vivek.unosimple.multiplayer.SubmitResult
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Online multiplayer ViewModel. Thin — delegates all state and action
 * plumbing to the injected [GameSyncService]. The VM exists so Compose
 * can collect the service's [StateFlow] via the lifecycle-aware
 * viewModelScope instead of leaking subscriptions.
 *
 * Typical wiring:
 * ```
 * val sync = FirebaseSyncService(myId = authUid, roomCode = "UNOX42")
 * val vm = OnlineGameViewModel(sync = sync)
 * ```
 *
 * For tests / local demos, pass an `InProcessSyncService` from
 * `Shared.clientFor(...)` — same contract.
 */
class OnlineGameViewModel(
    val sync: GameSyncService,
) : ViewModel() {

    val state: StateFlow<GameState?> = sync.state

    /** Stable id of *this* client's seat. Display treats this as the "me". */
    val myId: String get() = sync.myId

    /**
     * Start a new round as host. In a real room, only one player should call
     * this (typically whoever tapped "Start game" in the lobby). Others see
     * the new state via the sync flow.
     *
     * Seats are shuffled before dealing so the host isn't always first —
     * otherwise `newRound` picks `currentPlayerIndex = 0` and the lobby's
     * join-order becomes turn-order, which feels unfair online.
     */
    fun startRound(seats: List<PlayerSeat>, handSize: Int = HAND_SIZE) {
        val shuffled = seats.shuffled()
        viewModelScope.launch { sync.startRound(shuffled, handSize) }
    }

    fun playCard(card: Card, chosenColor: CardColor?, declareUno: Boolean) {
        val action = GameAction.PlayCard(
            playerId = myId,
            card = card,
            chosenColor = chosenColor,
            declareUno = declareUno,
        )
        viewModelScope.launch { sync.submit(action) }
    }

    fun drawCard() {
        viewModelScope.launch { sync.submit(GameAction.DrawCard(myId)) }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { sync.leave() }
    }

    /**
     * Convenience for callers that need to know whether the round can be
     * restarted by this client — rules of thumb, not enforced by the service:
     * the round's original host typically owns restarts.
     */
    fun requestNewRound(seats: List<PlayerSeat>) {
        startRound(seats)
    }

    /** Exposed for UI that wants to show a toast on failed submits. */
    suspend fun submitRaw(action: GameAction): SubmitResult = sync.submit(action)
}
