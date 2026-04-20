package com.vivek.unosimple.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivek.unosimple.engine.HAND_SIZE
import com.vivek.unosimple.engine.models.Card
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.GameAction
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.multiplayer.GameSyncService
import com.vivek.unosimple.multiplayer.InProcessSyncService
import com.vivek.unosimple.multiplayer.PlayerSeat
import com.vivek.unosimple.multiplayer.SubmitResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * "Hotseat" local multiplayer: 2–4 humans sharing one device, taking turns
 * with a pass-device prompt between each turn so no player accidentally sees
 * the next player's hand.
 *
 * Works entirely against [InProcessSyncService] — all seats register as
 * "clients" on a single shared backend, all observe the same state, and
 * the UI swaps which player's hand is face-up based on whose turn it is.
 *
 * When real online multiplayer lands, this class swaps to a different
 * [GameSyncService] impl (Firebase / Ktor / WebRTC) and the UI changes
 * nothing — each device only knows about its own seat, and the pass-device
 * overlay is unnecessary because each human looks at their own screen.
 */
class HotseatGameViewModel(
    seats: List<PlayerSeat>,
    random: Random = Random.Default,
    handSize: Int = HAND_SIZE,
) : ViewModel() {

    init {
        require(seats.size in 2..4) { "Hotseat supports 2-4 players, got ${seats.size}" }
        require(seats.map { it.id }.toSet().size == seats.size) { "Seat ids must be unique" }
    }

    private val shared = InProcessSyncService.Shared(random = random)
    private val clients: Map<String, GameSyncService> =
        seats.associate { it.id to shared.clientFor(it.id) }

    /** Shared authoritative state — same reference for every client. */
    val state: StateFlow<GameState?> = clients.values.first().state

    /** Seat list, in turn order. */
    val seats: List<PlayerSeat> = seats.toList()

    private val _phase = MutableStateFlow<Phase>(Phase.Handoff(nextSeat = seats.first()))
    val phase: StateFlow<Phase> = _phase.asStateFlow()

    init {
        viewModelScope.launch {
            clients.values.first().startRound(seats, handSize)
        }
    }

    /** Current player's client — null until [state] emits its first value. */
    private val currentClient: GameSyncService?
        get() = state.value?.currentPlayer?.id?.let { clients[it] }

    /** User tapped "Ready" on the pass-device overlay. */
    fun acknowledgeHandoff() {
        val s = state.value ?: return
        if (s.isRoundOver) return
        _phase.value = Phase.PlayerTurn(seat = requireNotNull(currentSeat(s)))
    }

    /**
     * Current player plays [card]. Caller is responsible for tap-to-play
     * legality (UI enables only legal cards); illegal submits return silently.
     */
    fun playCard(card: Card, chosenColor: CardColor?, declareUno: Boolean) {
        val client = currentClient ?: return
        val action = GameAction.PlayCard(
            playerId = client.myId,
            card = card,
            chosenColor = chosenColor,
            declareUno = declareUno,
        )
        viewModelScope.launch {
            if (client.submit(action) is SubmitResult.Accepted) {
                onTurnEnded()
            }
        }
    }

    /** Current player draws one and ends their turn (v1 rule). */
    fun drawCard() {
        val client = currentClient ?: return
        viewModelScope.launch {
            if (client.submit(GameAction.DrawCard(playerId = client.myId)) is SubmitResult.Accepted) {
                onTurnEnded()
            }
        }
    }

    /** Restart the round after the winner banner. */
    fun startNewRound() {
        viewModelScope.launch {
            clients.values.first().startRound(seats)
            _phase.value = Phase.Handoff(nextSeat = seats.first())
        }
    }

    private fun onTurnEnded() {
        val s = state.value ?: return
        if (s.isRoundOver) {
            _phase.value = Phase.RoundOver
        } else {
            val next = requireNotNull(currentSeat(s))
            _phase.value = Phase.Handoff(nextSeat = next)
        }
    }

    private fun currentSeat(s: GameState): PlayerSeat? =
        seats.firstOrNull { it.id == s.currentPlayer.id }

    /**
     * UI flow states. The hotseat surface always lives in one of:
     *
     * - [Handoff]: full-screen "Pass to {next player}" overlay. Nothing
     *   about the game is visible to prevent the previous player from
     *   peeking at the next hand.
     * - [PlayerTurn]: normal game surface — the active seat's hand is
     *   face-up and playable.
     * - [RoundOver]: winner banner + "New round" CTA.
     */
    sealed interface Phase {
        data class Handoff(val nextSeat: PlayerSeat) : Phase
        data class PlayerTurn(val seat: PlayerSeat) : Phase
        data object RoundOver : Phase
    }
}
