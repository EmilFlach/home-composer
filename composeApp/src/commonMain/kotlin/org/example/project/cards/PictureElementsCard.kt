package org.example.project.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.ktor.http.HttpHeaders
import org.example.project.auth.LocalHomeAssistantConfig
import org.example.project.auth.isHaApiUrl
import org.example.project.auth.resolveHaUrl
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.example.project.auth.HaEntityState
import org.example.project.auth.attributeString
import org.example.project.auth.friendlyName
import org.example.project.auth.icon
import org.example.project.auth.isActive
import org.example.project.auth.unitOfMeasurement
import org.example.project.icons.MdiIcon
import org.example.project.icons.haEntityIcon
import org.example.project.icons.mdiIconByName
import org.example.project.icons.mdiStringToHaIcon

@Composable
internal fun PictureElementsCard(
    config: LovelaceCardConfig,
    entityStates: Map<String, HaEntityState>,
    modifier: Modifier = Modifier,
) {
    val raw = config.raw
    val explicitImage = stringField(raw, "image")
    val cameraImage = stringField(raw, "camera_image")?.let { "/api/camera_proxy/$it" }
    val imageEntityId = stringField(raw, "image_entity")
    val imageEntityPicture = imageEntityId
        ?.let(entityStates::get)
        ?.attributeString("entity_picture")
    val stateImage = stateImageUrl(raw, config.entity, entityStates)
    val imageUrl = stateImage ?: explicitImage ?: imageEntityPicture ?: cameraImage

    val explicitAspectRatio = stringField(raw, "aspect_ratio")?.let(::parseAspectRatio)
    val altText = stringField(raw, "alt_text") ?: config.title ?: config.nameText

    val elements = parseElements(raw)

    PictureElementsScaffold(
        imageUrl = imageUrl,
        contentDescription = altText,
        explicitAspectRatio = explicitAspectRatio,
        modifier = modifier,
    ) {
        elements.forEach { element ->
            PictureElementHost(element = element, entityStates = entityStates)
        }
    }
}

@Composable
private fun PictureElementsScaffold(
    imageUrl: String?,
    contentDescription: String?,
    explicitAspectRatio: Float?,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit,
) {
    var naturalAspectRatio by remember(imageUrl) { mutableStateOf<Float?>(null) }
    val aspectRatio = explicitAspectRatio ?: naturalAspectRatio ?: (16f / 9f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio.coerceAtLeast(0.1f))
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (imageUrl != null) {
                FittedNetworkImage(
                    url = imageUrl,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    onIntrinsicSize = { width, height ->
                        if (explicitAspectRatio == null && width > 0f && height > 0f) {
                            naturalAspectRatio = width / height
                        }
                    },
                )
            }
            overlay()
        }
    }
}

@Composable
private fun FittedNetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onIntrinsicSize: (Float, Float) -> Unit,
) {
    val haConfig = LocalHomeAssistantConfig.current
    val baseUrl = haConfig?.baseUrl
    val resolvedUrl = resolveHaUrl(baseUrl, url)
    val authToken = haConfig?.token?.takeIf { isHaApiUrl(resolvedUrl, baseUrl) }

    val request = ImageRequest.Builder(LocalPlatformContext.current)
        .data(resolvedUrl)
        .crossfade(true)
        .apply {
            if (authToken != null) {
                httpHeaders(
                    NetworkHeaders.Builder()
                        .set(HttpHeaders.Authorization, "Bearer $authToken")
                        .build(),
                )
            }
        }
        .build()

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        onSuccess = { state ->
            val size = state.painter.intrinsicSize
            onIntrinsicSize(size.width, size.height)
        },
    )
}

@Composable
private fun PictureElementHost(
    element: PictureElement,
    entityStates: Map<String, HaEntityState>,
) {
    if (!evaluateVisibility(element.conditions, entityStates)) return
    PositionedElement(style = element.style) {
        when (element) {
            is PictureElement.IconEl -> IconElement(element)
            is PictureElement.StateIcon -> StateIconElement(element, entityStates)
            is PictureElement.StateBadge -> StateBadgeElement(element, entityStates)
            is PictureElement.StateLabel -> StateLabelElement(element, entityStates)
            is PictureElement.ServiceButton -> ServiceButtonElement(element)
            is PictureElement.ImageEl -> ImageElement(element, entityStates)
            is PictureElement.Conditional -> ConditionalElement(element, entityStates)
        }
    }
}

@Composable
private fun PositionedElement(
    style: ElementStyle,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(Constraints())
            layout(placeable.width, placeable.height) {
                val parentW = constraints.maxWidth
                val parentH = constraints.maxHeight
                val x = (parentW * style.leftFraction + placeable.width * style.translateXFraction).toInt()
                val y = (parentH * style.topFraction + placeable.height * style.translateYFraction).toInt()
                placeable.place(x, y)
            }
        }
    ) {
        content()
    }
}

@Composable
private fun IconElement(element: PictureElement.IconEl) {
    val icon = mdiStringToHaIcon(element.icon, fallback = mdiIconByName("help-circle-outline"))
    ClickableElement(
        tapAction = element.tapAction,
        holdAction = element.holdAction,
        doubleTapAction = element.doubleTapAction,
        contextEntity = element.entity,
    ) {
        MdiIcon(
            icon = icon,
            tint = Color.White,
            size = 28.dp,
        )
    }
}

@Composable
private fun StateIconElement(
    element: PictureElement.StateIcon,
    entityStates: Map<String, HaEntityState>,
) {
    val state = element.entity.let(entityStates::get)
    val icon = mdiStringToHaIcon(
        element.icon ?: state?.icon,
        fallback = haEntityIcon(state, element.entity),
    )
    val tint = if (state?.isActive() == true) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White
    }
    ClickableElement(
        tapAction = element.tapAction,
        holdAction = element.holdAction,
        doubleTapAction = element.doubleTapAction,
        contextEntity = element.entity,
        defaultMoreInfo = true,
    ) {
        MdiIcon(icon = icon, tint = tint, size = 28.dp)
    }
}

@Composable
private fun StateBadgeElement(
    element: PictureElement.StateBadge,
    entityStates: Map<String, HaEntityState>,
) {
    val state = element.entity.let(entityStates::get)
    val icon = mdiStringToHaIcon(
        element.icon ?: state?.icon,
        fallback = haEntityIcon(state, element.entity),
    )
    val active = state?.isActive() == true
    val background = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Black.copy(alpha = 0.55f)
    }
    val contentColor = if (active) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        Color.White
    }
    ClickableElement(
        tapAction = element.tapAction,
        holdAction = element.holdAction,
        doubleTapAction = element.doubleTapAction,
        contextEntity = element.entity,
        defaultMoreInfo = true,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            MdiIcon(icon = icon, tint = contentColor, size = 24.dp)
        }
    }
}

@Composable
private fun StateLabelElement(
    element: PictureElement.StateLabel,
    entityStates: Map<String, HaEntityState>,
) {
    val state = element.entity.let(entityStates::get)
    val text = buildString {
        if (element.prefix != null) append(element.prefix)
        if (state != null) {
            val attribute = element.attribute
            val value = if (attribute != null) state.attributeString(attribute) ?: "" else state.state
            append(value)
            val unit = state.unitOfMeasurement
            if (attribute == null && unit != null) {
                append(' ')
                append(unit)
            }
        } else {
            append(element.entity)
        }
        if (element.suffix != null) append(element.suffix)
    }
    ClickableElement(
        tapAction = element.tapAction,
        holdAction = element.holdAction,
        doubleTapAction = element.doubleTapAction,
        contextEntity = element.entity,
        defaultMoreInfo = true,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ServiceButtonElement(element: PictureElement.ServiceButton) {
    ClickableElement(
        tapAction = element.tapAction,
        holdAction = element.holdAction,
        doubleTapAction = element.doubleTapAction,
        contextEntity = null,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (element.icon != null) {
                MdiIcon(
                    icon = mdiStringToHaIcon(element.icon, fallback = mdiIconByName("gesture-tap-button")),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    size = 18.dp,
                )
            }
            Text(
                text = element.title ?: "Action",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun ImageElement(
    element: PictureElement.ImageEl,
    entityStates: Map<String, HaEntityState>,
) {
    val resolved = element.image
        ?: element.cameraImage?.let { "/api/camera_proxy/$it" }
        ?: element.entity?.let(entityStates::get)?.attributeString("entity_picture")
        ?: return
    ClickableElement(
        tapAction = element.tapAction,
        holdAction = element.holdAction,
        doubleTapAction = element.doubleTapAction,
        contextEntity = element.entity,
    ) {
        NetworkImage(
            url = resolved,
            contentDescription = element.title,
            modifier = Modifier.size(width = element.widthDp.dp, height = element.heightDp.dp),
        )
    }
}

@Composable
private fun ConditionalElement(
    element: PictureElement.Conditional,
    entityStates: Map<String, HaEntityState>,
) {
    if (!evaluateVisibility(element.conditions, entityStates)) return
    Box {
        element.elements.forEach { child ->
            PictureElementHost(element = child, entityStates = entityStates)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClickableElement(
    tapAction: HaAction,
    holdAction: HaAction,
    doubleTapAction: HaAction,
    contextEntity: String?,
    defaultMoreInfo: Boolean = false,
    content: @Composable () -> Unit,
) {
    val onAction = LocalHaActionHandler.current
    val effectiveTap = if (tapAction is HaAction.None && defaultMoreInfo && contextEntity != null) {
        HaAction.MoreInfo(contextEntity)
    } else {
        tapAction
    }
    val hasAnyAction = effectiveTap !is HaAction.None ||
        holdAction !is HaAction.None ||
        doubleTapAction !is HaAction.None
    Box(
        modifier = if (hasAnyAction) {
            Modifier.combinedClickable(
                onClick = { onAction(effectiveTap, contextEntity) },
                onLongClick = if (holdAction !is HaAction.None) {
                    { onAction(holdAction, contextEntity) }
                } else null,
                onDoubleClick = if (doubleTapAction !is HaAction.None) {
                    { onAction(doubleTapAction, contextEntity) }
                } else null,
            )
        } else {
            Modifier
        },
    ) {
        content()
    }
}

private fun stateImageUrl(
    raw: JsonObject?,
    entityId: String?,
    entityStates: Map<String, HaEntityState>,
): String? {
    val map = raw?.get("state_image") as? JsonObject ?: return null
    val state = entityId?.let(entityStates::get)?.state ?: return null
    val direct = (map[state] as? JsonPrimitive)?.contentOrNull
    if (direct != null) return direct
    return (map["default"] as? JsonPrimitive)?.contentOrNull
}

private sealed class PictureElement {
    abstract val style: ElementStyle
    abstract val conditions: List<HaCondition>

    data class IconEl(
        val icon: String,
        val entity: String?,
        val title: String?,
        val tapAction: HaAction,
        val holdAction: HaAction,
        val doubleTapAction: HaAction,
        override val style: ElementStyle,
        override val conditions: List<HaCondition>,
    ) : PictureElement()

    data class StateIcon(
        val entity: String,
        val icon: String?,
        val title: String?,
        val tapAction: HaAction,
        val holdAction: HaAction,
        val doubleTapAction: HaAction,
        override val style: ElementStyle,
        override val conditions: List<HaCondition>,
    ) : PictureElement()

    data class StateBadge(
        val entity: String,
        val icon: String?,
        val title: String?,
        val tapAction: HaAction,
        val holdAction: HaAction,
        val doubleTapAction: HaAction,
        override val style: ElementStyle,
        override val conditions: List<HaCondition>,
    ) : PictureElement()

    data class StateLabel(
        val entity: String,
        val attribute: String?,
        val prefix: String?,
        val suffix: String?,
        val tapAction: HaAction,
        val holdAction: HaAction,
        val doubleTapAction: HaAction,
        override val style: ElementStyle,
        override val conditions: List<HaCondition>,
    ) : PictureElement()

    data class ServiceButton(
        val title: String?,
        val icon: String?,
        val tapAction: HaAction,
        val holdAction: HaAction,
        val doubleTapAction: HaAction,
        override val style: ElementStyle,
        override val conditions: List<HaCondition>,
    ) : PictureElement()

    data class ImageEl(
        val image: String?,
        val cameraImage: String?,
        val entity: String?,
        val title: String?,
        val widthDp: Int,
        val heightDp: Int,
        val tapAction: HaAction,
        val holdAction: HaAction,
        val doubleTapAction: HaAction,
        override val style: ElementStyle,
        override val conditions: List<HaCondition>,
    ) : PictureElement()

    data class Conditional(
        val elements: List<PictureElement>,
        override val style: ElementStyle,
        override val conditions: List<HaCondition>,
    ) : PictureElement()
}

private data class ElementStyle(
    val leftFraction: Float = 0f,
    val topFraction: Float = 0f,
    val translateXFraction: Float = 0f,
    val translateYFraction: Float = 0f,
)

private fun parseElements(raw: JsonObject?): List<PictureElement> {
    val arr = raw?.get("elements") as? JsonArray ?: return emptyList()
    return arr.mapNotNull { element ->
        (element as? JsonObject)?.let(::parseElement)
    }
}

private fun parseElement(obj: JsonObject): PictureElement? {
    val type = stringField(obj, "type") ?: return null
    val style = parseStyle(obj["style"])
    val tap = parseAction(obj, "tap_action")
    val hold = parseAction(obj, "hold_action")
    val doubleTap = parseAction(obj, "double_tap_action")
    val visibility = parseVisibility(obj["visibility"])
    return when (type) {
        "icon" -> {
            val icon = stringField(obj, "icon") ?: return null
            PictureElement.IconEl(
                icon = icon,
                entity = stringField(obj, "entity"),
                title = stringField(obj, "title"),
                tapAction = tap,
                holdAction = hold,
                doubleTapAction = doubleTap,
                style = style,
                conditions = visibility,
            )
        }
        "state-icon" -> {
            val entity = stringField(obj, "entity") ?: return null
            PictureElement.StateIcon(
                entity = entity,
                icon = stringField(obj, "icon"),
                title = stringField(obj, "title"),
                tapAction = tap,
                holdAction = hold,
                doubleTapAction = doubleTap,
                style = style,
                conditions = visibility,
            )
        }
        "state-badge" -> {
            val entity = stringField(obj, "entity") ?: return null
            PictureElement.StateBadge(
                entity = entity,
                icon = stringField(obj, "icon"),
                title = stringField(obj, "title"),
                tapAction = tap,
                holdAction = hold,
                doubleTapAction = doubleTap,
                style = style,
                conditions = visibility,
            )
        }
        "state-label" -> {
            val entity = stringField(obj, "entity") ?: return null
            PictureElement.StateLabel(
                entity = entity,
                attribute = stringField(obj, "attribute"),
                prefix = stringField(obj, "prefix"),
                suffix = stringField(obj, "suffix"),
                tapAction = tap,
                holdAction = hold,
                doubleTapAction = doubleTap,
                style = style,
                conditions = visibility,
            )
        }
        "service-button" -> PictureElement.ServiceButton(
            title = stringField(obj, "title"),
            icon = stringField(obj, "icon"),
            tapAction = tap,
            holdAction = hold,
            doubleTapAction = doubleTap,
            style = style,
            conditions = visibility,
        )
        "image" -> PictureElement.ImageEl(
            image = stringField(obj, "image"),
            cameraImage = stringField(obj, "camera_image"),
            entity = stringField(obj, "entity"),
            title = stringField(obj, "title"),
            widthDp = 80,
            heightDp = 80,
            tapAction = tap,
            holdAction = hold,
            doubleTapAction = doubleTap,
            style = style,
            conditions = visibility,
        )
        "conditional" -> {
            val nested = (obj["elements"] as? JsonArray)
                ?.mapNotNull { (it as? JsonObject)?.let(::parseElement) }
                .orEmpty()
            val nestedConditions = parseVisibility(obj["conditions"])
            PictureElement.Conditional(
                elements = nested,
                style = style,
                conditions = nestedConditions.ifEmpty { visibility },
            )
        }
        else -> null
    }
}

private fun parseStyle(element: kotlinx.serialization.json.JsonElement?): ElementStyle {
    val obj = element as? JsonObject ?: return ElementStyle()
    val left = parsePercent(stringField(obj, "left")) ?: 0f
    val top = parsePercent(stringField(obj, "top")) ?: 0f
    // HA default style for picture-elements is `transform: translate(-50%, -50%)`,
    // anchoring the element at its center on (left, top).
    val (tx, ty) = parseTranslate(stringField(obj, "transform"), default = -0.5f to -0.5f)
    return ElementStyle(
        leftFraction = left,
        topFraction = top,
        translateXFraction = tx,
        translateYFraction = ty,
    )
}

private fun parsePercent(value: String?): Float? {
    val v = value?.trim() ?: return null
    if (v.endsWith("%")) {
        return v.removeSuffix("%").trim().toFloatOrNull()?.let { it / 100f }
    }
    return null
}

private fun parseTranslate(transform: String?, default: Pair<Float, Float>): Pair<Float, Float> {
    if (transform == null) return default
    val translateStart = transform.indexOf("translate(")
    if (translateStart < 0) return default
    val open = translateStart + "translate(".length
    val close = transform.indexOf(')', open).takeIf { it > 0 } ?: return default
    val args = transform.substring(open, close).split(',').map { it.trim() }
    if (args.isEmpty()) return default
    val tx = parsePercent(args[0]) ?: 0f
    val ty = if (args.size >= 2) parsePercent(args[1]) ?: 0f else 0f
    return tx to ty
}
