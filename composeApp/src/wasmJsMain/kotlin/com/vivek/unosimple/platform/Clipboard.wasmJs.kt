package com.vivek.unosimple.platform

@JsFun("(t) => { if (navigator && navigator.clipboard) navigator.clipboard.writeText(t); }")
private external fun jsClipboardWrite(text: String)

actual object Clipboard {
    actual fun writeText(text: String) {
        jsClipboardWrite(text)
    }
}
