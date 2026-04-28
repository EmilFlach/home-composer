package org.example.project.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonObject

class HomeAssistantClient(private val httpClient: HttpClient) {
    suspend fun verify(config: HomeAssistantConfig): Result<Unit> = runCatching {
        val response: HttpResponse = httpClient.get("${config.baseUrl}/api/") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${config.token}")
            }
        }
        when (response.status) {
            HttpStatusCode.OK -> Unit
            HttpStatusCode.Unauthorized -> error("Invalid access token")
            HttpStatusCode.Forbidden -> error("Token rejected by Home Assistant")
            HttpStatusCode.NotFound -> error("API endpoint not found at this URL")
            else -> error("Unexpected response: ${response.status}")
        }
    }

    suspend fun getEntityJson(config: HomeAssistantConfig, entityId: String): Result<JsonObject> = runCatching {
        httpClient.get("${config.baseUrl}/api/states/$entityId") {
            headers { append(HttpHeaders.Authorization, "Bearer ${config.token}") }
        }.body<JsonObject>()
    }
}
