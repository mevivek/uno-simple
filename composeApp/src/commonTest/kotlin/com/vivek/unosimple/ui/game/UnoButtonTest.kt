package com.vivek.unosimple.ui.game

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.vivek.unosimple.ui.TestTags
import com.vivek.unosimple.viewmodel.GameViewModel
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Tests the explicit UNO-declaration flow. The engine already covers the
 * penalty rule (EngineTest); these tests lock in the UI contract:
 *
 * - Button is absent when the human has more than two cards.
 * - Button appears + is clickable when the human has exactly two cards on
 *   their turn.
 * - After declaring, the button disables (you don't keep tapping).
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class UnoButtonTest {

    @BeforeTest
    fun installMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun buttonIsAbsentAtNormalHandSizes() = runComposeUiTest {
        val vm = GameViewModel(random = Random(1), botTurnDelayMs = 0)
        vm.startGame(botCount = 1, handSize = 7)

        setContent {
            GameScreen(botCount = 1, onBackToHome = {}, vm = vm)
        }
        waitForIdle()

        onAllNodesWithTag(TestTags.UNO_BUTTON).assertCountEquals(0)
    }

    @Test
    fun buttonAppearsWhenHumanHasTwoCards() = runComposeUiTest {
        // Start with every player holding exactly two cards — the human
        // (p1) goes first, so the UNO button must be visible immediately.
        val vm = GameViewModel(random = Random(1), botTurnDelayMs = 0)
        vm.startGame(botCount = 1, handSize = 2)

        setContent {
            GameScreen(botCount = 1, onBackToHome = {}, vm = vm)
        }
        waitForIdle()

        onNodeWithTag(TestTags.UNO_BUTTON).assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun buttonDisablesAfterDeclaring() = runComposeUiTest {
        val vm = GameViewModel(random = Random(1), botTurnDelayMs = 0)
        vm.startGame(botCount = 1, handSize = 2)

        setContent {
            GameScreen(botCount = 1, onBackToHome = {}, vm = vm)
        }
        waitForIdle()

        onNodeWithTag(TestTags.UNO_BUTTON).performClick()
        waitForIdle()

        // After declaring, the button should remain but be disabled
        // (user can't "re-declare" on the same turn).
        onNodeWithTag(TestTags.UNO_BUTTON).assertIsDisplayed().assertIsNotEnabled()
    }
}
