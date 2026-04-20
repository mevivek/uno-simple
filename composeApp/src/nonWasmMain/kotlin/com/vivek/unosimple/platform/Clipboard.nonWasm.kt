package com.vivek.unosimple.platform

actual object Clipboard {
    // No-op on Android / iOS / Desktop for v1. Plug in the platform API
    // (ClipboardManager, UIPasteboard, Toolkit.getDefaultToolkit) when a
    // real use case needs it.
    actual fun writeText(text: String) {}
}
