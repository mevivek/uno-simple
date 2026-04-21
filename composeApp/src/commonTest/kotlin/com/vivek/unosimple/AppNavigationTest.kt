package com.vivek.unosimple

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.vivek.unosimple.ui.TestTags
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Exercises the whole App() navigation path — Home → Game → Menu back to Home.
 *
 * This is the test that would have surfaced the ViewModel-factory freeze the
 * first time it landed: on Wasm/Desktop the default `viewModel<T>()` call
 * throws `UnsupportedOperationException` without a registered factory, which
 * would crash `GameScreen` on entry. A click-through smoke test that asserts
 * the game-screen tag appears after the start button would have failed
 * immediately rather than waiting for the user to notice in the live preview.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class AppNavigationTest {

    @BeforeTest
    fun installMainDispatcher() {
        // GameViewModel's viewModelScope uses Dispatchers.Main. The Compose
        // UI test harness doesn't set this up, so the first bot-turn
        // coroutine crashes on launch. Install a test dispatcher so the
        // click-through flow actually reaches GameScreen.
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun clickingStartButtonTakesUserFromHomeToGame() = runComposeUiTest {
        setContent { App(overrideInitialScreen = com.vivek.unosimple.ui.Screen.Home) }

        onNodeWithTag(TestTags.HOME_SCREEN).assertIsDisplayed()
        onNodeWithTag(TestTags.HOME_START_BUTTON).performClick()
        waitForIdle()

        // GameScreen must appear without an exception (regression guard for
        // the VM factory bug).
        onNodeWithTag(TestTags.GAME_SCREEN).assertIsDisplayed()
        onNodeWithTag(TestTags.GAME_MENU_BUTTON).assertIsDisplayed()
        onNodeWithTag(TestTags.GAME_TURN_HEADER).assertIsDisplayed()
    }

    @Test
    fun menuButtonReturnsUserToHome() = runComposeUiTest {
        setContent { App(overrideInitialScreen = com.vivek.unosimple.ui.Screen.Home) }

        onNodeWithTag(TestTags.HOME_START_BUTTON).performClick()
        waitForIdle()
        onNodeWithTag(TestTags.GAME_SCREEN).assertIsDisplayed()

        // Menu button now opens the pause overlay. Tap Quit inside the
        // overlay to return to home.
        onNodeWithTag(TestTags.GAME_MENU_BUTTON).performClick()
        waitForIdle()
        onNodeWithTag("pause_overlay").assertIsDisplayed()
        onNodeWithTag("pause.quit").performClick()
        waitForIdle()
        onNodeWithTag(TestTags.HOME_SCREEN).assertIsDisplayed()
    }
}
