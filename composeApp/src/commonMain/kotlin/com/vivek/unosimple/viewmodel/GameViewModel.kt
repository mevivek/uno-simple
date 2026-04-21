package com.vivek.unosimple.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vivek.unosimple.engine.ActionResult
import com.vivek.unosimple.engine.Engine
import com.vivek.unosimple.engine.HAND_SIZE
import com.vivek.unosimple.engine.ai.Bot
import com.vivek.unosimple.engine.ai.HeuristicBot
import com.vivek.unosimple.engine.newRound
import com.vivek.unosimple.engine.models.Card
import com.vivek.unosimple.engine.models.CardColor
import com.vivek.unosimple.engine.models.GameAction
import com.vivek.unosimple.engine.models.GameState
import com.vivek.unosimple.engine.achievements.Achievement
import com.vivek.unosimple.persistence.AchievementRepository
import com.vivek.unosimple.persistence.HistoryRepository
import com.vivek.unosimple.persistence.RoundRecord
import com.vivek.unosimple.persistence.SessionStore
import com.vivek.unosimple.ui.game.BOTS
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Holds the active round and exposes it to the UI as a [StateFlow]. Every
 * player action funnels through here:
 *
 *     UI → vm.playCard(card, color?) → Engine.applyAction → _state.value = new
 *
 * The engine itself is pure; this view-model is the bridge between that pure
 * core and the Compose side of the world, and also drives AI opponents.
 *
 * @property humanPlayerId identifier of the human-controlled seat. The UI
 *   shows this player's hand face-up and accepts tap-to-play input only from
 *   them. Bots occupy every other seat and are advanced by [runBotsUntilHuman].
 */
class GameViewModel(
    private val random: Random = Random.Default,
    private val bot: Bot = HeuristicBot(),
    private val botTurnDelayMs: Long = DEFAULT_BOT_TURN_DELAY_MS,
    /**
     * Id of the human-controlled seat. `null` enables all-bots mode — every
     * seat is driven by [bot] and the coroutine scheduler never pauses for
     * human input. Used for integration tests that play a full game through
     * the ViewModel without involving the UI.
     */
    val humanPlayerId: String? = "p1",
    /**
     * Repositories for append-only history + achievements. App-level VM
     * instances share a single repo; tests can inject fresh instances.
     */
    val history: HistoryRepository = HistoryRepository(),
    val achievements: AchievementRepository = AchievementRepository(),
) : ViewModel() {

    /** Newly-unlocked achievements from the most recent round, or empty. */
    private val _recentUnlocks = MutableStateFlow<Set<Achievement>>(emptySet())
    val recentUnlocks: StateFlow<Set<Achievement>> = _recentUnlocks.asStateFlow()

    private val _state = MutableStateFlow<GameState?>(null)
    val state: StateFlow<GameState?> = _state.asStateFlow()

    /** Tracks bot count for the active round so we can persist + restore it. */
    private var currentBotCount: Int = 0

    init {
        // Persist any non-null state change so a Wasm page reload can resume
        // the active round. Drop(1) skips the initial `null` emission so we
        // don't immediately wipe a just-loaded saved session with a null.
        viewModelScope.launch {
            var previousRoundOver = false
            _state.drop(1).collect { s ->
                if (s == null || s.isRoundOver) {
                    SessionStore.write(SESSION_KEY, null)
                } else {
                    SessionStore.write(
                        SESSION_KEY,
                        sessionJson.encodeToString(
                            SavedSession(botCount = currentBotCount, state = s)
                        ),
                    )
                }
                // First transition into round-over → write history + check
                // for newly-unlocked achievements.
                val nowRoundOver = s?.isRoundOver == true
                if (nowRoundOver && !previousRoundOver) {
                    recordRoundEnd(s)
                }
                previousRoundOver = nowRoundOver
            }
        }
    }

    /** Write the completed round to [history] + re-evaluate achievements. */
    private fun recordRoundEnd(s: com.vivek.unosimple.engine.models.GameState) {
        val winner = s.players.find { it.id == s.winnerId } ?: return
        val delta = s.players.filter { it.id != s.winnerId }.sumOf { it.handScore }
        val botNames = s.players
            .filter { it.id != humanPlayerId }
            .map { it.name }
        val record = RoundRecord(
            endedAtMs = nowEpochMs(),
            seats = s.players.map { it.id },
            names = s.players.map { it.name },
            winnerId = winner.id,
            winnerName = winner.name,
            deltaPoints = delta,
            cumulativeScores = s.players.map { it.score },
            humanWon = humanPlayerId != null && winner.id == humanPlayerId,
            botNames = botNames,
        )
        history.append(record)
        humanPlayerId?.let { hid ->
            val all = history.records.value
            val knownPersonas = BOTS.map { it.name }.toSet()
            val newly = achievements.recordRound(all, hid, knownPersonas)
            if (newly.isNotEmpty()) _recentUnlocks.value = newly
        }
    }

    /**
     * Attempt to restore a previously-saved session. Returns the bot count
     * from that session so the caller can route to `Screen.Game(botCount)`,
     * or `null` if no valid session exists.
     */
    fun restoreSavedSession(): Int? {
        val raw = SessionStore.read(SESSION_KEY) ?: return null
        val saved = runCatching { sessionJson.decodeFromString<SavedSession>(raw) }.getOrNull()
            ?: run {
                SessionStore.write(SESSION_KEY, null)
                return null
            }
        currentBotCount = saved.botCount
        _state.value = saved.state
        runBotsUntilHuman()
        return saved.botCount
    }

    /**
     * Tracks the currently-running bot loop so we can cancel it when the user
     * leaves the screen, starts a new round mid-turn, or any other event that
     * invalidates the pending action.
     */
    private var botJob: Job? = null

    /**
     * Start a fresh round. In human-vs-bots mode (default), seats are
     * "p1" (You) + [botCount] bots. In all-bots mode ([humanPlayerId] = null),
     * seats are [botCount] + 1 bots ("bot0" through "botN"). Safe to call
     * repeatedly — each call cancels any pending bot work and deals a new
     * deck.
     */
    fun startGame(botCount: Int, handSize: Int = HAND_SIZE) {
        require(botCount in 1..9) { "botCount must be 1..9, got $botCount" }
        botJob?.cancel()
        currentBotCount = botCount
        val seats = if (humanPlayerId != null) {
            buildList {
                add(humanPlayerId to "You")
                for (i in 1..botCount) add("bot$i" to botNameFor(i))
            }
        } else {
            (0..botCount).map { "bot$it" to botNameFor(it.coerceAtLeast(1)) }
        }
        _state.value = newRound(seats, random, handSize = handSize)
        runBotsUntilHuman() // advances through every turn when humanPlayerId is null
    }

    /**
     * Play [card] from the human's hand. Silently ignores illegal actions —
     * the UI is responsible for only enabling legal cards.
     *
     * @param chosenColor required for wild cards, forbidden for non-wilds.
     * @param declareUno whether the human declared "UNO" before this play.
     *   The engine penalizes `declareUno = false` plays that leave the
     *   player with exactly one card (+2 penalty cards). The UI is
     *   responsible for tracking the declaration state between turns.
     */
    fun playCard(card: Card, chosenColor: CardColor? = null, declareUno: Boolean = false) {
        val current = _state.value ?: return
        if (current.isRoundOver) return
        if (current.currentPlayer.id != humanPlayerId) return

        val action = GameAction.PlayCard(
            playerId = humanPlayerId,
            card = card,
            chosenColor = chosenColor,
            declareUno = declareUno,
        )
        if (apply(action, current)) runBotsUntilHuman()
    }

    /** Human draws a card from the pile. Ends their turn (v1 rule). */
    fun drawCard() {
        val current = _state.value ?: return
        if (current.isRoundOver) return
        if (current.currentPlayer.id != humanPlayerId) return

        if (apply(GameAction.DrawCard(humanPlayerId), current)) runBotsUntilHuman()
    }

    /** Reset the current round (used by "back to menu"). Clears saved session too. */
    fun clear() {
        botJob?.cancel()
        _state.value = null
        SessionStore.write(SESSION_KEY, null)
    }

    /** True while the game surface is paused and bots should not advance. */
    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()

    /** Pause bot advancement. The human can still look at the screen but no
     *  further turns fire until [resume] is called. */
    fun pause() {
        if (_paused.value) return
        _paused.value = true
        botJob?.cancel()
    }

    /** Resume bot advancement. Picks back up from the current state. */
    fun resume() {
        if (!_paused.value) return
        _paused.value = false
        runBotsUntilHuman()
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /** Dispatches [action] against [from]. Returns true on success. */
    private fun apply(action: GameAction, from: GameState): Boolean {
        return when (val r = Engine.applyAction(from, action, random)) {
            is ActionResult.Success -> {
                _state.value = r.state
                true
            }
            is ActionResult.Failure -> false // UI should prevent illegal actions
        }
    }

    /**
     * While the current player is a bot, play their chosen action after a
     * small delay, then loop. Cancels any previously-running bot coroutine.
     *
     * The re-check inside the loop (after [delay]) is important — state can
     * change during the wait (user leaves the screen, starts a new game,
     * etc.) and we must bail rather than apply stale actions.
     */
    private fun runBotsUntilHuman() {
        botJob?.cancel()
        botJob = viewModelScope.launch {
            while (isActive) {
                val before = _state.value ?: return@launch
                if (before.isRoundOver) return@launch
                if (before.currentPlayer.id == humanPlayerId) return@launch

                delay(botTurnDelayMs)

                val current = _state.value ?: return@launch
                if (current.isRoundOver) return@launch
                if (current.currentPlayer.id == humanPlayerId) return@launch

                val action = bot.chooseAction(current, random)
                if (!apply(action, current)) return@launch
            }
        }
    }

    private fun botNameFor(oneBasedIndex: Int): String {
        val idx = ((oneBasedIndex - 1) % BOTS.size + BOTS.size) % BOTS.size
        return BOTS[idx].name
    }

    @Serializable
    private data class SavedSession(val botCount: Int, val state: GameState)

    companion object {
        private const val SESSION_KEY = "uno.session.v1"
        private val sessionJson = Json { ignoreUnknownKeys = true }

        /** Returns true if a resumable saved session exists in storage. */
        fun hasSavedSession(): Boolean = SessionStore.read(SESSION_KEY) != null

        /**
         * Pause before each bot move. Long enough that the player can see
         * the new card hit the pile and register whose turn is next — 800ms
         * was too fast to follow and the previous turn felt invisible. 1400ms
         * matches the time it takes to read the action banner.
         */
        const val DEFAULT_BOT_TURN_DELAY_MS: Long = 1400L

        /**
         * Placeholder wall-clock. Kotlin stdlib has no common
         * `currentTimeMillis` without kotlinx-datetime, which we haven't
         * pulled in. For v1 history records we write 0 — ordering comes
         * from the append-only list position, which is all the Stats
         * screen needs today. Phase 6 can add an expect/actual clock.
         */
        private fun nowEpochMs(): Long = 0L

        /**
         * Explicit factory so `viewModel(factory = GameViewModel.Factory)`
         * works on Compose Multiplatform Wasm / Desktop. The default
         * no-arg `viewModel<T>()` only works on Android because Android
         * AGP installs a `NewInstanceFactory` that reflects on a no-arg
         * constructor — non-Android targets throw UnsupportedOperation.
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { GameViewModel() }
        }
    }
}

