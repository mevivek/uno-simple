package com.vivek.unosimple.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.vivek.unosimple.settings.Settings
import com.vivek.unosimple.settings.SettingsRepository
import com.vivek.unosimple.ui.TestTags
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {

    @Test
    fun rendersAllControls() = runComposeUiTest {
        val repo = SettingsRepository()
        setContent {
            SettingsScreen(settings = repo, onDone = {})
        }

        onNodeWithTag(TestTags.SETTINGS_SCREEN).assertIsDisplayed()
        onNodeWithTag(TestTags.SETTINGS_SOUND_TOGGLE).assertIsDisplayed()
        onNodeWithTag(TestTags.SETTINGS_HAPTICS_TOGGLE).assertIsDisplayed()
        onNodeWithTag(TestTags.SETTINGS_SPEED_SLIDER).assertIsDisplayed()
        onNodeWithTag(TestTags.SETTINGS_BACK_BUTTON).assertIsDisplayed()
    }

    @Test
    fun defaultToggleStatesReflectDefaultSettings() = runComposeUiTest {
        val repo = SettingsRepository()
        setContent {
            SettingsScreen(settings = repo, onDone = {})
        }

        onNodeWithTag(TestTags.SETTINGS_SOUND_TOGGLE).assertIsOn()
        onNodeWithTag(TestTags.SETTINGS_HAPTICS_TOGGLE).assertIsOn()
    }

    @Test
    fun togglingSoundFlipsTheRepositoryState() = runComposeUiTest {
        val repo = SettingsRepository()
        setContent {
            SettingsScreen(settings = repo, onDone = {})
        }

        onNodeWithTag(TestTags.SETTINGS_SOUND_TOGGLE).performClick()
        waitForIdle()

        assertFalse(repo.state.value.soundEnabled)
        onNodeWithTag(TestTags.SETTINGS_SOUND_TOGGLE).assertIsOff()
    }

    @Test
    fun togglingHapticsFlipsTheRepositoryState() = runComposeUiTest {
        val repo = SettingsRepository()
        setContent {
            SettingsScreen(settings = repo, onDone = {})
        }

        assertTrue(repo.state.value.hapticsEnabled)
        onNodeWithTag(TestTags.SETTINGS_HAPTICS_TOGGLE).performClick()
        waitForIdle()
        assertFalse(repo.state.value.hapticsEnabled)
    }

    @Test
    fun backButtonFiresCallback() = runComposeUiTest {
        val repo = SettingsRepository()
        var backTapped = false
        setContent {
            SettingsScreen(settings = repo, onDone = { backTapped = true })
        }

        onNodeWithTag(TestTags.SETTINGS_BACK_BUTTON).performClick()
        waitForIdle()
        assertTrue(backTapped)
    }

    @Test
    fun settingsValidationClampsAnimationSpeed() {
        val repo = SettingsRepository()
        repo.setAnimationSpeed(10f)
        assertEquals(2.0f, repo.state.value.animationSpeedMultiplier)
        repo.setAnimationSpeed(0.01f)
        assertEquals(0.25f, repo.state.value.animationSpeedMultiplier)
    }

    @Test
    fun settingsDataClassRejectsOutOfRangeSpeed() {
        try {
            Settings(animationSpeedMultiplier = 3.0f)
            throw AssertionError("Should have thrown")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }
}
