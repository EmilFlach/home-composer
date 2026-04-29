package org.example.project.auth

import kotlin.math.roundToInt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class HaEntityState(
    @SerialName("entity_id") val entityId: String,
    val state: String,
    val attributes: JsonObject = JsonObject(emptyMap()),
    @SerialName("last_changed") val lastChanged: String? = null,
    @SerialName("last_updated") val lastUpdated: String? = null,
)

val HaEntityState.domain: String
    get() = entityId.substringBefore('.', missingDelimiterValue = "")

val HaEntityState.friendlyName: String?
    get() = (attributes["friendly_name"] as? JsonPrimitive)?.contentOrNull

val HaEntityState.unitOfMeasurement: String?
    get() = (attributes["unit_of_measurement"] as? JsonPrimitive)?.contentOrNull

val HaEntityState.icon: String?
    get() = (attributes["icon"] as? JsonPrimitive)?.contentOrNull

fun HaEntityState.attributeString(name: String): String? =
    attributes[name]?.let { it as? JsonPrimitive }?.contentOrNull

fun HaEntityState.isActive(): Boolean {
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

fun shouldRoundUnit(unit: String?): Boolean {
    if (unit == null) return false
    val trimmed = unit.trim()
    return trimmed == "%" ||
        trimmed.startsWith("°") ||
        trimmed.equals("ppm", ignoreCase = true) ||
        trimmed.startsWith("km", ignoreCase = true)
}

fun formatStateValue(value: String, unit: String?): String {
    if (!shouldRoundUnit(unit)) return value
    val number = value.toFloatOrNull() ?: return value
    return number.roundToInt().toString()
}
