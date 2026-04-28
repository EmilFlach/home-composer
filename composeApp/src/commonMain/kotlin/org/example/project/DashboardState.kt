package org.example.project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DashboardState {

    // ── Living Room ──────────────────────────────────────────
    var livingRoomLightOn       by mutableStateOf(true)
    var livingRoomBrightness    by mutableStateOf(0.75f)
    var livingRoomColorTemp     by mutableStateOf(0.2f)
    var livingRoomShutter       by mutableStateOf(0.35f)
    var livingRoomPresence      by mutableStateOf(PresenceState.Home)
    var livingRoomClimateMode   by mutableStateOf(ClimateMode.Auto)
    var livingRoomCurrentTemp   by mutableStateOf(21.5f)
    var livingRoomTargetTemp    by mutableStateOf(22.0f)
    var livingRoomMediaPlaying  by mutableStateOf(false)
    var livingRoomTrack         by mutableStateOf<String?>(null)

    // ── Kitchen ──────────────────────────────────────────────
    var kitchenLightOn          by mutableStateOf(true)
    var kitchenBrightness       by mutableStateOf(1.0f)
    var kitchenColorTemp        by mutableStateOf(0.8f)
    var kitchenFanOn            by mutableStateOf(true)
    var kitchenDishwasherOn     by mutableStateOf(false)
    var kitchenCurrentTemp      by mutableStateOf(23.4f)

    // ── Dining Room ──────────────────────────────────────────
    var diningLightOn           by mutableStateOf(false)
    var diningBrightness        by mutableStateOf(0.5f)
    var diningColorTemp         by mutableStateOf(0.3f)
    var diningShutter           by mutableStateOf(0.0f)
    var diningCurrentTemp       by mutableStateOf(22.2f)

    // ── Bedroom ──────────────────────────────────────────────
    var bedroomLightOn          by mutableStateOf(false)
    var bedroomBrightness       by mutableStateOf(0.4f)
    var bedroomColorTemp        by mutableStateOf(0.1f)
    var bedroomShutter          by mutableStateOf(1.0f)
    var bedroomPresence         by mutableStateOf(PresenceState.Away)
    var bedroomCurrentTemp      by mutableStateOf(19.8f)

    // ── Bathroom ─────────────────────────────────────────────
    var bathroomLightOn         by mutableStateOf(false)
    var bathroomBrightness      by mutableStateOf(0.6f)
    var bathroomColorTemp       by mutableStateOf(0.5f)
    var bathroomHeaterOn        by mutableStateOf(false)
    var bathroomCurrentTemp     by mutableStateOf(20.1f)
    var bathroomHumidity        by mutableStateOf(62f)

    // ── Office ───────────────────────────────────────────────
    var officeLightOn           by mutableStateOf(true)
    var officeBrightness        by mutableStateOf(0.9f)
    var officeColorTemp         by mutableStateOf(0.7f)
    var officeClimateMode       by mutableStateOf(ClimateMode.Heat)
    var officeCurrentTemp       by mutableStateOf(20.5f)
    var officeTargetTemp        by mutableStateOf(21.0f)
    var officePresence          by mutableStateOf(PresenceState.Home)

    // ── Hallway ──────────────────────────────────────────────
    var hallwayLightOn          by mutableStateOf(false)
    var hallwayBrightness       by mutableStateOf(0.3f)
    var hallwayColorTemp        by mutableStateOf(0.5f)
    var hallwayMotion           by mutableStateOf(false)

    // ── Garden ───────────────────────────────────────────────
    var gardenLightsOn          by mutableStateOf(false)
    var gardenLightsBrightness  by mutableStateOf(0.5f)
    var gardenCurrentTemp       by mutableStateOf(15.3f)
    var gardenMotion            by mutableStateOf(false)

    // ── Sensor history ───────────────────────────────────────
    val temperatureHistory = listOf(
        19.5f, 20.1f, 19.8f, 21.3f, 22.0f, 23.4f,
        22.8f, 21.5f, 20.9f, 21.8f, 22.5f, 23.1f
    )
    val humidityHistory = listOf(
        45f, 47f, 50f, 53f, 55f, 52f,
        48f, 46f, 44f, 43f, 45f, 47f
    )
    val timeLabels = listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "23:59")
}
