package org.example.project.auth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.time.Duration.Companion.milliseconds

class HomeAssistantWebSocketClient(private val httpClient: HttpClient) {
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

    val entityStates: StateFlow<Map<String, HaEntityState>> = combine(
        _rawEntityStates, _optimisticStates
    ) { raw, optimistic -> raw + optimistic }
        .stateIn(clientScope, SharingStarted.Eagerly, emptyMap())

    fun setOptimisticState(entityId: String, state: HaEntityState) {
        _optimisticStates.value = _optimisticStates.value + (entityId to state)
        clientScope.launch {
            delay(5_000.milliseconds)
            _optimisticStates.value = _optimisticStates.value - entityId
        }
    }

    suspend fun connect(config: HomeAssistantConfig) {
        var nextId = 1L
        val pendingRequests = mutableMapOf<Long, PendingRequest>()

        try {
        httpClient.webSocket(config.baseUrl.toWebSocketUrl()) {
            var sendJob: Job? = null
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
                        sendJob = launch {
                            for (msg in _serviceCallChannel) send(Frame.Text(msg))
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
                    }

                    "auth_invalid" -> error("Authentication failed")

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
                            continue
                        }

                        val result = element["result"] ?: continue

                        when (request) {
                            PendingRequest.DashboardsList -> {
                                val list = runCatching {
                                    json.decodeFromJsonElement<List<LovelaceDashboard>>(result)
                                }.getOrElse { emptyList() }
                                _dashboards.value = list
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
                                    _dashboardConfigs.value = _dashboardConfigs.value + (request.key to cfg)
                                }
                            }

                            PendingRequest.GetStates -> {
                                val list = runCatching {
                                    json.decodeFromJsonElement<List<HaEntityState>>(result)
                                }.getOrElse { emptyList() }
                                _rawEntityStates.value = list.associateBy { it.entityId }
                            }

                            PendingRequest.SubscribeEvents -> Unit
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
        }
        } catch (_: Exception) { }
    }

    private sealed interface PendingRequest {
        data object DashboardsList : PendingRequest
        data class DashboardConfig(val key: String) : PendingRequest
        data object GetStates : PendingRequest
        data object SubscribeEvents : PendingRequest
    }
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
