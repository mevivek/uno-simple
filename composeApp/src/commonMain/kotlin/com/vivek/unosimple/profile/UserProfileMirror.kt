package com.vivek.unosimple.profile

/**
 * Mirror the local [UserProfile] into the backend users registry so the
 * user appears in Firebase (visible to peers + the Firebase console) the
 * moment they complete onboarding, not lazily on first online join.
 *
 * Called on every profile change (name edit, avatar pick, tutorial
 * completion) from the App-root observer.
 *
 * Wasm writes directly to `users/{uid}/{displayName,avatarId}` via the
 * uno-db.js glue. Non-wasm targets are no-ops for now — the dev.gitlive
 * Firebase wrapper is already available to wire up when we exercise
 * those targets against a real server.
 */
expect fun writeUserProfileMirror(uid: String, displayName: String, avatarId: String?)
