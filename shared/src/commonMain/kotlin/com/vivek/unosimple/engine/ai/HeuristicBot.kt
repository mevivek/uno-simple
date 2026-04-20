package com.vivek.unosimple.engine.ai

import com.vivek.unosimple.engine.Rules
import com.vivek.unosimple.engine.models.Card
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.DrawTwoCard
import com.vivek.unosimple.engine.models.GameAction
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.engine.models.NumberCard
import com.vivek.unosimple.engine.models.Player
import com.vivek.unosimple.engine.models.ReverseCard
import com.vivek.unosimple.engine.models.SkipCard
import com.vivek.unosimple.engine.models.WildCard
import com.vivek.unosimple.engine.models.WildDrawFourCard
import com.vivek.unosimple.engine.models.isWild
import kotlin.random.Random

/**
 * Simple heuristic player. Strategy sketch:
 *
 * - **Always play if you can** — drawing is a last resort.
 * - **Save wild cards.** They're playable any time, so keep them for when
 *   you're genuinely stuck; playing them early wastes the flexibility.
 * - **Weaponize action cards when the next opponent is close to winning.**
 *   A Draw Two / Wild Draw Four aimed at a player with 1-2 cards is worth
 *   far more than one played early in the round.
 * - **Prefer cards of your dominant color** — playing off your big color
 *   keeps it as the active color, letting you unload more cards next turn.
 * - **Pick the wild color to match what you're holding** — choose the color
 *   you have the most of (non-wild cards only).
 * - **Declare UNO** when going from 2 → 1 cards (no reason not to).
 *
 * More sophisticated tactics (card counting, bluffing Wild Draw Fours,
 * modelling opponents' hands) are deferred; this level already plays
 * credibly and reliably beats [RandomBot].
 */
class HeuristicBot : Bot {

    override fun chooseAction(state: GameState, random: Random): GameAction {
        val me = state.currentPlayer
        val legal = Rules.legalPlaysFor(state, me.hand)
        if (legal.isEmpty()) return GameAction.DrawCard(me.id)

        val best = chooseBestCard(state, me, legal, random)
        val chosenColor = if (best.isWild) chooseWildColor(me) else null
        val declareUno = me.hand.size == 2
        return GameAction.PlayCard(
            playerId = me.id,
            card = best,
            chosenColor = chosenColor,
            declareUno = declareUno,
        )
    }

    /**
     * Score every legal candidate, return the best. Ties are broken randomly
     * using [random] so bot play isn't deterministic across runs for the same
     * state — but IS deterministic for a given (state, random) pair.
     */
    private fun chooseBestCard(
        state: GameState,
        me: Player,
        legal: List<Card>,
        random: Random,
    ): Card {
        val nextOpponent = state.players[state.nextPlayerIndex()]
        val nextIsVulnerable = nextOpponent.handSize <= 2

        data class Scored(val card: Card, val score: Int, val tiebreak: Int)

        val scored = legal.map { card ->
            Scored(
                card = card,
                score = scoreCard(card, me, nextIsVulnerable),
                tiebreak = random.nextInt(),
            )
        }
        return scored.sortedWith(
            compareByDescending<Scored> { it.score }.thenBy { it.tiebreak },
        ).first().card
    }

    private fun scoreCard(card: Card, me: Player, nextIsVulnerable: Boolean): Int {
        val baseByType = when (card) {
            is NumberCard -> 10
            is SkipCard -> if (nextIsVulnerable) 40 else 18
            is ReverseCard -> 15
            is DrawTwoCard -> if (nextIsVulnerable) 50 else 22
            WildCard -> 5                                  // keep for emergencies
            WildDrawFourCard -> if (nextIsVulnerable) 60 else 3
        }

        // Dominant-color bonus: playing off the color I hold most of keeps my
        // options open for next turn.
        val colorBonus = if (!card.isWild) {
            me.hand.count { it.color == card.color }
        } else {
            0
        }

        return baseByType + colorBonus
    }

    /**
     * When forced to pick a color for a wild, pick the color I hold the most
     * of. Ties go to RED (arbitrary but stable).
     */
    private fun chooseWildColor(me: Player): CardColor {
        val counts = CardColor.entries.associateWith { c -> me.hand.count { it.color == c } }
        val best = counts.entries.maxByOrNull { it.value }
        return best?.key ?: CardColor.RED
    }
}
