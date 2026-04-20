package com.vivek.unosimple.engine.ai

import com.vivek.unosimple.engine.ActionResult
import com.vivek.unosimple.engine.Engine
import com.vivek.unosimple.engine.Rules
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.GameAction
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.engine.models.isWild
import com.vivek.unosimple.engine.newRound
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.fail

/**
 * Both bots must NEVER return an illegal action, across many randomly-
 * generated states. This is the hard contract that the UI and engine rely on
 * — if a bot returns garbage, we'd have to validate in every caller.
 */
class BotLegalityTest {

    @Test
    fun randomBotNeverReturnsIllegalAction() {
        assertAlwaysLegal("RandomBot", RandomBot())
    }

    @Test
    fun heuristicBotNeverReturnsIllegalAction() {
        assertAlwaysLegal("HeuristicBot", HeuristicBot())
    }

    @Test
    fun randomBotDeclaresUnoExactlyWhenAppropriate() {
        assertDeclareUnoContract("RandomBot", RandomBot())
    }

    @Test
    fun heuristicBotDeclaresUnoExactlyWhenAppropriate() {
        assertDeclareUnoContract("HeuristicBot", HeuristicBot())
    }

    @Test
    fun heuristicBotChosenColorMatchesHandDominance() {
        // When playing a wild, HeuristicBot should pick the color it holds most of.
        val bot = HeuristicBot()
        repeat(200) { seed ->
            val rng = Random(seed.toLong())
            var state = newRound(
                listOf("p1" to "A", "p2" to "B", "p3" to "C"),
                rng,
            )

            // Drive the game forward until the bot plays a wild (or the round ends).
            var steps = 0
            while (!state.isRoundOver && steps < 500) {
                val action = bot.chooseAction(state, rng)
                if (action is GameAction.PlayCard && action.card.isWild) {
                    val me = state.currentPlayer
                    val nonWildCounts = CardColor.entries.associateWith { c ->
                        me.hand.count { it.color == c }
                    }
                    val dominant = nonWildCounts.maxByOrNull { it.value }?.value ?: 0
                    val chosenCount = nonWildCounts[action.chosenColor] ?: 0
                    // Chosen color must have count equal to the dominant color's count.
                    if (chosenCount != dominant) {
                        fail(
                            "Seed=$seed step=$steps: HeuristicBot picked ${action.chosenColor} " +
                                "(count=$chosenCount) but dominant count was $dominant (counts=$nonWildCounts)",
                        )
                    }
                }
                state = (Engine.applyAction(state, action, rng) as ActionResult.Success).state
                steps++
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun assertAlwaysLegal(botName: String, bot: Bot) {
        repeat(NUM_SEEDS) { seed ->
            val rng = Random(seed.toLong())
            var state = newRound(
                listOf("p1" to "A", "p2" to "B", "p3" to "C", "p4" to "D"),
                rng,
            )
            var steps = 0
            while (!state.isRoundOver && steps < MAX_STEPS_PER_GAME) {
                val action = bot.chooseAction(state, rng)
                validateActionIsLegal(botName, state, action, seed, steps)
                val result = Engine.applyAction(state, action, rng)
                assertIs<ActionResult.Success>(
                    result,
                    "Seed=$seed step=$steps: $botName returned action=$action that engine rejected",
                )
                state = result.state
                steps++
            }
        }
    }

    private fun validateActionIsLegal(
        botName: String,
        state: GameState,
        action: GameAction,
        seed: Int,
        step: Int,
    ) {
        val me = state.currentPlayer
        val legal = Rules.legalPlaysFor(state, me.hand)
        when (action) {
            is GameAction.PlayCard -> {
                require(action.playerId == me.id) {
                    "$botName[$seed/$step] played as ${action.playerId} but it's ${me.id}'s turn"
                }
                require(action.card in me.hand) {
                    "$botName[$seed/$step] plays ${action.card} not in hand=${me.hand}"
                }
                require(Rules.isLegalPlay(state, action.card)) {
                    "$botName[$seed/$step] plays illegal ${action.card} on ${state.topDiscard} (activeColor=${state.activeColor})"
                }
                if (action.card.isWild) {
                    requireNotNull(action.chosenColor) {
                        "$botName[$seed/$step] played wild without chosenColor"
                    }
                } else {
                    require(action.chosenColor == null) {
                        "$botName[$seed/$step] played non-wild with chosenColor"
                    }
                }
            }
            is GameAction.DrawCard -> {
                // Draw is always valid as long as it's this player's turn.
                require(action.playerId == me.id) {
                    "$botName[$seed/$step] drew as ${action.playerId} but it's ${me.id}'s turn"
                }
                // It's also fine to draw when you COULD have played — don't force the bot.
                @Suppress("UNUSED_VARIABLE")
                val _unused = legal // (intentional no-op; keeps the parameter meaningful)
            }
        }
    }

    private fun assertDeclareUnoContract(botName: String, bot: Bot) {
        // The simple contract bots implement: declareUno == (handSize == 2).
        // A missed declaration would cause a 2-card penalty; an unnecessary one
        // is benign but we want consistency for now.
        repeat(100) { seed ->
            val rng = Random(seed.toLong())
            var state = newRound(
                listOf("p1" to "A", "p2" to "B"),
                rng,
            )
            var steps = 0
            while (!state.isRoundOver && steps < MAX_STEPS_PER_GAME) {
                val action = bot.chooseAction(state, rng)
                if (action is GameAction.PlayCard) {
                    val before = state.currentPlayer.hand.size
                    val shouldDeclare = before == 2
                    require(action.declareUno == shouldDeclare) {
                        "$botName[$seed/$steps]: expected declareUno=$shouldDeclare (handSize was $before) but was ${action.declareUno}"
                    }
                }
                state = (Engine.applyAction(state, action, rng) as ActionResult.Success).state
                steps++
            }
        }
    }

    companion object {
        private const val NUM_SEEDS = 100
        private const val MAX_STEPS_PER_GAME = 1000
    }
}
