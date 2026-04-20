package com.vivek.unosimple.ui.lobby

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.vivek.unosimple.multiplayer.PlayerSeat
import com.vivek.unosimple.ui.TestTags
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class LobbyScreenTest {

    @Test
    fun rendersCountChipsAndDefaultNameFields() = runComposeUiTest {
        setContent { LobbyScreen(onStart = {}, onBack = {}) }

        onNodeWithTag(TestTags.LOBBY_SCREEN).assertIsDisplayed()
        onNodeWithTag("${TestTags.LOBBY_COUNT_CHIP_PREFIX}2").assertIsDisplayed()
        onNodeWithTag("${TestTags.LOBBY_COUNT_CHIP_PREFIX}3").assertIsDisplayed()
        onNodeWithTag("${TestTags.LOBBY_COUNT_CHIP_PREFIX}4").assertIsDisplayed()
        // Default count = 2, so two fields visible.
        onNodeWithTag("${TestTags.LOBBY_NAME_FIELD_PREFIX}0").assertIsDisplayed()
        onNodeWithTag("${TestTags.LOBBY_NAME_FIELD_PREFIX}1").assertIsDisplayed()
        onAllNodesWithTag("${TestTags.LOBBY_NAME_FIELD_PREFIX}2")
            .fetchSemanticsNodes()
            .let { assertEquals(0, it.size, "3rd field should not render at count=2") }
    }

    @Test
    fun bumpingCountChipRevealsMoreFields() = runComposeUiTest {
        setContent { LobbyScreen(onStart = {}, onBack = {}) }

        onNodeWithTag("${TestTags.LOBBY_COUNT_CHIP_PREFIX}4").performClick()
        waitForIdle()

        onNodeWithTag("${TestTags.LOBBY_NAME_FIELD_PREFIX}0").assertIsDisplayed()
        onNodeWithTag("${TestTags.LOBBY_NAME_FIELD_PREFIX}1").assertIsDisplayed()
        onNodeWithTag("${TestTags.LOBBY_NAME_FIELD_PREFIX}2").assertIsDisplayed()
        onNodeWithTag("${TestTags.LOBBY_NAME_FIELD_PREFIX}3").assertIsDisplayed()
    }

    @Test
    fun startButtonReportsSeatsForCurrentCount() = runComposeUiTest {
        var received: List<PlayerSeat>? = null
        setContent {
            LobbyScreen(onStart = { received = it }, onBack = {})
        }

        onNodeWithTag("${TestTags.LOBBY_COUNT_CHIP_PREFIX}3").performClick()
        waitForIdle()
        onNodeWithTag(TestTags.LOBBY_START_BUTTON).performClick()
        waitForIdle()

        assertEquals(3, received?.size)
        assertEquals(listOf("p1", "p2", "p3"), received?.map { it.id })
        assertEquals(listOf("Player 1", "Player 2", "Player 3"), received?.map { it.displayName })
    }
}
