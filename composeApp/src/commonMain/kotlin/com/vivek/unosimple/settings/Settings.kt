package com.vivek.unosimple.settings

import com.vivek.unosimple.persistence.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * User-tweakable settings. Persisted to [SessionStore] as JSON under a
 * dedicated key, so values survive a page reload on Wasm (localStorage)
 * and a process restart wherever else the SessionStore has a backing impl.
 *
 * @property soundEnabled master audio on/off.
 * @property hapticsEnabled master haptics on/off.
 * @property animationSpeedMultiplier multiplier on animation durations.
 *   `1.0` = normal, `0.5` = twice as fast, `1.5` = slower / more cinematic.
 *   Clamped to `[0.25, 2.0]` so nothing is instant or excruciating.
 */
@Serializable
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
 * Persistent settings store — JSON-backed via [SessionStore].
 * Reads once on construction; writes on every mutation.
 */
class SettingsRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _state: MutableStateFlow<Settings> = MutableStateFlow(loadInitial())
    val state: StateFlow<Settings> = _state.asStateFlow()

    fun setSoundEnabled(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }
    fun setHapticsEnabled(enabled: Boolean) = update { it.copy(hapticsEnabled = enabled) }
    fun setReducedMotion(enabled: Boolean) = update { it.copy(reducedMotion = enabled) }
    fun setAnimationSpeed(multiplier: Float) = update {
        it.copy(animationSpeedMultiplier = multiplier.coerceIn(0.25f, 2.0f))
    }

    private inline fun update(block: (Settings) -> Settings) {
        val next = block(_state.value)
        _state.value = next
        persist(next)
    }

    private fun loadInitial(): Settings {
        val raw = SessionStore.read(KEY) ?: return Settings()
        return runCatching { json.decodeFromString<Settings>(raw) }.getOrElse { Settings() }
    }

    private fun persist(settings: Settings) {
        SessionStore.write(KEY, json.encodeToString(settings))
    }

    companion object {
        private const val KEY = "uno.settings.v1"
    }
}
