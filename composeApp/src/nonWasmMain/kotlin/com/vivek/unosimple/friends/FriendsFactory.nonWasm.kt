package com.vivek.unosimple.friends

/**
 * Android / iOS / Desktop placeholder. The dev.gitlive Firebase RTDB
 * plumbing already works for the online game sync; extending it to the
 * friends feature is a follow-up (mostly the same query patterns as the
 * wasm impl, just via the Kotlin SDK).
 */
actual fun createFriendsService(
    myUid: String,
    myDisplayName: String,
    myAvatarId: String?,
): FriendsService = SilentFriendsService()
