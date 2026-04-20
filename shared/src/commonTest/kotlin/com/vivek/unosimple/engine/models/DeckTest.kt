package com.vivek.unosimple.engine.models

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DeckTest {
    @Test
    fun standardDeckHas108Cards() {
        assertEquals(108, Deck.standard().size)
        assertEquals(Deck.TOTAL_CARDS, Deck.standard().size)
    }

    @Test
    fun standardDeckHasOneZeroPerColor() {
        val zeros = Deck.standard().filterIsInstance<NumberCard>().filter { it.number == 0 }
        assertEquals(4, zeros.size)
        assertEquals(setOf(CardColor.RED, CardColor.YELLOW, CardColor.GREEN, CardColor.BLUE), zeros.map { it.color }.toSet())
    }

    @Test
    fun standardDeckHasTwoOfEachNumber1Through9PerColor() {
        val numberCards = Deck.standard().filterIsInstance<NumberCard>()
        for (color in CardColor.entries) {
            for (n in 1..9) {
                val count = numberCards.count { it.color == color && it.number == n }
                assertEquals(2, count, "Expected 2x $color $n, got $count")
            }
        }
        // Total non-zero number cards: 4 colors * 9 values * 2 = 72
        assertEquals(72, numberCards.count { it.number != 0 })
    }

    @Test
    fun standardDeckHasTwoOfEachActionCardPerColor() {
        val deck = Deck.standard()
        for (color in CardColor.entries) {
            assertEquals(2, deck.count { it is SkipCard && it.color == color }, "Skip $color")
            assertEquals(2, deck.count { it is ReverseCard && it.color == color }, "Reverse $color")
            assertEquals(2, deck.count { it is DrawTwoCard && it.color == color }, "DrawTwo $color")
        }
        // Totals: 2 * 4 = 8 of each action type
        assertEquals(8, deck.count { it is SkipCard })
        assertEquals(8, deck.count { it is ReverseCard })
        assertEquals(8, deck.count { it is DrawTwoCard })
    }

    @Test
    fun standardDeckHas4WildAnd4WildDrawFour() {
        val deck = Deck.standard()
        assertEquals(4, deck.count { it === WildCard })
        assertEquals(4, deck.count { it === WildDrawFourCard })
    }

    @Test
    fun standardDeckScoresTo1240Points() {
        // Canonical Mattel-deck total: 1240 points.
        // Zeros:          4 * 0              = 0
        // Numbers 1..9:   4 colors * 2 copies * (1+2+...+9) = 8 * 45 = 360
        // Action (Skip/Reverse/DrawTwo): (8+8+8) * 20       = 480
        // Wild + WildDrawFour:           (4+4) * 50         = 400
        // Total: 360 + 480 + 400 = 1240
        assertEquals(1240, Deck.standard().sumOf { it.scoreValue })
    }

    @Test
    fun shuffledDeckPreservesComposition() {
        val shuffled = Deck.shuffled(Random(42))
        assertEquals(108, shuffled.size)
        // Same multiset of cards as the standard deck
        assertEquals(Deck.standard().groupingBy { it }.eachCount(), shuffled.groupingBy { it }.eachCount())
    }

    @Test
    fun shuffleIsDeterministicGivenSameSeed() {
        assertEquals(Deck.shuffled(Random(7)), Deck.shuffled(Random(7)))
    }

    @Test
    fun differentSeedsProduceDifferentOrderings() {
        assertNotEquals(Deck.shuffled(Random(1)), Deck.shuffled(Random(2)))
    }
}
