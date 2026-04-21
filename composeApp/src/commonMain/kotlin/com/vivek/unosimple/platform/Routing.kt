package com.vivek.unosimple.platform

/**
 * Current URL path on Wasm (e.g. "/admin"), empty string elsewhere.
 * Used at cold-boot to deep-link into hidden screens.
 */
expect fun currentUrlPath(): String
