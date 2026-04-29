package org.example.project.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.example.project.auth.HaEntityState
import org.example.project.auth.attributeString
import org.example.project.auth.domain
import org.example.project.auth.formatStateValue
import org.example.project.auth.isActive
import org.example.project.auth.unitOfMeasurement
import org.example.project.icons.MdiIcon
import org.example.project.icons.haEntityIcon
import org.example.project.icons.mdiIconByName

private val DEFAULT_SENSOR_CLASSES = listOf("temperature", "humidity")
private val DEFAULT_ALERT_CLASSES = listOf("motion", "moisture")

private val DOMAIN_CHIP_ORDER = listOf(
    "light", "switch", "fan", "cover", "media_player", "climate",
)

@Composable
internal fun AreaCard(
    config: LovelaceCardConfig,
    entityStates: Map<String, HaEntityState>,
    modifier: Modifier = Modifier,
) {
    val raw = config.raw
    val areaId = stringField(raw, "area")
    val registry = LocalHaRegistry.current
    val areaName = areaId?.let { registry.areas[it] }

    val sensorClasses = stringList(raw, "sensor_classes") ?: DEFAULT_SENSOR_CLASSES
    val alertClasses = stringList(raw, "alert_classes") ?: DEFAULT_ALERT_CLASSES
    val showCamera = boolField(raw, "show_camera", default = false)
    val navigationPath = stringField(raw, "navigation_path")

    val areaEntityIds = areaId?.let(registry::entitiesInArea).orEmpty()
    val areaStates: List<HaEntityState> = areaEntityIds.mapNotNull(entityStates::get)

    val cameraImage = if (showCamera) {
        areaStates.firstOrNull { it.domain == "camera" }
            ?.let { "/api/camera_proxy/${it.entityId}" }
    } else null
    val explicitImage = stringField(raw, "image")
    val resolvedImage = explicitImage ?: cameraImage

    val displayName = config.nameText ?: areaName ?: areaId ?: "Area"

    val sensors = pickSensors(areaStates, sensorClasses)
    val alerts = pickAlerts(areaStates, alertClasses)
    val domainCounts = countActiveByDomain(areaStates)

    val aspectRatio = stringField(raw, "aspect_ratio")?.let(::parseAspectRatio) ?: (16f / 9f)

    val handler = LocalHaActionHandler.current
    val onClick: (() -> Unit)? = navigationPath?.let { path ->
        { handler(HaAction.Navigate(path), null) }
    }

    val cardShape = RoundedCornerShape(16.dp)
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )

    @Composable
    fun Body() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio.coerceAtLeast(0.1f))
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (resolvedImage != null) {
                NetworkImage(
                    url = resolvedImage,
                    contentDescription = displayName,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (alerts.isNotEmpty()) {
                AlertBadges(
                    alerts = alerts,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                )
            }
            AreaBottomOverlay(
                name = displayName,
                sensorText = formatSensors(sensors),
                domainCounts = domainCounts,
                hasImage = resolvedImage != null,
            )
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = cardShape,
            colors = cardColors,
        ) { Body() }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = cardShape,
            colors = cardColors,
        ) { Body() }
    }
}

private fun HaRegistry.entitiesInArea(areaId: String): List<String> {
    val result = LinkedHashSet<String>()
    for ((entityId, entityArea) in entityAreaIds) {
        if (entityArea == areaId) result.add(entityId)
    }
    val devicesInArea = deviceAreaIds.entries
        .asSequence()
        .filter { it.value == areaId }
        .mapTo(mutableSetOf()) { it.key }
    if (devicesInArea.isNotEmpty()) {
        for ((entityId, deviceId) in entityDeviceIds) {
            if (deviceId in devicesInArea && entityAreaIds[entityId] == null) {
                result.add(entityId)
            }
        }
    }
    return result.toList()
}

private fun pickSensors(
    states: List<HaEntityState>,
    sensorClasses: List<String>,
): List<HaEntityState> = sensorClasses.mapNotNull { cls ->
    states.firstOrNull {
        it.domain == "sensor" && it.attributeString("device_class") == cls
    }
}

private fun pickAlerts(
    states: List<HaEntityState>,
    alertClasses: List<String>,
): List<HaEntityState> = states.filter {
    it.domain == "binary_sensor" &&
        it.state.equals("on", ignoreCase = true) &&
        it.attributeString("device_class") in alertClasses
}

private fun countActiveByDomain(states: List<HaEntityState>): Map<String, Int> {
    val counts = mutableMapOf<String, Int>()
    for (s in states) {
        if (s.domain in DOMAIN_CHIP_ORDER && s.isActive()) {
            counts[s.domain] = (counts[s.domain] ?: 0) + 1
        }
    }
    return counts
}

private fun formatSensors(sensors: List<HaEntityState>): String? {
    if (sensors.isEmpty()) return null
    return sensors.joinToString(" · ") { state ->
        val unit = state.unitOfMeasurement
        val value = formatStateValue(state.state, unit)
        if (unit != null) "$value$unit" else value
    }
}

@Composable
private fun AlertBadges(alerts: List<HaEntityState>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (alert in alerts) {
            MdiIcon(
                icon = haEntityIcon(alert, alert.entityId),
                tint = Color.White,
                size = 16.dp,
            )
        }
    }
}

@Composable
private fun AreaBottomOverlay(
    name: String,
    sensorText: String?,
    domainCounts: Map<String, Int>,
    hasImage: Boolean,
) {
    val nameColor = if (hasImage) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryColor = if (hasImage) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
    val chipBg = if (hasImage) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest
    val chipFg = if (hasImage) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (hasImage) {
                    Modifier.background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.7f),
                        ),
                    )
                } else Modifier,
            ),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = nameColor,
            )
            if (sensorText != null) {
                Text(
                    text = sensorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryColor,
                )
            }
            if (domainCounts.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (domain in DOMAIN_CHIP_ORDER) {
                        val count = domainCounts[domain] ?: continue
                        DomainChip(domain = domain, count = count, background = chipBg, foreground = chipFg)
                    }
                }
            }
        }
    }
}

@Composable
private fun DomainChip(domain: String, count: Int, background: Color, foreground: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MdiIcon(
            icon = mdiIconByName(domainChipIconName(domain)),
            tint = foreground,
            size = 14.dp,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = foreground,
        )
    }
}

private fun domainChipIconName(domain: String): String = when (domain) {
    "light" -> "lightbulb"
    "switch" -> "toggle-switch"
    "fan" -> "fan"
    "cover" -> "window-shutter-open"
    "media_player" -> "cast"
    "climate" -> "thermostat"
    else -> "circle-small"
}

private fun stringList(obj: JsonObject?, key: String): List<String>? {
    val arr = obj?.get(key) as? JsonArray ?: return null
    return arr.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
}

private fun boolField(obj: JsonObject?, key: String, default: Boolean): Boolean {
    val element = obj?.get(key) as? JsonPrimitive ?: return default
    return when (element.content.lowercase()) {
        "true" -> true
        "false" -> false
        else -> default
    }
}
