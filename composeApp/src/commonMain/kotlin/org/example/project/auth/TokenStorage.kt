package org.example.project.auth

import com.russhwolf.settings.Settings

class TokenStorage(private val settings: Settings) {
    fun load(): HomeAssistantConfig? {
        val baseUrl = settings.getStringOrNull(KEY_BASE_URL) ?: return null
        val token = settings.getStringOrNull(KEY_TOKEN) ?: return null
        if (baseUrl.isBlank() || token.isBlank()) return null
        return HomeAssistantConfig(baseUrl = baseUrl, token = token)
    }

    fun save(config: HomeAssistantConfig) {
        settings.putString(KEY_BASE_URL, config.baseUrl)
        settings.putString(KEY_TOKEN, config.token)
    }

    fun clear() {
        settings.remove(KEY_BASE_URL)
        settings.remove(KEY_TOKEN)
    }

    companion object {
        private const val KEY_BASE_URL = "ha_base_url"
        private const val KEY_TOKEN = "ha_token"
    }
}
