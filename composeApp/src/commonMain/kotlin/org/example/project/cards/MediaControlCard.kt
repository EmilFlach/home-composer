package org.example.project.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import org.example.project.auth.HaEntityState
import org.example.project.auth.attributeString
import org.example.project.auth.friendlyName
import org.example.project.auth.LocalHomeAssistantConfig
import org.example.project.auth.isHaApiUrl
import org.example.project.auth.resolveHaUrl
import io.ktor.http.HttpHeaders as KtorHttpHeaders

private const val SUPPORT_PAUSE = 1
private const val SUPPORT_PREVIOUS_TRACK = 16
private const val SUPPORT_NEXT_TRACK = 32
private const val SUPPORT_TURN_ON = 128
private const val SUPPORT_STOP = 4096
private const val SUPPORT_PLAY = 16384

@Composable
internal fun MediaControlCard(
    config: LovelaceCardConfig,
    entityStates: Map<String, HaEntityState>,
    modifier: Modifier = Modifier,
) {
    val entityId = config.entity
    if (entityId == null) {
        UnknownCardStub(config, modifier)
        return
    }
    val state = entityStates[entityId]

    val title = state?.attributeString("media_title")
    val artist = state?.attributeString("media_artist")
    val album = state?.attributeString("media_album_name")
    val coverUrl = state?.attributeString("entity_picture")
    val supportedFeatures = (state?.attributes?.get("supported_features") as? JsonPrimitive)?.intOrNull ?: 0
    var stableFeatures by remember(entityId) { mutableStateOf(supportedFeatures) }
    if (supportedFeatures != 0) stableFeatures = supportedFeatures

    val rawState = state?.state.orEmpty()
    val isPlaying = rawState == "playing"
    val isOff = rawState == "off" || rawState == "unavailable"

    val displayName = config.nameText ?: state?.friendlyName ?: entityId

    val handler = LocalHaActionHandler.current
    fun call(service: String, data: JsonObject? = null) {
        handler(
            HaAction.PerformAction(
                action = "media_player.$service",
                target = buildJsonObject { put("entity_id", entityId) },
                data = data,
            ),
            null,
        )
    }

    // Palette extraction
    val haConfig = LocalHomeAssistantConfig.current
    val platformContext = LocalPlatformContext.current
    var palette by remember(coverUrl) { mutableStateOf<MediaPalette?>(null) }
    LaunchedEffect(coverUrl) {
        palette = null
        if (coverUrl == null) return@LaunchedEffect
        val resolvedUrl = resolveHaUrl(haConfig?.baseUrl, coverUrl)
        val authToken = haConfig?.token?.takeIf { isHaApiUrl(resolvedUrl, haConfig.baseUrl) }
        val request = ImageRequest.Builder(platformContext)
            .data(resolvedUrl)
            .apply {
                if (authToken != null) {
                    httpHeaders(
                        NetworkHeaders.Builder()
                            .set(KtorHttpHeaders.Authorization, "Bearer $authToken")
                            .build()
                    )
                }
            }
            .build()
        val result = SingletonImageLoader.get(platformContext).execute(request)
        val bitmapImage = (result as? SuccessResult)?.image as? BitmapImage
        palette = bitmapImage?.let { extractMediaPalette(it) }
    }

    // Animated colors
    val defaultBg = MaterialTheme.colorScheme.surfaceContainer
    val bgColor by animateColorAsState(palette?.background ?: defaultBg, animationSpec = tween(600))
    val surfaceColor by animateColorAsState(palette?.surface ?: MaterialTheme.colorScheme.surfaceContainerHigh, animationSpec = tween(600))
    val accentColor by animateColorAsState(palette?.accent ?: MaterialTheme.colorScheme.primary, animationSpec = tween(600))
    val textPrimary by animateColorAsState(palette?.onBackground ?: MaterialTheme.colorScheme.onSurface, animationSpec = tween(600))
    val textSecondary by animateColorAsState((palette?.onBackground ?: MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.75f), animationSpec = tween(600))

    val cardShape = RoundedCornerShape(16.dp)
    val density = LocalDensity.current
    var columnHeightPx by remember { mutableStateOf(0) }
    val coverSizeDp = with(density) { columnHeightPx.toDp() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = bgColor),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (coverUrl != null && columnHeightPx > 0) {
                NetworkImage(
                    url = coverUrl,
                    contentDescription = title ?: displayName,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(coverSizeDp)
                        .aspectRatio(1f, matchHeightConstraintsFirst = true),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { columnHeightPx = it.height }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.62f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = title ?: humanizeMediaState(rawState),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (artist != null) {
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else if (album != null) {
                        Text(
                            text = album,
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                MediaControlsRow(
                    isPlaying = isPlaying,
                    isOff = isOff,
                    supportedFeatures = stableFeatures,
                    accentColor = accentColor,
                    iconColor = textPrimary,
                    onPrev = { call("media_previous_track") },
                    onPlayPause = {
                        when {
                            isOff && stableFeatures.has(SUPPORT_TURN_ON) -> call("turn_on")
                            isPlaying -> call("media_pause")
                            else -> call("media_play")
                        }
                    },
                    onStop = { call("media_stop") },
                    onNext = { call("media_next_track") },
                )
            }
        }
    }
}

@Composable
private fun MediaControlsRow(
    isPlaying: Boolean,
    isOff: Boolean,
    supportedFeatures: Int,
    accentColor: Color,
    iconColor: Color,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (supportedFeatures.has(SUPPORT_PREVIOUS_TRACK)) {
            IconButton(onClick = onPrev, enabled = !isOff) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = iconColor)
            }
        }
        val canPlay = supportedFeatures.has(SUPPORT_PLAY) || supportedFeatures.has(SUPPORT_PAUSE)
        if (canPlay || supportedFeatures.has(SUPPORT_TURN_ON)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                    )
                }
            }
        }
        if (supportedFeatures.has(SUPPORT_STOP)) {
            IconButton(onClick = onStop, enabled = !isOff) {
                Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = iconColor)
            }
        }
        if (supportedFeatures.has(SUPPORT_NEXT_TRACK)) {
            IconButton(onClick = onNext, enabled = !isOff) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = iconColor)
            }
        }
    }
}

private fun Int.has(flag: Int): Boolean = (this and flag) == flag

private fun humanizeMediaState(state: String): String = when (state.lowercase()) {
    "playing" -> "Playing"
    "paused" -> "Paused"
    "idle" -> "Idle"
    "standby" -> "Standby"
    "off" -> "Off"
    "on" -> "On"
    "buffering" -> "Buffering"
    "unavailable" -> "Unavailable"
    "unknown", "" -> "—"
    else -> state.replaceFirstChar { it.uppercase() }
}
