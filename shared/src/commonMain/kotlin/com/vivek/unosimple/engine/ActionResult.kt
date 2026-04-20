package com.vivek.unosimple.engine

import com.vivek.unosimple.engine.models.GameState

/**
 * Outcome of [Engine.applyAction]. Either the action succeeded, yielding a new
 * [GameState], or it was invalid with a human-readable [reason] and the state
 * is unchanged.
 *
 * We don't throw from the engine — invalid actions are a normal part of a
 * running game (e.g. a UI sends a stale action) and must be communicable to
 * the caller without stack unwinding.
 */
sealed interface ActionResult {
    data class Success(val state: GameState) : ActionResult
    data class Failure(val state: GameState, val reason: String) : ActionResult
}
