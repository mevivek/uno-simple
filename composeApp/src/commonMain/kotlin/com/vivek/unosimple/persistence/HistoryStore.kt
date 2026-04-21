package com.vivek.unosimple.persistence

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Append-only log of every completed round. Backs the Stats + Achievements
 * dashboards. Persisted as JSON in [SessionStore]; because that's tiny
 * in-memory on non-Wasm targets, the log effectively lives only for the
 * current session on Android / iOS / Desktop until those targets gain
 * proper persistence — acceptable for a hobby build.
 */
@Serializable
data class RoundRecord(
    /** Epoch-millis-ish clock; wall-clock on Wasm, system time elsewhere. */
    val endedAtMs: Long,
    /** Seat ids at the table, in turn order. */
    val seats: List<String>,
    /** Display names, parallel to [seats]. */
    val names: List<String>,
    /** Winner's seat id. */
    val winnerId: String,
    /** Winner's seat name (denormalized so renaming later doesn't lose history). */
    val winnerName: String,
    /** Score delta the winner collected this round. */
    val deltaPoints: Int,
    /** Cumulative score per seat at round end, parallel to [seats]. */
    val cumulativeScores: List<Int>,
    /** Whether the human player was the winner. */
    val humanWon: Boolean,
    /** Bot roster for the session (e.g. "Rosie", "Max" for solo runs). */
    val botNames: List<String>,
)

@Serializable
private data class HistoryPayload(val records: List<RoundRecord> = emptyList())

/**
 * Aggregate view of the history — cheap derived values the Stats screen
 * can render without re-walking the whole list on every frame.
 */
data class HistoryStats(
    val gamesPlayed: Int,
    val gamesWon: Int,
    val currentWinStreak: Int,
    val bestWinStreak: Int,
    val bestRoundDelta: Int,
    /** Opponent → times faced. */
    val opponentCounts: Map<String, Int>,
    /** Opponent → times the human lost to them. */
    val opponentLosses: Map<String, Int>,
) {
    val winRatePct: Int get() = if (gamesPlayed == 0) 0 else (gamesWon * 100) / gamesPlayed
}

/**
 * Singleton-style repository for round history. Load at construction,
 * append on every round end, emit a [StateFlow] for observers. Mirrors
 * the shape of [com.vivek.unosimple.profile.ProfileRepository].
 */
class HistoryRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val _records = MutableStateFlow<List<RoundRecord>>(loadInitial())
    val records: StateFlow<List<RoundRecord>> = _records.asStateFlow()

    /** Append a new record + persist. */
    fun append(record: RoundRecord) {
        _records.value = _records.value + record
        persist()
    }

    /** Wipe all history — exposed for a future "reset stats" settings action. */
    fun clear() {
        _records.value = emptyList()
        SessionStore.write(KEY, null)
    }

    /** Derived aggregate for the Stats screen. */
    fun stats(humanId: String): HistoryStats {
        val all = _records.value
        val human = all.filter { humanId in it.seats }
        val gamesPlayed = human.size
        val wins = human.count { it.humanWon }
        val current = run {
            var n = 0
            for (r in human.reversed()) {
                if (r.humanWon) n++ else break
            }
            n
        }
        val best = run {
            var best = 0; var cur = 0
            human.forEach { r ->
                if (r.humanWon) { cur++; if (cur > best) best = cur } else cur = 0
            }
            best
        }
        val bestDelta = human.filter { it.humanWon }.maxOfOrNull { it.deltaPoints } ?: 0
        val counts = mutableMapOf<String, Int>()
        val losses = mutableMapOf<String, Int>()
        human.forEach { r ->
            r.botNames.forEach { counts[it] = (counts[it] ?: 0) + 1 }
            if (!r.humanWon) {
                val winner = r.winnerName
                losses[winner] = (losses[winner] ?: 0) + 1
            }
        }
        return HistoryStats(
            gamesPlayed = gamesPlayed,
            gamesWon = wins,
            currentWinStreak = current,
            bestWinStreak = best,
            bestRoundDelta = bestDelta,
            opponentCounts = counts,
            opponentLosses = losses,
        )
    }

    private fun loadInitial(): List<RoundRecord> {
        val raw = SessionStore.read(KEY) ?: return emptyList()
        return runCatching { json.decodeFromString<HistoryPayload>(raw).records }.getOrElse { emptyList() }
    }

    private fun persist() {
        SessionStore.write(KEY, json.encodeToString(HistoryPayload(_records.value)))
    }

    companion object {
        private const val KEY = "uno.history.v1"
    }
}
