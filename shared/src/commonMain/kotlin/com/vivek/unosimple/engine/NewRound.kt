package com.vivek.unosimple.engine

import com.vivek.unosimple.engine.models.Card
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.Deck
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.engine.models.NumberCard
import com.vivek.unosimple.engine.models.PlayDirection
import com.vivek.unosimple.engine.models.Player
import kotlin.random.Random

/**
 * Construct the starting [GameState] for a new round.
 *
 * Deals [HAND_SIZE] cards to each player, flips the top card as the starter,
 * and sets the active color accordingly. We simplify the classic "first
 * flipped card is an action card" ruleset by filtering the starter to a
 * [NumberCard] — the first number card in the shuffled deck becomes the
 * starter; any action / wild cards encountered before it are pushed to the
 * bottom of the draw pile. This is a v1 simplification tracked in the
 * roadmap and keeps the opening state deterministic without special-casing
 * five different card types.
 *
 * @throws IllegalArgumentException if [playerSeats] is outside `2..10`
 */
fun newRound(
    playerSeats: List<Pair<String, String>>, // (id, name) pairs in turn order
    random: Random,
    handSize: Int = HAND_SIZE,
): GameState {
    require(playerSeats.size in GameState.MIN_PLAYERS..GameState.MAX_PLAYERS) {
        "UNO supports ${GameState.MIN_PLAYERS}..${GameState.MAX_PLAYERS} players, got ${playerSeats.size}"
    }
    require(playerSeats.map { it.first }.toSet().size == playerSeats.size) {
        "Player ids must be unique"
    }

    val deck: ArrayDeque<Card> = ArrayDeque(Deck.shuffled(random))

    // Deal hands first.
    val players = playerSeats.map { (id, name) ->
        val hand = List(handSize) { deck.removeFirst() }
        Player(id = id, name = name, hand = hand)
    }

    // Find the first NumberCard in the remaining deck as the starter; rotate
    // any non-number cards encountered to the back so they'll be drawn later.
    val rotated = ArrayDeque<Card>()
    var starter: NumberCard? = null
    while (deck.isNotEmpty()) {
        val c = deck.removeFirst()
        if (starter == null && c is NumberCard) {
            starter = c
        } else {
            rotated.addLast(c)
        }
    }
    checkNotNull(starter) { "Deck unexpectedly had no NumberCard after dealing" }

    return GameState(
        players = players,
        drawPile = rotated.toList(),
        discardPile = listOf(starter),
        currentPlayerIndex = 0,
        direction = PlayDirection.CLOCKWISE,
        activeColor = starter.color,
    )
}

/** Classic UNO opening-hand size. */
const val HAND_SIZE: Int = 7
