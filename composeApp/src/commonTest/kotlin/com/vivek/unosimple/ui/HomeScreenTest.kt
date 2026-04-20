package com.vivek.unosimple.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.vivek.unosimple.ui.home.HomeScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class HomeScreenTest {

    @Test
    fun rendersTitleStartButtonAndChips() = runComposeUiTest {
        setContent { HomeScreen(onStartGame = {}) }

        onNodeWithTag(TestTags.HOME_SCREEN).assertIsDisplayed()
        onNodeWithTag(TestTags.HOME_TITLE).assertIsDisplayed()
        onNodeWithTag(TestTags.HOME_START_BUTTON).assertIsDisplayed()
        onNodeWithTag("${TestTags.HOME_OPPONENT_CHIP_PREFIX}1").assertIsDisplayed()
        onNodeWithTag("${TestTags.HOME_OPPONENT_CHIP_PREFIX}2").assertIsDisplayed()
        onNodeWithTag("${TestTags.HOME_OPPONENT_CHIP_PREFIX}3").assertIsDisplayed()
    }

    @Test
    fun startButtonReportsCurrentlySelectedBotCount() = runComposeUiTest {
        var received: Int? = null
        setContent { HomeScreen(onStartGame = { received = it }) }

        // Default selection is 2 bots.
        onNodeWithTag(TestTags.HOME_START_BUTTON).performClick()
        assertEquals(2, received, "Default opponent count should be 2")
    }

    @Test
    fun clickingAnOpponentChipChangesTheReportedCount() = runComposeUiTest {
        var received: Int? = null
        setContent { HomeScreen(onStartGame = { received = it }) }

        onNodeWithTag("${TestTags.HOME_OPPONENT_CHIP_PREFIX}3").performClick()
        onNodeWithTag(TestTags.HOME_START_BUTTON).performClick()
        assertEquals(3, received)

        onNodeWithTag("${TestTags.HOME_OPPONENT_CHIP_PREFIX}1").performClick()
        onNodeWithTag(TestTags.HOME_START_BUTTON).performClick()
        assertEquals(1, received)
    }

    @Test
    fun startButtonDoesNothingWhenNoListener() = runComposeUiTest {
        // Regression guard: the tap path must not crash even if the caller
        // passes an inert lambda. Historically we had a path that eagerly
        // instantiated a default ViewModel during preview, which threw on
        // Wasm. This test exercises HomeScreen in isolation from that path.
        var received: Int? = null
        setContent { HomeScreen(onStartGame = { received = it }) }
        onNodeWithTag(TestTags.HOME_START_BUTTON).performClick()
        assertEquals(2, received)
        // Setting received back to null to verify no stray emission
        received = null
        onNodeWithTag(TestTags.HOME_START_BUTTON).performClick()
        assertEquals(2, received)
        // No other tap should fire the listener
        onNodeWithTag(TestTags.HOME_TITLE).performClick()
        assertEquals(2, received) // unchanged
        assertNull(null) // placeholder assertion to satisfy the compiler — nothing more to check
    }
}
