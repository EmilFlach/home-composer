package org.example.project.auth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class HomeAssistantWebSocketClient(private val httpClient: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

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

    suspend fun connect(config: HomeAssistantConfig) {
        var nextId = 1L
        val pendingRequests = mutableMapOf<Long, PendingRequest>()

        httpClient.webSocket(config.baseUrl.toWebSocketUrl()) {
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
                        val listId = nextId++
                        pendingRequests[listId] = PendingRequest.DashboardsList
                        send(Frame.Text("""{"id":$listId,"type":"lovelace/dashboards/list"}"""))

                        val defaultConfigId = nextId++
                        pendingRequests[defaultConfigId] = PendingRequest.DashboardConfig(DEFAULT_DASHBOARD_KEY)
                        send(Frame.Text("""{"id":$defaultConfigId,"type":"lovelace/config"}"""))

                        val subId = nextId++
                        pendingRequests[subId] = PendingRequest.SubscribeEvents
                        send(Frame.Text("""{"id":$subId,"type":"subscribe_events"}"""))
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

                            PendingRequest.SubscribeEvents -> Unit
                        }
                    }
                }
            }
        }
    }

    private sealed interface PendingRequest {
        data object DashboardsList : PendingRequest
        data class DashboardConfig(val key: String) : PendingRequest
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
