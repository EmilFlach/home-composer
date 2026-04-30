package org.example.project.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.example.project.auth.attributeString
import org.example.project.auth.friendlyName
import org.example.project.auth.unitOfMeasurement

@Composable
internal fun PictureEntityCard(
    config: LovelaceCardConfig,
    modifier: Modifier = Modifier,
) {
    val raw = config.raw
    val entityId = config.entity
    val state = rememberEntityState(entityId)

    val explicitImage = stringField(raw, "image")
    val cameraImage = entityId?.takeIf { isCameraEntity(it) }
        ?.let { "/api/camera_proxy/$it" }
    val entityPicture = state?.attributeString("entity_picture")
    val resolvedImage = explicitImage ?: cameraImage ?: entityPicture

    val showName = boolField(raw, "show_name", default = true)
    val showState = boolField(raw, "show_state", default = true)

    val displayName = if (showName) {
        config.nameText ?: state?.friendlyName ?: entityId
    } else null

    val displayState = if (showState && state != null) {
        val unit = state.unitOfMeasurement
        if (unit != null) "${state.state} $unit" else state.state
    } else null

    val altText = stringField(raw, "alt_text") ?: displayName ?: config.title
    val aspectRatio = stringField(raw, "aspect_ratio")?.let(::parseAspectRatio) ?: 16f / 9f

    PictureCardScaffold(
        imageUrl = resolvedImage,
        contentDescription = altText,
        aspectRatio = aspectRatio,
        modifier = modifier,
        overlay = if (displayName != null || displayState != null) {
            { BottomLabelOverlay(name = displayName, state = displayState) }
        } else null,
    )
}

private fun isCameraEntity(entityId: String): Boolean =
    entityId.substringBefore('.', missingDelimiterValue = "") == "camera"

private fun boolField(
    obj: kotlinx.serialization.json.JsonObject?,
    key: String,
    default: Boolean,
): Boolean {
    val element = obj?.get(key) as? kotlinx.serialization.json.JsonPrimitive ?: return default
    return when (element.content.lowercase()) {
        "true" -> true
        "false" -> false
        else -> default
    }
}
