package com.vivek.unosimple.haptics

/** Discrete haptic cues the game triggers. */
enum class HapticPattern {
    TICK,       // small confirm — chip select, button tap
    BUMP,       // medium — card played, draw
    THUMP,      // stronger — draw-two / draw-four landed
    CELEBRATE,  // win pattern
}

interface HapticsService {
    /** Emit [pattern]. Never throws; no-ops when the platform lacks haptics. */
    fun emit(pattern: HapticPattern)

    /** Disable all haptic output. */
    fun setEnabled(enabled: Boolean)
}

/** Default no-op implementation. Desktop and Wasm have no haptics. */
class NoHapticsService : HapticsService {
    override fun emit(pattern: HapticPattern) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
}
