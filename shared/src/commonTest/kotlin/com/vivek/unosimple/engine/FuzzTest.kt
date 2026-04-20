package com.vivek.unosimple.engine

import com.vivek.unosimple.engine.models.Deck
import com.vivek.unosimple.engine.models.GameAction
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.engine.models.NumberCard
import com.vivek.unosimple.engine.models.isWild
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Property-based / fuzz tests. Random agents drive random legal actions against
 * the engine across many seeds, asserting core invariants hold throughout.
 *
 * This is the backstop against "the edge case we didn't think of": if we can
 * play N random games without the total-card-count invariant breaking or a
 * non-currentPlayer's hand mutating, the engine is probably correct.
 */
class FuzzTest {

    /**
     * Core invariant: the total number of cards in the game never changes.
     * Draw pile + discard pile + all hands must always equal 108.
     */
    private fun totalCards(state: GameState): Int =
        state.drawPile.size + state.discardPile.size + state.players.sumOf { it.handSize }

    @Test
    fun cardCountIsPreservedAcrossRandomPlayouts() {
        repeat(NUM_SEEDS) { seed ->
            val rng = Random(seed.toLong())
            var state = newRound(
                listOf("p1" to "A", "p2" to "B", "p3" to "C", "p4" to "D"),
                rng,
            )
            assertEquals(Deck.TOTAL_CARDS, totalCards(state), "initial deal, seed=$seed")

            var steps = 0
            while (!state.isRoundOver && steps < MAX_STEPS_PER_GAME) {
                val action = pickRandomLegalAction(state, rng)
                val result = Engine.applyAction(state, action, rng)
                when (result) {
                    is ActionResult.Success -> {
                        state = result.state
                        assertEquals(
                            Deck.TOTAL_CARDS,
                            totalCards(state),
                            "after step $steps, seed=$seed, action=$action",
                        )
                    }
                    is ActionResult.Failure -> fail(
                        "Unexpected failure on seed=$seed step=$steps action=$action: ${result.reason}",
                    )
                }
                steps++
            }
        }
    }

    @Test
    fun currentPlayerIndexAlwaysInRange() {
        repeat(NUM_SEEDS) { seed ->
            val rng = Random(seed.toLong())
            var state = newRound(
                listOf("p1" to "A", "p2" to "B", "p3" to "C"),
                rng,
            )
            var steps = 0
            while (!state.isRoundOver && steps < MAX_STEPS_PER_GAME) {
                assertTrue(
                    state.currentPlayerIndex in state.players.indices,
                    "currentPlayerIndex out of range on seed=$seed step=$steps",
                )
                val action = pickRandomLegalAction(state, rng)
                val result = Engine.applyAction(state, action, rng)
                state = (result as ActionResult.Success).state
                steps++
            }
        }
    }

    @Test
    fun topDiscardNeverFalsifiesGameState() {
        // The top discard is always a valid (non-wild) card to match activeColor
        // against, OR a wild whose chosenColor lives in activeColor.
        repeat(NUM_SEEDS) { seed ->
            val rng = Random(seed.toLong())
            var state = newRound(listOf("p1" to "A", "p2" to "B"), rng)
            var steps = 0
            while (!state.isRoundOver && steps < MAX_STEPS_PER_GAME) {
                val top = state.topDiscard
                if (!top.isWild) {
                    assertEquals(
                        top.color,
                        state.activeColor,
                        "non-wild top must match activeColor, seed=$seed step=$steps",
                    )
                }
                val result = Engine.applyAction(
                    state,
                    pickRandomLegalAction(state, rng),
                    rng,
                )
                state = (result as ActionResult.Success).state
                steps++
            }
        }
    }

    @Test
    fun gamesEventuallyEnd() {
        // Most random-playout games end well under the 1000-step cap. This
        // test would fail if we had an infinite-loop bug in the engine.
        var completed = 0
        repeat(NUM_SEEDS) { seed ->
            val rng = Random(seed.toLong())
            var state = newRound(
                listOf("p1" to "A", "p2" to "B", "p3" to "C"),
                rng,
            )
            var steps = 0
            while (!state.isRoundOver && steps < MAX_STEPS_PER_GAME) {
                state = (Engine.applyAction(state, pickRandomLegalAction(state, rng), rng) as ActionResult.Success).state
                steps++
            }
            if (state.isRoundOver) completed++
        }
        // With random play, some games may hit the step cap — but the vast
        // majority should finish naturally. Allow a generous threshold.
        assertTrue(
            completed >= (NUM_SEEDS * 9) / 10,
            "Only $completed / $NUM_SEEDS games reached a natural end",
        )
    }

    /**
     * Choose a legal action for the current player. Prefers PlayCard on a
     * random legal hand card; falls back to DrawCard when no card is playable.
     * For wild plays, picks a random color.
     */
    private fun pickRandomLegalAction(state: GameState, rng: Random): GameAction {
        val actor = state.currentPlayer
        val legal = Rules.legalPlaysFor(state, actor.hand)
        if (legal.isEmpty()) return GameAction.DrawCard(actor.id)

        val card = legal.random(rng)
        val chosenColor = if (card.isWild) {
            com.vivek.unosimple.engine.models.CardColor.entries.random(rng)
        } else {
            null
        }
        // Randomly declare UNO about half the time — exercises both paths.
        val declareUno = actor.hand.size == 2 && rng.nextBoolean()
        return GameAction.PlayCard(
            playerId = actor.id,
            card = card,
            chosenColor = chosenColor,
            declareUno = declareUno,
        )
    }

    companion object {
        /** Number of random seeds to run per property. Balances coverage and test time. */
        private const val NUM_SEEDS: Int = 50

        /**
         * Upper bound on actions per simulated game. Real UNO games finish in
         * well under 200 actions; this cap is a safety net against engine
         * bugs producing infinite loops.
         */
        private const val MAX_STEPS_PER_GAME: Int = 1000
    }
}
