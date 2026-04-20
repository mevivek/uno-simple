package com.vivek.unosimple.engine.models

import kotlinx.serialization.Serializable

/**
 * Complete, serializable state of an in-progress UNO round.
 *
 * All transitions go through [com.vivek.unosimple.engine.Engine.applyAction];
 * no field should be mutated directly. The state is designed to be
 * round-trippable via kotlinx.serialization so it can travel over a network
 * unchanged (future online multiplayer).
 *
 * @property drawPile top of the draw pile is `drawPile.first()`; cards are
 *   removed from the head when drawn.
 * @property discardPile top of the discard pile is `discardPile.last()`;
 *   newly played cards are appended.
 * @property activeColor the color currently in effect. Normally equal to
 *   `topDiscard.color`, but after a wild is played it holds the chosen color
 *   (since wild cards themselves have no color).
 * @property winnerId set to the winning player's id once their hand empties;
 *   null while the round is in progress.
 */
@Serializable
data class GameState(
    val players: List<Player>,
    val drawPile: List<Card>,
    val discardPile: List<Card>,
    val currentPlayerIndex: Int,
    val direction: PlayDirection,
    val activeColor: CardColor,
    val winnerId: String? = null,
    /**
     * When a +2 or +4 has just been played, the next player may stack another
     * +2 / +4 (same type) to pass the growing penalty onward. [pendingDraw]
     * tracks the accumulated penalty. `0` means no stack is in progress. When
     * the player facing the stack declines to stack (draws / plays nothing),
     * they eat the whole stack and the turn skips them.
     *
     * [pendingDrawType] records which stack card kicked it off, so `+2` and
     * `+4` can't mix (Mattel and most house rules treat them as separate
     * stacks — a +4 can be stacked only by another +4).
     */
    val pendingDraw: Int = 0,
    val pendingDrawType: PendingDrawType? = null,
) {
    init {
        require(players.size in MIN_PLAYERS..MAX_PLAYERS) {
            "UNO supports $MIN_PLAYERS..$MAX_PLAYERS players, got ${players.size}"
        }
        require(players.map { it.id }.toSet().size == players.size) {
            "Player ids must be unique"
        }
        require(currentPlayerIndex in players.indices) {
            "currentPlayerIndex $currentPlayerIndex out of range for ${players.size} players"
        }
        require(discardPile.isNotEmpty()) {
            "Discard pile must have at least one card (the starter)"
        }
    }

    val currentPlayer: Player get() = players[currentPlayerIndex]

    val topDiscard: Card get() = discardPile.last()

    val isRoundOver: Boolean get() = winnerId != null

    /** Index of the player whose turn it would be after the current one. */
    fun nextPlayerIndex(): Int = advanceIndex(currentPlayerIndex, direction.step())

    /** Advance an index by `steps` positions in the current direction, with wrap-around. */
    fun advanceIndex(from: Int, steps: Int): Int {
        val n = players.size
        return ((from + steps) % n + n) % n
    }

    companion object {
        const val MIN_PLAYERS: Int = 2
        const val MAX_PLAYERS: Int = 10
    }
}

/** What kind of draw-stack is pending; used to prevent cross-stacking +2 onto +4. */
@Serializable
enum class PendingDrawType { DRAW_TWO, DRAW_FOUR }
