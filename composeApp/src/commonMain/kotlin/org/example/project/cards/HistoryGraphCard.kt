package org.example.project.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import org.example.project.auth.HaEntityState
import org.example.project.auth.HaHistorySeries
import org.example.project.auth.attributeString
import org.example.project.auth.formatStateValue
import org.example.project.auth.friendlyName
import org.example.project.auth.unitOfMeasurement

internal val LocalHaHistoryProvider =
    staticCompositionLocalOf<suspend (List<String>, Int) -> List<HaHistorySeries>> {
        { _: List<String>, _: Int -> emptyList() }
    }

@Composable
internal fun HistoryGraphCard(
    config: LovelaceCardConfig,
    entityStates: Map<String, HaEntityState>,
    modifier: Modifier = Modifier,
) {
    if (config.entities.isEmpty()) {
        UnknownCardStub(config, modifier)
        return
    }
    val raw = config.raw
    val hoursToShow = (raw?.get("hours_to_show") as? JsonPrimitive)?.intOrNull?.coerceAtLeast(1) ?: 24
    val refreshIntervalSeconds = (raw?.get("refresh_interval") as? JsonPrimitive)?.intOrNull ?: 0
    val title = config.title ?: config.nameText

    val provider = LocalHaHistoryProvider.current
    val entityKey = config.entities.joinToString(",")
    var seriesByEntity by remember(entityKey, hoursToShow) {
        mutableStateOf<Map<String, HaHistorySeries>>(emptyMap())
    }
    var hasLoaded by remember(entityKey, hoursToShow) { mutableStateOf(false) }

    LaunchedEffect(entityKey, hoursToShow, refreshIntervalSeconds) {
        while (isActive) {
            val result = provider(config.entities, hoursToShow)
            seriesByEntity = result.associateBy { it.entityId }
            hasLoaded = true
            if (refreshIntervalSeconds <= 0) break
            delay(refreshIntervalSeconds.seconds)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            config.entities.forEach { entityId ->
                HistoryEntityRow(
                    entityId = entityId,
                    entityState = entityStates[entityId],
                    series = seriesByEntity[entityId],
                    hoursToShow = hoursToShow,
                    isLoading = !hasLoaded,
                )
            }
        }
    }
}

@Composable
private fun HistoryEntityRow(
    entityId: String,
    entityState: HaEntityState?,
    series: HaHistorySeries?,
    hoursToShow: Int,
    isLoading: Boolean,
) {
    val displayName = entityState?.friendlyName ?: entityId
    val unit = entityState?.unitOfMeasurement
    val currentValue = entityState?.let { formatStateValue(it.state, unit) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = true),
            )
            if (currentValue != null) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = currentValue,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (unit != null) {
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }
            }
        }

        val deviceClass = entityState?.attributeString("device_class")
        val numericPoints = series?.numericPoints().orEmpty()
        val hasNumericData = numericPoints.size >= 2 && deviceClass != "enum"

        when {
            isLoading && series == null -> HistoryPlaceholder("Loading…")
            series == null || series.points.isEmpty() -> HistoryPlaceholder("No history in last ${hoursToShow}h")
            hasNumericData -> NumericHistoryGraph(
                points = numericPoints,
                hoursToShow = hoursToShow,
                color = MaterialTheme.colorScheme.primary,
            )
            else -> CategoricalHistoryStrip(
                series = series,
                hoursToShow = hoursToShow,
            )
        }
    }
}

@Composable
private fun HistoryPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NumericHistoryGraph(
    points: List<Pair<Long, Float>>,
    hoursToShow: Int,
    color: Color,
) {
    val nowMs = points.last().first
    val windowMs = hoursToShow * 60L * 60L * 1000L
    val startMs = nowMs - windowMs
    val visible = points.filter { it.first >= startMs }
    val effective = if (visible.size >= 2) visible else points

    val minVal = effective.minOf { it.second }
    val maxVal = effective.maxOf { it.second }
    val range = (maxVal - minVal).coerceAtLeast(0.1f)
    val rangeStartMs = effective.first().first.coerceAtMost(startMs)
    val rangeEndMs = effective.last().first
    val timeSpan = (rangeEndMs - rangeStartMs).coerceAtLeast(1L)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        val w = size.width
        val h = size.height
        val padV = h * 0.12f

        fun xAt(timeMs: Long): Float =
            ((timeMs - rangeStartMs).toFloat() / timeSpan.toFloat()) * w
        fun yAt(v: Float): Float =
            h - padV - (v - minVal) / range * (h - 2 * padV)

        repeat(3) { i ->
            val y = padV + i * (h - 2 * padV) / 2
            drawLine(
                color = color.copy(alpha = 0.10f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f,
            )
        }

        val fillPath = Path().apply {
            moveTo(xAt(effective.first().first), h)
            effective.forEach { (t, v) -> lineTo(xAt(t), yAt(v)) }
            lineTo(xAt(effective.last().first), h)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.30f), Color.Transparent),
                startY = 0f,
                endY = h,
            ),
        )

        val linePath = Path().apply {
            effective.forEachIndexed { i, (t, v) ->
                if (i == 0) moveTo(xAt(t), yAt(v)) else lineTo(xAt(t), yAt(v))
            }
        }
        drawPath(linePath, color = color.copy(alpha = 0.30f), style = Stroke(width = 7f, cap = StrokeCap.Round))
        drawPath(linePath, color = color, style = Stroke(width = 2f, cap = StrokeCap.Round))

        val (lastT, lastV) = effective.last()
        drawCircle(color = color.copy(alpha = 0.25f), radius = 8f, center = Offset(xAt(lastT), yAt(lastV)))
        drawCircle(color = color, radius = 3.5f, center = Offset(xAt(lastT), yAt(lastV)))
    }
}

@Composable
private fun CategoricalHistoryStrip(
    series: HaHistorySeries,
    hoursToShow: Int,
) {
    val nowMs = series.points.maxOf { it.timestampMs }
    val windowMs = hoursToShow * 60L * 60L * 1000L
    val startMs = nowMs - windowMs
    val timeSpan = (nowMs - startMs).coerceAtLeast(1L)

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val unavailableColor = MaterialTheme.colorScheme.outlineVariant

    val sorted = series.points.sortedBy { it.timestampMs }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
    ) {
        val w = size.width
        val h = size.height
        if (sorted.isEmpty()) return@Canvas

        for (i in sorted.indices) {
            val pointStart = sorted[i].timestampMs.coerceAtLeast(startMs)
            val pointEnd = if (i < sorted.lastIndex) sorted[i + 1].timestampMs else nowMs
            if (pointEnd <= startMs) continue
            val xStart = ((pointStart - startMs).toFloat() / timeSpan.toFloat()) * w
            val xEnd = ((pointEnd - startMs).toFloat() / timeSpan.toFloat()) * w
            val color = colorForCategoricalState(sorted[i].state, activeColor, inactiveColor, unavailableColor)
            drawRect(
                color = color,
                topLeft = Offset(xStart, 0f),
                size = Size((xEnd - xStart).coerceAtLeast(1f), h),
            )
        }
    }
}

private fun HaHistorySeries.numericPoints(): List<Pair<Long, Float>> =
    points.mapNotNull { p ->
        val v = p.state.toFloatOrNull() ?: return@mapNotNull null
        if (!v.isFinite()) return@mapNotNull null
        p.timestampMs to v
    }

private fun colorForCategoricalState(
    state: String,
    active: Color,
    inactive: Color,
    unavailable: Color,
): Color {
    val s = state.lowercase()
    return when {
        s == "unavailable" || s == "unknown" || s.isEmpty() -> unavailable
        s == "off" || s == "closed" || s == "locked" || s == "idle" || s == "standby" -> inactive
        s == "on" || s == "open" || s == "unlocked" || s == "playing" || s == "home" -> active
        else -> {
            val hash = s.fold(0) { acc, c -> acc * 31 + c.code }
            val palette = listOf(active, inactive, unavailable)
            palette[abs(hash) % palette.size]
        }
    }
}
