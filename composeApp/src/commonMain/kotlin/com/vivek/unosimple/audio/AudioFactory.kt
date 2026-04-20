package com.vivek.unosimple.audio

/**
 * Per-platform factory for the concrete [AudioService]. On Wasm this returns
 * a `WebAudioService` that synthesizes SFX via the browser AudioContext —
 * no asset files. On every other target it returns [SilentAudioService]
 * until a real Android/iOS/Desktop implementation is wired up (tracked in
 * TODO.md under "Real audio / haptics").
 */
expect fun createAudioService(): AudioService
