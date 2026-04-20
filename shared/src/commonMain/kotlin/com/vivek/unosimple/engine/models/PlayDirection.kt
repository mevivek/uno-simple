package com.vivek.unosimple.engine.models

import kotlinx.serialization.Serializable

/** Turn order direction. Reversed by the Reverse card. */
@Serializable
enum class PlayDirection {
    CLOCKWISE,
    COUNTER_CLOCKWISE;

    /** The direction obtained by playing a Reverse card. */
    fun reversed(): PlayDirection = when (this) {
        CLOCKWISE -> COUNTER_CLOCKWISE
        COUNTER_CLOCKWISE -> CLOCKWISE
    }

    /**
     * Step value applied to `currentPlayerIndex` in a list of size [playerCount].
     * +1 for clockwise, -1 for counter-clockwise. Callers still need to apply
     * modular arithmetic: `(index + step + playerCount) % playerCount`.
     */
    fun step(): Int = when (this) {
        CLOCKWISE -> 1
        COUNTER_CLOCKWISE -> -1
    }
}
