package org.example.project.cards

import androidx.compose.runtime.staticCompositionLocalOf

data class HaRegistry(
    val areas: Map<String, String> = emptyMap(),           // area_id -> area_name
    val entityAreaIds: Map<String, String> = emptyMap(),   // entity_id -> area_id (direct assignment)
    val entityNames: Map<String, String> = emptyMap(),     // entity_id -> entity registry name
    val deviceNames: Map<String, String> = emptyMap(),     // device_id -> display_name
    val deviceAreaIds: Map<String, String> = emptyMap(),   // device_id -> area_id (device-level assignment)
    val entityDeviceIds: Map<String, String> = emptyMap(), // entity_id -> device_id
    val floors: Map<String, String> = emptyMap(),          // floor_id -> floor_name
    val areaFloorIds: Map<String, String> = emptyMap(),    // area_id -> floor_id
    val temperatureUnit: String? = null,                   // system temperature unit, e.g. "°C"
)

internal val LocalHaRegistry = staticCompositionLocalOf { HaRegistry() }

internal fun HaRegistry.resolveNameConfig(nameConfig: CardNameConfig, entityId: String?, friendlyName: String?): String? {
    return when (nameConfig) {
        is CardNameConfig.Static -> nameConfig.text
        is CardNameConfig.Typed -> {
            nameConfig.parts.mapNotNull { part ->
                resolveNamePart(part.type, entityId, friendlyName, part.text)
            }.joinToString(" ").takeIf { it.isNotBlank() }
        }
    }
}

private fun HaRegistry.resolveNamePart(type: String, entityId: String?, friendlyName: String?, text: String?): String? {
    return when (type) {
        "area" -> entityAreaId(entityId)?.let { areas[it] }
        "device" -> entityId?.let { entityDeviceIds[it] }?.let { deviceNames[it] }
        "entity" -> entityId?.let { entityNames[it] } ?: friendlyName
        "floor" -> entityAreaId(entityId)?.let { areaFloorIds[it] }?.let { floors[it] }
        "text" -> text
        else -> null
    }
}

// Entity area: direct assignment on entity, or inherited from its device.
private fun HaRegistry.entityAreaId(entityId: String?): String? {
    if (entityId == null) return null
    return entityAreaIds[entityId]
        ?: entityDeviceIds[entityId]?.let { deviceAreaIds[it] }
}
