package org.example.project.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull
import org.example.project.auth.HaEntityState
import org.example.project.auth.friendlyName

@Composable
internal fun WeatherForecastCard(
    config: LovelaceCardConfig,
    modifier: Modifier = Modifier,
) {
    val entityId = config.entity
    if (entityId == null) {
        WeatherForecastCardStub(config, modifier)
        return
    }

    val entityState = rememberEntityState(entityId)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = LocalCardShape.current,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        if (entityState == null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else {
            WeatherCardContent(config, entityId, entityState)
        }
    }
}

@Composable
private fun WeatherCardContent(
    config: LovelaceCardConfig,
    entityId: String,
    entityState: HaEntityState,
) {
    val attrs = entityState.attributes
    val condition = entityState.state
    val tempUnit = (attrs["temperature_unit"] as? JsonPrimitive)?.content ?: "°C"
    val currentTemp = (attrs["temperature"] as? JsonPrimitive)?.floatOrNull
    val todayForecast = (attrs["forecast"] as? JsonArray)?.getOrNull(0) as? JsonObject
    val high = (todayForecast?.get("temperature") as? JsonPrimitive)?.floatOrNull
    val low = (todayForecast?.get("templow") as? JsonPrimitive)?.floatOrNull
    val avgTemp = if (high != null && low != null) (high + low) / 2f else currentTemp

    val displayName = config.nameText ?: config.title ?: entityState.friendlyName ?: entityId

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = conditionIcon(condition),
                contentDescription = condition,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = conditionLabel(condition),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (avgTemp != null) {
            Text(
                text = "${avgTemp.fmtTemp()}$tempUnit",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun conditionLabel(condition: String): String = when (condition) {
    "clear-night" -> "Clear night"
    "partlycloudy" -> "Partly cloudy"
    "snowy-rainy" -> "Snowy rainy"
    "lightning-rainy" -> "Lightning rainy"
    "windy-variant" -> "Windy"
    else -> condition.replace("-", " ").replaceFirstChar { it.uppercase() }
}

private fun conditionIcon(condition: String): ImageVector = when (condition) {
    "sunny", "exceptional" -> Icons.Filled.WbSunny
    "clear-night" -> Icons.Filled.NightsStay
    "rainy", "pouring" -> Icons.Filled.WaterDrop
    "snowy", "snowy-rainy", "hail" -> Icons.Filled.AcUnit
    "windy", "windy-variant" -> Icons.Filled.Air
    "lightning", "lightning-rainy" -> Icons.Filled.Bolt
    else -> Icons.Filled.Cloud
}

private fun Float.fmtTemp(): String = kotlin.math.round(this).toInt().toString()
