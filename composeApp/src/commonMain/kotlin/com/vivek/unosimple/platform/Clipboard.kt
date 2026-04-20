package com.vivek.unosimple.platform

/**
 * Tiny cross-platform clipboard write. Wasm uses `navigator.clipboard`;
 * other targets currently no-op (Android/iOS/Desktop can wire up platform
 * clipboard APIs when needed — not blocking hobby-game usage).
 */
expect object Clipboard {
    fun writeText(text: String)
}
