package org.example.project.auth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.example.project.cards.HaRegistry
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class HomeAssistantWebSocketClient(
    private val httpClient: HttpClient,
    private val cache: DashboardCache? = null,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _serviceCallChannel = Channel<String>(Channel.UNLIMITED)
    private var _serviceCallNextId = 10_000L

    fun callService(
        domain: String,
        service: String,
        target: JsonObject? = null,
        data: JsonObject? = null,
    ) {
        val id = _serviceCallNextId++
        val msg = buildString {
            append("""{"id":$id,"type":"call_service","domain":"$domain","service":"$service"""")
            if (target != null) append(""","target":$target""")
            if (data != null) append(""","service_data":$data""")
            append("}")
        }
        _serviceCallChannel.trySend(msg)
    }

    private val _frameCount = MutableStateFlow(0L)
    val frameCount: StateFlow<Long> = _frameCount.asStateFlow()

    private val _latestFrame = MutableStateFlow<String?>(null)
    val latestFrame: StateFlow<String?> = _latestFrame.asStateFlow()

    private val _dashboards = MutableStateFlow<List<LovelaceDashboard>>(emptyList())
    val dashboards: StateFlow<List<LovelaceDashboard>> = _dashboards.asStateFlow()

    private val _dashboardConfigs = MutableStateFlow<Map<String, LovelaceConfig>>(emptyMap())
    val dashboardConfigs: StateFlow<Map<String, LovelaceConfig>> = _dashboardConfigs.asStateFlow()

    private val _dashboardErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val dashboardErrors: StateFlow<Map<String, String>> = _dashboardErrors.asStateFlow()

    private val _rawEntityStates = MutableStateFlow<Map<String, HaEntityState>>(emptyMap())
    private val _optimisticStates = MutableStateFlow<Map<String, HaEntityState>>(emptyMap())

    private val _entityStatesLoaded = MutableStateFlow(false)
    val entityStatesLoaded: StateFlow<Boolean> = _entityStatesLoaded.asStateFlow()

    // Null until the initial get_states response arrives, ensuring entityStatesLoaded and
    // the entity data are always in sync within the same Compose frame (no "unavailable" flash).
    val entityStates: StateFlow<Map<String, HaEntityState>?> = combine(
        _rawEntityStates, _optimisticStates, _entityStatesLoaded
    ) { raw, optimistic, loaded ->
        if (loaded) raw + optimistic else null
    }.stateIn(clientScope, SharingStarted.Eagerly, null)

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Checking)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    // area_id -> area_name
    private val _areas = MutableStateFlow<Map<String, String>>(emptyMap())
    // entity_id -> area_id
    private val _entityAreaIds = MutableStateFlow<Map<String, String>>(emptyMap())
    // entity_id -> registry name (may be null for unnamed entities)
    private val _entityNames = MutableStateFlow<Map<String, String>>(emptyMap())
    // device_id -> display_name
    private val _deviceNames = MutableStateFlow<Map<String, String>>(emptyMap())
    // device_id -> area_id (devices can have their own area assignment)
    private val _deviceAreaIds = MutableStateFlow<Map<String, String>>(emptyMap())
    // entity_id -> device_id
    private val _entityDeviceIds = MutableStateFlow<Map<String, String>>(emptyMap())
    // floor_id -> floor_name
    private val _floors = MutableStateFlow<Map<String, String>>(emptyMap())
    // area_id -> floor_id
    private val _areaFloorIds = MutableStateFlow<Map<String, String>>(emptyMap())
    // system temperature unit, e.g. "°C"
    private val _temperatureUnit = MutableStateFlow<String?>(null)

    init {
        cache?.loadDashboards()?.let { _dashboards.value = it }
        cache?.loadDashboardConfigs()?.let { _dashboardConfigs.value = it }
        cache?.loadRegistry()?.let { reg ->
            _areas.value = reg.areas
            _entityAreaIds.value = reg.entityAreaIds
            _entityNames.value = reg.entityNames
            _deviceNames.value = reg.deviceNames
            _deviceAreaIds.value = reg.deviceAreaIds
            _entityDeviceIds.value = reg.entityDeviceIds
            _floors.value = reg.floors
            _areaFloorIds.value = reg.areaFloorIds
            if (reg.temperatureUnit != null) _temperatureUnit.value = reg.temperatureUnit
        }
    }

    private data class RegistryPart1(
        val areas: Map<String, String>,
        val entityAreaIds: Map<String, String>,
        val entityNames: Map<String, String>,
    )
    private data class RegistryPart2(
        val deviceNames: Map<String, String>,
        val deviceAreaIds: Map<String, String>,
        val entityDeviceIds: Map<String, String>,
        val floors: Map<String, String>,
        val areaFloorIds: Map<String, String>,
    )

    val haRegistry: StateFlow<HaRegistry> = combine(
        combine(_areas, _entityAreaIds, _entityNames) { a, b, c -> RegistryPart1(a, b, c) },
        combine(_deviceNames, _deviceAreaIds, _entityDeviceIds, _floors, _areaFloorIds) { d, e, f, g, h ->
            RegistryPart2(d, e, f, g, h)
        },
        _temperatureUnit,
    ) { p1, p2, tempUnit ->
        HaRegistry(
            areas = p1.areas,
            entityAreaIds = p1.entityAreaIds,
            entityNames = p1.entityNames,
            deviceNames = p2.deviceNames,
            deviceAreaIds = p2.deviceAreaIds,
            entityDeviceIds = p2.entityDeviceIds,
            floors = p2.floors,
            areaFloorIds = p2.areaFloorIds,
            temperatureUnit = tempUnit,
        )
    }.stateIn(clientScope, SharingStarted.Eagerly, HaRegistry())

    fun setOptimisticState(entityId: String, state: HaEntityState) {
        _optimisticStates.value = _optimisticStates.value + (entityId to state)
        clientScope.launch {
            delay(5_000.milliseconds)
            _optimisticStates.value = _optimisticStates.value - entityId
        }
    }

    private data class HistoryRequest(
        val entityIds: List<String>,
        val hoursToShow: Int,
        val deferred: CompletableDeferred<List<HaHistorySeries>>,
    )

    private val _historyChannel = Channel<HistoryRequest>(Channel.UNLIMITED)

    suspend fun fetchHistory(entityIds: List<String>, hoursToShow: Int): List<HaHistorySeries> {
        if (entityIds.isEmpty() || hoursToShow <= 0) return emptyList()
        val deferred = CompletableDeferred<List<HaHistorySeries>>()
        _historyChannel.trySend(HistoryRequest(entityIds, hoursToShow, deferred))
        return withTimeoutOrNull(15_000.milliseconds) { deferred.await() } ?: emptyList()
    }

    suspend fun connect(config: HomeAssistantConfig) {
        _connectionStatus.value = ConnectionStatus.Checking
        var nextId = 1L
        val pendingRequests = mutableMapOf<Long, PendingRequest>()
        var registriesReceived = 0

        try {
        httpClient.webSocket(config.baseUrl.toWebSocketUrl()) {
            var sendJob: Job? = null
            var historyJob: Job? = null
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val text = frame.readText()
                _frameCount.value += 1
                _latestFrame.value = text

                val element = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: continue
                val type = element["type"]?.jsonPrimitive?.contentOrNull

                when (type) {
                    "auth_required" -> {
                        send(Frame.Text("""{"type":"auth","access_token":"${config.token}"}"""))
                    }

                    "auth_ok" -> {
                        _connectionStatus.value = ConnectionStatus.Connected
                        sendJob = launch {
                            for (msg in _serviceCallChannel) send(Frame.Text(msg))
                        }
                        historyJob = launch {
                            for (req in _historyChannel) {
                                if (req.deferred.isCompleted) continue
                                val id = nextId++
                                pendingRequests[id] = PendingRequest.HistoryQuery(req.deferred)
                                val now = Clock.System.now()
                                val start: Instant = now - req.hoursToShow.hours
                                val entityIdsJson = req.entityIds.joinToString(",") { "\"$it\"" }
                                send(
                                    Frame.Text(
                                        """{"id":$id,"type":"history/history_during_period",""" +
                                            """"start_time":"$start","end_time":"$now",""" +
                                            """"entity_ids":[$entityIdsJson],""" +
                                            """"minimal_response":true,"no_attributes":true,"significant_changes_only":true}""",
                                    ),
                                )
                            }
                        }

                        val listId = nextId++
                        pendingRequests[listId] = PendingRequest.DashboardsList
                        send(Frame.Text("""{"id":$listId,"type":"lovelace/dashboards/list"}"""))

                        val defaultConfigId = nextId++
                        pendingRequests[defaultConfigId] = PendingRequest.DashboardConfig(DEFAULT_DASHBOARD_KEY)
                        send(Frame.Text("""{"id":$defaultConfigId,"type":"lovelace/config"}"""))

                        val statesId = nextId++
                        pendingRequests[statesId] = PendingRequest.GetStates
                        send(Frame.Text("""{"id":$statesId,"type":"get_states"}"""))

                        val subId = nextId++
                        pendingRequests[subId] = PendingRequest.SubscribeEvents
                        send(Frame.Text("""{"id":$subId,"type":"subscribe_events","event_type":"state_changed"}"""))

                        val areaId = nextId++
                        pendingRequests[areaId] = PendingRequest.GetAreaRegistry
                        send(Frame.Text("""{"id":$areaId,"type":"config/area_registry/list"}"""))

                        val entityRegId = nextId++
                        pendingRequests[entityRegId] = PendingRequest.GetEntityRegistry
                        send(Frame.Text("""{"id":$entityRegId,"type":"config/entity_registry/list"}"""))

                        val deviceId = nextId++
                        pendingRequests[deviceId] = PendingRequest.GetDeviceRegistry
                        send(Frame.Text("""{"id":$deviceId,"type":"config/device_registry/list"}"""))

                        val floorId = nextId++
                        pendingRequests[floorId] = PendingRequest.GetFloorRegistry
                        send(Frame.Text("""{"id":$floorId,"type":"config/floor_registry/list"}"""))

                        val configId = nextId++
                        pendingRequests[configId] = PendingRequest.GetConfig
                        send(Frame.Text("""{"id":$configId,"type":"get_config"}"""))
                    }

                    "auth_invalid" -> {
                        _connectionStatus.value = ConnectionStatus.Disconnected("Authentication failed")
                        error("Authentication failed")
                    }

                    "result" -> {
                        val id = element["id"]?.jsonPrimitive?.longOrNull ?: continue
                        val request = pendingRequests.remove(id) ?: continue
                        val success = element["success"]?.jsonPrimitive?.boolean ?: false

                        if (!success) {
                            val errMessage = element["error"]?.jsonObject
                                ?.get("message")?.jsonPrimitive?.contentOrNull
                                ?: "Request failed"
                            if (request is PendingRequest.DashboardConfig) {
                                _dashboardErrors.value = _dashboardErrors.value + (request.key to errMessage)
                            }
                            if (request is PendingRequest.HistoryQuery) {
                                request.deferred.complete(emptyList())
                            }
                            continue
                        }

                        val result = element["result"] ?: continue

                        when (request) {
                            PendingRequest.DashboardsList -> {
                                val list = runCatching {
                                    json.decodeFromJsonElement<List<LovelaceDashboard>>(result)
                                }.getOrElse { emptyList() }
                                _dashboards.value = list
                                cache?.saveDashboards(list)
                                for (dashboard in list) {
                                    val urlPath = dashboard.urlPath ?: continue
                                    val cfgId = nextId++
                                    pendingRequests[cfgId] = PendingRequest.DashboardConfig(urlPath)
                                    send(
                                        Frame.Text(
                                            """{"id":$cfgId,"type":"lovelace/config","url_path":"$urlPath"}"""
                                        )
                                    )
                                }
                            }

                            is PendingRequest.DashboardConfig -> {
                                val cfg = runCatching {
                                    json.decodeFromJsonElement<LovelaceConfig>(result)
                                }.getOrNull()
                                if (cfg != null) {
                                    val updated = _dashboardConfigs.value + (request.key to cfg)
                                    _dashboardConfigs.value = updated
                                    cache?.saveDashboardConfigs(updated)
                                }
                            }

                            PendingRequest.GetStates -> {
                                val list = runCatching {
                                    json.decodeFromJsonElement<List<HaEntityState>>(result)
                                }.getOrElse { emptyList() }
                                _rawEntityStates.value = list.associateBy { it.entityId }
                                _entityStatesLoaded.value = true
                            }

                            PendingRequest.SubscribeEvents -> Unit

                            is PendingRequest.HistoryQuery -> {
                                request.deferred.complete(parseHistoryResult(result))
                            }

                            PendingRequest.GetAreaRegistry -> {
                                val arr = result as? JsonArray ?: continue
                                val areaNames = buildMap<String, String> {
                                    for (item in arr) {
                                        val o = item as? JsonObject ?: continue
                                        val id = (o["area_id"] as? JsonPrimitive)?.contentOrNull ?: continue
                                        val name = (o["name"] as? JsonPrimitive)?.contentOrNull ?: continue
                                        put(id, name)
                                    }
                                }
                                val floorIds = buildMap<String, String> {
                                    for (item in arr) {
                                        val o = item as? JsonObject ?: continue
                                        val id = (o["area_id"] as? JsonPrimitive)?.contentOrNull ?: continue
                                        val floorId = (o["floor_id"] as? JsonPrimitive)?.contentOrNull ?: continue
                                        put(id, floorId)
                                    }
                                }
                                _areas.value = areaNames
                                _areaFloorIds.value = floorIds
                                if (++registriesReceived == 4) maybeSaveRegistry()
                            }

                            PendingRequest.GetEntityRegistry -> {
                                val arr = result as? JsonArray ?: continue
                                val areaIds = buildMap<String, String> {
                                    for (item in arr) {
                                        val o = item as? JsonObject ?: continue
                                        val entityId = (o["entity_id"] as? JsonPrimitive)?.contentOrNull ?: continue
                                        val areaId = (o["area_id"] as? JsonPrimitive)?.contentOrNull ?: continue
                                        put(entityId, areaId)
                                    }
                                }
                                val deviceIds = buildMap<String, String> {
                                    for (item in arr) {
                                        val o = item as? JsonObject ?: continue
                                        val entityId = (o["entity_id"] as? JsonPrimitive)?.contentOrNull ?: continue
                                        val deviceId = (o["device_id"] as? JsonPrimitive)?.contentOrNull ?: continue
                                        put(entityId, deviceId)
                                    }
                                }
                                val names = buildMap<String, String> {
                                    for (item in arr) {
                                        val o = item as? JsonObject ?: continue
                                        val entityId = (o["entity_id"] as? JsonPrimitive)?.contentOrNull ?: continue
                                        val name = (o["name"] as? JsonPrimitive)?.contentOrNull
                                            ?: (o["original_name"] as? JsonPrimitive)?.contentOrNull
                                            ?: continue
                                        put(entityId, name)
                                    }
                                }
                                _entityAreaIds.value = areaIds
                                _entityDeviceIds.value = deviceIds
                                _entityNames.value = names
                                if (++registriesReceived == 4) maybeSaveRegistry()
                            }

                            PendingRequest.GetDeviceRegistry -> {
                                val arr = result as? JsonArray ?: continue
                                val names = buildMap<String, String> {
                                    for (item in arr) {
                                        val o = item as? JsonObject ?: continue
                                        val id = (o["id"] as? JsonPrimitive)?.contentOrNull ?: continue
                                        val name = (o["name_by_user"] as? JsonPrimitive)?.contentOrNull
                                            ?: (o["name"] as? JsonPrimitive)?.contentOrNull
                                            ?: continue
                                        put(id, name)
                                    }
                                }
                                val areaIds = buildMap<String, String> {
                                    for (item in arr) {
                                        val o = item as? JsonObject ?: continue
                                        val id = (o["id"] as? JsonPrimitive)?.contentOrNull ?: continue
                                        val areaId = (o["area_id"] as? JsonPrimitive)?.contentOrNull ?: continue
                                        put(id, areaId)
                                    }
                                }
                                _deviceNames.value = names
                                _deviceAreaIds.value = areaIds
                                if (++registriesReceived == 4) maybeSaveRegistry()
                            }

                            PendingRequest.GetFloorRegistry -> {
                                val arr = result as? JsonArray ?: continue
                                _floors.value = buildMap {
                                    for (item in arr) {
                                        val o = item as? JsonObject ?: continue
                                        val id = (o["floor_id"] as? JsonPrimitive)?.contentOrNull ?: continue
                                        val name = (o["name"] as? JsonPrimitive)?.contentOrNull ?: continue
                                        put(id, name)
                                    }
                                }
                                if (++registriesReceived == 4) maybeSaveRegistry()
                            }

                            PendingRequest.GetConfig -> {
                                val obj = result as? JsonObject ?: continue
                                val unitSystem = obj["unit_system"] as? JsonObject ?: continue
                                val tempUnit = (unitSystem["temperature"] as? JsonPrimitive)?.contentOrNull
                                if (tempUnit != null) {
                                    _temperatureUnit.value = tempUnit
                                    if (registriesReceived == 4) maybeSaveRegistry()
                                }
                            }
                        }
                    }

                    "event" -> {
                        val event = element["event"]?.jsonObject ?: continue
                        val eventType = event["event_type"]?.jsonPrimitive?.contentOrNull
                        if (eventType != "state_changed") continue
                        val data = event["data"]?.jsonObject ?: continue
                        val entityId = data["entity_id"]?.jsonPrimitive?.contentOrNull ?: continue
                        val newState = data["new_state"]
                        _optimisticStates.value = _optimisticStates.value - entityId
                        if (newState == null || newState is JsonNull) {
                            _rawEntityStates.value = _rawEntityStates.value - entityId
                        } else {
                            val parsed = runCatching {
                                json.decodeFromJsonElement<HaEntityState>(newState)
                            }.getOrNull()
                            if (parsed != null) {
                                _rawEntityStates.value = _rawEntityStates.value + (entityId to parsed)
                            }
                        }
                    }
                }
            }
            sendJob?.cancel()
            historyJob?.cancel()
            for ((_, pending) in pendingRequests) {
                if (pending is PendingRequest.HistoryQuery && !pending.deferred.isCompleted) {
                    pending.deferred.complete(emptyList())
                }
            }
        }
        } catch (_: Exception) { }
        _connectionStatus.value = ConnectionStatus.Disconnected("Connection lost")
    }

    private fun maybeSaveRegistry() {
        cache?.saveRegistry(
            CachedRegistry(
                areas = _areas.value,
                entityAreaIds = _entityAreaIds.value,
                entityNames = _entityNames.value,
                deviceNames = _deviceNames.value,
                deviceAreaIds = _deviceAreaIds.value,
                entityDeviceIds = _entityDeviceIds.value,
                floors = _floors.value,
                areaFloorIds = _areaFloorIds.value,
                temperatureUnit = _temperatureUnit.value,
            )
        )
    }

    private sealed interface PendingRequest {
        data object DashboardsList : PendingRequest
        data class DashboardConfig(val key: String) : PendingRequest
        data object GetStates : PendingRequest
        data object SubscribeEvents : PendingRequest
        data class HistoryQuery(val deferred: CompletableDeferred<List<HaHistorySeries>>) : PendingRequest
        data object GetAreaRegistry : PendingRequest
        data object GetEntityRegistry : PendingRequest
        data object GetDeviceRegistry : PendingRequest
        data object GetFloorRegistry : PendingRequest
        data object GetConfig : PendingRequest
    }
}

private fun parseHistoryResult(result: JsonElement): List<HaHistorySeries> {
    val obj = result as? JsonObject ?: return emptyList()
    return obj.entries.map { (entityId, value) ->
        val arr = value as? JsonArray ?: return@map HaHistorySeries(entityId, emptyList())
        val points = arr.mapNotNull { item ->
            val o = item as? JsonObject ?: return@mapNotNull null
            val state = (o["state"] as? JsonPrimitive)?.contentOrNull
                ?: (o["s"] as? JsonPrimitive)?.contentOrNull
                ?: return@mapNotNull null
            val timeMs = parseHistoryTimestamp(o) ?: return@mapNotNull null
            HaHistoryPoint(timeMs, state)
        }
        HaHistorySeries(entityId, points)
    }
}

private fun parseHistoryTimestamp(o: JsonObject): Long? {
    val isoStr = (o["last_changed"] as? JsonPrimitive)?.contentOrNull
        ?: (o["last_updated"] as? JsonPrimitive)?.contentOrNull
    if (isoStr != null) {
        return runCatching { Instant.parse(isoStr).toEpochMilliseconds() }.getOrNull()
    }
    val epochSec = (o["lc"] as? JsonPrimitive)?.doubleOrNull
        ?: (o["lu"] as? JsonPrimitive)?.doubleOrNull
    return epochSec?.let { (it * 1000).toLong() }
}

private fun String.toWebSocketUrl(): String {
    val trimmed = trim().removeSuffix("/")
    val converted = when {
        trimmed.startsWith("https://") -> "wss://" + trimmed.removePrefix("https://")
        trimmed.startsWith("http://") -> "ws://" + trimmed.removePrefix("http://")
        trimmed.startsWith("wss://") || trimmed.startsWith("ws://") -> trimmed
        else -> "wss://$trimmed"
    }
    return "$converted/api/websocket"
}
