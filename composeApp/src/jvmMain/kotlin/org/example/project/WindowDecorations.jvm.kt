package org.example.project

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import com.jetbrains.JBR
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Window

@Composable
fun FrameWindowScope.rememberJbrTitleBarInsets(headerHeightDp: Float): PaddingValues {
    var insets by remember { mutableStateOf(PaddingValues(0.dp)) }

    DisposableEffect(window) {
        val isMac = System.getProperty("os.name").lowercase().contains("mac")
        if (isMac && JBR.isWindowDecorationsSupported()) {
            val titleBar = JBR.getWindowDecorations().createCustomTitleBar()
            titleBar.height = headerHeightDp
            JBR.getWindowDecorations().setCustomTitleBar(window, titleBar)
            // Tell JBR to forward all clicks in the title-bar zone to AWT/Compose.
            // We handle window dragging ourselves via pointerInput on the header.
            titleBar.forceHitTest(true)
            insets = PaddingValues(
                start = titleBar.leftInset.dp,
                end = titleBar.rightInset.dp,
            )
        }
        onDispose { }
    }
    return insets
}

fun Modifier.dragWindowOnDrag(window: Window): Modifier = pointerInput(window) {
    var startMouse = Point()
    var startWindow = Point()
    detectDragGestures(
        onDragStart = {
            startMouse = MouseInfo.getPointerInfo()?.location ?: Point()
            startWindow = window.location
        },
        onDrag = { _, _ ->
            val current = MouseInfo.getPointerInfo()?.location ?: return@detectDragGestures
            window.setLocation(
                startWindow.x + (current.x - startMouse.x),
                startWindow.y + (current.y - startMouse.y),
            )
        },
    )
}
