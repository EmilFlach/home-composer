package org.example.project.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.example.project.auth.LocalHomeAssistantConfig
import org.example.project.auth.attributeString
import org.example.project.auth.isHaApiUrl
import org.example.project.auth.resolveHaUrl

@Composable
internal fun PictureCard(
    config: LovelaceCardConfig,
    modifier: Modifier = Modifier,
) {
    val raw = config.raw
    val explicitImage = stringField(raw, "image")
    val imageEntityId = stringField(raw, "image_entity")
    val imageEntityPicture = rememberEntityState(imageEntityId)
        ?.attributeString("entity_picture")
    val imageEntityFallback = imageEntityId?.takeIf { it.startsWith("camera.") }
        ?.let { "/api/camera_proxy/$it" }
    val imageUrl = explicitImage ?: imageEntityPicture ?: imageEntityFallback

    val caption = config.title ?: stringField(raw, "caption")
    val altText = stringField(raw, "alt_text") ?: caption
    val aspectRatio = stringField(raw, "aspect_ratio")?.let(::parseAspectRatio) ?: 16f / 9f

    PictureCardScaffold(
        imageUrl = imageUrl,
        contentDescription = altText,
        aspectRatio = aspectRatio,
        modifier = modifier,
        overlay = caption?.let { text ->
            { CaptionOverlay(title = text) }
        },
    )
}

@Composable
internal fun PictureCardScaffold(
    imageUrl: String?,
    contentDescription: String?,
    aspectRatio: Float,
    modifier: Modifier = Modifier,
    overlay: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = LocalCardShape.current,
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
            if (imageUrl == null) {
                MissingImagePlaceholder()
            } else {
                NetworkImage(
                    url = imageUrl,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            overlay?.invoke()
        }
    }
}

@Composable
internal fun NetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val config = LocalHomeAssistantConfig.current
    val baseUrl = config?.baseUrl
    val resolvedUrl = resolveHaUrl(baseUrl, url)
    val authToken = config?.token?.takeIf { isHaApiUrl(resolvedUrl, baseUrl) }

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
        contentScale = contentScale,
    )
}

@Composable
private fun MissingImagePlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No image",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CaptionOverlay(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.6f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.55f),
                ),
            ),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
internal fun BottomLabelOverlay(
    name: String?,
    state: String?,
) {
    if (name == null && state == null) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.5f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.6f),
                ),
            ),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (name != null) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
            if (state != null) {
                Text(
                    text = state,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}

internal fun parseAspectRatio(value: String): Float? {
    val trimmed = value.trim()
    if (trimmed.endsWith("%")) {
        val number = trimmed.removeSuffix("%").toFloatOrNull() ?: return null
        if (number <= 0f) return null
        return 100f / number
    }
    val parts = trimmed.split(":", "/")
    if (parts.size == 2) {
        val a = parts[0].toFloatOrNull()
        val b = parts[1].toFloatOrNull()
        if (a != null && b != null && b > 0f) return a / b
    }
    val direct = trimmed.toFloatOrNull()
    if (direct != null && direct > 0f) return direct
    return null
}

internal fun stringField(obj: JsonObject?, key: String): String? {
    val primitive = obj?.get(key) as? JsonPrimitive ?: return null
    return primitive.contentOrNull?.takeIf { it.isNotBlank() }
}
