package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.auth.DEFAULT_DASHBOARD_KEY
import org.example.project.auth.HaEntityState
import org.example.project.auth.LovelaceConfig
import org.example.project.auth.LovelaceDashboard
import org.example.project.auth.LovelaceSection
import org.example.project.auth.LovelaceView
import org.example.project.cards.LovelaceCard

@Composable
fun LovelaceDashboardList(
    dashboards: List<LovelaceDashboard>,
    configs: Map<String, LovelaceConfig>,
    errors: Map<String, String>,
    entityStates: Map<String, HaEntityState>,
    darkTheme: Boolean,
    onToggleDarkMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = remember(dashboards, configs, errors) {
        buildDashboardEntries(dashboards, configs, errors)
    }

    var selectedKey by remember { mutableStateOf<String?>(null) }
    val effectiveSelectedKey = selectedKey
        ?.takeIf { key -> entries.any { it.key == key } }
        ?: entries.firstOrNull()?.key
    val selectedEntry = entries.firstOrNull { it.key == effectiveSelectedKey }

    var selectedViewIndex by remember(effectiveSelectedKey) { mutableStateOf(0) }
    val views = selectedEntry?.config?.views.orEmpty()
    val safeViewIndex = selectedViewIndex.coerceIn(0, (views.size - 1).coerceAtLeast(0))
    val selectedView = views.getOrNull(safeViewIndex)

    val items = remember(entries) {
        entries.map { DashboardSelectorItem(key = it.key, title = it.title) }
    }

    val bgModifier = if (darkTheme)
        Modifier.background(NeonGradients.ScreenBackground)
    else
        Modifier.background(MaterialTheme.colorScheme.background)

    Column(modifier = modifier.fillMaxSize().then(bgModifier)) {
        DashboardHeader(
            items = items,
            selectedKey = effectiveSelectedKey,
            onSelectDashboard = { selectedKey = it },
            darkTheme = darkTheme,
            onToggleDarkMode = onToggleDarkMode,
        )

        if (views.isNotEmpty()) {
            ViewTabs(
                views = views,
                selectedIndex = safeViewIndex,
                onSelect = { selectedViewIndex = it },
            )
        }

        when {
            entries.isEmpty() -> LoadingPlaceholder(modifier = Modifier.fillMaxWidth().padding(24.dp))
            selectedEntry == null -> Unit
            else -> ViewContent(
                entry = selectedEntry,
                view = selectedView,
                entityStates = entityStates,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ViewTabs(
    views: List<LovelaceView>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 12.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        views.forEachIndexed { index, view ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                text = {
                    Text(
                        text = view.tabLabel(index),
                        fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
            )
        }
    }
}

@Composable
private fun ViewContent(
    entry: DashboardEntry,
    view: LovelaceView?,
    entityStates: Map<String, HaEntityState>,
    modifier: Modifier = Modifier,
) {
    if (entry.error != null) {
        EmptyState(
            title = "Could not load dashboard",
            message = entry.error,
            modifier = modifier,
        )
        return
    }
    val config = entry.config
    if (config == null) {
        EmptyState(
            title = "Loading…",
            message = "Fetching configuration",
            modifier = modifier,
            showSpinner = true,
        )
        return
    }
    if (config.views.isEmpty()) {
        val strategy = config.strategy
        EmptyState(
            title = strategy?.let { "Strategy dashboard" } ?: "No views",
            message = strategy?.let { "type: ${it.type}" }
                ?: "This dashboard has no configured views.",
            modifier = modifier,
        )
        return
    }
    if (view == null) {
        EmptyState(title = "No view", message = "Select a view above.", modifier = modifier)
        return
    }
    LazyView(view = view, entityStates = entityStates, modifier = modifier)
}

@Composable
private fun LazyView(
    view: LovelaceView,
    entityStates: Map<String, HaEntityState>,
    modifier: Modifier = Modifier,
) {
    val rootCards = view.cards
    val sections = view.sections

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    ) {
        view.strategy?.let { strategy ->
            item(key = "strategy") {
                StrategyBanner(strategyType = strategy.type)
            }
        }

        if (rootCards.isEmpty() && sections.isEmpty() && view.strategy == null) {
            item(key = "empty") {
                EmptyState(
                    title = "No cards",
                    message = "This view has no cards configured.",
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                )
            }
        }

        items(rootCards.size, key = { "root-card-$it" }) { index ->
            LovelaceCard(card = rootCards[index], entityStates = entityStates)
        }

        sections.forEachIndexed { sectionIndex, section ->
            item(key = "section-$sectionIndex") {
                SectionHeader(section = section, index = sectionIndex)
            }
            items(section.cards.size, key = { "section-$sectionIndex-card-$it" }) { cardIndex ->
                LovelaceCard(card = section.cards[cardIndex], entityStates = entityStates)
            }
        }
    }
}

@Composable
private fun SectionHeader(section: LovelaceSection, index: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = section.title ?: "Section ${index + 1}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "${section.cards.size} card${if (section.cards.size == 1) "" else "s"}" +
                (section.type?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StrategyBanner(strategyType: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Strategy view",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = "type: $strategyType — cards generated by Home Assistant.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    showSpinner: Boolean = false,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            if (showSpinner) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoadingPlaceholder(modifier: Modifier = Modifier) {
    EmptyState(
        title = "Loading dashboards…",
        message = "Fetching from Home Assistant",
        showSpinner = true,
        modifier = modifier,
    )
}

internal data class DashboardEntry(
    val key: String,
    val title: String,
    val icon: String?,
    val mode: String?,
    val urlPath: String?,
    val config: LovelaceConfig?,
    val error: String?,
)

internal fun buildDashboardEntries(
    dashboards: List<LovelaceDashboard>,
    configs: Map<String, LovelaceConfig>,
    errors: Map<String, String>,
): List<DashboardEntry> {
    val entries = mutableListOf<DashboardEntry>()
    val defaultConfig = configs[DEFAULT_DASHBOARD_KEY]
    val defaultError = errors[DEFAULT_DASHBOARD_KEY]
    val defaultDashboard = dashboards.firstOrNull { it.urlPath == null || it.urlPath == DEFAULT_DASHBOARD_KEY }
    if (defaultConfig != null || defaultError != null || defaultDashboard != null) {
        entries += DashboardEntry(
            key = DEFAULT_DASHBOARD_KEY,
            title = defaultDashboard?.title?.ifBlank { null } ?: defaultConfig?.title ?: "Overview",
            icon = defaultDashboard?.icon,
            mode = defaultDashboard?.mode ?: "default",
            urlPath = null,
            config = defaultConfig,
            error = defaultError,
        )
    }
    for (dashboard in dashboards) {
        val key = dashboard.urlPath ?: dashboard.id
        if (entries.any { it.key == key }) continue
        entries += DashboardEntry(
            key = key,
            title = dashboard.title.ifBlank { dashboard.urlPath ?: dashboard.id },
            icon = dashboard.icon,
            mode = dashboard.mode,
            urlPath = dashboard.urlPath,
            config = dashboard.urlPath?.let(configs::get),
            error = dashboard.urlPath?.let(errors::get),
        )
    }
    return entries
}

private fun LovelaceView.tabLabel(index: Int): String =
    title?.takeIf { it.isNotBlank() }
        ?: path?.takeIf { it.isNotBlank() }
        ?: "View ${index + 1}"
