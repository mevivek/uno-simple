package com.vivek.unosimple.audio

/**
 * Android / iOS / Desktop still ship a silent audio service. Real
 * implementations are deferred per TODO.md: `AndroidAudioService`
 * (`MediaPlayer` / `ExoPlayer`), `IosAudioService` (`AVAudioPlayer`), and
 * a desktop impl (`javazoom`). All need packaged asset files that don't
 * exist yet.
 */
actual fun createAudioService(): AudioService = SilentAudioService()
