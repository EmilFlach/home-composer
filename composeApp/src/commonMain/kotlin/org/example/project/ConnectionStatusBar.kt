package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.example.project.auth.ConnectionStatus

private const val LATEST_FRAME_PREVIEW_LENGTH = 80

@Composable
fun ConnectionStatusBar(
    status: ConnectionStatus,
    baseUrl: String,
    frameCount: Long = 0L,
    latestFrame: String? = null,
    modifier: Modifier = Modifier,
) {
    val (dotColor, label) = when (status) {
        ConnectionStatus.Checking -> Color(0xFFFFC107) to "Checking $baseUrl…"
        ConnectionStatus.Connected -> Color(0xFF4CAF50) to "Connected to $baseUrl"
        is ConnectionStatus.Disconnected -> Color(0xFFF44336) to "Disconnected: ${status.message}"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val preview = latestFrame?.take(LATEST_FRAME_PREVIEW_LENGTH)
                val frameLine = buildString {
                    append("WS: ")
                    append(frameCount)
                    append(" frames")
                    if (!preview.isNullOrEmpty()) {
                        append(" • ")
                        append(preview)
                        if ((latestFrame.length) > LATEST_FRAME_PREVIEW_LENGTH) append('…')
                    }
                }
                Text(
                    text = frameLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
