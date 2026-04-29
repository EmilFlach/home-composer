package org.example.project.icons

import org.example.project.auth.HaEntityState
import org.example.project.auth.attributeString
import org.example.project.auth.domain
import org.example.project.auth.isActive

/**
 * Resolve the default HA display icon for an entity, matching the HA frontend's
 * computeStateIcon() logic. Used as the fallback when no explicit icon override is set.
 */
internal fun haEntityIcon(state: HaEntityState?, entityId: String?): HaIcon {
    val domain = state?.domain ?: entityId?.substringBefore('.', missingDelimiterValue = "") ?: ""
    val deviceClass = state?.attributeString("device_class")
    val entityIdStr = state?.entityId ?: entityId ?: ""
    val isActive = state?.isActive() ?: false

    val name: String = when (domain) {
        "binary_sensor" -> binarySensorIconName(deviceClass, isActive)
        "light" -> if (isActive) "lightbulb" else "lightbulb-outline"
        "switch" -> if (isActive) "toggle-switch" else "toggle-switch-off-outline"
        "input_boolean" -> if (isActive) "toggle-switch" else "toggle-switch-off-outline"
        "fan" -> if (isActive) "fan" else "fan-off"
        "cover" -> coverIconName(deviceClass, isActive)
        "lock" -> if (isActive) "lock-open" else "lock"
        "climate", "water_heater" -> "thermostat"
        "media_player" -> if (isActive) "cast" else "cast-off"
        "sensor" -> sensorIconName(deviceClass, entityIdStr)
        "person", "device_tracker" -> if (isActive) "account" else "account-outline"
        "automation" -> if (isActive) "robot" else "robot-off"
        "script" -> "script-text"
        "weather" -> "weather-cloudy"
        "sun" -> if (isActive) "white-balance-sunny" else "weather-night"
        "alarm_control_panel" -> alarmIconName(state?.state)
        "humidifier" -> if (isActive) "air-humidifier" else "air-humidifier-off"
        "siren" -> if (isActive) "alarm-light" else "alarm-light-off-outline"
        "vacuum" -> "robot-vacuum"
        "input_button", "button" -> "gesture-tap-button"
        "zone" -> "map-marker-radius"
        "group" -> "google-circles-communities"
        else -> "help-circle-outline"
    }
    return mdiIconByName(name)
}

private fun binarySensorIconName(deviceClass: String?, isActive: Boolean): String = when (deviceClass) {
    "battery" -> if (isActive) "battery" else "battery-outline"
    "battery_charging" -> if (isActive) "battery-charging" else "battery-outline"
    "co" -> if (isActive) "molecule-co" else "check-circle"
    "cold" -> if (isActive) "thermometer-snowflake" else "thermometer"
    "connectivity" -> if (isActive) "lan-connect" else "lan-disconnect"
    "door" -> if (isActive) "door-open" else "door-closed"
    "garage_door" -> if (isActive) "garage-open" else "garage"
    "gas" -> if (isActive) "smoke-detector-variant-alert" else "smoke-detector-variant"
    "heat" -> if (isActive) "fire" else "fire-off"
    "light" -> if (isActive) "brightness-7" else "brightness-5"
    "lock" -> if (isActive) "lock-open" else "lock"
    "moisture" -> if (isActive) "water" else "water-off"
    "motion" -> if (isActive) "motion-sensor" else "motion-sensor-off"
    "moving" -> if (isActive) "motion" else "motion-sensor-off"
    "occupancy" -> if (isActive) "home" else "home-outline"
    "opening" -> if (isActive) "square-rounded-outline" else "minus-box"
    "plug" -> if (isActive) "power-plug" else "power-plug-off"
    "power" -> if (isActive) "lightning-bolt" else "lightning-bolt-outline"
    "presence" -> if (isActive) "home" else "home-outline"
    "problem" -> if (isActive) "alert-circle" else "check-circle"
    "running" -> if (isActive) "play" else "stop"
    "safety" -> if (isActive) "shield-off" else "shield"
    "smoke" -> if (isActive) "smoke-detector-variant-alert" else "smoke-detector-variant"
    "sound" -> if (isActive) "music-note" else "music-note-off"
    "tamper" -> if (isActive) "bell-ring" else "bell-ring-outline"
    "update" -> if (isActive) "package-up" else "package"
    "vibration" -> if (isActive) "vibrate" else "vibrate-off"
    "window" -> if (isActive) "window-open" else "window-closed"
    else -> if (isActive) "radiobox-marked" else "radiobox-blank"
}

private fun coverIconName(deviceClass: String?, isActive: Boolean): String = when (deviceClass) {
    "blind" -> if (isActive) "blinds-open" else "blinds"
    "curtain" -> if (isActive) "curtains" else "curtains-closed"
    "door" -> if (isActive) "door-open" else "door-closed"
    "garage" -> if (isActive) "garage-open" else "garage"
    "gate" -> if (isActive) "gate-open" else "gate"
    "shade" -> if (isActive) "roller-shade" else "roller-shade-closed"
    "window" -> if (isActive) "window-open" else "window-closed"
    else -> if (isActive) "window-shutter-open" else "window-shutter"
}

private fun sensorIconName(deviceClass: String?, entityId: String): String = when (deviceClass) {
    "temperature" -> "thermometer"
    "humidity" -> "water-percent"
    "pressure" -> "gauge"
    "illuminance" -> "brightness-5"
    "power" -> "lightning-bolt"
    "energy" -> "lightning-bolt"
    "voltage" -> "sine-wave"
    "current" -> "current-ac"
    "battery" -> "battery"
    "signal_strength" -> "wifi"
    "carbon_dioxide" -> "molecule-co2"
    "carbon_monoxide" -> "molecule-co"
    "pm25" -> "air-filter"
    "pm10" -> "air-filter"
    "volatile_organic_compounds" -> "air-filter"
    "gas" -> "meter-gas"
    "water" -> "water"
    "monetary" -> "cash"
    "distance" -> "ruler"
    "speed" -> "speedometer"
    "wind_speed" -> "weather-windy"
    else -> when {
        "sunrise" in entityId || "dawn" in entityId || "next_rising" in entityId -> "weather-sunset-up"
        "sunset" in entityId || "dusk" in entityId || "next_setting" in entityId -> "weather-sunset-down"
        else -> "eye"
    }
}

private fun alarmIconName(state: String?): String = when (state?.lowercase()) {
    "armed_away" -> "shield-lock"
    "armed_home" -> "shield-home"
    "armed_night" -> "shield-moon"
    "armed_vacation" -> "shield-airplane"
    "armed_custom_bypass" -> "security"
    "triggered" -> "bell-ring"
    "disarmed" -> "shield-off"
    else -> "shield-home"
}
