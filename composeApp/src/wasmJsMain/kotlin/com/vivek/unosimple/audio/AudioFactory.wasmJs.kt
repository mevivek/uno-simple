package com.vivek.unosimple.audio

/** Wasm picks the Web Audio-backed synthesizer. */
actual fun createAudioService(): AudioService = WebAudioService()
