package com.vivek.unosimple.multiplayer

import kotlin.random.Random

actual fun createFirebaseSyncService(
    myId: String,
    roomCode: String,
    random: Random,
): GameSyncService? = FirebaseSyncService(myId = myId, roomCode = roomCode, random = random)

actual val firebaseSupported: Boolean = true
