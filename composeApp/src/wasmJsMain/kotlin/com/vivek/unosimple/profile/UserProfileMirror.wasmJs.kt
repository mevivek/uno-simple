package com.vivek.unosimple.profile

@JsFun("(path, value) => window.unoDbSet(path, value)")
private external fun unoDbSet(path: String, value: String)

@JsFun("(path) => window.unoDbDelete(path)")
private external fun unoDbDelete(path: String)

/**
 * Wasm actual — writes flat `users/{uid}/displayName` + `/avatarId`
 * sub-paths via the uno-db.js Firebase glue. Produces a nice tree in
 * the Firebase console (each field a separate entry) instead of one
 * opaque JSON blob.
 */
actual fun writeUserProfileMirror(uid: String, displayName: String, avatarId: String?) {
    if (uid.isBlank()) return
    unoDbSet("users/$uid/displayName", displayName)
    if (avatarId != null) {
        unoDbSet("users/$uid/avatarId", avatarId)
    } else {
        runCatching { unoDbDelete("users/$uid/avatarId") }
    }
}
