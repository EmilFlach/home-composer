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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
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
        if (badge.icon == null && badge.text == null) return
        BadgeChip(
            tapAction = badge.tapAction,
            holdAction = badge.holdAction,
            doubleTapAction = badge.doubleTapAction,
            contextEntity = badge.entity,
        ) {
            if (badge.icon != null) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
            if (badge.text != null) {
                Text(
                    text = badge.text,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                )
            }
        }
        return
    }

    // Entity badge
    val state = badge.entity?.let(entityStates::get)
    val displayName = badge.name ?: state?.friendlyName ?: badge.entity ?: return

    // Badge icon: explicit override → entity attribute icon
    val resolvedIcon = badge.icon ?: state?.icon

    // State content: explicit list → "state" if show_state → nothing
    val contentKeys = when {
        badge.stateContent.isNotEmpty() -> badge.stateContent
        badge.showState -> listOf("state")
        else -> emptyList()
    }
    val contentParts = contentKeys.mapNotNull { key ->
        when (key) {
            "state" -> state?.let {
                val unit = it.unitOfMeasurement
                if (unit != null) "${it.state} $unit" else it.state
            }
            "name" -> null // already rendered as the label
            "last_changed", "last_updated" -> null // requires relative time formatting
            else -> state?.attributeString(key)
        }
    }

    BadgeChip(
        tapAction = badge.tapAction,
        holdAction = badge.holdAction,
        doubleTapAction = badge.doubleTapAction,
        contextEntity = badge.entity,
    ) {
        if (badge.showIcon && resolvedIcon != null) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = displayName,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
        )
        if (contentParts.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = contentParts.joinToString(" · "),
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
    val showState: Boolean = false,
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
                val showState = (element["show_state"] as? JsonPrimitive)?.booleanOrNull ?: false
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

                if (type == "button" && icon == null && text == null) return@mapNotNull null
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
