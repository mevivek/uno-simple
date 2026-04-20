package com.vivek.unosimple.engine.models

import kotlinx.serialization.Serializable

/**
 * A seat at the table. The engine doesn't distinguish human vs. bot —
 * that distinction lives in the UI/ViewModel layer, which picks where
 * each player's actions come from (user input vs. [com.vivek.unosimple.engine.ai.Bot]).
 *
 * @property id stable identifier (e.g. "p1", "bot-a") used to reference
 *   this player across state updates and across the wire.
 * @property hasCalledUno true after the player presses "UNO" while on
 *   two cards; resets when the hand size changes back to >1 (next round,
 *   drawing cards, etc.).
 * @property score cumulative points won across rounds (classic UNO
 *   scoring: round winner adds opponents' remaining hand values).
 */
@Serializable
data class Player(
    val id: String,
    val name: String,
    val hand: List<Card> = emptyList(),
    val hasCalledUno: Boolean = false,
    val score: Int = 0,
) {
    val handSize: Int get() = hand.size

    /** A player with an empty hand has won the round. */
    val hasWon: Boolean get() = hand.isEmpty()

    /** Total point value of the remaining hand (used in round-end scoring). */
    val handScore: Int get() = hand.sumOf { it.scoreValue }
}
