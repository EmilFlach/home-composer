package org.example.project

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.example.project.auth.HaEntityState
import org.example.project.cards.LovelaceCardConfig

internal fun showcaseCardConfig(type: String, entityId: String) = LovelaceCardConfig(
    type = type, title = null, nameConfig = null, icon = null,
    entity = entityId, entities = emptyList(), cardCount = 0, raw = null,
)

internal fun buildShowcaseEntityStates(): Map<String, HaEntityState> {
    val weather = HaEntityState(
        entityId = "weather.home",
        state = "partlycloudy",
        attributes = buildJsonObject {
            put("friendly_name", "Home")
            put("temperature", 21.5)
            put("temperature_unit", "°C")
            put("forecast", buildJsonArray {
                add(buildJsonObject {
                    put("temperature", 24.0)
                    put("templow", 16.0)
                    put("condition", "partlycloudy")
                })
            })
        },
    )
    val temperature = HaEntityState(
        entityId = "sensor.temperature",
        state = "21.8",
        attributes = buildJsonObject {
            put("friendly_name", "Living Room")
            put("unit_of_measurement", "°C")
        },
    )
    val humidity = HaEntityState(
        entityId = "sensor.humidity",
        state = "65",
        attributes = buildJsonObject {
            put("friendly_name", "Humidity")
            put("unit_of_measurement", "%")
        },
    )
    return mapOf(
        weather.entityId to weather,
        temperature.entityId to temperature,
        humidity.entityId to humidity,
    )
}
