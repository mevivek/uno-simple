package com.vivek.unosimple.engine.models

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayDirectionTest {
    @Test
    fun reverseFlips() {
        assertEquals(PlayDirection.COUNTER_CLOCKWISE, PlayDirection.CLOCKWISE.reversed())
        assertEquals(PlayDirection.CLOCKWISE, PlayDirection.COUNTER_CLOCKWISE.reversed())
    }

    @Test
    fun reversedTwiceIsIdentity() {
        for (d in PlayDirection.entries) assertEquals(d, d.reversed().reversed())
    }

    @Test
    fun stepIsPlusOrMinusOne() {
        assertEquals(1, PlayDirection.CLOCKWISE.step())
        assertEquals(-1, PlayDirection.COUNTER_CLOCKWISE.step())
    }
}
