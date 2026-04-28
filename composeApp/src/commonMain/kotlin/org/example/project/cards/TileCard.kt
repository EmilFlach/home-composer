package org.example.project.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Blinds
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.example.project.auth.HaEntityState
import org.example.project.auth.domain
import org.example.project.auth.friendlyName
import org.example.project.auth.unitOfMeasurement

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
    val icon = iconFor(state, entityId)
    val hideState = boolField(raw, "hide_state", default = false)
    val vertical = boolField(raw, "vertical", default = false)
    val stateText = if (hideState) null else state.formatStateText(entityId)
    val percent = state?.percentValue()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (percent != null) Modifier.percentBackground(percent, accent) else Modifier),
        ) {
            if (vertical) {
                VerticalTileBody(
                    icon = icon,
                    accent = accent,
                    name = displayName,
                    stateText = stateText,
                )
            } else {
                HorizontalTileBody(
                    icon = icon,
                    accent = accent,
                    name = displayName,
                    stateText = stateText,
                )
            }
        }
    }
}

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
    icon: ImageVector,
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
    icon: ImageVector,
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
private fun TileIconBadge(icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = accent.copy(alpha = 0.18f), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun HaEntityState.isActive(): Boolean {
    val s = state.lowercase()
    if (s in INACTIVE_STATES) return false
    return when (domain) {
        "light", "switch", "fan", "input_boolean", "binary_sensor", "automation",
        "remote", "siren", "humidifier", "vacuum" -> s == "on"
        "cover" -> s == "open" || s == "opening"
        "lock" -> s == "unlocked"
        "climate", "water_heater" -> s != "off"
        "media_player" -> s == "playing" || s == "paused" || s == "on"
        "person", "device_tracker" -> s == "home"
        "alarm_control_panel" -> s.startsWith("armed") || s == "triggered"
        "sun" -> s == "above_horizon"
        "weather" -> false
        else -> s != "off"
    }
}

private val INACTIVE_STATES = setOf(
    "off", "closed", "locked", "not_home", "away", "disarmed",
    "idle", "standby", "unavailable", "unknown", "none", "",
)

private fun HaEntityState?.formatStateText(entityIdFallback: String?): String {
    if (this == null) return if (entityIdFallback == null) "—" else "Unavailable"
    val unit = unitOfMeasurement
    val value = humanizeState(state, domain)
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

private fun iconFor(state: HaEntityState?, entityId: String?): ImageVector {
    val domain = state?.domain ?: entityId?.substringBefore('.', missingDelimiterValue = "") ?: ""
    val isOn = state?.isActive() ?: false
    return when (domain) {
        "light" -> Icons.Filled.Lightbulb
        "switch", "input_boolean" -> Icons.Filled.ToggleOn
        "fan" -> Icons.Filled.Air
        "cover" -> Icons.Filled.Blinds
        "lock" -> if (isOn) Icons.Filled.LockOpen else Icons.Filled.Lock
        "climate", "water_heater" -> Icons.Filled.Thermostat
        "media_player" -> Icons.Filled.PlayArrow
        "binary_sensor" -> Icons.Filled.MotionPhotosOn
        "sensor" -> Icons.Filled.Sensors
        "person", "device_tracker" -> Icons.Filled.Person
        "automation", "script" -> Icons.Filled.Power
        "weather" -> Icons.Filled.WbSunny
        "sun" -> Icons.Filled.WbSunny
        "alarm_control_panel" -> Icons.Filled.Notifications
        "humidifier" -> Icons.Filled.WaterDrop
        "siren" -> Icons.Filled.Whatshot
        "input_button", "button" -> Icons.Filled.Power
        "zone" -> Icons.Filled.Home
        "vacuum" -> Icons.Filled.Home
        "binary_sensor.door", "door" -> Icons.Filled.DoorFront
        "window" -> Icons.Filled.Window
        else -> Icons.AutoMirrored.Filled.HelpOutline
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
