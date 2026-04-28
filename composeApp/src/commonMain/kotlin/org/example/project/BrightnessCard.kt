package org.example.project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val AmberColor = Color(0xFFFFC107)

@Composable
fun BrightnessCard(
    name: String,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                Text(
                    "${(brightness * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = AmberColor
                )
            }
            Spacer(Modifier.height(12.dp))
            Canvas(modifier = Modifier.size(52.dp)) {
                drawSun(color = AmberColor.copy(alpha = 0.3f + brightness * 0.7f))
            }
            Spacer(Modifier.height(12.dp))
            Slider(
                value = brightness,
                onValueChange = onBrightnessChange,
                colors = SliderDefaults.colors(
                    thumbColor = AmberColor,
                    activeTrackColor = AmberColor,
                    inactiveTrackColor = AmberColor.copy(alpha = 0.3f)
                )
            )
        }
    }
}

private fun DrawScope.drawSun(color: Color) {
    val cx = size.width / 2
    val cy = size.height / 2
    val coreRadius = size.minDimension * 0.23f
    val rayInner = size.minDimension * 0.32f
    val rayOuter = size.minDimension * 0.46f
    drawCircle(color = color, radius = coreRadius, center = Offset(cx, cy))
    repeat(8) { i ->
        val angle = (i * PI / 4).toFloat()
        drawLine(
            color = color,
            start = Offset(cx + cos(angle) * rayInner, cy + sin(angle) * rayInner),
            end = Offset(cx + cos(angle) * rayOuter, cy + sin(angle) * rayOuter),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
}
