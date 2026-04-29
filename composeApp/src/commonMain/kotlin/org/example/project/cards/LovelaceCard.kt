package org.example.project.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.example.project.auth.HaEntityState


@Composable
fun LovelaceCard(
    card: JsonElement,
    entityStates: Map<String, HaEntityState>,
    modifier: Modifier = Modifier,
) {
    val config = card.toCardConfig()
    when (config.type) {
        "sensor" -> SensorCard(config, entityStates, modifier)

        "alarm-panel" -> AlarmPanelCardStub(config, modifier)
        "area" -> AreaCardStub(config, modifier)
        "button" -> ButtonCardStub(config, modifier)
        "conditional" -> ConditionalCardStub(config, modifier)
        "entities" -> EntitiesCardStub(config, modifier)
        "entity" -> EntityCardStub(config, modifier)
        "gauge" -> GaugeCardStub(config, modifier)
        "glance" -> GlanceCardStub(config, modifier)
        "grid" -> GridCardStub(config, modifier)
        "heading" -> HeadingCard(config, entityStates, modifier)
        "history-graph" -> HistoryGraphCardStub(config, modifier)
        "horizontal-stack" -> HorizontalStackCardStub(config, modifier)
        "iframe" -> IframeCardStub(config, modifier)
        "light" -> LightCardStub(config, modifier)
        "logbook" -> LogbookCardStub(config, modifier)
        "map" -> MapCardStub(config, modifier)
        "markdown" -> MarkdownCardStub(config, modifier)
        "media-control" -> MediaControlCard(config, entityStates, modifier)
        "picture" -> PictureCard(config, entityStates, modifier)
        "picture-elements" -> PictureElementsCard(config, entityStates, modifier)
        "picture-entity" -> PictureEntityCard(config, entityStates, modifier)
        "picture-glance" -> PictureGlanceCardStub(config, modifier)
        "plant-status" -> PlantStatusCardStub(config, modifier)
        "shopping-list" -> ShoppingListCardStub(config, modifier)
        "statistic" -> StatisticCardStub(config, modifier)
        "statistics-graph" -> StatisticsGraphCardStub(config, modifier)
        "thermostat" -> ThermostatCardStub(config, modifier)
        "tile" -> TileCard(config, entityStates, modifier)
        "todo-list" -> TodoListCardStub(config, modifier)
        "vertical-stack" -> VerticalStackCardStub(config, modifier)
        "weather-forecast" -> WeatherForecastCard(config, entityStates, modifier)

        else -> UnknownCardStub(config, modifier)
    }
}

internal data class LovelaceCardConfig(
    val type: String?,
    val title: String?,
    val name: String?,
    val icon: String?,
    val entity: String?,
    val entities: List<String>,
    val cardCount: Int,
    val raw: JsonObject?,
)

internal fun JsonElement.toCardConfig(): LovelaceCardConfig {
    val obj = this as? JsonObject
        ?: return LovelaceCardConfig(null, null, null, null, null, emptyList(), 0, null)
    val type = (obj["type"] as? JsonPrimitive)?.contentOrNull
    val title = (obj["title"] as? JsonPrimitive)?.contentOrNull
    val name = (obj["name"] as? JsonPrimitive)?.contentOrNull
    val icon = (obj["icon"] as? JsonPrimitive)?.contentOrNull
    val entity = (obj["entity"] as? JsonPrimitive)?.contentOrNull
    val entities = (obj["entities"] as? JsonArray)?.mapNotNull { e ->
        when (e) {
            is JsonPrimitive -> e.contentOrNull
            is JsonObject -> (e["entity"] as? JsonPrimitive)?.contentOrNull
            else -> null
        }
    }.orEmpty()
    val nested = (obj["cards"] as? JsonArray)?.size ?: 0
    return LovelaceCardConfig(
        type = type,
        title = title,
        name = name,
        icon = icon,
        entity = entity,
        entities = entities,
        cardCount = nested,
        raw = obj,
    )
}

@Composable
internal fun StubScaffold(
    config: LovelaceCardConfig,
    modifier: Modifier = Modifier,
    body: @Composable (Modifier) -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TypePill(text = config.type ?: "card")
                val heading = config.title ?: config.name
                if (heading != null) {
                    Text(
                        text = heading,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Box(modifier = Modifier.weight(1f))
                }
                Text(
                    text = "stub",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            body(Modifier)
        }
    }
}

@Composable
internal fun TypePill(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
internal fun StubLine(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
