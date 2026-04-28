package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.example.project.auth.ConnectionStatus
import org.example.project.auth.HomeAssistantClient
import org.example.project.auth.HomeAssistantConfig
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Blinds
import org.example.project.auth.HomeAssistantWebSocketClient

private const val CONNECTION_POLL_INTERVAL_MS = 30_000L

@Composable
fun DashboardScreen(
    config: HomeAssistantConfig,
    client: HomeAssistantClient,
    wsClient: HomeAssistantWebSocketClient,
    darkTheme: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    onLogout: () -> Unit = {},
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
    LaunchedEffect(wsClient, config) {
        runCatching { wsClient.connect(config) }
    }
    val frameCount by wsClient.frameCount.collectAsStateWithLifecycle()
    val latestFrame by wsClient.latestFrame.collectAsStateWithLifecycle()
    val dashboards by wsClient.dashboards.collectAsStateWithLifecycle()
    val dashboardConfigs by wsClient.dashboardConfigs.collectAsStateWithLifecycle()
    val dashboardErrors by wsClient.dashboardErrors.collectAsStateWithLifecycle()
    val entityStates by wsClient.entityStates.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize()) {
        LovelaceDashboardList(
            dashboards = dashboards,
            configs = dashboardConfigs,
            errors = dashboardErrors,
            entityStates = entityStates,
            darkTheme = darkTheme,
            onToggleDarkMode = onToggleDarkMode,
            onLogout = onLogout,
            modifier = Modifier.weight(1f),
        )
        ConnectionStatusBar(
            status = connectionStatus,
            baseUrl = config.baseUrl,
            frameCount = frameCount,
            latestFrame = latestFrame,
        )
    }
}

@Composable
private fun DashboardContent(
    modifier: Modifier = Modifier,
    darkTheme: Boolean,
    onToggleDarkMode: () -> Unit,
) {
    val state = remember { DashboardState() }
    var selectedEntity by remember { mutableStateOf<Pair<HomeEntity, HomeRoom>?>(null) }

    val rooms = rememberRooms(state)

    val bgModifier = if (darkTheme)
        Modifier.background(NeonGradients.ScreenBackground)
    else
        Modifier.background(MaterialTheme.colorScheme.background)

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .then(bgModifier),
        verticalArrangement = Arrangement.spacedBy(40.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(rooms, key = { it.id }) { room ->
            RoomCard(
                room = room,
                onEntityClick = { entity -> selectedEntity = entity to room },
                onBrightnessChange = { brightness ->
                    when (room.id) {
                        "living_room" -> state.livingRoomBrightness = brightness
                        "kitchen"     -> state.kitchenBrightness = brightness
                        "dining_room" -> state.diningBrightness = brightness
                        "bedroom"     -> state.bedroomBrightness = brightness
                        "bathroom"    -> state.bathroomBrightness = brightness
                        "office"      -> state.officeBrightness = brightness
                        "hallway"     -> state.hallwayBrightness = brightness
                        "garden"      -> state.gardenLightsBrightness = brightness
                    }
                },
                onLightSliderTap = {
                    room.entities
                        .firstOrNull { it.state is EntityState.Light }
                        ?.let { selectedEntity = it to room }
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
        }

        item {
            SectionLabel("Sensors", modifier = Modifier.padding(horizontal = 16.dp))
            GraphCard(
                title = "Temperature",
                unit = "°C",
                currentValue = state.temperatureHistory.last(),
                dataPoints = state.temperatureHistory,
                timeLabels = state.timeLabels,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
        }
        item {
            GraphCard(
                title = "Humidity",
                unit = "%",
                currentValue = state.humidityHistory.last(),
                dataPoints = state.humidityHistory,
                timeLabels = state.timeLabels,
                color = NeonColors.NeonCyan,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
        }
    }

    selectedEntity?.let { (entity, room) ->
        EntityDetailSheet(
            entity = entity,
            room = room,
            state = state,
            onDismiss = { selectedEntity = null }
        )
    }
}

@Composable
private fun rememberRooms(state: DashboardState): List<HomeRoom> = remember(
    state.livingRoomLightOn, state.livingRoomBrightness, state.livingRoomColorTemp,
    state.livingRoomShutter, state.livingRoomPresence, state.livingRoomClimateMode,
    state.livingRoomCurrentTemp, state.livingRoomMediaPlaying,
    state.kitchenLightOn, state.kitchenBrightness, state.kitchenFanOn,
    state.kitchenDishwasherOn, state.kitchenCurrentTemp,
    state.diningLightOn, state.diningBrightness, state.diningShutter, state.diningCurrentTemp,
    state.bedroomLightOn, state.bedroomBrightness, state.bedroomShutter,
    state.bedroomPresence, state.bedroomCurrentTemp,
    state.bathroomLightOn, state.bathroomBrightness, state.bathroomHeaterOn,
    state.bathroomCurrentTemp, state.bathroomHumidity,
    state.officeLightOn, state.officeBrightness, state.officeClimateMode,
    state.officePresence, state.officeCurrentTemp,
    state.hallwayLightOn, state.hallwayBrightness, state.hallwayMotion,
    state.gardenLightsOn, state.gardenLightsBrightness, state.gardenCurrentTemp, state.gardenMotion
) {
    listOf(
        HomeRoom(
            id = "living_room",
            name = "Living Room",
            icon = Icons.Filled.Weekend,
            entities = listOf(
                HomeEntity("lr_light", "Light", Icons.Filled.Lightbulb,
                    EntityState.Light(state.livingRoomLightOn, state.livingRoomBrightness, state.livingRoomColorTemp)),
                HomeEntity("lr_shutter", "Blinds", Icons.Filled.Blinds,
                    EntityState.Shutter(state.livingRoomShutter)),
                HomeEntity("lr_presence", "Presence", Icons.Filled.Person,
                    EntityState.Presence(state.livingRoomPresence)),
                HomeEntity("lr_climate", "Climate", Icons.Filled.Thermostat,
                    EntityState.Climate(state.livingRoomClimateMode, state.livingRoomCurrentTemp, state.livingRoomTargetTemp)),
                HomeEntity("lr_media", "Media", Icons.Filled.PlayArrow,
                    EntityState.MediaPlayer(state.livingRoomMediaPlaying, state.livingRoomTrack)),
            )
        ),
        HomeRoom(
            id = "kitchen",
            name = "Kitchen",
            icon = Icons.Filled.Kitchen,
            entities = listOf(
                HomeEntity("ki_light", "Light", Icons.Filled.Lightbulb,
                    EntityState.Light(state.kitchenLightOn, state.kitchenBrightness, state.kitchenColorTemp)),
                HomeEntity("ki_fan", "Fan", Icons.Filled.Air,
                    EntityState.Switch(state.kitchenFanOn)),
                HomeEntity("ki_dishwasher", "Dishwasher", Icons.Filled.DinnerDining,
                    EntityState.Switch(state.kitchenDishwasherOn)),
                HomeEntity("ki_temp", "Temp", Icons.Filled.Thermostat,
                    EntityState.Sensor(state.kitchenCurrentTemp, "°C")),
            )
        ),
        HomeRoom(
            id = "dining_room",
            name = "Dining Room",
            icon = Icons.Filled.DinnerDining,
            entities = listOf(
                HomeEntity("dr_light", "Light", Icons.Filled.Lightbulb,
                    EntityState.Light(state.diningLightOn, state.diningBrightness, state.diningColorTemp)),
                HomeEntity("dr_shutter", "Blinds", Icons.Filled.Blinds,
                    EntityState.Shutter(state.diningShutter)),
                HomeEntity("dr_temp", "Temp", Icons.Filled.Thermostat,
                    EntityState.Sensor(state.diningCurrentTemp, "°C")),
            )
        ),
        HomeRoom(
            id = "bedroom",
            name = "Bedroom",
            icon = Icons.Filled.Bed,
            entities = listOf(
                HomeEntity("be_light", "Light", Icons.Filled.Lightbulb,
                    EntityState.Light(state.bedroomLightOn, state.bedroomBrightness, state.bedroomColorTemp)),
                HomeEntity("be_shutter", "Blinds", Icons.Filled.Blinds,
                    EntityState.Shutter(state.bedroomShutter)),
                HomeEntity("be_presence", "Presence", Icons.Filled.Person,
                    EntityState.Presence(state.bedroomPresence)),
                HomeEntity("be_temp", "Temp", Icons.Filled.Thermostat,
                    EntityState.Sensor(state.bedroomCurrentTemp, "°C")),
            )
        ),
        HomeRoom(
            id = "bathroom",
            name = "Bathroom",
            icon = Icons.Filled.Bathtub,
            entities = listOf(
                HomeEntity("ba_light", "Light", Icons.Filled.Lightbulb,
                    EntityState.Light(state.bathroomLightOn, state.bathroomBrightness, state.bathroomColorTemp)),
                HomeEntity("ba_heater", "Heater", Icons.Filled.Whatshot,
                    EntityState.Switch(state.bathroomHeaterOn)),
                HomeEntity("ba_temp", "Temp", Icons.Filled.Thermostat,
                    EntityState.Sensor(state.bathroomCurrentTemp, "°C")),
                HomeEntity("ba_humidity", "Humidity", Icons.Filled.WaterDrop,
                    EntityState.Sensor(state.bathroomHumidity, "%")),
            )
        ),
        HomeRoom(
            id = "office",
            name = "Office",
            icon = Icons.Filled.Computer,
            entities = listOf(
                HomeEntity("of_light", "Light", Icons.Filled.Lightbulb,
                    EntityState.Light(state.officeLightOn, state.officeBrightness, state.officeColorTemp)),
                HomeEntity("of_climate", "Climate", Icons.Filled.Thermostat,
                    EntityState.Climate(state.officeClimateMode, state.officeCurrentTemp, state.officeTargetTemp)),
                HomeEntity("of_presence", "Presence", Icons.Filled.Person,
                    EntityState.Presence(state.officePresence)),
            )
        ),
        HomeRoom(
            id = "hallway",
            name = "Hallway",
            icon = Icons.Filled.MeetingRoom,
            entities = listOf(
                HomeEntity("ha_light", "Light", Icons.Filled.Lightbulb,
                    EntityState.Light(state.hallwayLightOn, state.hallwayBrightness, state.hallwayColorTemp)),
                HomeEntity("ha_motion", "Motion", Icons.Filled.MotionPhotosOn,
                    EntityState.Switch(state.hallwayMotion)),
            )
        ),
        HomeRoom(
            id = "garden",
            name = "Garden",
            icon = Icons.Filled.Yard,
            entities = listOf(
                HomeEntity("ga_lights", "Lights", Icons.Filled.Lightbulb,
                    EntityState.Light(state.gardenLightsOn, state.gardenLightsBrightness, 0.5f)),
                HomeEntity("ga_temp", "Temp", Icons.Filled.Thermostat,
                    EntityState.Sensor(state.gardenCurrentTemp, "°C")),
                HomeEntity("ga_motion", "Motion", Icons.Filled.MotionPhotosOn,
                    EntityState.Switch(state.gardenMotion)),
            )
        ),
    )
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = NeonColors.NeonCyan,
        letterSpacing = 2.sp,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}
