package org.example.project

import androidx.compose.ui.graphics.vector.ImageVector

sealed interface EntityState {
    data class Light(
        val isOn: Boolean,
        val brightness: Float,
        val colorTemperature: Float
    ) : EntityState

    data class Shutter(val position: Float) : EntityState

    data class Climate(
        val mode: ClimateMode,
        val currentTemp: Float,
        val targetTemp: Float
    ) : EntityState

    data class Presence(val state: PresenceState) : EntityState

    data class MediaPlayer(
        val isPlaying: Boolean,
        val trackName: String?
    ) : EntityState

    data class Switch(val isOn: Boolean) : EntityState

    data class Sensor(val value: Float, val unit: String) : EntityState
}

enum class ClimateMode { Auto, Heat, Cool, Off }
enum class PresenceState { Home, Away }

data class HomeEntity(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val state: EntityState
)

data class HomeRoom(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val entities: List<HomeEntity>
)

fun HomeRoom.temperature(): Float? =
    entities.firstNotNullOfOrNull { e ->
        (e.state as? EntityState.Sensor)?.takeIf { it.unit == "°C" }?.value
    }

fun HomeRoom.activeLightCount(): Int =
    entities.count { (it.state as? EntityState.Light)?.isOn == true }

fun HomeRoom.lightCount(): Int =
    entities.count { it.state is EntityState.Light }

fun HomeEntity.chipLabel(): String = when (val s = state) {
    is EntityState.Light -> if (s.isOn) "${(s.brightness * 100).toInt()}%" else "Off"
    is EntityState.Shutter -> when {
        s.position < 0.05f -> "Open"
        s.position > 0.95f -> "Closed"
        else -> "${(s.position * 100).toInt()}%"
    }
    is EntityState.Climate -> "${s.mode.name} · ${fmtSensor(s.currentTemp)}°C"
    is EntityState.Presence -> s.state.name
    is EntityState.MediaPlayer -> if (s.isPlaying) s.trackName ?: "Playing" else "Not playing"
    is EntityState.Switch -> if (s.isOn) "On" else "Off"
    is EntityState.Sensor -> "${fmtSensor(s.value)} ${s.unit}"
}

fun HomeEntity.isActive(): Boolean = when (val s = state) {
    is EntityState.Light -> s.isOn
    is EntityState.Switch -> s.isOn
    is EntityState.MediaPlayer -> s.isPlaying
    is EntityState.Climate -> s.mode != ClimateMode.Off
    is EntityState.Presence -> s.state == PresenceState.Home
    else -> false
}

private fun fmtSensor(v: Float): String {
    val i = (v * 10).toInt()
    return "${i / 10}.${i % 10}"
}
