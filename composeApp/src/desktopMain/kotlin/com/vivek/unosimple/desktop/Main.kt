package com.vivek.unosimple.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.vivek.unosimple.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "UNO Simple",
    ) {
        App()
    }
}
