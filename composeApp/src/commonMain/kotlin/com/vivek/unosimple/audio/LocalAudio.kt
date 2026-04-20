package com.vivek.unosimple.audio

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * App-wide [AudioService] available to any composable. Provided at the root
 * in `App()` via `CompositionLocalProvider`. Defaults to [SilentAudioService]
 * so isolated previews + unit tests don't need wiring.
 *
 * Call-sites:
 *   val audio = LocalAudio.current
 *   audio.play(SoundEffect.CARD_PLAY)
 */
val LocalAudio = staticCompositionLocalOf<AudioService> { SilentAudioService() }
