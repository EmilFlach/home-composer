package org.example.project

import kotlin.math.abs
import kotlin.math.round

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun GraphCard(
    title: String,
    unit: String,
    currentValue: Float,
    dataPoints: List<Float>,
    timeLabels: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF6B57FF)
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        currentValue.fmt1d(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = color
                    )
                    Text(
                        unit,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                if (dataPoints.size < 2) return@Canvas
                val minVal = dataPoints.min()
                val maxVal = dataPoints.max()
                val range = (maxVal - minVal).coerceAtLeast(0.1f)
                val w = size.width
                val h = size.height
                val padV = h * 0.1f
                val stepX = w / (dataPoints.size - 1)

                fun xAt(i: Int) = i * stepX
                fun yAt(v: Float) = h - padV - (v - minVal) / range * (h - 2 * padV)

                // Subtle grid lines
                repeat(4) { i ->
                    val y = padV + i * (h - 2 * padV) / 3
                    drawLine(Color.Gray.copy(alpha = 0.1f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                }

                // Filled area under the curve
                val fillPath = Path().apply {
                    moveTo(xAt(0), h)
                    dataPoints.forEachIndexed { i, v -> lineTo(xAt(i), yAt(v)) }
                    lineTo(xAt(dataPoints.lastIndex), h)
                    close()
                }
                drawPath(fillPath, color = color.copy(alpha = 0.15f))

                // Line
                val linePath = Path().apply {
                    dataPoints.forEachIndexed { i, v ->
                        if (i == 0) moveTo(xAt(0), yAt(v)) else lineTo(xAt(i), yAt(v))
                    }
                }
                drawPath(linePath, color = color, style = Stroke(width = 3f, cap = StrokeCap.Round))

                // Dot at latest value
                val lastX = xAt(dataPoints.lastIndex)
                val lastY = yAt(dataPoints.last())
                drawCircle(color = color, radius = 6f, center = Offset(lastX, lastY))
                drawCircle(color = Color.White, radius = 3f, center = Offset(lastX, lastY))
            }
            if (timeLabels.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    timeLabels.forEach { label ->
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun Float.fmt1d(): String {
    val r = round(this * 10).toInt()
    return "${r / 10}.${abs(r % 10)}"
}
