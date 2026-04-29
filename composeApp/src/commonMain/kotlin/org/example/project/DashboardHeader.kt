package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.example.project.auth.ConnectionStatus

data class DashboardSelectorItem(
    val key: String,
    val title: String,
)

@Composable
fun DashboardHeader(
    items: List<DashboardSelectorItem>,
    selectedKey: String?,
    onSelectDashboard: (String) -> Unit,
    connectionStatus: ConnectionStatus,
    darkTheme: Boolean,
    onToggleDarkMode: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedTitle = items.firstOrNull { it.key == selectedKey }?.title
        ?: items.firstOrNull()?.title
        ?: "Dashboards"

    val gradientColors = if (darkTheme) {
        arrayOf(
            0.0f to Color(0xFF3A0080),
            0.6f to Color(0xFF1A0040),
            1.0f to Color(0xFF080010),
        )
    } else {
        arrayOf(
            0.0f to Color(0xFF7C3AED),
            0.6f to Color(0xFF6D28D9),
            1.0f to Color(0xFF5B21B6),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colorStops = gradientColors))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .align(Alignment.Center)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                DashboardDropdown(
                    title = selectedTitle,
                    items = items,
                    selectedKey = selectedKey,
                    onSelect = onSelectDashboard,
                    modifier = Modifier.weight(1f, fill = false),
                )
                ConnectionStatusDot(connectionStatus)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = if (darkTheme) "Light mode" else "Dark mode",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White.copy(alpha = 0.75f),
                    )
                }
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Log out",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusDot(status: ConnectionStatus) {
    val (color, label) = when (status) {
        ConnectionStatus.Checking -> Color(0xFFFFC107) to "Checking connection"
        ConnectionStatus.Connected -> Color(0xFF4CAF50) to "Connected"
        is ConnectionStatus.Disconnected -> Color(0xFFF44336) to "Disconnected: ${status.message}"
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
            .semantics { contentDescription = label },
    )
}

@Composable
private fun DashboardDropdown(
    title: String,
    items: List<DashboardSelectorItem>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val enabled = items.isNotEmpty()
    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = if (enabled) 0.18f else 0.10f),
            contentColor = Color.White,
            modifier = Modifier
                .widthIn(min = 120.dp)
                .clickable(enabled = enabled) { expanded = true },
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = if (expanded) "Close menu" else "Open menu",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White.copy(alpha = 0.85f),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item.title,
                            fontWeight = if (item.key == selectedKey) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSelect(item.key)
                        expanded = false
                    },
                )
            }
        }
    }
}
