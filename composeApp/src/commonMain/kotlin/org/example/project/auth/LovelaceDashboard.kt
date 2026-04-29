package org.example.project.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LovelaceDashboard(
    val id: String,
    @SerialName("url_path") val urlPath: String? = null,
    val title: String = "",
    val icon: String? = null,
    val mode: String? = null,
    @SerialName("show_in_sidebar") val showInSidebar: Boolean = true,
    @SerialName("require_admin") val requireAdmin: Boolean = false,
)

@Serializable
data class LovelaceConfig(
    val title: String? = null,
    val views: List<LovelaceView> = emptyList(),
    val strategy: LovelaceStrategy? = null,
)

@Serializable
data class LovelaceView(
    val title: String? = null,
    val path: String? = null,
    val icon: String? = null,
    val type: String? = null,
    val cards: List<JsonElement> = emptyList(),
    val sections: List<LovelaceSection> = emptyList(),
    val badges: List<JsonElement> = emptyList(),
    val strategy: LovelaceStrategy? = null,
    @SerialName("max_columns") val maxColumns: Int? = null,
)

@Serializable
data class LovelaceSection(
    val title: String? = null,
    val type: String? = null,
    val cards: List<JsonElement> = emptyList(),
    val visibility: JsonElement? = null,
)

@Serializable
data class LovelaceStrategy(
    val type: String,
)

fun LovelaceView.allCards(): List<JsonElement> =
    cards + sections.flatMap { it.cards }

const val DEFAULT_DASHBOARD_KEY = "lovelace"
