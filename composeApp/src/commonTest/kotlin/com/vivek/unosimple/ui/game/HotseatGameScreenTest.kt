package com.vivek.unosimple.ui.game

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.vivek.unosimple.multiplayer.PlayerSeat
import com.vivek.unosimple.ui.TestTags
import com.vivek.unosimple.viewmodel.HotseatGameViewModel
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class HotseatGameScreenTest {

    @BeforeTest
    fun setMain() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMain() {
        Dispatchers.resetMain()
    }

    @Test
    fun entersWithPassDeviceOverlayShown() = runComposeUiTest {
        val vm = HotseatGameViewModel(
            seats = listOf(
                PlayerSeat("p1", "Alice"),
                PlayerSeat("p2", "Bob"),
            ),
            random = Random(1),
        )
        setContent {
            HotseatGameScreen(vm = vm, onBackToHome = {})
        }
        waitForIdle()

        onNodeWithTag(TestTags.HOTSEAT_PASS_OVERLAY).assertIsDisplayed()
        onNodeWithTag(TestTags.HOTSEAT_READY_BUTTON).assertIsDisplayed()
    }

    @Test
    fun tappingReadyDismissesTheOverlay() = runComposeUiTest {
        val vm = HotseatGameViewModel(
            seats = listOf(
                PlayerSeat("p1", "Alice"),
                PlayerSeat("p2", "Bob"),
            ),
            random = Random(2),
        )
        setContent {
            HotseatGameScreen(vm = vm, onBackToHome = {})
        }
        waitForIdle()

        onNodeWithTag(TestTags.HOTSEAT_READY_BUTTON).performClick()
        waitForIdle()

        // Overlay should no longer be displayed.
        val matches = onAllNodesWithTag(TestTags.HOTSEAT_PASS_OVERLAY).fetchSemanticsNodes()
        kotlin.test.assertEquals(0, matches.size)
        // Hotseat root + game widgets visible.
        onNodeWithTag(TestTags.HOTSEAT_SCREEN).assertIsDisplayed()
    }
}
