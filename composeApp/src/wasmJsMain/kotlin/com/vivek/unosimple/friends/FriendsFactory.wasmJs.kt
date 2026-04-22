package com.vivek.unosimple.friends

/** Wasm target: real Firebase-backed friends service. */
actual fun createFriendsService(
    myUid: String,
    myDisplayName: String,
    myAvatarId: String?,
): FriendsService = WasmFriendsService(
    myUid = myUid,
    myDisplayName = myDisplayName,
    myAvatarId = myAvatarId,
)
