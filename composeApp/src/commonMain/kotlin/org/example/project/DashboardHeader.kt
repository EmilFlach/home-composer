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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
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
    onOpenSettings: () -> Unit = {},
    defaultDashboardKey: String? = null,
    modifier: Modifier = Modifier,
) {
    val selectedTitle = items.firstOrNull { it.key == selectedKey }?.title
        ?: items.firstOrNull()?.title
        ?: "Dashboards"

    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surface
    val gradientColors = if (darkTheme) {
        arrayOf(
            0.0f to primaryContainer,
            0.6f to surface,
            1.0f to surface,
        )
    } else {
        // In light mode the controls are white-on-something, so they must sit on
        // the saturated primary band — not the near-white surface tail. Push the
        // transition to the bottom of the header so the surface only appears as
        // a thin trailing strip below the controls.
        arrayOf(
            0.0f to primary,
            0.65f to primary,
            1.0f to surface,
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
                    defaultKey = defaultDashboardKey,
                    onSelect = onSelectDashboard,
                    modifier = Modifier.weight(1f, fill = false),
                )
                ConnectionStatusDot(connectionStatus)
            }
            Box {
                var menuExpanded by remember { mutableStateOf(false) }
                val menuContainerColor = if (darkTheme) primaryContainer else primary
                val menuIconTint = Color.White.copy(alpha = 0.85f)
                val itemPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                IconButton(
                    onClick = { menuExpanded = !menuExpanded },
                    modifier = Modifier.size(54.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = if (menuExpanded) "Close menu" else "Open menu",
                        modifier = Modifier.size(30.dp),
                        tint = Color.White.copy(alpha = 0.75f),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.width(48.dp),
                    containerColor = menuContainerColor,
                    tonalElevation = 0.dp,
                ) {
                    DropdownMenuItem(
                        text = {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Settings, contentDescription = "Settings", modifier = Modifier.size(20.dp), tint = menuIconTint)
                            }
                        },
                        onClick = { onOpenSettings(); menuExpanded = false },
                        contentPadding = itemPadding,
                        colors = MenuDefaults.itemColors(textColor = Color.White),
                    )
                    DropdownMenuItem(
                        text = {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = if (darkTheme) "Light mode" else "Dark mode",
                                    modifier = Modifier.size(20.dp),
                                    tint = menuIconTint,
                                )
                            }
                        },
                        onClick = { onToggleDarkMode() },
                        contentPadding = itemPadding,
                        colors = MenuDefaults.itemColors(textColor = Color.White),
                    )
                    DropdownMenuItem(
                        text = {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out", modifier = Modifier.size(20.dp), tint = menuIconTint)
                            }
                        },
                        onClick = { onLogout(); menuExpanded = false },
                        contentPadding = itemPadding,
                        colors = MenuDefaults.itemColors(textColor = Color.White),
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
    defaultKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val enabled = items.isNotEmpty()
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            enabled = enabled,
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = if (enabled) 0.18f else 0.10f),
            contentColor = Color.White,
            modifier = Modifier.widthIn(min = 120.dp),
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
                    trailingIcon = if (item.key == defaultKey) ({
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Default dashboard",
                            modifier = Modifier.size(16.dp),
                        )
                    }) else null,
                    onClick = {
                        onSelect(item.key)
                        expanded = false
                    },
                )
            }
        }
    }
}
