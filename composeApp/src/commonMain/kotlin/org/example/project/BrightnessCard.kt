package org.example.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LightCardContent(
    name: String,
    isOn: Boolean,
    brightness: Float,
    colorTemperature: Float,
    onToggle: (Boolean) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onColorTemperatureChange: (Float) -> Unit,
) {
    val warmColor = NeonColors.NeonAmber
    val coolColor = MaterialTheme.colorScheme.secondary
    val sunColor = lerp(warmColor, coolColor, colorTemperature)
    val sunAlpha = if (isOn) 0.3f + brightness * 0.7f else 0f

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Canvas(Modifier.size(28.dp)) {
                drawSun(color = if (isOn) sunColor.copy(alpha = sunAlpha) else warmColor.copy(alpha = 0.2f))
            }
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isOn,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = warmColor,
                    checkedTrackColor = warmColor.copy(alpha = 0.35f),
                    checkedBorderColor = warmColor.copy(alpha = 0.55f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }
        if (isOn) {
            LightSliderRow(
                label = "Brightness",
                value = brightness,
                onValueChange = onBrightnessChange,
                thumbColor = warmColor,
                activeTrackColor = warmColor,
                inactiveTrackColor = warmColor.copy(alpha = 0.25f)
            )
            LightSliderRow(
                label = "Color temp",
                value = colorTemperature,
                onValueChange = onColorTemperatureChange,
                thumbColor = lerp(warmColor, coolColor, colorTemperature),
                activeTrackColor = warmColor,
                inactiveTrackColor = coolColor.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun LightCard(
    name: String,
    isOn: Boolean,
    brightness: Float,
    colorTemperature: Float, // 0f = warm (2700K), 1f = cool (6500K)
    onToggle: (Boolean) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onColorTemperatureChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val warmColor = NeonColors.NeonAmber
    val coolColor = MaterialTheme.colorScheme.secondary
    val sunColor = lerp(warmColor, coolColor, colorTemperature)
    val sunAlpha = if (isOn) 0.3f + brightness * 0.7f else 0f
    val tempLabel = when {
        colorTemperature < 0.33f -> "Warm"
        colorTemperature < 0.67f -> "Neutral"
        else -> "Cool"
    }

    Card(modifier = modifier.neonCardBorder()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Canvas(Modifier.size(28.dp)) {
                    drawSun(
                        color = if (isOn) sunColor.copy(alpha = sunAlpha) else warmColor.copy(alpha = 0.2f)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (isOn) "${(brightness * 100).toInt()}%  ·  $tempLabel" else "Off",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOn) sunColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AnimatedVisibility(visible = isOn) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Collapse controls" else "Expand controls",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isOn,
                    onCheckedChange = {
                        onToggle(it)
                        if (!it) expanded = false
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = warmColor,
                        checkedTrackColor = warmColor.copy(alpha = 0.35f),
                        checkedBorderColor = warmColor.copy(alpha = 0.55f),
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            }

            AnimatedVisibility(
                visible = isOn && expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LightSliderRow(
                        label = "Brightness",
                        value = brightness,
                        onValueChange = onBrightnessChange,
                        thumbColor = warmColor,
                        activeTrackColor = warmColor,
                        inactiveTrackColor = warmColor.copy(alpha = 0.25f)
                    )
                    LightSliderRow(
                        label = "Color temp",
                        value = colorTemperature,
                        onValueChange = onColorTemperatureChange,
                        thumbColor = lerp(warmColor, coolColor, colorTemperature),
                        activeTrackColor = warmColor,
                        inactiveTrackColor = coolColor.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LightSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    thumbColor: Color,
    activeTrackColor: Color,
    inactiveTrackColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(width = 72.dp, height = 24.dp),
            maxLines = 1
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = thumbColor,
                activeTrackColor = activeTrackColor,
                inactiveTrackColor = inactiveTrackColor
            )
        )
    }
}

private fun DrawScope.drawSun(color: Color, scale: Float = 1f) {
    val cx = size.width / 2
    val cy = size.height / 2
    val coreRadius = size.minDimension * 0.23f * scale
    val rayInner = size.minDimension * 0.32f * scale
    val rayOuter = size.minDimension * 0.46f * scale
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
