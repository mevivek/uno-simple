package com.vivek.unosimple.profile

/**
 * No-op on Android/iOS/Desktop for now. dev.gitlive's Firebase wrapper is
 * available on these targets (see FirebaseSyncService) — wiring it here
 * is a one-liner once we exercise these targets against a live backend.
 */
actual fun writeUserProfileMirror(uid: String, displayName: String, avatarId: String?) {
    // TODO: wire dev.gitlive's Firebase.database.reference("users/$uid")
    //  .setValue(...) when we run the Android/iOS/Desktop builds against
    //  a real Firebase project.
}
