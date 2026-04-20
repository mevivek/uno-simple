package com.vivek.unosimple.persistence

/**
 * Tiny string KV store for persisting the active game across page reloads.
 *
 * - **Wasm** → `window.localStorage` — survives refresh.
 * - **Other targets** → in-memory map — reload isn't a thing on native apps,
 *   and Android/Desktop already have process-level state between rotations
 *   via ViewModelStoreOwner. Persistent storage on those targets can be
 *   added later when the need arises (DataStore / Preferences / a file).
 *
 * Values are opaque strings; callers serialize and versioning themselves.
 */
expect object SessionStore {
    fun read(key: String): String?
    fun write(key: String, value: String?)
}
