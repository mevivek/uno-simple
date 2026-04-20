package com.vivek.unosimple.profile

import com.vivek.unosimple.persistence.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Lightweight local profile. Persisted as JSON in [SessionStore]; no network
 * round-trip, no Firebase Auth — the [uid] is a random string generated
 * once at first launch and then stable across reloads (until the user
 * clears site data).
 *
 * Treat this as the device's "identity" in online multiplayer: other
 * clients see [displayName] and the avatar derived from [uid]. F2 (friends)
 * will mirror this profile into Firebase RTDB under `users/{uid}` so other
 * clients can look up your name / avatar when you join their room.
 *
 * @property uid stable identifier used as the multiplayer client id.
 * @property displayName shown on opponent tiles + lobby seats. 1–20 chars.
 */
@Serializable
data class UserProfile(
    val uid: String,
    val displayName: String,
) {
    init {
        require(uid.isNotBlank()) { "uid must not be blank" }
        require(displayName.length in 1..20) { "displayName length must be 1..20" }
    }
}

class ProfileRepository(
    private val random: Random = Random.Default,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _profile: MutableStateFlow<UserProfile> = MutableStateFlow(loadOrCreate())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    fun setDisplayName(name: String) {
        val trimmed = name.trim().take(20).ifBlank { "Player" }
        val next = _profile.value.copy(displayName = trimmed)
        _profile.value = next
        persist(next)
    }

    private fun loadOrCreate(): UserProfile {
        val raw = SessionStore.read(PROFILE_KEY)
        val loaded = raw?.let { runCatching { json.decodeFromString<UserProfile>(it) }.getOrNull() }
        if (loaded != null) return loaded
        val fresh = UserProfile(
            uid = generateUid(random),
            displayName = "Player",
        )
        persist(fresh)
        return fresh
    }

    private fun persist(p: UserProfile) {
        SessionStore.write(PROFILE_KEY, json.encodeToString(p))
    }

    companion object {
        private const val PROFILE_KEY = "uno.profile.v1"

        /**
         * 12-char lowercase alphanumeric UID. Long enough for per-device
         * uniqueness in a hobby game; short enough to use directly as a
         * friend code if F2 wants that.
         */
        internal fun generateUid(rng: Random): String {
            val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
            return buildString(12) { repeat(12) { append(alphabet[rng.nextInt(alphabet.length)]) } }
        }
    }
}
