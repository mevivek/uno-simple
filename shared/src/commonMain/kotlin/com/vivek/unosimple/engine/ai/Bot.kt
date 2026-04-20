package com.vivek.unosimple.engine.ai

import com.vivek.unosimple.engine.models.GameAction
import com.vivek.unosimple.engine.models.GameState
import kotlin.random.Random

/**
 * Picks one action for the current player in [state].
 *
 * Bots are conceptually pure: given the same state and the same seeded
 * [Random], they must return the same action. This lets us replay games
 * deterministically from a seed and keeps bot behavior testable.
 *
 * Implementations must never return an illegal action — any call site can
 * trust the returned [GameAction] and feed it straight into
 * [com.vivek.unosimple.engine.Engine.applyAction] without extra validation.
 */
fun interface Bot {
    fun chooseAction(state: GameState, random: Random): GameAction
}
