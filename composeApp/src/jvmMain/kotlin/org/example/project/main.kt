package org.example.project

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "home-composer",
        alwaysOnTop = true,
        state = rememberWindowState(
            position = WindowPosition(50.dp, 50.dp),
            width = 450.dp,
            height = 1000.dp
        )
    ) {
        App()
    }
}