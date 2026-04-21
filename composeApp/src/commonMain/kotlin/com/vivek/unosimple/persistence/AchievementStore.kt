package com.vivek.unosimple.persistence

import com.vivek.unosimple.engine.achievements.Achievement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Tracks which [Achievement]s the human has unlocked. Persisted under a
 * dedicated key so history + achievements can be reset independently.
 *
 * [recordRound] evaluates every criterion after each round and returns the
 * newly-unlocked ones — the caller typically queues them as toast /
 * celebration popovers.
 */
class AchievementRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private val _unlocked = MutableStateFlow(loadInitial())
    val unlocked: StateFlow<Set<Achievement>> = _unlocked.asStateFlow()

    /**
     * Re-evaluate every criterion against the rolling [history]. Returns
     * the set of achievements newly unlocked this round (possibly empty).
     * Uses [personaBeaten] to determine which unique personas the human
     * has beaten across the whole history.
     */
    fun recordRound(
        history: List<RoundRecord>,
        humanId: String,
        knownPersonas: Set<String>,
    ): Set<Achievement> {
        val human = history.filter { humanId in it.seats }
        if (human.isEmpty()) return emptySet()

        val newly = mutableSetOf<Achievement>()
        val already = _unlocked.value

        fun check(a: Achievement, condition: () -> Boolean) {
            if (a !in already && condition()) newly += a
        }

        val wins = human.count { it.humanWon }
        val gamesPlayed = human.size
        val currentStreak = run {
            var n = 0
            for (r in human.reversed()) { if (r.humanWon) n++ else break }
            n
        }
        val bestDelta = human.filter { it.humanWon }.maxOfOrNull { it.deltaPoints } ?: 0
        val beatenPersonas = human.filter { it.humanWon }
            .flatMap { it.botNames }
            .toSet()

        check(Achievement.FIRST_WIN) { wins >= 1 }
        check(Achievement.STREAK_OF_THREE) { currentStreak >= 3 }
        check(Achievement.STREAK_OF_FIVE) { currentStreak >= 5 }
        check(Achievement.PLAYED_TEN) { gamesPlayed >= 10 }
        check(Achievement.BIG_POINTS) { bestDelta >= 100 }
        check(Achievement.MASSIVE_POINTS) { bestDelta >= 200 }
        check(Achievement.BEAT_ALL_PERSONAS) {
            knownPersonas.isNotEmpty() && knownPersonas.all { it in beatenPersonas }
        }

        if (newly.isNotEmpty()) {
            _unlocked.value = already + newly
            persist()
        }
        return newly
    }

    fun clear() {
        _unlocked.value = emptySet()
        SessionStore.write(KEY, null)
    }

    @Serializable
    private data class Payload(val unlocked: List<String> = emptyList())

    private fun loadInitial(): Set<Achievement> {
        val raw = SessionStore.read(KEY) ?: return emptySet()
        val names = runCatching { json.decodeFromString<Payload>(raw).unlocked }.getOrElse { emptyList() }
        return names.mapNotNull { n -> Achievement.entries.firstOrNull { it.name == n } }.toSet()
    }

    private fun persist() {
        SessionStore.write(
            KEY,
            json.encodeToString(Payload(_unlocked.value.map { it.name })),
        )
    }

    companion object {
        private const val KEY = "uno.achievements.v1"
    }
}
