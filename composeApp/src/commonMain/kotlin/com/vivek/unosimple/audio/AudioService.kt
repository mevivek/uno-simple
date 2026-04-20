package com.vivek.unosimple.audio

/**
 * Sound events the game emits. The concrete impl of [AudioService] decides
 * how to render each — a real impl loads WAV/OGG assets, a stub ignores them.
 */
enum class SoundEffect {
    CARD_DEAL,   // any card sliding into a hand on new round
    CARD_PLAY,   // playing a card onto the discard pile
    SHUFFLE,     // reshuffling discard back into draw
    UNO_CALL,    // player hits the UNO! button
    WIN,         // round-over celebration
    ERROR,       // invalid action or illegal play attempt
}

interface AudioService {
    /** Play the given effect. Should never throw; failures are silent. */
    fun play(sound: SoundEffect)

    /** Toggle all sound output without losing the underlying player state. */
    fun setMuted(muted: Boolean)
}

/** Default no-op implementation used on targets that don't ship audio yet. */
class SilentAudioService : AudioService {
    override fun play(sound: SoundEffect) = Unit
    override fun setMuted(muted: Boolean) = Unit
}
