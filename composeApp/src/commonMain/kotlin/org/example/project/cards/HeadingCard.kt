package org.example.project.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import org.example.project.auth.formatStateValue
import org.example.project.auth.friendlyName
import org.example.project.auth.icon
import org.example.project.auth.unitOfMeasurement
import org.example.project.icons.HaIcon
import org.example.project.icons.MdiIcon
import org.example.project.icons.haEntityIcon
import org.example.project.icons.mdiIconByName
import org.example.project.icons.mdiStringToHaIcon

@Composable
internal fun HeadingCard(
    config: LovelaceCardConfig,
    entityStates: Map<String, HaEntityState>,
    modifier: Modifier = Modifier,
) {
    val raw = config.raw
    val text = stringField(raw, "heading")
        ?: config.title
        ?: config.nameText
    val style = stringField(raw, "heading_style")?.lowercase() ?: "title"
    val isSubtitle = style == "subtitle"
    val icon = stringField(raw, "icon")
    val badges = parseBadges(raw)

    if (text == null && badges.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (isSubtitle) 4.dp else 16.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (text != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon != null) {
                    MdiIcon(
                        icon = mdiStringToHaIcon(icon, fallback = mdiIconByName("bookmark")),
                        tint = MaterialTheme.colorScheme.primary,
                        size = if (isSubtitle) 18.dp else 22.dp,
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
            if (badge.icon != null) {
                MdiIcon(
                    icon = mdiStringToHaIcon(badge.icon, fallback = haEntityIcon(null, badge.entity)),
                    size = 18.dp,
                )
            }
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        return
    }

        // Entity badge
    val registry = LocalHaRegistry.current
    val state = badge.entity?.let(entityStates::get)
    val badgeHaIcon: HaIcon? = if (badge.showIcon) {
        mdiStringToHaIcon(badge.icon ?: state?.icon, fallback = haEntityIcon(state, badge.entity))
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
                else -> {
                    val raw = state?.attributeString(key) ?: return@mapNotNull null
                    val unit = state?.unitOfMeasurement
                        ?: if ("temp" in key) {
                            state?.attributeString("temperature_unit")
                                ?: registry.temperatureUnit
                        } else null
                    if (unit != null && raw.toFloatOrNull() != null) {
                        "${formatStateValue(raw, unit)} $unit"
                    } else {
                        raw
                    }
                }
            }
        }
        if (parts.isEmpty()) return
        contentText = parts.joinToString(" · ")
    }

    val badgeIconColor = resolveBadgeColor(badge.color)
    BadgeChip(
        tapAction = badge.tapAction,
        holdAction = badge.holdAction,
        doubleTapAction = badge.doubleTapAction,
        contextEntity = badge.entity,
    ) {
        if (badgeHaIcon != null) {
            MdiIcon(
                icon = badgeHaIcon,
                size = 18.dp,
                modifier = Modifier.offset(2.dp),
                tint = badgeIconColor ?: LocalContentColor.current,
            )
        }
        if (displayName != null) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodySmall,
            )
        } else if (contentText != null) {
            Text(
                text = contentText,
                style = MaterialTheme.typography.bodySmall,
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

    val chipBackground = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.97f)
    val chipContentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(chipBackground)
            .then(
                if (hasAnyAction) Modifier.combinedClickable(
                    onClick = { onAction(tapAction, contextEntity) },
                    onLongClick = if (holdAction !is HaAction.None) {
                        { onAction(holdAction, contextEntity) }
                    } else null,
                    onDoubleClick = if (doubleTapAction !is HaAction.None) {
                        { onAction(doubleTapAction, contextEntity) }
                    } else null,
                ) else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides chipContentColor) {
            content()
        }
    }
}

@Composable
private fun resolveBadgeColor(colorName: String?): Color? {
    val scheme = MaterialTheme.colorScheme
    return colorName?.lowercase()?.let { name ->
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
    if (unit != null) return "${formatStateValue(state, unit)} $unit"
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


