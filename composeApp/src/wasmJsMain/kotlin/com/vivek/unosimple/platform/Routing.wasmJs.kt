package com.vivek.unosimple.platform

/** Reads `window.location.pathname` from the browser. */
actual fun currentUrlPath(): String = runCatching { readLocationPath() }.getOrDefault("")

@JsFun("() => (globalThis.location && globalThis.location.pathname) ? globalThis.location.pathname : ''")
private external fun readLocationPath(): String
