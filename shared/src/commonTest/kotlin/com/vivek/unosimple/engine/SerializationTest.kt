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
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The whole engine state must round-trip through JSON without loss. This is
 * the prerequisite for future server-authoritative multiplayer: the server
 * can reconstruct and replay any client's state from a wire message.
 */
class SerializationTest {

    private val json = Json { prettyPrint = false }

    private inline fun <reified T> roundTrip(value: T): T {
        val encoded = json.encodeToString<T>(value)
        val decoded = json.decodeFromString<T>(encoded)
        return decoded
    }

    // ------------------------------------------------------------------
    // Individual card types
    // ------------------------------------------------------------------

    @Test
    fun numberCardRoundTrips() {
        val card: Card = NumberCard(CardColor.RED, 7)
        assertEquals(card, roundTrip(card))
    }

    @Test
    fun skipCardRoundTrips() {
        val card: Card = SkipCard(CardColor.BLUE)
        assertEquals(card, roundTrip(card))
    }

    @Test
    fun reverseCardRoundTrips() {
        val card: Card = ReverseCard(CardColor.GREEN)
        assertEquals(card, roundTrip(card))
    }

    @Test
    fun drawTwoCardRoundTrips() {
        val card: Card = DrawTwoCard(CardColor.YELLOW)
        assertEquals(card, roundTrip(card))
    }

    @Test
    fun wildCardRoundTrips() {
        val card: Card = WildCard
        assertEquals(card, roundTrip(card))
    }

    @Test
    fun wildDrawFourCardRoundTrips() {
        val card: Card = WildDrawFourCard
        assertEquals(card, roundTrip(card))
    }

    @Test
    fun polymorphicDiscriminatorUsesExpectedNames() {
        // Regression guard: wire-format names are part of our API — we don't
        // want them changing by accident and silently breaking future clients.
        val encoded = json.encodeToString<Card>(NumberCard(CardColor.RED, 5))
        assertTrue(encoded.contains("\"type\":\"number\""), "got: $encoded")
        assertTrue(json.encodeToString<Card>(SkipCard(CardColor.RED)).contains("\"type\":\"skip\""))
        assertTrue(json.encodeToString<Card>(ReverseCard(CardColor.RED)).contains("\"type\":\"reverse\""))
        assertTrue(json.encodeToString<Card>(DrawTwoCard(CardColor.RED)).contains("\"type\":\"drawTwo\""))
        assertTrue(json.encodeToString<Card>(WildCard).contains("\"type\":\"wild\""))
        assertTrue(json.encodeToString<Card>(WildDrawFourCard).contains("\"type\":\"wildDrawFour\""))
    }

    // ------------------------------------------------------------------
    // Higher-level types
    // ------------------------------------------------------------------

    @Test
    fun playerRoundTrips() {
        val p = Player(
            id = "p1",
            name = "Vivek",
            hand = listOf(NumberCard(CardColor.RED, 3), WildCard, SkipCard(CardColor.BLUE)),
            hasCalledUno = true,
            score = 137,
        )
        assertEquals(p, roundTrip(p))
    }

    @Test
    fun gameStateRoundTrips() {
        val state = newRound(
            listOf("p1" to "A", "p2" to "B", "p3" to "C"),
            Random(42),
        )
        assertEquals(state, roundTrip(state))
    }

    @Test
    fun gameActionPlayCardRoundTrips() {
        val action: GameAction = GameAction.PlayCard(
            playerId = "p1",
            card = WildDrawFourCard,
            chosenColor = CardColor.GREEN,
            declareUno = true,
        )
        assertEquals(action, roundTrip(action))
    }

    @Test
    fun gameActionDrawCardRoundTrips() {
        val action: GameAction = GameAction.DrawCard("p2")
        assertEquals(action, roundTrip(action))
    }

    @Test
    fun playDirectionRoundTrips() {
        for (d in PlayDirection.entries) assertEquals(d, roundTrip(d))
    }
}
