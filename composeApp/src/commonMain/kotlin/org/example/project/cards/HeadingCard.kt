package org.example.project.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Blinds
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MotionPhotosOn
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
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import org.example.project.auth.HaEntityState
import org.example.project.auth.attributeString
import org.example.project.auth.domain
import org.example.project.auth.friendlyName
import org.example.project.auth.icon
import org.example.project.auth.unitOfMeasurement

@Composable
internal fun HeadingCard(
    config: LovelaceCardConfig,
    entityStates: Map<String, HaEntityState>,
    modifier: Modifier = Modifier,
) {
    val raw = config.raw
    val text = stringField(raw, "heading")
        ?: config.title
        ?: config.name
    val style = stringField(raw, "heading_style")?.lowercase() ?: "title"
    val isSubtitle = style == "subtitle"
    val icon = stringField(raw, "icon")
    val badges = parseBadges(raw)

    if (text == null && badges.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (isSubtitle) 4.dp else 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (text != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isSubtitle) 18.dp else 22.dp),
                    )
                }
                Text(
                    text = text,
                    style = if (isSubtitle) {
                        MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    } else {
                        MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    },
                    color = if (isSubtitle) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
        if (badges.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                badges.forEach { badge ->
                    HeadingBadge(badge = badge, entityStates = entityStates)
                }
            }
        }
    }
}

@Composable
private fun HeadingBadge(
    badge: HeadingBadgeConfig,
    entityStates: Map<String, HaEntityState>,
) {
    if (!evaluateVisibility(badge.visibility, entityStates)) return
    if (badge.type == "button") {
        val label = badge.name ?: badge.text
        if (badge.icon == null && label == null) return
        BadgeChip(
            tapAction = badge.tapAction,
            holdAction = badge.holdAction,
            doubleTapAction = badge.doubleTapAction,
            contextEntity = badge.entity,
        ) {
            val btnIcon = mdiToImageVector(badge.icon)
            if (btnIcon != null) {
                Icon(
                    imageVector = btnIcon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                )
            }
        }
        return
    }

    // Entity badge
    val state = badge.entity?.let(entityStates::get)
    val badgeIcon: ImageVector? = if (badge.showIcon) {
        mdiToImageVector(badge.icon ?: state?.icon) ?: resolveEntityBadgeIcon(state, badge.entity)
    } else null

    val contentKeys = when {
        badge.stateContent.isNotEmpty() -> badge.stateContent
        badge.showState -> listOf("state")
        else -> emptyList()
    }

    // Resolve what to display before entering the chip lambda (return not allowed inside non-inline lambda)
    val displayName: String?
    val contentText: String?
    if (contentKeys.isEmpty()) {
        displayName = badge.name ?: state?.friendlyName ?: badge.entity ?: return
        contentText = null
    } else {
        displayName = null
        val parts = contentKeys.mapNotNull { key ->
            when (key) {
                "state" -> state?.formatBadgeState()
                "name" -> badge.name ?: state?.friendlyName
                "last_changed", "last_updated" -> null
                else -> state?.attributeString(key)
            }
        }
        if (parts.isEmpty()) return
        contentText = parts.joinToString(" · ")
    }

    BadgeChip(
        tapAction = badge.tapAction,
        holdAction = badge.holdAction,
        doubleTapAction = badge.doubleTapAction,
        contextEntity = badge.entity,
    ) {
        if (badgeIcon != null) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        if (displayName != null) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            )
        } else if (contentText != null) {
            Text(
                text = contentText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BadgeChip(
    tapAction: HaAction = HaAction.None,
    holdAction: HaAction = HaAction.None,
    doubleTapAction: HaAction = HaAction.None,
    contextEntity: String? = null,
    content: @Composable () -> Unit,
) {
    val onAction = LocalHaActionHandler.current
    val hasAnyAction = tapAction !is HaAction.None ||
        holdAction !is HaAction.None ||
        doubleTapAction !is HaAction.None

    Surface(
        modifier = if (hasAnyAction) {
            Modifier.combinedClickable(
                onClick = { onAction(tapAction, contextEntity) },
                onLongClick = if (holdAction !is HaAction.None) {
                    { onAction(holdAction, contextEntity) }
                } else null,
                onDoubleClick = if (doubleTapAction !is HaAction.None) {
                    { onAction(doubleTapAction, contextEntity) }
                } else null,
            )
        } else Modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            content()
        }
    }
}

private data class HeadingBadgeConfig(
    val type: String = "entity",
    val entity: String? = null,
    val name: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val showIcon: Boolean = true,
    val showState: Boolean = true,
    val stateContent: List<String> = emptyList(),
    val text: String? = null,
    val visibility: List<HaCondition> = emptyList(),
    val tapAction: HaAction = HaAction.None,
    val holdAction: HaAction = HaAction.None,
    val doubleTapAction: HaAction = HaAction.None,
)

private fun parseBadges(raw: JsonObject?): List<HeadingBadgeConfig> {
    val arr = raw?.get("badges") as? JsonArray ?: return emptyList()
    return arr.mapNotNull { element ->
        when (element) {
            is JsonPrimitive -> element.contentOrNull?.let { HeadingBadgeConfig(entity = it) }
            is JsonObject -> {
                val type = (element["type"] as? JsonPrimitive)?.contentOrNull ?: "entity"
                val entity = (element["entity"] as? JsonPrimitive)?.contentOrNull
                val name = (element["name"] as? JsonPrimitive)?.contentOrNull
                val icon = (element["icon"] as? JsonPrimitive)?.contentOrNull
                val color = (element["color"] as? JsonPrimitive)?.contentOrNull
                val showIcon = (element["show_icon"] as? JsonPrimitive)?.booleanOrNull ?: true
                val showState = (element["show_state"] as? JsonPrimitive)?.booleanOrNull ?: true
                val stateContent = when (val sc = element["state_content"]) {
                    is JsonPrimitive -> sc.contentOrNull?.let { listOf(it) } ?: emptyList()
                    is JsonArray -> sc.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                    else -> emptyList()
                }
                val text = (element["text"] as? JsonPrimitive)?.contentOrNull
                val visibility = parseVisibility(element["visibility"])
                val tapAction = parseAction(element, "tap_action")
                val holdAction = parseAction(element, "hold_action")
                val doubleTapAction = parseAction(element, "double_tap_action")

                if (type == "button" && icon == null && text == null && name == null) return@mapNotNull null
                if (type != "button" && entity == null && name == null) return@mapNotNull null

                HeadingBadgeConfig(
                    type = type,
                    entity = entity,
                    name = name,
                    icon = icon,
                    color = color,
                    showIcon = showIcon,
                    showState = showState,
                    stateContent = stateContent,
                    text = text,
                    visibility = visibility,
                    tapAction = tapAction,
                    holdAction = holdAction,
                    doubleTapAction = doubleTapAction,
                )
            }
            else -> null
        }
    }
}

private fun HaEntityState.formatBadgeState(): String {
    val unit = unitOfMeasurement
    if (unit != null) return "$state $unit"
    if (domain != "binary_sensor") return state
    val isOn = state.lowercase() == "on"
    return when (attributeString("device_class")) {
        "occupancy", "motion", "presence", "vibration" -> if (isOn) "Detected" else "Clear"
        "door", "window", "garage_door", "opening" -> if (isOn) "Open" else "Closed"
        "lock" -> if (isOn) "Unlocked" else "Locked"
        "smoke", "co", "co2", "gas", "safety" -> if (isOn) "Detected" else "Clear"
        "moisture" -> if (isOn) "Wet" else "Dry"
        "battery" -> if (isOn) "Low" else "Normal"
        "connectivity" -> if (isOn) "Connected" else "Disconnected"
        "plug", "power" -> if (isOn) "Plugged in" else "Unplugged"
        "light" -> if (isOn) "Detected" else "Clear"
        "running" -> if (isOn) "Running" else "Not running"
        "problem" -> if (isOn) "Problem" else "OK"
        "tamper" -> if (isOn) "Tampered" else "Clear"
        "update" -> if (isOn) "Update available" else "Up to date"
        else -> if (isOn) "On" else "Off"
    }
}

private fun mdiToImageVector(mdi: String?): ImageVector? = when (mdi) {
    // Motion / occupancy
    "mdi:motion-sensor", "mdi:motion-sensor-off",
    "mdi:run", "mdi:walk" -> Icons.Filled.MotionPhotosOn
    // Door / window
    "mdi:door-open", "mdi:door-closed" -> Icons.Filled.DoorFront
    "mdi:window-open", "mdi:window-closed" -> Icons.Filled.Window
    "mdi:curtains", "mdi:curtains-closed" -> Icons.Filled.Blinds
    "mdi:blinds", "mdi:blinds-horizontal",
    "mdi:blinds-open", "mdi:blinds-horizontal-closed",
    "mdi:garage", "mdi:garage-open" -> Icons.Filled.Blinds
    // Lights
    "mdi:lightbulb", "mdi:lightbulb-on", "mdi:lightbulb-off",
    "mdi:lightbulb-auto", "mdi:lightbulb-auto-outline",
    "mdi:ceiling-light", "mdi:ceiling-light-outline",
    "mdi:floor-lamp", "mdi:led-strip-variant" -> Icons.Filled.Lightbulb
    // Lock
    "mdi:lock", "mdi:lock-outline" -> Icons.Filled.Lock
    "mdi:lock-open", "mdi:lock-open-outline" -> Icons.Filled.LockOpen
    // Climate
    "mdi:thermostat", "mdi:thermometer" -> Icons.Filled.Thermostat
    "mdi:sun-thermometer", "mdi:sun-thermometer-outline" -> Icons.Filled.WbSunny
    // Media
    "mdi:play-circle", "mdi:music", "mdi:music-note",
    "mdi:filmstrip", "mdi:cast", "mdi:cast-off",
    "mdi:sony-playstation", "mdi:television", "mdi:speaker" -> Icons.Filled.PlayArrow
    // People
    "mdi:account", "mdi:account-circle" -> Icons.Filled.Person
    // Sensors / environment
    "mdi:water", "mdi:water-alert" -> Icons.Filled.WaterDrop
    "mdi:smoke-detector", "mdi:smoke", "mdi:fire", "mdi:fire-alert" -> Icons.Filled.Whatshot
    "mdi:toggle-switch", "mdi:toggle-switch-off" -> Icons.Filled.ToggleOn
    "mdi:power", "mdi:power-plug", "mdi:power-plug-off" -> Icons.Filled.Power
    "mdi:home-automation" -> Icons.Filled.Power
    "mdi:weather-sunny" -> Icons.Filled.WbSunny
    else -> null
}

private fun resolveEntityBadgeIcon(state: HaEntityState?, entityId: String?): ImageVector? {
    val domain = state?.domain ?: entityId?.substringBefore('.', missingDelimiterValue = "") ?: return null
    val isOn = state?.state?.lowercase() == "on"
    return when (domain) {
        "binary_sensor" -> when (state?.attributeString("device_class")) {
            "occupancy", "motion", "presence", "vibration" -> Icons.Filled.MotionPhotosOn
            "door", "opening" -> Icons.Filled.DoorFront
            "window" -> Icons.Filled.Window
            "moisture" -> Icons.Filled.WaterDrop
            "smoke", "co", "co2", "gas" -> Icons.Filled.Whatshot
            "lock" -> if (isOn) Icons.Filled.LockOpen else Icons.Filled.Lock
            "connectivity" -> Icons.Filled.Sensors
            else -> Icons.Filled.Sensors
        }
        "sensor" -> Icons.Filled.Sensors
        "media_player" -> Icons.Filled.PlayArrow
        "cover" -> Icons.Filled.Blinds
        "light" -> Icons.Filled.Lightbulb
        "switch", "input_boolean" -> Icons.Filled.ToggleOn
        "lock" -> if (isOn) Icons.Filled.LockOpen else Icons.Filled.Lock
        "group", "automation", "script" -> Icons.Filled.Power
        "person", "device_tracker" -> Icons.Filled.Person
        "weather", "sun" -> Icons.Filled.WbSunny
        else -> Icons.AutoMirrored.Filled.HelpOutline
    }
}
