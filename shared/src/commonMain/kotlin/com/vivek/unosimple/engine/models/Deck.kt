package com.vivek.unosimple.engine.models

import kotlin.random.Random

/**
 * Factory for the classic Mattel UNO deck (108 cards).
 *
 * Composition per color (4 colors × 25 cards = 100 colored cards):
 * - 1 zero
 * - 2 each of 1..9 (18 cards)
 * - 2 Skip, 2 Reverse, 2 Draw Two
 *
 * Plus 4 Wild + 4 Wild Draw Four = 8 wild cards.
 *
 * Total: 100 + 8 = 108.
 */
object Deck {
    /** The full 108-card deck in a canonical unshuffled order. */
    fun standard(): List<Card> = buildList(TOTAL_CARDS) {
        for (color in CardColor.entries) {
            add(NumberCard(color, 0))
            for (n in 1..9) {
                add(NumberCard(color, n))
                add(NumberCard(color, n))
            }
            add(SkipCard(color)); add(SkipCard(color))
            add(ReverseCard(color)); add(ReverseCard(color))
            add(DrawTwoCard(color)); add(DrawTwoCard(color))
        }
        repeat(4) { add(WildCard) }
        repeat(4) { add(WildDrawFourCard) }
    }

    /** A shuffled full deck; seed [random] for deterministic shuffles in tests. */
    fun shuffled(random: Random): List<Card> = standard().shuffled(random)

    const val TOTAL_CARDS: Int = 108
}
