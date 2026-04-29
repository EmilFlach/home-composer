package org.example.project

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ShutterCardContent(
    name: String,
    position: Float,
    onOpen: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
) {
    val animatedPosition by animateFloatAsState(
        targetValue = position,
        animationSpec = tween(durationMillis = 600)
    )
    val accentColor = MaterialTheme.colorScheme.secondary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            val statusText = when {
                position < 0.05f -> "Open"
                position > 0.95f -> "Closed"
                else -> "${(position * 100).roundToInt()}%"
            }
            Text(statusText, style = MaterialTheme.typography.titleSmall, color = accentColor)
        }
        Canvas(modifier = Modifier.size(width = 72.dp, height = 88.dp)) {
            val stroke = 4f
            val innerW = size.width - stroke * 2
            val innerH = size.height - stroke * 2
            drawRoundRect(color = NeonColors.ShutterGlass, topLeft = Offset(stroke, stroke), size = Size(innerW, innerH), cornerRadius = CornerRadius(6f))
            drawRoundRect(color = NeonColors.ShutterFrame, size = size, cornerRadius = CornerRadius(8f), style = Stroke(width = stroke))
            drawRoundRect(color = accentColor.copy(alpha = 0.3f), size = size, cornerRadius = CornerRadius(8f), style = Stroke(width = stroke * 0.5f))
            val covered = innerH * animatedPosition
            val slatHeight = 9f; val slatGap = 1.5f; val period = slatHeight + slatGap
            val numSlats = (covered / period).toInt() + 1
            repeat(numSlats) { i ->
                val top = stroke + i * period
                val bottom = (top + slatHeight).coerceAtMost(stroke + covered)
                if (bottom <= top) return@repeat
                drawRect(color = NeonColors.ShutterSlat, topLeft = Offset(stroke, top), size = Size(innerW, bottom - top))
                if (bottom - top > 3f) drawLine(color = NeonColors.ShutterNeonEdge.copy(alpha = 0.70f), start = Offset(stroke, top + 1.5f), end = Offset(stroke + innerW, top + 1.5f), strokeWidth = 1.5f)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onOpen, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp)) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Open") }
            OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp)) { Icon(Icons.Filled.Stop, contentDescription = "Stop") }
            FilledTonalButton(onClick = onClose, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp)) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close") }
        }
    }
}

@Composable
fun ShutterCard(
    name: String,
    position: Float, // 0f = fully open, 1f = fully closed
    onOpen: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedPosition by animateFloatAsState(
        targetValue = position,
        animationSpec = tween(durationMillis = 600)
    )
    val accentColor = MaterialTheme.colorScheme.secondary

    Card(modifier = modifier.neonCardBorder()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                val statusText = when {
                    position < 0.05f -> "Open"
                    position > 0.95f -> "Closed"
                    else -> "${(position * 100).roundToInt()}%"
                }
                Text(
                    statusText,
                    style = MaterialTheme.typography.titleSmall,
                    color = accentColor
                )
            }
            Spacer(Modifier.height(12.dp))
            Canvas(modifier = Modifier.size(width = 72.dp, height = 88.dp)) {
                val stroke = 4f
                val innerW = size.width - stroke * 2
                val innerH = size.height - stroke * 2

                // Deep dark glass background
                drawRoundRect(
                    color = NeonColors.ShutterGlass,
                    topLeft = Offset(stroke, stroke),
                    size = Size(innerW, innerH),
                    cornerRadius = CornerRadius(6f)
                )
                // Window frame (dark steel)
                drawRoundRect(
                    color = NeonColors.ShutterFrame,
                    size = size,
                    cornerRadius = CornerRadius(8f),
                    style = Stroke(width = stroke)
                )
                // Neon corner glow on frame
                drawRoundRect(
                    color = accentColor.copy(alpha = 0.3f),
                    size = size,
                    cornerRadius = CornerRadius(8f),
                    style = Stroke(width = stroke * 0.5f)
                )

                // Cyberpunk metallic shutter slats rolling down from top
                val covered = innerH * animatedPosition
                val slatHeight = 9f
                val slatGap = 1.5f
                val period = slatHeight + slatGap
                val numSlats = (covered / period).toInt() + 1
                repeat(numSlats) { i ->
                    val top = stroke + i * period
                    val bottom = (top + slatHeight).coerceAtMost(stroke + covered)
                    if (bottom <= top) return@repeat
                    // Dark metallic slat body
                    drawRect(
                        color = NeonColors.ShutterSlat,
                        topLeft = Offset(stroke, top),
                        size = Size(innerW, bottom - top)
                    )
                    // Neon electric-blue edge highlight at top of each slat
                    if (bottom - top > 3f) {
                        drawLine(
                            color = NeonColors.ShutterNeonEdge.copy(alpha = 0.70f),
                            start = Offset(stroke, top + 1.5f),
                            end = Offset(stroke + innerW, top + 1.5f),
                            strokeWidth = 1.5f
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp)
                ) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Open") }
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp)
                ) { Icon(Icons.Filled.Stop, contentDescription = "Stop") }
                FilledTonalButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp)
                ) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close") }
            }
        }
    }
}
