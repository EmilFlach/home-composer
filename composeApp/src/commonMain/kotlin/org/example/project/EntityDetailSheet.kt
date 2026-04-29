package org.example.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityDetailSheet(
    entity: HomeEntity,
    room: HomeRoom,
    state: DashboardState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet header
            Column {
                Text(
                    entity.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    room.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when (val id = entity.id) {
                // ── Lights ───────────────────────────────────────────────────
                "lr_light" -> LightCardContent(
                    name = "Light",
                    isOn = state.livingRoomLightOn,
                    brightness = state.livingRoomBrightness,
                    colorTemperature = state.livingRoomColorTemp,
                    onToggle = { state.livingRoomLightOn = it },
                    onBrightnessChange = { state.livingRoomBrightness = it },
                    onColorTemperatureChange = { state.livingRoomColorTemp = it }
                )
                "ki_light" -> LightCardContent(
                    name = "Light",
                    isOn = state.kitchenLightOn,
                    brightness = state.kitchenBrightness,
                    colorTemperature = state.kitchenColorTemp,
                    onToggle = { state.kitchenLightOn = it },
                    onBrightnessChange = { state.kitchenBrightness = it },
                    onColorTemperatureChange = { state.kitchenColorTemp = it }
                )
                "dr_light" -> LightCardContent(
                    name = "Light",
                    isOn = state.diningLightOn,
                    brightness = state.diningBrightness,
                    colorTemperature = state.diningColorTemp,
                    onToggle = { state.diningLightOn = it },
                    onBrightnessChange = { state.diningBrightness = it },
                    onColorTemperatureChange = { state.diningColorTemp = it }
                )
                "be_light" -> LightCardContent(
                    name = "Light",
                    isOn = state.bedroomLightOn,
                    brightness = state.bedroomBrightness,
                    colorTemperature = state.bedroomColorTemp,
                    onToggle = { state.bedroomLightOn = it },
                    onBrightnessChange = { state.bedroomBrightness = it },
                    onColorTemperatureChange = { state.bedroomColorTemp = it }
                )
                "ba_light" -> LightCardContent(
                    name = "Light",
                    isOn = state.bathroomLightOn,
                    brightness = state.bathroomBrightness,
                    colorTemperature = state.bathroomColorTemp,
                    onToggle = { state.bathroomLightOn = it },
                    onBrightnessChange = { state.bathroomBrightness = it },
                    onColorTemperatureChange = { state.bathroomColorTemp = it }
                )
                "of_light" -> LightCardContent(
                    name = "Light",
                    isOn = state.officeLightOn,
                    brightness = state.officeBrightness,
                    colorTemperature = state.officeColorTemp,
                    onToggle = { state.officeLightOn = it },
                    onBrightnessChange = { state.officeBrightness = it },
                    onColorTemperatureChange = { state.officeColorTemp = it }
                )
                "ha_light" -> LightCardContent(
                    name = "Light",
                    isOn = state.hallwayLightOn,
                    brightness = state.hallwayBrightness,
                    colorTemperature = state.hallwayColorTemp,
                    onToggle = { state.hallwayLightOn = it },
                    onBrightnessChange = { state.hallwayBrightness = it },
                    onColorTemperatureChange = { state.hallwayColorTemp = it }
                )
                "ga_lights" -> LightCardContent(
                    name = "Lights",
                    isOn = state.gardenLightsOn,
                    brightness = state.gardenLightsBrightness,
                    colorTemperature = 0.5f,
                    onToggle = { state.gardenLightsOn = it },
                    onBrightnessChange = { state.gardenLightsBrightness = it },
                    onColorTemperatureChange = {}
                )

                // ── Shutters ─────────────────────────────────────────────────
                "lr_shutter" -> ShutterCardContent(
                    name = "Blinds",
                    position = state.livingRoomShutter,
                    onOpen = { state.livingRoomShutter = 0f },
                    onStop = {},
                    onClose = { state.livingRoomShutter = 1f }
                )
                "dr_shutter" -> ShutterCardContent(
                    name = "Blinds",
                    position = state.diningShutter,
                    onOpen = { state.diningShutter = 0f },
                    onStop = {},
                    onClose = { state.diningShutter = 1f }
                )
                "be_shutter" -> ShutterCardContent(
                    name = "Blinds",
                    position = state.bedroomShutter,
                    onOpen = { state.bedroomShutter = 0f },
                    onStop = {},
                    onClose = { state.bedroomShutter = 1f }
                )

                // ── Switches ─────────────────────────────────────────────────
                "ki_fan", "ki_dishwasher", "ba_heater", "ha_motion", "ga_motion" -> {
                    val (isOn, setOn) = when (id) {
                        "ki_fan"       -> state.kitchenFanOn       to { v: Boolean -> state.kitchenFanOn = v }
                        "ki_dishwasher"-> state.kitchenDishwasherOn to { v: Boolean -> state.kitchenDishwasherOn = v }
                        "ba_heater"    -> state.bathroomHeaterOn    to { v: Boolean -> state.bathroomHeaterOn = v }
                        "ha_motion"    -> state.hallwayMotion        to { v: Boolean -> state.hallwayMotion = v }
                        else           -> state.gardenMotion         to { v: Boolean -> state.gardenMotion = v }
                    }
                    SwitchControl(
                        name = entity.name,
                        isOn = isOn,
                        onToggle = setOn
                    )
                }

                // ── Media ────────────────────────────────────────────────────
                "lr_media" -> MediaControl(
                    isPlaying = state.livingRoomMediaPlaying,
                    trackName = state.livingRoomTrack,
                    onToggle = { state.livingRoomMediaPlaying = it }
                )

                // ── Climate ──────────────────────────────────────────────────
                "lr_climate" -> ClimateControl(
                    mode = state.livingRoomClimateMode,
                    currentTemp = state.livingRoomCurrentTemp,
                    targetTemp = state.livingRoomTargetTemp,
                    onModeChange = { state.livingRoomClimateMode = it },
                    onTargetChange = { state.livingRoomTargetTemp = it }
                )
                "of_climate" -> ClimateControl(
                    mode = state.officeClimateMode,
                    currentTemp = state.officeCurrentTemp,
                    targetTemp = state.officeTargetTemp,
                    onModeChange = { state.officeClimateMode = it },
                    onTargetChange = { state.officeTargetTemp = it }
                )

                // ── Sensors (read-only) ───────────────────────────────────────
                else -> {
                    val sensorVal = when (val s = entity.state) {
                        is EntityState.Sensor -> "${s.value.roundToInt()} ${s.unit}"
                        else -> entity.chipLabel()
                    }
                    Text(
                        sensorVal,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SwitchControl(name: String, isOn: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge)
        val accentColor = MaterialTheme.colorScheme.secondary
        Switch(
            checked = isOn,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.35f),
                checkedBorderColor = accentColor.copy(alpha = 0.55f),
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun MediaControl(isPlaying: Boolean, trackName: String?, onToggle: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (isPlaying) (trackName ?: "Now Playing") else "Not playing",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilledTonalButton(
            onClick = { onToggle(!isPlaying) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(if (isPlaying) "Pause" else "Play", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun ClimateControl(
    mode: ClimateMode,
    currentTemp: Float,
    targetTemp: Float,
    onModeChange: (ClimateMode) -> Unit,
    onTargetChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Current: ${currentTemp.roundToInt()}°C",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ClimateMode.entries.forEach { m ->
                val selected = m == mode
                FilledTonalButton(
                    onClick = { onModeChange(m) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        m.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Target: ${targetTemp.roundToInt()}°C", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { onTargetChange(targetTemp - 0.5f) }) { Text("−") }
                FilledTonalButton(onClick = { onTargetChange(targetTemp + 0.5f) }) { Text("+") }
            }
        }
    }
}
