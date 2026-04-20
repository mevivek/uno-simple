package com.vivek.unosimple.viewmodel

import com.vivek.unosimple.engine.ai.HeuristicBot
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * End-to-end integration tests driving the whole game through the ViewModel
 * with no human. Catches bugs that pure-engine tests miss, including:
 *
 * - ViewModel instantiation on Wasm/Desktop (historical: the default
 *   `viewModel<T>()` factory threw `UnsupportedOperationException` on
 *   non-Android targets — this test would have caught it immediately).
 * - Bot-turn coroutine scheduling + cancellation.
 * - StateFlow propagation across ViewModel.state.
 * - The complete applyAction loop across every turn.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelBotVsBotTest {

    @BeforeTest
    fun setUp() {
        // viewModelScope uses Dispatchers.Main; swap in a test dispatcher so
        // advanceUntilIdle() works deterministically on all targets.
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun fourBotsPlayAFullGameThatFinishes() = runTest {
        val vm = GameViewModel(
            random = Random(42),
            bot = HeuristicBot(),
            botTurnDelayMs = 0,       // no artificial pacing in tests
            humanPlayerId = null,     // all-bots mode
        )

        vm.startGame(botCount = 3)    // 4 bots total
        advanceUntilIdle()

        val state = vm.state.value
        assertNotNull(state, "state must be set after startGame")
        assertTrue(state.isRoundOver, "4-bot game should finish; state=$state")
        assertNotNull(state.winnerId, "winnerId must be set when round is over")
        assertTrue(
            state.winnerId!!.startsWith("bot"),
            "winner must be one of the bots; got ${state.winnerId}",
        )
    }

    @Test
    fun twoBotsPlayAFullGameThatFinishes() = runTest {
        val vm = GameViewModel(
            random = Random(7),
            botTurnDelayMs = 0,
            humanPlayerId = null,
        )

        vm.startGame(botCount = 1)
        advanceUntilIdle()

        val state = vm.state.value
        assertNotNull(state)
        assertTrue(state.isRoundOver, "2-bot game should finish")
        assertNotNull(state.winnerId)
    }

    @Test
    fun sameSeedGivesSameOutcome() = runTest {
        // Same Random(99) — bot decisions and reshuffles are deterministic,
        // so both runs must produce the same winner.
        val vmA = GameViewModel(random = Random(99), botTurnDelayMs = 0, humanPlayerId = null)
        vmA.startGame(2)
        advanceUntilIdle()
        val winnerA = vmA.state.value?.winnerId

        val vmB = GameViewModel(random = Random(99), botTurnDelayMs = 0, humanPlayerId = null)
        vmB.startGame(2)
        advanceUntilIdle()
        val winnerB = vmB.state.value?.winnerId

        assertNotNull(winnerA)
        assertEquals(winnerA, winnerB, "Bot games must be deterministic given the same seed")
    }

    @Test
    fun clearCancelsBotLoopAndResetsState() = runTest {
        val vm = GameViewModel(
            random = Random(1),
            botTurnDelayMs = 10,     // small delay so clear can race with bot loop
            humanPlayerId = null,
        )
        vm.startGame(2)
        // Don't advanceUntilIdle — the bot loop is still running.
        vm.clear()
        advanceUntilIdle()

        assertEquals(null, vm.state.value, "clear() must null out state")
    }
}
