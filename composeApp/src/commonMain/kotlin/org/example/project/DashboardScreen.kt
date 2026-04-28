package org.example.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.example.project.auth.ConnectionStatus
import org.example.project.auth.HomeAssistantClient
import org.example.project.auth.HomeAssistantConfig

private val TWO_COLUMN_THRESHOLD: Dp = 450.dp
private const val CONNECTION_POLL_INTERVAL_MS = 30_000L

@Composable
fun DashboardScreen(
    config: HomeAssistantConfig,
    client: HomeAssistantClient,
    darkTheme: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
) {
    var connectionStatus by remember { mutableStateOf<ConnectionStatus>(ConnectionStatus.Checking) }
    LaunchedEffect(config) {
        while (true) {
            connectionStatus = ConnectionStatus.Checking
            client.verify(config)
                .onSuccess { connectionStatus = ConnectionStatus.Connected }
                .onFailure {
                    connectionStatus = ConnectionStatus.Disconnected(it.message ?: "Unknown error")
                }
            delay(CONNECTION_POLL_INTERVAL_MS)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        DashboardContent(
            modifier = Modifier.weight(1f),
            darkTheme = darkTheme,
            onToggleDarkMode = onToggleDarkMode,
        )
        ConnectionStatusBar(status = connectionStatus, baseUrl = config.baseUrl)
    }
}

@Composable
private fun DashboardContent(
    modifier: Modifier = Modifier,
    darkTheme: Boolean,
    onToggleDarkMode: () -> Unit,
) {
    var livingRoomBrightness by remember { mutableStateOf(0.75f) }
    var bedroomBrightness by remember { mutableStateOf(0.4f) }

    var livingRoomShutter by remember { mutableStateOf(0.6f) }
    var bedroomShutter by remember { mutableStateOf(0.0f) }

    var kitchenFan by remember { mutableStateOf(true) }
    var gardenLights by remember { mutableStateOf(false) }
    var heater by remember { mutableStateOf(true) }
    var dishwasher by remember { mutableStateOf(false) }

    val temperatureData = remember {
        listOf(19.5f, 20.1f, 19.8f, 21.3f, 22.0f, 23.4f, 22.8f, 21.5f, 20.9f, 21.8f, 22.5f, 23.1f)
    }
    val humidityData = remember {
        listOf(45f, 47f, 50f, 53f, 55f, 52f, 48f, 46f, 44f, 43f, 45f, 47f)
    }
    val timeLabels = remember {
        listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "23:59")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Home", style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = onToggleDarkMode) {
                    Icon(
                        imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = if (darkTheme) "Switch to light mode" else "Switch to dark mode"
                    )
                }
            }
        }

        item { SectionLabel("Sensors") }

        item {
            GraphCard(
                title = "Temperature",
                unit = "°C",
                currentValue = temperatureData.last(),
                dataPoints = temperatureData,
                timeLabels = timeLabels,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            GraphCard(
                title = "Humidity",
                unit = "%",
                currentValue = humidityData.last(),
                dataPoints = humidityData,
                timeLabels = timeLabels,
                color = Color(0xFF00BCD4),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { SectionLabel("Lights") }

        item {
            TwoColumnAdaptive(
                first = { mod ->
                    BrightnessCard(
                        name = "Living Room",
                        brightness = livingRoomBrightness,
                        onBrightnessChange = { livingRoomBrightness = it },
                        modifier = mod
                    )
                },
                second = { mod ->
                    BrightnessCard(
                        name = "Bedroom",
                        brightness = bedroomBrightness,
                        onBrightnessChange = { bedroomBrightness = it },
                        modifier = mod
                    )
                }
            )
        }

        item { SectionLabel("Shutters") }

        item {
            TwoColumnAdaptive(
                first = { mod ->
                    ShutterCard(
                        name = "Living Room",
                        position = livingRoomShutter,
                        onOpen = { livingRoomShutter = 0f },
                        onStop = { },
                        onClose = { livingRoomShutter = 1f },
                        modifier = mod
                    )
                },
                second = { mod ->
                    ShutterCard(
                        name = "Bedroom",
                        position = bedroomShutter,
                        onOpen = { bedroomShutter = 0f },
                        onStop = { },
                        onClose = { bedroomShutter = 1f },
                        modifier = mod
                    )
                }
            )
        }

        item { SectionLabel("Switches") }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TwoColumnAdaptive(
                    first = { mod ->
                        ToggleCard(
                            name = "Kitchen Fan",
                            icon = Icons.Filled.Air,
                            isOn = kitchenFan,
                            onToggle = { kitchenFan = it },
                            modifier = mod
                        )
                    },
                    second = { mod ->
                        ToggleCard(
                            name = "Garden Lights",
                            icon = Icons.Filled.Yard,
                            isOn = gardenLights,
                            onToggle = { gardenLights = it },
                            modifier = mod
                        )
                    }
                )
                TwoColumnAdaptive(
                    first = { mod ->
                        ToggleCard(
                            name = "Heater",
                            icon = Icons.Filled.Whatshot,
                            isOn = heater,
                            onToggle = { heater = it },
                            modifier = mod
                        )
                    },
                    second = { mod ->
                        ToggleCard(
                            name = "Dishwasher",
                            icon = Icons.Filled.DinnerDining,
                            isOn = dishwasher,
                            onToggle = { dishwasher = it },
                            modifier = mod
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun TwoColumnAdaptive(
    modifier: Modifier = Modifier,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth >= TWO_COLUMN_THRESHOLD) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}
