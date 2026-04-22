package com.vivek.unosimple.persistence

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Tracks the currently-active online room so a Wasm page reload puts the
 * player back into the room instead of dumping them on Home. Written when
 * entering the Online screen, cleared when leaving.
 */
@Serializable
data class OnlineSessionInfo(
    val displayName: String,
    val roomCode: String,
    val isHost: Boolean,
)

object OnlineSessionStore {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private const val KEY = "uno.online.v1"

    fun read(): OnlineSessionInfo? {
        val raw = SessionStore.read(KEY) ?: return null
        return runCatching { json.decodeFromString<OnlineSessionInfo>(raw) }.getOrNull()
    }

    fun write(info: OnlineSessionInfo) {
        SessionStore.write(KEY, json.encodeToString(info))
    }

    fun clear() {
        SessionStore.write(KEY, null)
    }
}
