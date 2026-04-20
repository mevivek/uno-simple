package com.vivek.unosimple.persistence

@JsFun("(k) => { const v = window.localStorage.getItem(k); return v == null ? '' : v; }")
private external fun lsGet(key: String): String

@JsFun("(k, v) => window.localStorage.setItem(k, v)")
private external fun lsSet(key: String, value: String)

@JsFun("(k) => window.localStorage.removeItem(k)")
private external fun lsRemove(key: String)

actual object SessionStore {
    actual fun read(key: String): String? {
        val v = lsGet(key)
        return if (v.isEmpty()) null else v
    }

    actual fun write(key: String, value: String?) {
        if (value == null) lsRemove(key) else lsSet(key, value)
    }
}
