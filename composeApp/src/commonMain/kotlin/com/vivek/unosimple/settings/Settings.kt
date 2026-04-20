package com.vivek.unosimple.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User-tweakable settings. Persisted to platform storage later (SharedPrefs,
 * NSUserDefaults, localStorage); in-memory for now — settings survive until
 * app restart and that's enough while we ship the UI.
 *
 * @property soundEnabled master audio on/off.
 * @property hapticsEnabled master haptics on/off.
 * @property animationSpeedMultiplier multiplier on animation durations.
 *   `1.0` = normal, `0.5` = twice as fast, `1.5` = slower / more cinematic.
 *   Clamped to `[0.25, 2.0]` so nothing is instant or excruciating.
 */
data class Settings(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val animationSpeedMultiplier: Float = 1.0f,
    /**
     * When true, disables decorative motion: confetti on win, the home
     * backdrop bob, card-entry stagger, legal-card glow pulse. Turn-state
     * transitions (card land, opponent shake) remain because they're
     * functional feedback, not chrome.
     */
    val reducedMotion: Boolean = false,
) {
    init {
        require(animationSpeedMultiplier in 0.25f..2.0f) {
            "animationSpeedMultiplier out of range: $animationSpeedMultiplier"
        }
    }
}

/**
 * In-memory settings store exposing a [StateFlow]. Swap the implementation
 * for a platform-backed one when we add persistence.
 */
class SettingsRepository {
    private val _state = MutableStateFlow(Settings())
    val state: StateFlow<Settings> = _state.asStateFlow()

    fun setSoundEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(soundEnabled = enabled)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(hapticsEnabled = enabled)
    }

    fun setAnimationSpeed(multiplier: Float) {
        val clamped = multiplier.coerceIn(0.25f, 2.0f)
        _state.value = _state.value.copy(animationSpeedMultiplier = clamped)
    }

    fun setReducedMotion(enabled: Boolean) {
        _state.value = _state.value.copy(reducedMotion = enabled)
    }
}
