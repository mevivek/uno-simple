package com.vivek.unosimple.engine

import com.vivek.unosimple.engine.models.Card
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.DrawTwoCard
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.engine.models.NumberCard
import com.vivek.unosimple.engine.models.PendingDrawType
import com.vivek.unosimple.engine.models.ReverseCard
import com.vivek.unosimple.engine.models.SkipCard
import com.vivek.unosimple.engine.models.WildCard
import com.vivek.unosimple.engine.models.WildDrawFourCard
import com.vivek.unosimple.engine.models.isWild

/**
 * Pure rule helpers. Called by [Engine] and by bots to decide which plays are
 * legal. No state mutation, no I/O.
 */
object Rules {

    /**
     * True if [card] can legally be played on top of the current discard pile
     * given [state]. Classic UNO match rules:
     *
     * - Wild cards (WildCard, WildDrawFourCard) are always legal.
     * - A colored card matches if its color equals [GameState.activeColor], OR
     *   if it matches the top discard's type/value (same number, both Skip,
     *   both Reverse, both DrawTwo).
     *
     * (Classic UNO's "WildDrawFour challenge" rule — only playable when you
     *  have no card matching the active color — is not enforced in v1. It's
     *  a known simplification tracked in the roadmap.)
     */
    fun isLegalPlay(state: GameState, card: Card): Boolean {
        // Stacking: when a +2 / +4 stack is pending, the only legal plays are
        // matching stack cards (Mattel + common house rules — +2 stacks on +2,
        // +4 stacks on +4). Any other card forces the current player to eat
        // the stack via a draw action.
        if (state.pendingDraw > 0) {
            return when (state.pendingDrawType) {
                PendingDrawType.DRAW_TWO -> card is DrawTwoCard
                PendingDrawType.DRAW_FOUR -> card is WildDrawFourCard
                null -> false
            }
        }

        if (card.isWild) return true
        val top = state.topDiscard

        // Same color as currently active → always legal.
        if (card.color == state.activeColor) return true

        // Otherwise must match the top card by type/value.
        return when (card) {
            is NumberCard -> top is NumberCard && top.number == card.number
            is SkipCard -> top is SkipCard
            is ReverseCard -> top is ReverseCard
            is DrawTwoCard -> top is DrawTwoCard
            WildCard, WildDrawFourCard -> true // unreachable due to isWild guard above
        }
    }

    /** The subset of a player's hand that can legally be played right now. */
    fun legalPlaysFor(state: GameState, hand: List<Card>): List<Card> =
        hand.filter { isLegalPlay(state, it) }

    /**
     * Validates the color choice that accompanies a wild card. For wild cards
     * the caller MUST provide a concrete [CardColor]; for non-wild cards the
     * color choice must be null (chosenColor is ignored but musn't mislead).
     */
    fun validateColorChoice(card: Card, chosenColor: CardColor?): String? {
        return if (card.isWild) {
            if (chosenColor == null) "Wild cards require a chosenColor" else null
        } else {
            if (chosenColor != null) "Non-wild cards must not specify chosenColor" else null
        }
    }
}
