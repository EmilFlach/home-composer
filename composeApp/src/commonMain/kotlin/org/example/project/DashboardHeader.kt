package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DashboardHeader(
    locationName: String = "Home",
    outdoorTemp: Float? = null,
    darkTheme: Boolean,
    onToggleDarkMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF3A0080),
                        0.6f to Color(0xFF1A0040),
                        1.0f to Color(0xFF080010)
                    )
                )
            )
    ) {
        // Top bar: location pill + temp pill + theme toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderPill(text = locationName)
                if (outdoorTemp != null) {
                    val i = (outdoorTemp * 10).toInt()
                    HeaderPill(
                        text = "${i / 10}.${i % 10}°C",
                        icon = { Icon(Icons.Filled.Thermostat, null, Modifier.size(12.dp)) }
                    )
                }
            }
            IconButton(
                onClick = onToggleDarkMode,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = if (darkTheme) "Light mode" else "Dark mode",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // "Overview" title at bottom-left
        Text(
            text = "Overview",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 20.dp)
        )
    }
}

@Composable
private fun HeaderPill(
    text: String,
    icon: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.15f),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon?.invoke()
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}
