package org.example.project

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
        // JBR macOS: leftInset = titleBarHeight + 2*shrinkingFactor*20.
        // Use the standard macOS height (28) so leftInset is just enough to clear
        // the traffic lights (~68dp) without excess horizontal padding.
        val titleBarInsets = rememberJbrTitleBarInsets(headerHeightDp = 28f)
        val dragModifier = remember(window) { Modifier.dragWindowOnDrag(window) }
        CompositionLocalProvider(
            LocalWindowDecorationInsets provides titleBarInsets,
            LocalHeaderDragModifier provides dragModifier,
            LocalHeaderTopPadding provides 32.dp,
        ) {
            App()
        }
    }
}
