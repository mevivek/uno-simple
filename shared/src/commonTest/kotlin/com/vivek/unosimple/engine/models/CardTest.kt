package com.vivek.unosimple.engine.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CardTest {
    @Test
    fun numberCardAcceptsValidRange() {
        for (n in 0..9) NumberCard(CardColor.RED, n) // no throw
    }

    @Test
    fun numberCardRejectsOutOfRange() {
        assertFailsWith<IllegalArgumentException> { NumberCard(CardColor.RED, -1) }
        assertFailsWith<IllegalArgumentException> { NumberCard(CardColor.RED, 10) }
    }

    @Test
    fun wildCardsHaveNoColor() {
        assertNull(WildCard.color)
        assertNull(WildDrawFourCard.color)
    }

    @Test
    fun coloredCardsReportTheirColor() {
        assertEquals(CardColor.GREEN, NumberCard(CardColor.GREEN, 5).color)
        assertEquals(CardColor.BLUE, SkipCard(CardColor.BLUE).color)
        assertEquals(CardColor.RED, ReverseCard(CardColor.RED).color)
        assertEquals(CardColor.YELLOW, DrawTwoCard(CardColor.YELLOW).color)
    }

    @Test
    fun isWildDistinguishesWilds() {
        assertTrue(WildCard.isWild)
        assertTrue(WildDrawFourCard.isWild)
        assertFalse(NumberCard(CardColor.RED, 3).isWild)
        assertFalse(SkipCard(CardColor.RED).isWild)
        assertFalse(ReverseCard(CardColor.RED).isWild)
        assertFalse(DrawTwoCard(CardColor.RED).isWild)
    }

    @Test
    fun scoringFollowsMattelRules() {
        // Numbered cards = face value
        for (n in 0..9) assertEquals(n, NumberCard(CardColor.RED, n).scoreValue)
        // Action cards = 20
        assertEquals(20, SkipCard(CardColor.RED).scoreValue)
        assertEquals(20, ReverseCard(CardColor.BLUE).scoreValue)
        assertEquals(20, DrawTwoCard(CardColor.GREEN).scoreValue)
        // Wild cards = 50
        assertEquals(50, WildCard.scoreValue)
        assertEquals(50, WildDrawFourCard.scoreValue)
    }

    @Test
    fun equalColoredCardsCompareEqual() {
        assertEquals(NumberCard(CardColor.RED, 5), NumberCard(CardColor.RED, 5))
        assertEquals(SkipCard(CardColor.BLUE), SkipCard(CardColor.BLUE))
    }

    @Test
    fun differentCardsCompareUnequal() {
        assertTrue(NumberCard(CardColor.RED, 5) != NumberCard(CardColor.RED, 6))
        assertTrue(NumberCard(CardColor.RED, 5) != NumberCard(CardColor.BLUE, 5))
        assertTrue(SkipCard(CardColor.RED) != ReverseCard(CardColor.RED))
    }
}
