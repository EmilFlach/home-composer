package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp

@Composable
fun RoomCard(
    room: HomeRoom,
    onEntityClick: (HomeEntity) -> Unit,
    onBrightnessChange: (Float) -> Unit = {},
    onLightSliderTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val primaryLightEntity = room.entities.firstOrNull { it.state is EntityState.Light }
    val primaryLight = primaryLightEntity?.state as? EntityState.Light
    val brightness = if (primaryLight?.isOn == true) primaryLight.brightness else 0f
    val lightTotal = room.lightCount()
    val openLightDetails: () -> Unit = {
        if (onLightSliderTap != null) {
            onLightSliderTap()
        } else {
            primaryLightEntity?.let(onEntityClick)
        }
    }
    val subtitle = when {
        primaryLight?.isOn == true -> "${(primaryLight.brightness * 100).toInt()}%"
        lightTotal > 0 -> "Off"
        else -> "Off"
    }

    // Entities to show as chips — exclude lights (shown in slider)
    // Also exclude standalone °C sensors when a climate entity is present (temp is shown in the climate chip)
    val hasClimate = room.entities.any { it.state is EntityState.Climate }
    val chipEntities = room.entities
        .filter { it.state !is EntityState.Light }
        .filter { !(hasClimate && it.state is EntityState.Sensor && (it.state as EntityState.Sensor).unit == "°C") }
        .sortedBy { it.chipOrderPriority() }

    Column(modifier = modifier) {
        // ── Main card ────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (primaryLightEntity != null) {
                        Modifier.clickable(onClick = openLightDetails)
                    } else {
                        Modifier
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.98f),
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {

                // Title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = room.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            room.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Custom brightness slider
                Spacer(Modifier.height(12.dp))
                BrightnessSlider(
                    brightness = brightness,
                    isOn = primaryLight?.isOn == true,
                    onBrightnessChange = onBrightnessChange,
                    onTap = openLightDetails
                )
            }
        }

        // ── Chips row — standalone chips with card-adjacent color ─────────
        if (chipEntities.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                chipEntities.forEach { entity ->
                    EntityChip(
                        entity = entity,
                        onClick = { onEntityClick(entity) }
                    )
                }
            }
        }
    }
}

private fun HomeEntity.chipOrderPriority(): Int = when (state) {
    is EntityState.Presence -> 0
    is EntityState.Sensor -> if (state.unit == "°C") 1 else 2
    is EntityState.Shutter,
    is EntityState.Switch,
    is EntityState.Climate -> 2
    is EntityState.MediaPlayer -> 3
    is EntityState.Light -> 4
}

@Composable
private fun BrightnessSlider(
    brightness: Float,
    isOn: Boolean,
    onBrightnessChange: (Float) -> Unit,
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var trackWidthPx by remember { mutableStateOf(0f) }
    val handleWidth = 8.dp
    val trackHeight = 34.dp
    val fillGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFFE08C).copy(alpha = 0.96f),
            Color(0xFFFFCC66).copy(alpha = 0.97f),
            Color(0xFFFFB347).copy(alpha = 0.99f),
            Color(0xFFFF9F2F).copy(alpha = 1f)
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f))
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .pointerInput(isOn) {
                if (!isOn) return@pointerInput
                detectTapGestures { offset ->
                    onTap()
                    onBrightnessChange((offset.x / trackWidthPx).coerceIn(0f, 1f))
                }
            }
            .pointerInput(isOn) {
                if (!isOn) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        onBrightnessChange((offset.x / trackWidthPx).coerceIn(0f, 1f))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onBrightnessChange((change.position.x / trackWidthPx).coerceIn(0f, 1f))
                    }
                )
            }
    ) {
        // Fill track
        if (brightness > 0f && isOn) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(maxWidth * brightness)
                    .background(fillGradient)
            )
        }
        // Subtle handle indicator (instead of thumb dot)
        if (isOn && brightness > 0f) {
            val handleOffset = (maxWidth * brightness - handleWidth / 2)
                .coerceAtLeast(0.dp)
                .coerceAtMost(maxWidth - handleWidth)
            Box(
                modifier = Modifier
                    .width(handleWidth)
                    .fillMaxHeight()
                    .offset(x = handleOffset)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
fun EntityChip(
    entity: HomeEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = entity.isActive()
    val chipBackground = if (active) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.97f)
    }
    val chipContentColor = if (active) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(chipBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = entity.icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = chipContentColor
        )
        Text(
            entity.chipLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = chipContentColor
        )
    }
}
