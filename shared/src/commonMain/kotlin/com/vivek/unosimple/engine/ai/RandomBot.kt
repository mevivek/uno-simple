package com.vivek.unosimple.engine.ai

import com.vivek.unosimple.engine.Rules
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.GameAction
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.engine.models.isWild

/**
 * Baseline bot that picks a legal action uniformly at random. Used mainly as
 * a benchmark for smarter strategies — any bot worth shipping should reliably
 * beat [RandomBot] head-to-head.
 *
 * Also useful as a sanity check: running random agents against
 * [com.vivek.unosimple.engine.Engine.applyAction] in fuzz tests exposes state
 * transitions the code hasn't anticipated.
 */
class RandomBot : Bot {
    override fun chooseAction(state: GameState, random: kotlin.random.Random): GameAction {
        val me = state.currentPlayer
        val legal = Rules.legalPlaysFor(state, me.hand)
        if (legal.isEmpty()) return GameAction.DrawCard(me.id)

        val card = legal.random(random)
        val chosenColor = if (card.isWild) CardColor.entries.random(random) else null
        val declareUno = me.hand.size == 2
        return GameAction.PlayCard(
            playerId = me.id,
            card = card,
            chosenColor = chosenColor,
            declareUno = declareUno,
        )
    }
}
