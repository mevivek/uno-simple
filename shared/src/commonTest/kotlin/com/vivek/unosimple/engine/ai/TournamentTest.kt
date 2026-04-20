package com.vivek.unosimple.engine.ai

import com.vivek.unosimple.engine.ActionResult
import com.vivek.unosimple.engine.Engine
import com.vivek.unosimple.engine.newRound
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tournament-style test: two strategies play N head-to-head games and the
 * smart bot must win a supermajority. This is the "credible play" benchmark
 * the roadmap asks for (Phase 2 definition of done).
 */
class TournamentTest {

    @Test
    fun heuristicBotBeatsRandomBotInHeadToHead() {
        val games = 200
        var heuristicWins = 0
        var draws = 0 // step cap reached without a winner

        // Alternate who sits in seat 0 to remove first-player-advantage bias.
        for (i in 0 until games) {
            val swap = i % 2 == 0
            val result = playOneGame(
                seed = i.toLong() + 1,
                p1Bot = if (swap) HeuristicBot() else RandomBot(),
                p2Bot = if (swap) RandomBot() else HeuristicBot(),
            )
            when (result) {
                "p1" -> if (swap) heuristicWins++
                "p2" -> if (!swap) heuristicWins++
                null -> draws++
            }
        }

        val decided = games - draws
        val winRate = heuristicWins.toDouble() / decided
        assertTrue(
            winRate >= 0.60,
            "HeuristicBot won $heuristicWins/$decided = ${(winRate * 100).toInt()}% (draws=$draws); expected >=60%",
        )
        // Also require that the vast majority of games finish naturally.
        assertTrue(
            draws <= games / 20,
            "$draws of $games games hit the step cap without a winner — engine or bot may be looping",
        )
    }

    /**
     * Play one two-player game. Returns the winner id or null if the step
     * cap was reached.
     */
    private fun playOneGame(seed: Long, p1Bot: Bot, p2Bot: Bot): String? {
        val rng = Random(seed)
        var state = newRound(
            listOf("p1" to "A", "p2" to "B"),
            rng,
        )
        var steps = 0
        while (!state.isRoundOver && steps < MAX_STEPS_PER_GAME) {
            val currentBot = if (state.currentPlayer.id == "p1") p1Bot else p2Bot
            val action = currentBot.chooseAction(state, rng)
            val result = Engine.applyAction(state, action, rng)
            state = (result as ActionResult.Success).state
            steps++
        }
        return state.winnerId
    }

    companion object {
        private const val MAX_STEPS_PER_GAME = 2000
    }
}
