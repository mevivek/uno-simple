package com.vivek.unosimple.engine.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerTest {
    @Test
    fun defaultsAreEmptyHandNoUnoZeroScore() {
        val p = Player(id = "p1", name = "Vivek")
        assertEquals(0, p.handSize)
        assertTrue(p.hand.isEmpty())
        assertFalse(p.hasCalledUno)
        assertEquals(0, p.score)
        assertTrue(p.hasWon) // empty hand — technically "won" the round
    }

    @Test
    fun handSizeReflectsHand() {
        val p = Player(
            id = "p1",
            name = "A",
            hand = listOf(
                NumberCard(CardColor.RED, 3),
                SkipCard(CardColor.BLUE),
                WildCard,
            ),
        )
        assertEquals(3, p.handSize)
        assertFalse(p.hasWon)
    }

    @Test
    fun hasWonOnlyWhenHandIsEmpty() {
        val withCards = Player("p", "A", hand = listOf(NumberCard(CardColor.RED, 3)))
        assertFalse(withCards.hasWon)
        assertTrue(withCards.copy(hand = emptyList()).hasWon)
    }

    @Test
    fun handScoreSumsCardScoreValues() {
        val p = Player(
            id = "p1",
            name = "A",
            hand = listOf(
                NumberCard(CardColor.RED, 7),     // 7
                NumberCard(CardColor.BLUE, 0),    // 0
                SkipCard(CardColor.GREEN),        // 20
                WildCard,                         // 50
                WildDrawFourCard,                 // 50
            ),
        )
        assertEquals(7 + 0 + 20 + 50 + 50, p.handScore)
    }

    @Test
    fun emptyHandHasZeroHandScore() {
        assertEquals(0, Player("p", "A").handScore)
    }

    @Test
    fun copyAllowsImmutableUpdates() {
        val p = Player("p1", "A", hand = listOf(WildCard))
        val after = p.copy(hand = p.hand + NumberCard(CardColor.RED, 5), hasCalledUno = true)
        assertEquals(2, after.handSize)
        assertTrue(after.hasCalledUno)
        // Original unchanged
        assertEquals(1, p.handSize)
        assertFalse(p.hasCalledUno)
    }
}
