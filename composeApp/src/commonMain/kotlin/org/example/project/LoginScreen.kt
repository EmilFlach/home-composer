package org.example.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.example.project.auth.HaEntityState
import org.example.project.auth.LoginEvent
import org.example.project.auth.LoginUiState
import org.example.project.cards.LovelaceCardConfig
import org.example.project.cards.SensorCard
import org.example.project.cards.WeatherForecastCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    state: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
) {
    var showConnectSheet by remember { mutableStateOf(false) }
    val showcase = remember { buildShowcaseEntityStates() }

    Box(modifier = Modifier.fillMaxSize().safeContentPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 100.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "home composer",
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Your smart home, beautifully connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { ShowcaseLabel("Preview") }

            item {
                WeatherForecastCard(
                    config = showcaseCardConfig("weather-forecast", "weather.home"),
                    entityStates = showcase,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SensorCard(
                        config = showcaseCardConfig("sensor", "sensor.temperature"),
                        entityStates = showcase,
                        modifier = Modifier.weight(1f),
                    )
                    SensorCard(
                        config = showcaseCardConfig("sensor", "sensor.humidity"),
                        entityStates = showcase,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                GraphCard(
                    title = "Temperature",
                    unit = "°C",
                    currentValue = 21.8f,
                    dataPoints = listOf(17.9f, 17.4f, 18.2f, 20.5f, 22.9f, 23.4f, 22.8f, 21.8f),
                    timeLabels = listOf("00:00", "06:00", "12:00", "18:00"),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                GraphCard(
                    title = "Humidity",
                    unit = "%",
                    currentValue = 65f,
                    dataPoints = listOf(71f, 69f, 66f, 60f, 54f, 57f, 62f, 65f),
                    timeLabels = listOf("00:00", "06:00", "12:00", "18:00"),
                    color = NeonColors.NeonCyan,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Button(
            onClick = { showConnectSheet = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            Text("Connect to Home Assistant", style = MaterialTheme.typography.labelLarge)
        }
    }

    if (showConnectSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConnectSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            ConnectForm(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun ConnectForm(state: LoginUiState, onEvent: (LoginEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Connect to Home Assistant",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = "Generate a Long-Lived Access Token in your Home Assistant profile and paste it below.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = { onEvent(LoginEvent.UrlChanged(it)) },
            label = { Text("Server URL") },
            placeholder = { Text("http://homeassistant.local:8123") },
            singleLine = true,
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.token,
            onValueChange = { onEvent(LoginEvent.TokenChanged(it)) },
            label = { Text("Access Token") },
            singleLine = true,
            enabled = !state.isSubmitting,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Button(
            onClick = { onEvent(LoginEvent.Submit) },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Connect")
            }
        }
    }
}

@Composable
private fun ShowcaseLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

private fun showcaseCardConfig(type: String, entityId: String) = LovelaceCardConfig(
    type = type, title = null, name = null, icon = null,
    entity = entityId, entities = emptyList(), cardCount = 0, raw = null,
)

private fun buildShowcaseEntityStates(): Map<String, HaEntityState> {
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
