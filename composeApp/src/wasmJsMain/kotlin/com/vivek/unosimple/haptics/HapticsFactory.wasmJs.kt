package com.vivek.unosimple.haptics

/** Browsers expose navigator.vibrate on some Android devices but it's
 *  unreliable across platforms + mostly ignored on desktop. Wasm stays
 *  silent for now. */
actual fun createHapticsService(): HapticsService = NoHapticsService()
