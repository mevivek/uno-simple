package com.vivek.unosimple.engine

import com.vivek.unosimple.engine.models.Card
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.DrawTwoCard
import com.vivek.unosimple.engine.models.GameAction
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.engine.models.NumberCard
import com.vivek.unosimple.engine.models.PendingDrawType
import com.vivek.unosimple.engine.models.Player
import com.vivek.unosimple.engine.models.ReverseCard
import com.vivek.unosimple.engine.models.SkipCard
import com.vivek.unosimple.engine.models.WildCard
import com.vivek.unosimple.engine.models.WildDrawFourCard
import com.vivek.unosimple.engine.models.isWild
import kotlin.random.Random

/**
 * Pure game engine: `applyAction(state, action, random) -> ActionResult`.
 *
 * No mutation, no I/O, no top-level randomness. All state transitions are
 * encoded as a total function over the inputs — the same state + action +
 * seeded [Random] always yields the same outcome. This is the contract that
 * makes the engine trivially unit-testable, server-authoritative, and
 * replayable.
 *
 * Rules covered (ADR-004, classic UNO per Mattel instruction sheet 42001pr):
 * - Legal-play validation via [Rules.isLegalPlay].
 * - Action card effects: Skip, Reverse (2-player = skip), DrawTwo, WildDrawFour.
 * - Wild color choice baked into [GameAction.PlayCard.chosenColor].
 * - UNO-declaration penalty: if you play your second-to-last card without
 *   [GameAction.PlayCard.declareUno] = true, you draw **4** penalty cards
 *   (Mattel rule). In real play the penalty only fires if another player
 *   catches you before the next player's turn begins; v1 applies it
 *   automatically (conservative).
 * - Reshuffling the discard pile back into the draw pile when the draw pile
 *   is exhausted (the topmost discard stays as the new top).
 * - Win: `winnerId` is set when a player's hand becomes empty; the winner's
 *   score gains the sum of the other players' remaining hand values.
 *
 * Known v1 simplifications (tracked in the roadmap):
 * - No WildDrawFour challenge (Mattel: only legal when you have no
 *   matching-color card; challenger draws +2 if wrong).
 * - Starter card is always a NumberCard (Mattel rules define specific
 *   behavior for Skip / Reverse / DrawTwo / Wild / WildDrawFour as the
 *   opening discard).
 * - No 500-point multi-round game-end check.
 */
object Engine {

    /**
     * Classic Mattel rule: forgetting to declare UNO on your second-to-last
     * card costs you 4 penalty cards (instruction sheet 42001pr, "Going Out"
     * section). Exposed as a named constant so tests + docs share the same
     * value.
     */
    const val UNO_PENALTY_CARDS: Int = 4


    /** Apply [action] to [state]. Pure: never mutates inputs. */
    fun applyAction(state: GameState, action: GameAction, random: Random): ActionResult {
        if (state.isRoundOver) return ActionResult.Failure(state, "Round is already over")
        return when (action) {
            is GameAction.PlayCard -> applyPlayCard(state, action, random)
            is GameAction.DrawCard -> applyDrawCard(state, action, random)
        }
    }

    private fun applyPlayCard(
        state: GameState,
        action: GameAction.PlayCard,
        random: Random,
    ): ActionResult {
        // 1. It must be the actor's turn.
        val actor = state.currentPlayer
        if (actor.id != action.playerId) {
            return ActionResult.Failure(state, "Not ${action.playerId}'s turn (currently ${actor.id})")
        }

        // 2. The card must actually be in the actor's hand.
        val cardIndex = actor.hand.indexOf(action.card)
        if (cardIndex == -1) {
            return ActionResult.Failure(state, "${actor.id} does not hold ${action.card}")
        }

        // 3. Color-choice constraint (wild requires a color; non-wild forbids one).
        Rules.validateColorChoice(action.card, action.chosenColor)?.let {
            return ActionResult.Failure(state, it)
        }

        // 4. The card must be a legal play on top of the current discard.
        if (!Rules.isLegalPlay(state, action.card)) {
            return ActionResult.Failure(
                state,
                "${action.card} is not legal on ${state.topDiscard} (active color ${state.activeColor})",
            )
        }

        // --- Begin applying the play ---
        val newHand = actor.hand.toMutableList().also { it.removeAt(cardIndex) }
        val newActiveColor = if (action.card.isWild) action.chosenColor!! else action.card.color!!
        val newDiscard = state.discardPile + action.card

        // UNO declaration check: if going from 2 -> 1 cards and declareUno is
        // false, the actor must take the penalty. Mattel's official rule is
        // 4 cards (not 2) — the penalty is meant to be painful.
        val needsUnoPenalty = actor.hand.size == 2 && newHand.size == 1 && !action.declareUno
        val hasCalledUnoNext = newHand.size == 1 && action.declareUno
        var stateAfterPenalty: GameState = state
        var playersAfterPenalty: List<Player>
        var drawAfterPenalty: List<Card> = state.drawPile
        var discardAfterPenalty: List<Card> = newDiscard

        // Tentative player update (before Skip / DrawTwo effects push more cards).
        val updatedActor = actor.copy(hand = newHand, hasCalledUno = hasCalledUnoNext)
        playersAfterPenalty = state.players.mapIndexed { i, p -> if (i == state.currentPlayerIndex) updatedActor else p }

        if (needsUnoPenalty) {
            val (drawn, remainingDraw, newDiscardAfterReshuffle) =
                drawNFrom(drawAfterPenalty, discardAfterPenalty, UNO_PENALTY_CARDS, random)
            drawAfterPenalty = remainingDraw
            discardAfterPenalty = newDiscardAfterReshuffle
            val penalized = updatedActor.copy(hand = updatedActor.hand + drawn, hasCalledUno = false)
            playersAfterPenalty = playersAfterPenalty.mapIndexed { i, p -> if (i == state.currentPlayerIndex) penalized else p }
        }

        // Intermediate state before action-card effects.
        stateAfterPenalty = state.copy(
            players = playersAfterPenalty,
            drawPile = drawAfterPenalty,
            discardPile = discardAfterPenalty,
            activeColor = newActiveColor,
        )

        // Apply the per-card effect on the *next* player, and figure out whose
        // turn is next.
        val afterEffects = applyCardEffect(stateAfterPenalty, action.card, random)

        // Win check: current actor may have just emptied their hand. When
        // they do, the classic UNO rule awards them the sum of every other
        // player's remaining hand value (per [Card.scoreValue] — numbered
        // cards = face value, action cards = 20, wild cards = 50).
        val actorAfter = afterEffects.players[state.currentPlayerIndex]
        val final: GameState = if (actorAfter.hand.isEmpty()) {
            val scoreGained = afterEffects.players
                .filter { it.id != actorAfter.id }
                .sumOf { it.handScore }
            val scoredPlayers = afterEffects.players.map { p ->
                if (p.id == actorAfter.id) p.copy(score = p.score + scoreGained) else p
            }
            afterEffects.copy(players = scoredPlayers, winnerId = actorAfter.id)
        } else {
            afterEffects
        }
        return ActionResult.Success(final)
    }

    private fun applyDrawCard(
        state: GameState,
        action: GameAction.DrawCard,
        random: Random,
    ): ActionResult {
        val actor = state.currentPlayer
        if (actor.id != action.playerId) {
            return ActionResult.Failure(state, "Not ${action.playerId}'s turn (currently ${actor.id})")
        }

        // Case 1: a +2 / +4 stack is pending. Drawing means "I can't (or won't)
        // stack; I accept the penalty." The actor takes the whole stack, the
        // stack resets, and their turn ends (they don't get to play after).
        if (state.pendingDraw > 0) {
            val (drawn, remainingDraw, newDiscard) =
                drawNFrom(state.drawPile, state.discardPile, state.pendingDraw, random)
            val penalized = actor.copy(
                hand = actor.hand + drawn,
                hasCalledUno = false,
            )
            val newPlayers = state.players.mapIndexed { i, p ->
                if (i == state.currentPlayerIndex) penalized else p
            }
            return ActionResult.Success(
                state.copy(
                    players = newPlayers,
                    drawPile = remainingDraw,
                    discardPile = newDiscard,
                    currentPlayerIndex = state.nextPlayerIndex(),
                    pendingDraw = 0,
                    pendingDrawType = null,
                ),
            )
        }

        // Case 2: ordinary draw. Draw one card. If it's legal to play, keep
        // the turn on the actor; otherwise pass the turn.
        val (drawn, remainingDraw, newDiscard) = drawNFrom(state.drawPile, state.discardPile, 1, random)
        val newActor = actor.copy(
            hand = actor.hand + drawn,
            hasCalledUno = false,
        )
        val newPlayers = state.players.mapIndexed { i, p -> if (i == state.currentPlayerIndex) newActor else p }

        val drawnCard = drawn.firstOrNull()
        val canPlayDrawn = drawnCard != null && Rules.isLegalPlay(state, drawnCard)
        val nextIndex = if (canPlayDrawn) state.currentPlayerIndex else state.nextPlayerIndex()

        return ActionResult.Success(
            state.copy(
                players = newPlayers,
                drawPile = remainingDraw,
                discardPile = newDiscard,
                currentPlayerIndex = nextIndex,
            ),
        )
    }

    /**
     * Apply the post-play effect of [card] to [state]: update turn index
     * (skipping or reversing as needed) and push draw-penalty cards onto the
     * next player's hand.
     *
     * `state` here is the partial state *after* the card has been moved from
     * hand to discard and `activeColor` set, but *before* turn advancement
     * and any Draw-Two/Draw-Four effects.
     */
    private fun applyCardEffect(state: GameState, card: Card, random: Random): GameState {
        return when (card) {
            is NumberCard -> state.copy(currentPlayerIndex = state.nextPlayerIndex())

            is SkipCard -> {
                // Skip: advance two positions (past the next player).
                val skipTarget = state.advanceIndex(state.currentPlayerIndex, 2 * state.direction.step())
                state.copy(currentPlayerIndex = skipTarget)
            }

            is ReverseCard -> {
                val newDir = state.direction.reversed()
                // With 2 players, Reverse acts as Skip (you stay current).
                val newIndex = if (state.players.size == 2) {
                    state.currentPlayerIndex
                } else {
                    state.advanceIndex(state.currentPlayerIndex, newDir.step())
                }
                state.copy(direction = newDir, currentPlayerIndex = newIndex)
            }

            is DrawTwoCard -> {
                // Stacking: add 2 to the pending pile and pass the turn so the
                // next player gets a chance to stack their own +2. Whoever
                // breaks the chain (by drawing / playing something else illegally)
                // eats the whole stack via applyDrawCard.
                state.copy(
                    currentPlayerIndex = state.nextPlayerIndex(),
                    pendingDraw = state.pendingDraw + 2,
                    pendingDrawType = PendingDrawType.DRAW_TWO,
                )
            }

            WildCard -> state.copy(currentPlayerIndex = state.nextPlayerIndex())
            WildDrawFourCard -> state.copy(
                currentPlayerIndex = state.nextPlayerIndex(),
                pendingDraw = state.pendingDraw + 4,
                pendingDrawType = PendingDrawType.DRAW_FOUR,
            )
        }
    }

    /**
     * Draw [count] cards from the top of [drawPile], reshuffling [discardPile]
     * (except the top card) back into the draw pile when the draw pile is
     * exhausted. Returns the drawn cards plus the remaining piles.
     *
     * If even after reshuffling there aren't enough cards, we return as many
     * as we could — this is pathological (the full 108-card deck rarely runs
     * this dry) but we handle it gracefully rather than crashing.
     */
    internal fun drawNFrom(
        drawPile: List<Card>,
        discardPile: List<Card>,
        count: Int,
        random: Random,
    ): Triple<List<Card>, List<Card>, List<Card>> {
        val drawn = mutableListOf<Card>()
        var draw = drawPile
        var discard = discardPile

        repeat(count) {
            if (draw.isEmpty()) {
                if (discard.size <= 1) {
                    return@repeat // no cards to reshuffle; give up silently
                }
                // Keep the top discard, shuffle the rest into a new draw pile.
                val top = discard.last()
                val rest = discard.dropLast(1).shuffled(random)
                draw = rest
                discard = listOf(top)
            }
            if (draw.isEmpty()) return@repeat
            drawn.add(draw.first())
            draw = draw.drop(1)
        }

        return Triple(drawn, draw, discard)
    }
}
