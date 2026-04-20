package com.vivek.unosimple.engine.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Something a player wants to do during their turn.
 *
 * The engine is fully driven by actions — `applyAction(state, action) -> state`.
 * This keeps state transitions explicit, auditable, serializable over the wire,
 * and replayable for tests and deterministic bug reports.
 *
 * v1 covers the two fundamental moves (play / draw). Classic UNO's "call out
 * another player for forgetting UNO" is simplified: playing the second-to-last
 * card without `declareUno = true` triggers an automatic 2-card penalty.
 */
@Serializable
sealed interface GameAction {
    val playerId: String

    /**
     * Play [card] from [playerId]'s hand. Must be legal against the current top
     * of the discard pile / [GameState.activeColor].
     *
     * @property chosenColor required when [card] is a wild (WildCard or
     *   WildDrawFourCard); ignored otherwise.
     * @property declareUno true when the player is announcing "UNO" as they
     *   play their second-to-last card. If false and the card leaves the
     *   player with exactly one card, the engine applies a 4-card penalty
     *   (Mattel rule — see [com.vivek.unosimple.engine.Engine.UNO_PENALTY_CARDS]).
     */
    @Serializable
    @SerialName("play")
    data class PlayCard(
        override val playerId: String,
        val card: Card,
        val chosenColor: CardColor? = null,
        val declareUno: Boolean = false,
    ) : GameAction

    /**
     * Draw one card from the draw pile. In v1 the drawn card stays in the
     * player's hand; the turn advances immediately (the player may play the
     * drawn card on their next turn if it's still legal). A richer "draw-then-
     * optionally-play" flow is a post-v1 refinement.
     */
    @Serializable
    @SerialName("draw")
    data class DrawCard(
        override val playerId: String,
    ) : GameAction
}
