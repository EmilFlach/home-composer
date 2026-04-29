package org.example.project.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import org.example.project.auth.HaEntityState
import org.example.project.auth.attributeString
import org.example.project.auth.friendlyName
import org.example.project.icons.MdiIcon
import org.example.project.icons.mdiIconByName
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreInfoSheet(
    entityId: String,
    entityStates: Map<String, HaEntityState>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state = entityStates[entityId]
    val domain = entityId.substringBefore('.')
    val registry = LocalHaRegistry.current
    val handler = LocalHaActionHandler.current
    val displayName = registry.entityNames[entityId] ?: state?.friendlyName ?: entityId

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            HorizontalDivider()
            when (domain) {
                "cover" -> CoverSheetContent(state, entityId, handler)
                "climate" -> ClimateSheetContent(state, entityId, handler)
                "media_player" -> MediaPlayerSheetContent(state, entityId, handler)
                else -> DefaultSheetContent(state)
            }
        }
    }
}

@Composable
private fun CoverSheetContent(
    state: HaEntityState?,
    entityId: String,
    handler: (HaAction, String?) -> Unit,
) {
    val position = (state?.attributes?.get("current_position") as? JsonPrimitive)?.intOrNull

    if (position != null) {
        CoverPositionSlider(position = position, entityId = entityId, handler = handler)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = {
                handler(
                    HaAction.PerformAction(
                        action = "cover.open_cover",
                        target = buildJsonObject { put("entity_id", entityId) },
                    ),
                    null,
                )
            },
            modifier = Modifier.weight(1f),
        ) {
            MdiIcon(icon = mdiIconByName("arrow-up"), size = 18.dp)
            Spacer(Modifier.width(6.dp))
            Text("Open")
        }
        OutlinedButton(
            onClick = {
                handler(
                    HaAction.PerformAction(
                        action = "cover.stop_cover",
                        target = buildJsonObject { put("entity_id", entityId) },
                    ),
                    null,
                )
            },
            modifier = Modifier.weight(1f),
        ) {
            MdiIcon(icon = mdiIconByName("stop"), size = 18.dp)
            Spacer(Modifier.width(6.dp))
            Text("Stop")
        }
        OutlinedButton(
            onClick = {
                handler(
                    HaAction.PerformAction(
                        action = "cover.close_cover",
                        target = buildJsonObject { put("entity_id", entityId) },
                    ),
                    null,
                )
            },
            modifier = Modifier.weight(1f),
        ) {
            MdiIcon(icon = mdiIconByName("arrow-down"), size = 18.dp)
            Spacer(Modifier.width(6.dp))
            Text("Close")
        }
    }
}

@Composable
private fun CoverPositionSlider(
    position: Int,
    entityId: String,
    handler: (HaAction, String?) -> Unit,
) {
    val normalized = (position / 100f).coerceIn(0f, 1f)
    var sliderValue by remember { mutableStateOf(normalized) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(normalized) {
        if (!isDragging) sliderValue = normalized
    }

    val accent = MaterialTheme.colorScheme.primary
    var trackWidthPx by remember { mutableStateOf(0f) }
    val handleWidth = 8.dp

    fun sendPosition(value: Float) {
        val pct = (value * 100f).roundToInt().coerceIn(0, 100)
        handler(
            HaAction.PerformAction(
                action = "cover.set_cover_position",
                target = buildJsonObject { put("entity_id", entityId) },
                data = buildJsonObject { put("position", pct) },
            ),
            null,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Position",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${(sliderValue * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val value = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        sliderValue = value
                        sendPosition(value)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            sliderValue = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            sliderValue = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            sendPosition(sliderValue)
                        },
                        onDragCancel = { isDragging = false },
                    )
                },
        ) {
            if (sliderValue > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(maxWidth * sliderValue)
                        .background(accent.copy(alpha = 0.35f)),
                )
            }
            val handleOffset = (maxWidth * sliderValue - handleWidth / 2)
                .coerceAtLeast(0.dp)
                .coerceAtMost(maxWidth - handleWidth)
            Box(
                modifier = Modifier
                    .width(handleWidth)
                    .fillMaxHeight()
                    .offset(x = handleOffset)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(99.dp))
                    .background(accent),
            )
        }
    }
}

@Composable
private fun ClimateSheetContent(
    state: HaEntityState?,
    entityId: String,
    handler: (HaAction, String?) -> Unit,
) {
    val currentTemp = (state?.attributes?.get("current_temperature") as? JsonPrimitive)?.floatOrNull
    val targetTemp = (state?.attributes?.get("temperature") as? JsonPrimitive)?.floatOrNull
    val hvacMode = state?.state
    val hvacModesJson = state?.attributes?.get("hvac_modes") as? JsonArray
    val hvacModes = hvacModesJson?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        ?: listOf("off", "heat", "cool", "auto")
    val minTemp = (state?.attributes?.get("min_temp") as? JsonPrimitive)?.floatOrNull ?: 7f
    val maxTemp = (state?.attributes?.get("max_temp") as? JsonPrimitive)?.floatOrNull ?: 35f
    val tempStep = (state?.attributes?.get("target_temp_step") as? JsonPrimitive)?.floatOrNull ?: 0.5f

    var displayTemp by remember { mutableStateOf(targetTemp) }
    LaunchedEffect(targetTemp) { displayTemp = targetTemp }

    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (currentTemp != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${currentTemp.roundToInt()}°",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Light),
                )
                Text(
                    text = "Current",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (targetTemp != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val cur = displayTemp ?: return@IconButton
                            val new = (cur - tempStep).coerceAtLeast(minTemp)
                            displayTemp = new
                            handler(
                                HaAction.PerformAction(
                                    action = "climate.set_temperature",
                                    target = buildJsonObject { put("entity_id", entityId) },
                                    data = buildJsonObject { put("temperature", new) },
                                ),
                                null,
                            )
                        },
                        enabled = (displayTemp ?: 0f) > minTemp,
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = accent)
                    }
                    Text(
                        text = "${displayTemp?.roundToInt() ?: targetTemp.roundToInt()}°",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = accent,
                    )
                    IconButton(
                        onClick = {
                            val cur = displayTemp ?: return@IconButton
                            val new = (cur + tempStep).coerceAtMost(maxTemp)
                            displayTemp = new
                            handler(
                                HaAction.PerformAction(
                                    action = "climate.set_temperature",
                                    target = buildJsonObject { put("entity_id", entityId) },
                                    data = buildJsonObject { put("temperature", new) },
                                ),
                                null,
                            )
                        },
                        enabled = (displayTemp ?: 0f) < maxTemp,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase", tint = accent)
                    }
                }
                Text(
                    text = "Target",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Mode",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            hvacModes.forEach { mode ->
                FilterChip(
                    selected = mode == hvacMode,
                    onClick = {
                        handler(
                            HaAction.PerformAction(
                                action = "climate.set_hvac_mode",
                                target = buildJsonObject { put("entity_id", entityId) },
                                data = buildJsonObject { put("hvac_mode", mode) },
                            ),
                            null,
                        )
                    },
                    label = { Text(humanizeHvacMode(mode)) },
                    leadingIcon = {
                        MdiIcon(
                            icon = mdiIconByName(hvacModeIconName(mode)),
                            size = FilterChipDefaults.IconSize,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun MediaPlayerSheetContent(
    state: HaEntityState?,
    entityId: String,
    handler: (HaAction, String?) -> Unit,
) {
    val title = state?.attributeString("media_title")
    val artist = state?.attributeString("media_artist")
    val album = state?.attributeString("media_album_name")
    val coverUrl = state?.attributeString("entity_picture")
    val supportedFeatures = (state?.attributes?.get("supported_features") as? JsonPrimitive)?.intOrNull ?: 0
    var stableFeatures by remember(entityId) { mutableStateOf(supportedFeatures) }
    if (supportedFeatures != 0) stableFeatures = supportedFeatures
    val isPlaying = state?.state == "playing"
    val isOff = state?.state == "off" || state?.state == "unavailable"
    val volumeLevel = (state?.attributes?.get("volume_level") as? JsonPrimitive)?.floatOrNull
    val isMuted = state?.attributeString("is_volume_muted")?.toBooleanStrictOrNull() ?: false

    fun call(service: String, data: JsonObject? = null) {
        handler(
            HaAction.PerformAction(
                action = "media_player.$service",
                target = buildJsonObject { put("entity_id", entityId) },
                data = data,
            ),
            null,
        )
    }

    if (coverUrl != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            NetworkImage(
                url = coverUrl,
                contentDescription = title ?: "Album art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }

    if (title != null || artist != null || album != null) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            val subtitle = artist ?: album
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (stableFeatures.hasFlag(MEDIA_SUPPORT_PREVIOUS_TRACK)) {
            IconButton(onClick = { call("media_previous_track") }, enabled = !isOff) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
            }
        }
        val canPlay = stableFeatures.hasFlag(MEDIA_SUPPORT_PLAY) || stableFeatures.hasFlag(MEDIA_SUPPORT_PAUSE)
        if (canPlay || stableFeatures.hasFlag(MEDIA_SUPPORT_TURN_ON)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = {
                        when {
                            isOff && stableFeatures.hasFlag(MEDIA_SUPPORT_TURN_ON) -> call("turn_on")
                            isPlaying -> call("media_pause")
                            else -> call("media_play")
                        }
                    },
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                    )
                }
            }
        }
        if (stableFeatures.hasFlag(MEDIA_SUPPORT_STOP)) {
            IconButton(onClick = { call("turn_off") }, enabled = !isOff) {
                Icon(Icons.Filled.Stop, contentDescription = "Stop")
            }
        }
        if (stableFeatures.hasFlag(MEDIA_SUPPORT_NEXT_TRACK)) {
            IconButton(onClick = { call("media_next_track") }, enabled = !isOff) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next")
            }
        }
    }

    val effectiveVolume = if (isMuted) 0f else (volumeLevel ?: 0f).coerceIn(0f, 1f)
    if (volumeLevel != null) {
        VolumeSlider(volume = effectiveVolume, entityId = entityId, handler = handler)
    }
}

@Composable
private fun VolumeSlider(
    volume: Float,
    entityId: String,
    handler: (HaAction, String?) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    var sliderValue by remember { mutableStateOf(volume) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(volume) {
        if (!isDragging) sliderValue = volume
    }

    var trackWidthPx by remember { mutableStateOf(0f) }
    val handleWidth = 6.dp

    fun sendVolume(value: Float) {
        handler(
            HaAction.PerformAction(
                action = "media_player.volume_set",
                target = buildJsonObject { put("entity_id", entityId) },
                data = buildJsonObject { put("volume_level", value) },
            ),
            null,
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Volume",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${(sliderValue * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val value = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        sliderValue = value
                        sendVolume(value)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            sliderValue = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            sliderValue = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            sendVolume(sliderValue)
                        },
                        onDragCancel = { isDragging = false },
                    )
                },
        ) {
            if (sliderValue > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(maxWidth * sliderValue)
                        .background(accent.copy(alpha = 0.35f)),
                )
            }
            val handleOffset = (maxWidth * sliderValue - handleWidth / 2)
                .coerceAtLeast(0.dp)
                .coerceAtMost(maxWidth - handleWidth)
            Box(
                modifier = Modifier
                    .width(handleWidth)
                    .fillMaxHeight()
                    .offset(x = handleOffset)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(99.dp))
                    .background(accent),
            )
        }
    }
}

@Composable
private fun DefaultSheetContent(state: HaEntityState?) {
    if (state == null) {
        Text(
            text = "Unavailable",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Text(
        text = state.state.replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.bodyLarge,
    )
}

private fun humanizeHvacMode(mode: String): String = when (mode) {
    "off" -> "Off"
    "heat" -> "Heat"
    "cool" -> "Cool"
    "auto" -> "Auto"
    "heat_cool" -> "Heat/Cool"
    "fan_only" -> "Fan"
    "dry" -> "Dry"
    else -> mode.replaceFirstChar { it.uppercase() }
}

private fun hvacModeIconName(mode: String): String = when (mode) {
    "heat" -> "fire"
    "cool" -> "snowflake"
    "auto" -> "thermostat-auto"
    "heat_cool" -> "autorenew"
    "fan_only" -> "fan"
    "dry" -> "water-percent"
    "off" -> "power"
    else -> "thermostat"
}

private const val MEDIA_SUPPORT_PAUSE = 1
private const val MEDIA_SUPPORT_PREVIOUS_TRACK = 16
private const val MEDIA_SUPPORT_NEXT_TRACK = 32
private const val MEDIA_SUPPORT_TURN_ON = 128
private const val MEDIA_SUPPORT_STOP = 4096
private const val MEDIA_SUPPORT_PLAY = 16384

private fun Int.hasFlag(flag: Int): Boolean = (this and flag) == flag
