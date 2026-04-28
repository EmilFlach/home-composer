package org.example.project.auth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeAssistantWebSocketClient(private val httpClient: HttpClient) {
    private val _frameCount = MutableStateFlow(0L)
    val frameCount: StateFlow<Long> = _frameCount.asStateFlow()

    private val _latestFrame = MutableStateFlow<String?>(null)
    val latestFrame: StateFlow<String?> = _latestFrame.asStateFlow()

    suspend fun connect(config: HomeAssistantConfig) {
        httpClient.webSocket(config.baseUrl.toWebSocketUrl()) {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val text = frame.readText()
                _frameCount.value += 1
                _latestFrame.value = text

                when {
                    text.contains("\"type\":\"auth_required\"") -> {
                        send(Frame.Text("""{"type":"auth","access_token":"${config.token}"}"""))
                    }
                    text.contains("\"type\":\"auth_ok\"") -> {
                        send(Frame.Text("""{"id":1,"type":"subscribe_events"}"""))
                    }
                    text.contains("\"type\":\"auth_invalid\"") -> {
                        error("Authentication failed")
                    }
                }
            }
        }
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
