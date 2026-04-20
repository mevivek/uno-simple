package com.vivek.unosimple.multiplayer

import kotlin.random.Random

/**
 * Platform-aware factory for Firebase-backed online multiplayer.
 *
 * `dev.gitlive:firebase-*` ships variants for Android / iOS / JVM / JS but
 * not Wasm, so the `actual` declarations split: non-Wasm targets return a
 * real [FirebaseSyncService]; Wasm returns `null`, letting the UI degrade
 * gracefully ("online multiplayer not available on web" hint).
 *
 * When Firebase adds Wasm support (or we switch to direct JS-interop
 * bindings), the Wasm actual becomes a real implementation and the
 * contract stays the same.
 */
expect fun createFirebaseSyncService(
    myId: String,
    roomCode: String,
    random: Random = Random.Default,
): GameSyncService?

/** Convenience for call sites that want to know "can I even offer the online option?" */
expect val firebaseSupported: Boolean
