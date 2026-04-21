package com.vivek.unosimple.engine.achievements

/**
 * Enumerable set of unlockable badges. Evaluation criteria are owned by
 * the consumer (ViewModel / HistoryRepository) — this file just declares
 * what exists + their display metadata. Kept in `:shared` so the engine
 * + tests can reason about unlock conditions without pulling in Compose.
 */
enum class Achievement(
    val displayName: String,
    val description: String,
) {
    FIRST_WIN(
        displayName = "First Blood",
        description = "Win your first round.",
    ),
    STREAK_OF_THREE(
        displayName = "Hat Trick",
        description = "Win three rounds in a row.",
    ),
    STREAK_OF_FIVE(
        displayName = "On Fire",
        description = "Win five rounds in a row.",
    ),
    PLAYED_TEN(
        displayName = "Ten Rounds In",
        description = "Finish ten rounds (win or lose).",
    ),
    BIG_POINTS(
        displayName = "Big Haul",
        description = "Win a round worth at least 100 points.",
    ),
    MASSIVE_POINTS(
        displayName = "Jackpot",
        description = "Win a round worth at least 200 points.",
    ),
    BEAT_ALL_PERSONAS(
        displayName = "Roster Buster",
        description = "Beat every bot persona at least once.",
    ),
}
