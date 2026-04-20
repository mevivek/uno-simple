package com.vivek.unosimple.multiplayer

import kotlin.random.Random

/**
 * Wasm implementation. Routes through [WasmFirebaseSyncService] which talks
 * to the Firebase Web SDK directly via the uno-db.js glue layer — a
 * workaround for `dev.gitlive:firebase-*` not publishing Wasm variants.
 */
actual fun createFirebaseSyncService(
    myId: String,
    roomCode: String,
    random: Random,
): GameSyncService? = WasmFirebaseSyncService(myId = myId, roomCode = roomCode, random = random)

actual val firebaseSupported: Boolean = true
