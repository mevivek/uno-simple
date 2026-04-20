package com.vivek.unosimple.engine.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameStateTest {

    private fun baseState(
        players: List<Player> = listOf(
            Player("p1", "A"),
            Player("p2", "B"),
            Player("p3", "C"),
        ),
        currentPlayerIndex: Int = 0,
        direction: PlayDirection = PlayDirection.CLOCKWISE,
        discardPile: List<Card> = listOf(NumberCard(CardColor.RED, 3)),
        activeColor: CardColor = CardColor.RED,
    ) = GameState(
        players = players,
        drawPile = emptyList(),
        discardPile = discardPile,
        currentPlayerIndex = currentPlayerIndex,
        direction = direction,
        activeColor = activeColor,
    )

    @Test
    fun currentPlayerPointsAtCurrentIndex() {
        val s = baseState(currentPlayerIndex = 1)
        assertEquals("p2", s.currentPlayer.id)
    }

    @Test
    fun topDiscardIsLastOfDiscardPile() {
        val red3 = NumberCard(CardColor.RED, 3)
        val blue5 = NumberCard(CardColor.BLUE, 5)
        val s = baseState(discardPile = listOf(red3, blue5))
        assertEquals(blue5, s.topDiscard)
    }

    @Test
    fun isRoundOverFollowsWinnerId() {
        val s = baseState()
        assertFalse(s.isRoundOver)
        assertNull(s.winnerId)
        val won = s.copy(winnerId = "p1")
        assertTrue(won.isRoundOver)
    }

    @Test
    fun nextPlayerIndexWrapsClockwise() {
        val s3 = baseState(currentPlayerIndex = 2, direction = PlayDirection.CLOCKWISE)
        assertEquals(0, s3.nextPlayerIndex())
    }

    @Test
    fun nextPlayerIndexWrapsCounterClockwise() {
        val s = baseState(currentPlayerIndex = 0, direction = PlayDirection.COUNTER_CLOCKWISE)
        assertEquals(2, s.nextPlayerIndex())
    }

    @Test
    fun advanceIndexSupportsArbitrarySteps() {
        val s = baseState(currentPlayerIndex = 0, direction = PlayDirection.CLOCKWISE)
        // With 3 players: +1 -> 1, +2 -> 2, +3 -> 0, -1 -> 2, -4 -> 2
        assertEquals(1, s.advanceIndex(0, 1))
        assertEquals(2, s.advanceIndex(0, 2))
        assertEquals(0, s.advanceIndex(0, 3))
        assertEquals(2, s.advanceIndex(0, -1))
        assertEquals(2, s.advanceIndex(0, -4))
    }

    @Test
    fun rejectsTooFewPlayers() {
        assertFailsWith<IllegalArgumentException> {
            GameState(
                players = listOf(Player("p1", "A")),
                drawPile = emptyList(),
                discardPile = listOf(NumberCard(CardColor.RED, 3)),
                currentPlayerIndex = 0,
                direction = PlayDirection.CLOCKWISE,
                activeColor = CardColor.RED,
            )
        }
    }

    @Test
    fun rejectsTooManyPlayers() {
        val eleven = (1..11).map { Player("p$it", "P$it") }
        assertFailsWith<IllegalArgumentException> { baseState(players = eleven) }
    }

    @Test
    fun rejectsDuplicatePlayerIds() {
        assertFailsWith<IllegalArgumentException> {
            baseState(players = listOf(Player("p1", "A"), Player("p1", "B")))
        }
    }

    @Test
    fun rejectsCurrentPlayerIndexOutOfRange() {
        assertFailsWith<IllegalArgumentException> { baseState(currentPlayerIndex = 3) }
        assertFailsWith<IllegalArgumentException> { baseState(currentPlayerIndex = -1) }
    }

    @Test
    fun rejectsEmptyDiscardPile() {
        assertFailsWith<IllegalArgumentException> { baseState(discardPile = emptyList()) }
    }

    @Test
    fun minAndMaxPlayersConstantsAre2And10() {
        assertEquals(2, GameState.MIN_PLAYERS)
        assertEquals(10, GameState.MAX_PLAYERS)
    }
}
