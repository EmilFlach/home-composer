package org.example.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import org.example.project.auth.ConnectionStatus
import org.example.project.auth.DEFAULT_DASHBOARD_KEY
import org.example.project.auth.HaEntityState
import org.example.project.auth.LovelaceConfig
import org.example.project.auth.LovelaceDashboard
import org.example.project.auth.LovelaceSection
import org.example.project.auth.LovelaceView
import org.example.project.cards.LovelaceCard
import org.example.project.cards.evaluateVisibility
import org.example.project.cards.parseVisibility

@Composable
fun LovelaceDashboardList(
    dashboards: List<LovelaceDashboard>,
    configs: Map<String, LovelaceConfig>,
    errors: Map<String, String>,
    entityStates: Map<String, HaEntityState>,
    connectionStatus: ConnectionStatus,
    darkTheme: Boolean,
    onToggleDarkMode: () -> Unit,
    onLogout: () -> Unit,
    defaultDashboardKey: String? = null,
    onSaveDefaultDashboard: (String?) -> Unit = {},
    currentSeedColor: androidx.compose.ui.graphics.Color? = null,
    onThemeChange: (androidx.compose.ui.graphics.Color) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val entries = remember(dashboards, configs, errors) {
        buildDashboardEntries(dashboards, configs, errors)
    }

    var selectedKey by remember { mutableStateOf<String?>(defaultDashboardKey) }
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

    // Show toast once when entries load and saved default key is not available
    var showUnavailableToast by remember { mutableStateOf(false) }
    var defaultChecked by remember { mutableStateOf(false) }
    LaunchedEffect(entries) {
        if (!defaultChecked && entries.isNotEmpty()) {
            defaultChecked = true
            if (defaultDashboardKey != null && entries.none { it.key == defaultDashboardKey }) {
                showUnavailableToast = true
            }
        }
    }
    LaunchedEffect(showUnavailableToast) {
        if (showUnavailableToast) {
            delay(5.seconds)
            showUnavailableToast = false
        }
    }

    var showSettings by remember { mutableStateOf(false) }

    val bgModifier = if (darkTheme)
        Modifier.background(NeonGradients.ScreenBackground)
    else
        Modifier.background(MaterialTheme.colorScheme.background)

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().then(bgModifier)) {
            DashboardHeader(
                items = items,
                selectedKey = effectiveSelectedKey,
                onSelectDashboard = { selectedKey = it },
                connectionStatus = connectionStatus,
                darkTheme = darkTheme,
                onToggleDarkMode = onToggleDarkMode,
                onLogout = onLogout,
                onOpenSettings = { showSettings = true },
                defaultDashboardKey = defaultDashboardKey,
            )

            if (views.size > 1) {
                ViewTabs(
                    views = views,
                    selectedIndex = safeViewIndex,
                    onSelect = { selectedViewIndex = it },
                    darkTheme = darkTheme,
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

        AnimatedVisibility(
            visible = showUnavailableToast,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Surface(
                color = Color(0xFFFF9800),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Selected dashboard is not available",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            items = items,
            currentDefaultKey = defaultDashboardKey,
            currentSeedColor = currentSeedColor,
            onSave = { key ->
                onSaveDefaultDashboard(key)
                showSettings = false
            },
            onThemeChange = onThemeChange,
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun ViewTabs(
    views: List<LovelaceView>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    darkTheme: Boolean = false,
) {
    val tabContainerColor = if (darkTheme) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
    val tabContentColor   = if (darkTheme) MaterialTheme.colorScheme.primary  else MaterialTheme.colorScheme.onPrimary
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 12.dp,
        containerColor = tabContainerColor,
        contentColor = tabContentColor,
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
    val listState = rememberLazyListState()
    val statesLoaded = entityStates.isNotEmpty()

    LaunchedEffect(statesLoaded) {
        if (statesLoaded) listState.scrollToItem(0)
    }

    LazyColumn(
        state = listState,
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

        val maxColumns = view.maxColumns
        if (sections.isNotEmpty() && maxColumns != null) {
            item(key = "sections-grid") {
                SectionsGrid(
                    sections = sections,
                    entityStates = entityStates,
                    maxColumns = maxColumns,
                )
            }
        } else {
            sections.forEachIndexed { sectionIndex, section ->
                val isVisible = evaluateVisibility(parseVisibility(section.visibility), entityStates)
                item(key = "section-$sectionIndex") {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column {
                            SectionContent(section = section, entityStates = entityStates)
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionsGrid(
    sections: List<LovelaceSection>,
    entityStates: Map<String, HaEntityState>,
    maxColumns: Int,
    modifier: Modifier = Modifier,
) {
    if (sections.isEmpty()) return

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val effectiveCols = when {
            maxWidth < 600.dp -> 1
            maxWidth < 1200.dp -> minOf(maxColumns, 2)
            else -> maxColumns
        }.coerceAtLeast(1)

        val gap = 20.dp
        val maxContentWidth = 400.dp * effectiveCols + gap * (effectiveCols - 1)

        if (effectiveCols <= 1) {
            Column(
                modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sections.forEach { section ->
                    val isVisible = evaluateVisibility(parseVisibility(section.visibility), entityStates)
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        SectionContent(section = section, entityStates = entityStates)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.Top,
            ) {
                for (colIndex in 0 until effectiveCols) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        sections
                            .filterIndexed { idx, _ -> idx % effectiveCols == colIndex }
                            .forEach { section ->
                                val isVisible = evaluateVisibility(parseVisibility(section.visibility), entityStates)
                                AnimatedVisibility(
                                    visible = isVisible,
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    SectionContent(section = section, entityStates = entityStates)
                                }
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionContent(
    section: LovelaceSection,
    entityStates: Map<String, HaEntityState>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        section.title?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
        section.cards.forEach { card ->
            LovelaceCard(card = card, entityStates = entityStates)
        }
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
