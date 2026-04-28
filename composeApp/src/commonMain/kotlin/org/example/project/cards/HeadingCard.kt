package org.example.project.cards

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
import kotlinx.serialization.json.contentOrNull
import org.example.project.auth.HaEntityState
import org.example.project.auth.friendlyName
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
    val state = badge.entity?.let(entityStates::get)
    val name = badge.name ?: state?.friendlyName ?: badge.entity ?: return
    val valueText = state?.let {
        val unit = it.unitOfMeasurement
        if (unit != null) "${it.state} $unit" else it.state
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            )
            if (valueText != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private data class HeadingBadgeConfig(
    val entity: String?,
    val name: String?,
)

private fun parseBadges(raw: JsonObject?): List<HeadingBadgeConfig> {
    val arr = raw?.get("badges") as? JsonArray ?: return emptyList()
    return arr.mapNotNull { element ->
        when (element) {
            is JsonPrimitive -> element.contentOrNull?.let { HeadingBadgeConfig(entity = it, name = null) }
            is JsonObject -> {
                val entity = (element["entity"] as? JsonPrimitive)?.contentOrNull
                val name = (element["name"] as? JsonPrimitive)?.contentOrNull
                if (entity == null && name == null) null
                else HeadingBadgeConfig(entity = entity, name = name)
            }
            else -> null
        }
    }
}
