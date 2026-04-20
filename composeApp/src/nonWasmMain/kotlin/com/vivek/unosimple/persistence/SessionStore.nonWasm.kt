package com.vivek.unosimple.persistence

actual object SessionStore {
    private val memory = mutableMapOf<String, String>()

    actual fun read(key: String): String? = memory[key]

    actual fun write(key: String, value: String?) {
        if (value == null) memory.remove(key) else memory[key] = value
    }
}
