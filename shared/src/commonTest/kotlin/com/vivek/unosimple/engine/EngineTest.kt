package com.vivek.unosimple.engine

import com.vivek.unosimple.engine.models.Card
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.DrawTwoCard
import com.vivek.unosimple.engine.models.GameAction
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.engine.models.NumberCard
import com.vivek.unosimple.engine.models.PlayDirection
import com.vivek.unosimple.engine.models.Player
import com.vivek.unosimple.engine.models.ReverseCard
import com.vivek.unosimple.engine.models.SkipCard
import com.vivek.unosimple.engine.models.WildCard
import com.vivek.unosimple.engine.models.WildDrawFourCard
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Rule-by-rule coverage of [Engine.applyAction]. Tests construct minimal states
 * tailored to exercise one rule at a time. Randomness is seeded for
 * reproducibility.
 */
class EngineTest {

    private val fixedRandom get() = Random(0)

    /** Minimal 3-player state, red 5 on top, current player = p1 (index 0). */
    private fun threePlayerState(
        p1Hand: List<Card> = emptyList(),
        p2Hand: List<Card> = emptyList(),
        p3Hand: List<Card> = emptyList(),
        drawPile: List<Card> = List(20) { NumberCard(CardColor.GREEN, it % 10) },
        top: Card = NumberCard(CardColor.RED, 5),
        activeColor: CardColor = CardColor.RED,
        direction: PlayDirection = PlayDirection.CLOCKWISE,
        currentPlayerIndex: Int = 0,
    ) = GameState(
        players = listOf(
            Player("p1", "A", hand = p1Hand),
            Player("p2", "B", hand = p2Hand),
            Player("p3", "C", hand = p3Hand),
        ),
        drawPile = drawPile,
        discardPile = listOf(top),
        currentPlayerIndex = currentPlayerIndex,
        direction = direction,
        activeColor = activeColor,
    )

    private fun twoPlayerState(
        p1Hand: List<Card> = emptyList(),
        p2Hand: List<Card> = emptyList(),
        drawPile: List<Card> = List(20) { NumberCard(CardColor.GREEN, it % 10) },
        top: Card = NumberCard(CardColor.RED, 5),
        activeColor: CardColor = CardColor.RED,
    ) = GameState(
        players = listOf(
            Player("p1", "A", hand = p1Hand),
            Player("p2", "B", hand = p2Hand),
        ),
        drawPile = drawPile,
        discardPile = listOf(top),
        currentPlayerIndex = 0,
        direction = PlayDirection.CLOCKWISE,
        activeColor = activeColor,
    )

    private fun assertSuccess(result: ActionResult): GameState {
        return when (result) {
            is ActionResult.Success -> result.state
            is ActionResult.Failure -> fail("Expected success but got failure: ${result.reason}")
        }
    }

    private fun assertFailure(result: ActionResult, messageSubstring: String? = null): ActionResult.Failure {
        assertIs<ActionResult.Failure>(result, "Expected failure but got success")
        if (messageSubstring != null) {
            assertTrue(
                result.reason.contains(messageSubstring),
                "Expected failure reason to contain '$messageSubstring' but was '${result.reason}'",
            )
        }
        return result
    }

    // ------------------------------------------------------------------
    // Turn / ownership validation
    // ------------------------------------------------------------------

    @Test
    fun rejectsActionFromNonCurrentPlayer() {
        val state = threePlayerState(p2Hand = listOf(NumberCard(CardColor.RED, 1)))
        val result = Engine.applyAction(
            state,
            GameAction.PlayCard("p2", NumberCard(CardColor.RED, 1)),
            fixedRandom,
        )
        assertFailure(result, "Not p2's turn")
    }

    @Test
    fun rejectsPlayingCardNotInHand() {
        val state = threePlayerState(p1Hand = listOf(NumberCard(CardColor.RED, 1)))
        val result = Engine.applyAction(
            state,
            GameAction.PlayCard("p1", NumberCard(CardColor.BLUE, 9)),
            fixedRandom,
        )
        assertFailure(result, "does not hold")
    }

    @Test
    fun rejectsIllegalPlayAgainstTopCard() {
        val state = threePlayerState(
            p1Hand = listOf(NumberCard(CardColor.BLUE, 9)),
            top = NumberCard(CardColor.RED, 5),
            activeColor = CardColor.RED,
        )
        val result = Engine.applyAction(
            state,
            GameAction.PlayCard("p1", NumberCard(CardColor.BLUE, 9)),
            fixedRandom,
        )
        assertFailure(result, "not legal")
    }

    @Test
    fun rejectsWildWithoutChosenColor() {
        val state = threePlayerState(p1Hand = listOf(WildCard))
        val result = Engine.applyAction(
            state,
            GameAction.PlayCard("p1", WildCard, chosenColor = null),
            fixedRandom,
        )
        assertFailure(result, "Wild cards require a chosenColor")
    }

    @Test
    fun rejectsNonWildWithChosenColor() {
        val state = threePlayerState(p1Hand = listOf(NumberCard(CardColor.RED, 5)))
        val result = Engine.applyAction(
            state,
            GameAction.PlayCard("p1", NumberCard(CardColor.RED, 5), chosenColor = CardColor.BLUE),
            fixedRandom,
        )
        assertFailure(result, "Non-wild cards must not specify chosenColor")
    }

    @Test
    fun rejectsActionsAfterRoundOver() {
        val state = threePlayerState(p1Hand = listOf(NumberCard(CardColor.RED, 5)))
            .copy(winnerId = "p2")
        val result = Engine.applyAction(
            state,
            GameAction.PlayCard("p1", NumberCard(CardColor.RED, 5)),
            fixedRandom,
        )
        assertFailure(result, "Round is already over")
    }

    // ------------------------------------------------------------------
    // Basic plays
    // ------------------------------------------------------------------

    @Test
    fun playCardBySameColorMovesCardAndAdvancesTurn() {
        val red5 = NumberCard(CardColor.RED, 5)
        val red7 = NumberCard(CardColor.RED, 7)
        val state = threePlayerState(p1Hand = listOf(red7), top = red5, activeColor = CardColor.RED)

        val newState = assertSuccess(
            Engine.applyAction(state, GameAction.PlayCard("p1", red7), fixedRandom),
        )

        assertEquals(red7, newState.topDiscard)
        assertEquals(CardColor.RED, newState.activeColor)
        assertEquals(0, newState.players[0].handSize)
        assertEquals("p2", newState.currentPlayer.id)
    }

    @Test
    fun playCardBySameNumberDifferentColorChangesActiveColor() {
        val red5 = NumberCard(CardColor.RED, 5)
        val blue5 = NumberCard(CardColor.BLUE, 5)
        val state = threePlayerState(p1Hand = listOf(blue5), top = red5, activeColor = CardColor.RED)

        val newState = assertSuccess(
            Engine.applyAction(state, GameAction.PlayCard("p1", blue5), fixedRandom),
        )

        assertEquals(CardColor.BLUE, newState.activeColor)
    }

    // ------------------------------------------------------------------
    // Action cards: Skip / Reverse / DrawTwo
    // ------------------------------------------------------------------

    @Test
    fun skipCardAdvancesPastNextPlayer() {
        val redSkip = SkipCard(CardColor.RED)
        val state = threePlayerState(p1Hand = listOf(redSkip))

        val newState = assertSuccess(
            Engine.applyAction(state, GameAction.PlayCard("p1", redSkip), fixedRandom),
        )

        // Skipping past p2, landing on p3.
        assertEquals("p3", newState.currentPlayer.id)
    }

    @Test
    fun reverseCardFlipsDirectionAndMovesOnePlayerInNewDirection() {
        val redReverse = ReverseCard(CardColor.RED)
        val state = threePlayerState(p1Hand = listOf(redReverse))

        val newState = assertSuccess(
            Engine.applyAction(state, GameAction.PlayCard("p1", redReverse), fixedRandom),
        )

        assertEquals(PlayDirection.COUNTER_CLOCKWISE, newState.direction)
        // From p1 going counter-clockwise → p3.
        assertEquals("p3", newState.currentPlayer.id)
    }

    @Test
    fun reverseActsAsSkipWithTwoPlayers() {
        val redReverse = ReverseCard(CardColor.RED)
        val state = twoPlayerState(p1Hand = listOf(redReverse))

        val newState = assertSuccess(
            Engine.applyAction(state, GameAction.PlayCard("p1", redReverse), fixedRandom),
        )

        assertEquals(PlayDirection.COUNTER_CLOCKWISE, newState.direction)
        // Same player keeps the turn.
        assertEquals("p1", newState.currentPlayer.id)
    }

    @Test
    fun drawTwoStartsPendingStackNextPlayerEatsItOnDraw() {
        val redDrawTwo = DrawTwoCard(CardColor.RED)
        val drawPile = listOf(
            NumberCard(CardColor.GREEN, 1),
            NumberCard(CardColor.GREEN, 2),
            NumberCard(CardColor.GREEN, 3),
        )
        val state = threePlayerState(
            // 3 cards so playing the +2 doesn't trip the UNO penalty (2→1).
            p1Hand = listOf(redDrawTwo, NumberCard(CardColor.BLUE, 9), NumberCard(CardColor.BLUE, 8)),
            drawPile = drawPile,
        )

        // Stage 1: p1 plays +2. Turn passes to p2 with pendingDraw = 2.
        val afterPlay = assertSuccess(
            Engine.applyAction(state, GameAction.PlayCard("p1", redDrawTwo), fixedRandom),
        )
        assertEquals("p2", afterPlay.currentPlayer.id)
        assertEquals(2, afterPlay.pendingDraw)
        assertEquals(0, afterPlay.players[1].handSize) // not penalized yet

        // Stage 2: p2 draws → eats the stack (2 cards), stack resets, turn
        // passes to p3.
        val afterDraw = assertSuccess(
            Engine.applyAction(afterPlay, GameAction.DrawCard("p2"), fixedRandom),
        )
        assertEquals(2, afterDraw.players[1].handSize)
        assertEquals("p3", afterDraw.currentPlayer.id)
        assertEquals(0, afterDraw.pendingDraw)
    }

    // ------------------------------------------------------------------
    // Wild cards
    // ------------------------------------------------------------------

    @Test
    fun wildCardSetsActiveColorAndAdvancesTurn() {
        val state = threePlayerState(p1Hand = listOf(WildCard))

        val newState = assertSuccess(
            Engine.applyAction(
                state,
                GameAction.PlayCard("p1", WildCard, chosenColor = CardColor.GREEN),
                fixedRandom,
            ),
        )

        assertEquals(WildCard, newState.topDiscard)
        assertEquals(CardColor.GREEN, newState.activeColor)
        assertEquals("p2", newState.currentPlayer.id)
    }

    @Test
    fun wildDrawFourStartsPendingStackNextPlayerEatsItOnDraw() {
        val drawPile = List(10) { NumberCard(CardColor.GREEN, it) }
        val state = threePlayerState(
            p1Hand = listOf(WildDrawFourCard, NumberCard(CardColor.BLUE, 9), NumberCard(CardColor.BLUE, 8)),
            drawPile = drawPile,
        )

        // Stage 1: p1 plays +4. Turn passes to p2 with pendingDraw = 4.
        val afterPlay = assertSuccess(
            Engine.applyAction(
                state,
                GameAction.PlayCard("p1", WildDrawFourCard, chosenColor = CardColor.YELLOW),
                fixedRandom,
            ),
        )
        assertEquals(CardColor.YELLOW, afterPlay.activeColor)
        assertEquals("p2", afterPlay.currentPlayer.id)
        assertEquals(4, afterPlay.pendingDraw)
        assertEquals(0, afterPlay.players[1].handSize)

        // Stage 2: p2 draws → eats 4, stack resets, turn passes to p3.
        val afterDraw = assertSuccess(
            Engine.applyAction(afterPlay, GameAction.DrawCard("p2"), fixedRandom),
        )
        assertEquals(4, afterDraw.players[1].handSize)
        assertEquals("p3", afterDraw.currentPlayer.id)
        assertEquals(0, afterDraw.pendingDraw)
        assertEquals(6, afterDraw.drawPile.size)
    }

    // ------------------------------------------------------------------
    // UNO declaration
    // ------------------------------------------------------------------

    @Test
    fun declaringUnoOnSecondToLastCardSetsFlagAndAvoidsPenalty() {
        val red5 = NumberCard(CardColor.RED, 5)
        val red7 = NumberCard(CardColor.RED, 7)
        val state = threePlayerState(
            p1Hand = listOf(red5, red7),
            top = NumberCard(CardColor.RED, 3),
            activeColor = CardColor.RED,
        )

        val newState = assertSuccess(
            Engine.applyAction(
                state,
                GameAction.PlayCard("p1", red5, declareUno = true),
                fixedRandom,
            ),
        )

        val p1After = newState.players[0]
        assertEquals(1, p1After.handSize)
        assertTrue(p1After.hasCalledUno)
    }

    @Test
    fun forgettingUnoIncursFourCardPenalty() {
        // Classic Mattel rule: UNO penalty is 4 cards, not 2.
        val red5 = NumberCard(CardColor.RED, 5)
        val red7 = NumberCard(CardColor.RED, 7)
        val drawPile = List(6) { NumberCard(CardColor.GREEN, it) }
        val state = threePlayerState(
            p1Hand = listOf(red5, red7),
            drawPile = drawPile,
            top = NumberCard(CardColor.RED, 3),
            activeColor = CardColor.RED,
        )

        val newState = assertSuccess(
            Engine.applyAction(
                state,
                GameAction.PlayCard("p1", red5, declareUno = false),
                fixedRandom,
            ),
        )

        val p1After = newState.players[0]
        // 1 card remaining after play + 4 penalty = 5 cards.
        assertEquals(5, p1After.handSize)
        assertTrue(!p1After.hasCalledUno)
        assertEquals(2, newState.drawPile.size) // 6 - 4
        // Winner isn't set on penalty play (hand isn't empty).
        assertEquals(null, newState.winnerId)
    }

    // ------------------------------------------------------------------
    // Win condition
    // ------------------------------------------------------------------

    @Test
    fun playingLastCardSetsWinnerId() {
        val red5 = NumberCard(CardColor.RED, 5)
        val state = threePlayerState(
            p1Hand = listOf(red5),
            top = NumberCard(CardColor.RED, 3),
            activeColor = CardColor.RED,
        )

        val newState = assertSuccess(
            Engine.applyAction(
                state,
                GameAction.PlayCard("p1", red5, declareUno = false),
                fixedRandom,
            ),
        )

        assertEquals("p1", newState.winnerId)
        assertTrue(newState.isRoundOver)
        assertEquals(0, newState.players[0].handSize)
    }

    @Test
    fun winnerCollectsSumOfOtherPlayersHandValues() {
        // p1 is about to play their last card. p2 holds a 7 (value 7) + a
        // Skip (value 20) + a Wild (value 50) = 77. p3 holds only a WildDrawFour
        // (value 50). Winner should collect 77 + 50 = 127 points.
        val red5 = NumberCard(CardColor.RED, 5)
        val state = threePlayerState(
            p1Hand = listOf(red5),
            p2Hand = listOf(
                NumberCard(CardColor.BLUE, 7),
                SkipCard(CardColor.GREEN),
                WildCard,
            ),
            p3Hand = listOf(WildDrawFourCard),
            top = NumberCard(CardColor.RED, 3),
            activeColor = CardColor.RED,
        )
            .let { s ->
                // Give p1 a prior score so we verify additive scoring.
                s.copy(players = s.players.mapIndexed { i, p -> if (i == 0) p.copy(score = 25) else p })
            }

        val newState = assertSuccess(
            Engine.applyAction(
                state,
                GameAction.PlayCard("p1", red5, declareUno = false),
                fixedRandom,
            ),
        )

        assertEquals("p1", newState.winnerId)
        // Winner: 25 + (77 + 50) = 152. Losers: unchanged.
        assertEquals(25 + 7 + 20 + 50 + 50, newState.players[0].score)
        assertEquals(0, newState.players[1].score)
        assertEquals(0, newState.players[2].score)
    }

    @Test
    fun nonWinningPlaysDoNotChangeScore() {
        val red5 = NumberCard(CardColor.RED, 5)
        val state = threePlayerState(
            p1Hand = listOf(red5, NumberCard(CardColor.BLUE, 2)),
            top = NumberCard(CardColor.RED, 3),
            activeColor = CardColor.RED,
        )

        val newState = assertSuccess(
            Engine.applyAction(
                state,
                GameAction.PlayCard("p1", red5, declareUno = false),
                fixedRandom,
            ),
        )

        // Everyone's score stays 0 — no one won.
        for (p in newState.players) assertEquals(0, p.score)
    }

    // ------------------------------------------------------------------
    // DrawCard action
    // ------------------------------------------------------------------

    @Test
    fun drawCardAddsOneCardToHandAndAdvancesTurn() {
        val drawPile = listOf(NumberCard(CardColor.GREEN, 1), NumberCard(CardColor.YELLOW, 7))
        val state = threePlayerState(drawPile = drawPile)

        val newState = assertSuccess(
            Engine.applyAction(state, GameAction.DrawCard("p1"), fixedRandom),
        )

        assertEquals(1, newState.players[0].handSize)
        assertEquals(NumberCard(CardColor.GREEN, 1), newState.players[0].hand.first())
        assertEquals("p2", newState.currentPlayer.id)
        assertEquals(1, newState.drawPile.size)
    }

    @Test
    fun drawCardReshufflesWhenDrawPileIsEmpty() {
        val top = NumberCard(CardColor.RED, 5)
        val discardPile = listOf(
            NumberCard(CardColor.BLUE, 1),
            NumberCard(CardColor.YELLOW, 2),
            NumberCard(CardColor.GREEN, 3),
            top,
        )
        val state = GameState(
            players = listOf(Player("p1", "A"), Player("p2", "B")),
            drawPile = emptyList(),
            discardPile = discardPile,
            currentPlayerIndex = 0,
            direction = PlayDirection.CLOCKWISE,
            activeColor = CardColor.RED,
        )

        val newState = assertSuccess(
            Engine.applyAction(state, GameAction.DrawCard("p1"), fixedRandom),
        )

        // p1 drew 1 card; discard pile reduced to just the old top.
        assertEquals(1, newState.players[0].handSize)
        assertEquals(top, newState.topDiscard)
        assertEquals(1, newState.discardPile.size)
        // Draw pile holds the reshuffled remainder: 3 cards - 1 drawn = 2.
        assertEquals(2, newState.drawPile.size)
    }

    @Test
    fun drawCardFromWrongPlayerFails() {
        val state = threePlayerState()
        val result = Engine.applyAction(state, GameAction.DrawCard("p2"), fixedRandom)
        assertFailure(result, "Not p2's turn")
    }

    // ------------------------------------------------------------------
    // Determinism
    // ------------------------------------------------------------------

    @Test
    fun sameSeedProducesIdenticalReshuffle() {
        val state = GameState(
            players = listOf(Player("p1", "A"), Player("p2", "B")),
            drawPile = emptyList(),
            discardPile = listOf(
                NumberCard(CardColor.BLUE, 1),
                NumberCard(CardColor.YELLOW, 2),
                NumberCard(CardColor.GREEN, 3),
                NumberCard(CardColor.RED, 5),
            ),
            currentPlayerIndex = 0,
            direction = PlayDirection.CLOCKWISE,
            activeColor = CardColor.RED,
        )

        val a = assertSuccess(Engine.applyAction(state, GameAction.DrawCard("p1"), Random(42)))
        val b = assertSuccess(Engine.applyAction(state, GameAction.DrawCard("p1"), Random(42)))
        assertEquals(a, b)
    }

    @Test
    fun differentSeedsYieldDifferentReshuffle() {
        // Use a larger discard pile so there are enough permutations that two
        // independent shuffles are overwhelmingly likely to differ. We compare
        // the whole resulting draw-pile ordering, not just the single drawn
        // card, to stay robust against collisions on the first draw.
        val discardPile = listOf(
            NumberCard(CardColor.BLUE, 1),
            NumberCard(CardColor.YELLOW, 2),
            NumberCard(CardColor.GREEN, 3),
            NumberCard(CardColor.RED, 4),
            NumberCard(CardColor.BLUE, 5),
            NumberCard(CardColor.YELLOW, 6),
            NumberCard(CardColor.GREEN, 7),
            NumberCard(CardColor.RED, 8),
            NumberCard(CardColor.BLUE, 9),
            NumberCard(CardColor.RED, 5),
        )
        val state = GameState(
            players = listOf(Player("p1", "A"), Player("p2", "B")),
            drawPile = emptyList(),
            discardPile = discardPile,
            currentPlayerIndex = 0,
            direction = PlayDirection.CLOCKWISE,
            activeColor = CardColor.RED,
        )

        val a = assertSuccess(Engine.applyAction(state, GameAction.DrawCard("p1"), Random(1)))
        val b = assertSuccess(Engine.applyAction(state, GameAction.DrawCard("p1"), Random(2)))
        // Compare the post-reshuffle draw pile (high variance: 9! possible orderings)
        // plus the drawn card — that gives us the full shuffle outcome.
        assertNotEquals(
            a.drawPile to a.players[0].hand,
            b.drawPile to b.players[0].hand,
        )
    }

    // ------------------------------------------------------------------
    // Drawing invalidates prior UNO call
    // ------------------------------------------------------------------

    @Test
    fun drawingInvalidatesPriorUnoFlag() {
        val state = threePlayerState(
            p1Hand = listOf(NumberCard(CardColor.BLUE, 7)), // 1 card
        ).let { s ->
            s.copy(
                players = s.players.mapIndexed { i, p ->
                    if (i == 0) p.copy(hasCalledUno = true) else p
                },
            )
        }

        val newState = assertSuccess(
            Engine.applyAction(state, GameAction.DrawCard("p1"), fixedRandom),
        )

        assertTrue(!newState.players[0].hasCalledUno)
    }

    // ------------------------------------------------------------------
    // No reshuffle past single-card discard
    // ------------------------------------------------------------------

    @Test
    fun drawGracefullyStopsWhenNoMoreCardsAvailable() {
        // Pathological: draw pile empty, discard has only the top.
        val state = GameState(
            players = listOf(Player("p1", "A"), Player("p2", "B")),
            drawPile = emptyList(),
            discardPile = listOf(NumberCard(CardColor.RED, 5)),
            currentPlayerIndex = 0,
            direction = PlayDirection.CLOCKWISE,
            activeColor = CardColor.RED,
        )

        val newState = assertSuccess(
            Engine.applyAction(state, GameAction.DrawCard("p1"), fixedRandom),
        )

        // No cards were actually drawn; turn still advances.
        assertEquals(0, newState.players[0].handSize)
        assertEquals("p2", newState.currentPlayer.id)
        assertNull(newState.winnerId)
    }
}
