package org.example.project.auth

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DashboardCache(private val settings: Settings) {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadDashboards(): List<LovelaceDashboard>? =
        settings.getStringOrNull(KEY_DASHBOARDS)?.let {
            runCatching { json.decodeFromString<List<LovelaceDashboard>>(it) }.getOrNull()
        }

    fun saveDashboards(list: List<LovelaceDashboard>) {
        runCatching { settings.putString(KEY_DASHBOARDS, json.encodeToString(list)) }
    }

    fun loadDashboardConfigs(): Map<String, LovelaceConfig>? =
        settings.getStringOrNull(KEY_DASHBOARD_CONFIGS)?.let {
            runCatching { json.decodeFromString<Map<String, LovelaceConfig>>(it) }.getOrNull()
        }

    fun saveDashboardConfigs(configs: Map<String, LovelaceConfig>) {
        runCatching { settings.putString(KEY_DASHBOARD_CONFIGS, json.encodeToString(configs)) }
    }

    fun loadRegistry(): CachedRegistry? =
        settings.getStringOrNull(KEY_REGISTRY)?.let {
            runCatching { json.decodeFromString<CachedRegistry>(it) }.getOrNull()
        }

    fun saveRegistry(registry: CachedRegistry) {
        runCatching { settings.putString(KEY_REGISTRY, json.encodeToString(registry)) }
    }

    companion object {
        private const val KEY_DASHBOARDS = "cache_dashboards"
        private const val KEY_DASHBOARD_CONFIGS = "cache_dashboard_configs"
        private const val KEY_REGISTRY = "cache_registry"
    }
}

@Serializable
data class CachedRegistry(
    val areas: Map<String, String>,
    val entityAreaIds: Map<String, String>,
    val entityNames: Map<String, String>,
    val deviceNames: Map<String, String>,
    val deviceAreaIds: Map<String, String>,
    val entityDeviceIds: Map<String, String>,
    val floors: Map<String, String>,
    val areaFloorIds: Map<String, String>,
    val temperatureUnit: String? = null,
)
