package org.example.project.cards

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import org.example.project.icons.HaIcon
import org.example.project.icons.MdiIcon
import org.example.project.icons.haEntityIcon
import org.example.project.icons.mdiStringToHaIcon
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.put
import org.example.project.auth.HaEntityState
import org.example.project.auth.domain
import org.example.project.auth.formatStateValue
import org.example.project.auth.friendlyName
import org.example.project.auth.icon
import org.example.project.auth.isActive
import org.example.project.auth.unitOfMeasurement
import kotlin.math.roundToInt

@Composable
internal fun TileCard(
    config: LovelaceCardConfig,
    entityStates: Map<String, HaEntityState>,
    modifier: Modifier = Modifier,
) {
    val raw = config.raw
    val entityId = config.entity
    val state = entityId?.let(entityStates::get)

    val displayName = config.name
        ?: state?.friendlyName
        ?: entityId
        ?: "Unknown"

    val isActive = state?.isActive() ?: false
    val accent = resolveAccent(stringField(raw, "color"), isActive)
    val icon: HaIcon = mdiStringToHaIcon(config.icon ?: state?.icon, fallback = haEntityIcon(state, entityId))
    val hideState = boolField(raw, "hide_state", default = false)
    val vertical = boolField(raw, "vertical", default = false)
    val stateText = if (hideState) null else state.formatStateText(entityId)
    val percent = state?.percentValue()

    val domain = state?.domain ?: entityId?.substringBefore('.', missingDelimiterValue = "") ?: ""

    val featuresArray = raw?.get("features") as? JsonArray
    val featureTypes: Set<String>? = featuresArray?.mapNotNull {
        ((it as? JsonObject)?.get("type") as? JsonPrimitive)?.content
    }?.toSet()
    val showBrightness = domain == "light" && (featureTypes == null || "light-brightness" in featureTypes)
    val showTempControl = domain == "climate" && (featureTypes == null || "target-temperature" in featureTypes)

    val isToggleable = domain in TOGGLEABLE_DOMAINS
    val handler = LocalHaActionHandler.current
    val onToggle: (() -> Unit)? = if (isToggleable && entityId != null) {
        { handler(HaAction.Toggle(entityId), entityId) }
    } else null

    val cardShape = RoundedCornerShape(16.dp)
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

    @Composable
    fun TileBody() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (percent != null) Modifier.percentBackground(percent, accent) else Modifier),
        ) {
            if (vertical) {
                VerticalTileBody(icon = icon, accent = accent, name = displayName, stateText = stateText)
            } else {
                HorizontalTileBody(icon = icon, accent = accent, name = displayName, stateText = stateText)
            }
        }
        if (showBrightness) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            LightBrightnessFeature(state = state, accent = accent, entityId = entityId ?: "", handler = handler)
        }
        if (showTempControl) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ClimateTemperatureFeature(state = state, accent = accent, entityId = entityId ?: "", handler = handler)
        }
    }

    if (onToggle != null) {
        Card(
            onClick = onToggle,
            modifier = modifier.fillMaxWidth(),
            shape = cardShape,
            colors = cardColors,
        ) { TileBody() }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = cardShape,
            colors = cardColors,
        ) { TileBody() }
    }
}

@Composable
private fun LightBrightnessFeature(
    state: HaEntityState?,
    accent: Color,
    entityId: String,
    handler: (HaAction, String?) -> Unit,
) {
    val rawBrightness = (state?.attributes?.get("brightness") as? JsonPrimitive)?.floatOrNull
    val normalizedBrightness = rawBrightness?.let { it / 255f }?.coerceIn(0f, 1f) ?: 0f
    val isOn = state?.isActive() ?: false

    var sliderValue by remember { mutableStateOf(normalizedBrightness) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(normalizedBrightness) {
        if (!isDragging) sliderValue = normalizedBrightness
    }

    fun sendBrightness(value: Float) {
        val pct = (value * 100f).roundToInt().coerceIn(0, 100)
        handler(
            HaAction.PerformAction(
                action = "light.turn_on",
                target = buildJsonObject { put("entity_id", entityId) },
                data = buildJsonObject { put("brightness_pct", pct) },
            ),
            null,
        )
    }

    val animatedSliderValue by animateFloatAsState(
        targetValue = sliderValue,
        animationSpec = if (isDragging) snap() else spring(stiffness = Spring.StiffnessMediumLow),
    )

    var trackWidthPx by remember { mutableStateOf(0f) }
    val handleWidth = 8.dp
    val fillGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFFE08C).copy(alpha = 0.96f),
            Color(0xFFFFCC66).copy(alpha = 0.97f),
            Color(0xFFFFB347).copy(alpha = 0.99f),
            Color(0xFFFF9F2F).copy(alpha = 1f),
        )
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .pointerInput(isOn) {
                if (!isOn) return@pointerInput
                detectTapGestures { offset ->
                    val value = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                    sliderValue = value
                    sendBrightness(value)
                }
            }
            .pointerInput(isOn) {
                if (!isOn) return@pointerInput
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
                        sendBrightness(sliderValue)
                    },
                    onDragCancel = { isDragging = false },
                )
            }
    ) {
        if (animatedSliderValue > 0f && isOn) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(maxWidth * animatedSliderValue)
                    .background(fillGradient)
            )
        }
        if (isOn && animatedSliderValue > 0f) {
            val handleOffset = (maxWidth * animatedSliderValue - handleWidth / 2)
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
private fun ClimateTemperatureFeature(
    state: HaEntityState?,
    accent: Color,
    entityId: String,
    handler: (HaAction, String?) -> Unit,
) {
    val targetTemp = (state?.attributes?.get("temperature") as? JsonPrimitive)?.floatOrNull
    val currentTemp = (state?.attributes?.get("current_temperature") as? JsonPrimitive)?.floatOrNull
    val minTemp = (state?.attributes?.get("min_temp") as? JsonPrimitive)?.floatOrNull ?: 7f
    val maxTemp = (state?.attributes?.get("max_temp") as? JsonPrimitive)?.floatOrNull ?: 35f

    var displayTemp by remember { mutableStateOf(targetTemp) }
    LaunchedEffect(targetTemp) { displayTemp = targetTemp }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                val current = displayTemp ?: return@IconButton
                val newTemp = (current - 0.5f).coerceAtLeast(minTemp)
                displayTemp = newTemp
                handler(
                    HaAction.PerformAction(
                        action = "climate.set_temperature",
                        target = buildJsonObject { put("entity_id", entityId) },
                        data = buildJsonObject { put("temperature", newTemp) },
                    ),
                    null,
                )
            },
            enabled = displayTemp != null && (displayTemp ?: 0f) > minTemp,
        ) {
            Icon(imageVector = Icons.Filled.Remove, contentDescription = "Decrease", tint = accent)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text(
                text = displayTemp?.let { formatTemp(it) } ?: "—",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = accent,
            )
            if (currentTemp != null) {
                Text(
                    text = "now ${formatTemp(currentTemp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        IconButton(
            onClick = {
                val current = displayTemp ?: return@IconButton
                val newTemp = (current + 0.5f).coerceAtMost(maxTemp)
                displayTemp = newTemp
                handler(
                    HaAction.PerformAction(
                        action = "climate.set_temperature",
                        target = buildJsonObject { put("entity_id", entityId) },
                        data = buildJsonObject { put("temperature", newTemp) },
                    ),
                    null,
                )
            },
            enabled = displayTemp != null && (displayTemp ?: 0f) < maxTemp,
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Increase", tint = accent)
        }
    }
}

private fun formatTemp(temp: Float): String = "${temp.roundToInt()}°"

private fun Modifier.percentBackground(fraction: Float, color: Color): Modifier =
    drawBehind {
        val width = size.width * fraction
        if (width <= 0f) return@drawBehind
        drawRoundRect(
            color = color.copy(alpha = 0.22f),
            size = Size(width, size.height),
            cornerRadius = CornerRadius(16.dp.toPx()),
        )
    }

private fun HaEntityState.percentValue(): Float? {
    if (unitOfMeasurement != "%") return null
    val value = state.toFloatOrNull() ?: return null
    return (value / 100f).coerceIn(0f, 1f)
}

@Composable
private fun HorizontalTileBody(
    icon: HaIcon,
    accent: Color,
    name: String,
    stateText: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TileIconBadge(icon = icon, accent = accent)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (stateText != null) {
                Text(
                    text = stateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun VerticalTileBody(
    icon: HaIcon,
    accent: Color,
    name: String,
    stateText: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TileIconBadge(icon = icon, accent = accent)
        Spacer(Modifier.height(2.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (stateText != null) {
            Text(
                text = stateText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TileIconBadge(icon: HaIcon, accent: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = accent.copy(alpha = 0.18f), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        MdiIcon(icon = icon, tint = accent, size = 22.dp)
    }
}

private val TOGGLEABLE_DOMAINS = setOf(
    "light", "switch", "fan", "input_boolean",
)

private fun HaEntityState?.formatStateText(entityIdFallback: String?): String {
    if (this == null) return if (entityIdFallback == null) "—" else "Unavailable"
    val unit = unitOfMeasurement
    val humanized = humanizeState(state, domain)
    val value = if (humanized == state) formatStateValue(state, unit) else humanized

    if (domain == "climate" && state != "off") {
        val currentTemp = (attributes["current_temperature"] as? JsonPrimitive)?.floatOrNull
        if (currentTemp != null) return "$value · ${formatTemp(currentTemp)}"
    }

    return if (unit != null) "$value $unit" else value
}

private fun humanizeState(raw: String, domain: String): String {
    val lower = raw.lowercase()
    return when (lower) {
        "on" -> "On"
        "off" -> "Off"
        "open" -> "Open"
        "closed" -> "Closed"
        "opening" -> "Opening"
        "closing" -> "Closing"
        "locked" -> "Locked"
        "unlocked" -> "Unlocked"
        "home" -> "Home"
        "not_home" -> "Away"
        "playing" -> "Playing"
        "paused" -> "Paused"
        "idle" -> "Idle"
        "standby" -> "Standby"
        "unavailable" -> "Unavailable"
        "unknown" -> "Unknown"
        "above_horizon" -> "Day"
        "below_horizon" -> "Night"
        else -> when (domain) {
            "climate", "water_heater" -> raw.replace("_", " ").replaceFirstChar { it.uppercase() }
            else -> raw
        }
    }
}


@Composable
private fun resolveAccent(colorName: String?, isActive: Boolean): Color {
    val scheme = MaterialTheme.colorScheme
    val named = colorName?.lowercase()?.let { name ->
        when (name) {
            "primary" -> scheme.primary
            "accent" -> scheme.secondary
            "red" -> Color(0xFFE53935)
            "pink" -> Color(0xFFD81B60)
            "purple" -> Color(0xFF8E24AA)
            "deep-purple", "deep_purple" -> Color(0xFF5E35B1)
            "indigo" -> Color(0xFF3949AB)
            "blue" -> Color(0xFF1E88E5)
            "light-blue", "light_blue" -> Color(0xFF039BE5)
            "cyan" -> Color(0xFF00ACC1)
            "teal" -> Color(0xFF00897B)
            "green" -> Color(0xFF43A047)
            "light-green", "light_green" -> Color(0xFF7CB342)
            "lime" -> Color(0xFFC0CA33)
            "yellow" -> Color(0xFFFDD835)
            "amber" -> Color(0xFFFFB300)
            "orange" -> Color(0xFFFB8C00)
            "deep-orange", "deep_orange" -> Color(0xFFF4511E)
            "brown" -> Color(0xFF6D4C41)
            "grey", "gray" -> Color(0xFF757575)
            "blue-grey", "blue_grey" -> Color(0xFF546E7A)
            "disabled" -> scheme.outline
            else -> null
        }
    }
    if (named != null) return named
    return if (isActive) scheme.primary else scheme.onSurfaceVariant
}

private fun boolField(obj: JsonObject?, key: String, default: Boolean): Boolean {
    val element = obj?.get(key) as? JsonPrimitive ?: return default
    return when (element.content.lowercase()) {
        "true" -> true
        "false" -> false
        else -> default
    }
}
