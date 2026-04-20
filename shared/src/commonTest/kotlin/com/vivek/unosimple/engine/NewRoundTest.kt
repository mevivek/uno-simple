package com.vivek.unosimple.engine

import com.vivek.unosimple.engine.models.Deck
import com.vivek.unosimple.engine.models.NumberCard
import com.vivek.unosimple.engine.models.PlayDirection
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NewRoundTest {

    private val fourPlayers = listOf("p1" to "A", "p2" to "B", "p3" to "C", "p4" to "D")

    @Test
    fun dealsSevenCardsPerPlayerByDefault() {
        val state = newRound(fourPlayers, Random(1))
        for (p in state.players) assertEquals(HAND_SIZE, p.handSize)
    }

    @Test
    fun startsWithFirstPlayerClockwise() {
        val state = newRound(fourPlayers, Random(1))
        assertEquals(0, state.currentPlayerIndex)
        assertEquals(PlayDirection.CLOCKWISE, state.direction)
    }

    @Test
    fun starterIsAlwaysANumberCard() {
        // Run many seeds to reliably cover the non-number-first case.
        repeat(100) { seed ->
            val state = newRound(fourPlayers, Random(seed.toLong()))
            assertIs<NumberCard>(state.topDiscard, "seed=$seed")
            assertEquals(state.topDiscard.color, state.activeColor, "seed=$seed")
        }
    }

    @Test
    fun totalCardsAccountedFor() {
        val state = newRound(fourPlayers, Random(42))
        val handTotal = state.players.sumOf { it.handSize }
        val discardTotal = state.discardPile.size
        val drawTotal = state.drawPile.size
        assertEquals(Deck.TOTAL_CARDS, handTotal + discardTotal + drawTotal)
    }

    @Test
    fun sameSeedProducesIdenticalDeal() {
        val a = newRound(fourPlayers, Random(7))
        val b = newRound(fourPlayers, Random(7))
        assertEquals(a, b)
    }

    @Test
    fun differentSeedsProduceDifferentDeals() {
        val a = newRound(fourPlayers, Random(1))
        val b = newRound(fourPlayers, Random(2))
        assertNotEquals(a, b)
    }

    @Test
    fun rejectsFewerThanTwoPlayers() {
        assertFailsWith<IllegalArgumentException> {
            newRound(listOf("p1" to "A"), Random(0))
        }
    }

    @Test
    fun rejectsDuplicatePlayerIds() {
        assertFailsWith<IllegalArgumentException> {
            newRound(listOf("p1" to "A", "p1" to "B"), Random(0))
        }
    }

    @Test
    fun handSizeIsCustomizable() {
        val state = newRound(fourPlayers, Random(0), handSize = 3)
        for (p in state.players) assertEquals(3, p.handSize)
    }

    @Test
    fun winnerIdIsNullAtStart() {
        val state = newRound(fourPlayers, Random(0))
        assertTrue(state.winnerId == null)
        assertTrue(!state.isRoundOver)
    }
}
