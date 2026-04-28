package org.example.project.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun AlarmPanelCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        config.entity?.let { StubLine("entity", it) }
    }
}

@Composable
internal fun AreaCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        val area = (config.raw?.get("area"))?.toString()?.trim('"')
        if (area != null) StubLine("area", area)
    }
}

@Composable
internal fun ButtonCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        config.entity?.let { StubLine("entity", it) }
    }
}

@Composable
internal fun ConditionalCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        if (config.cardCount > 0) {
            StubLine("nested", "${config.cardCount} card${if (config.cardCount == 1) "" else "s"}")
        }
    }
}

@Composable
internal fun EntitiesCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        if (config.entities.isNotEmpty()) {
            val preview = config.entities.take(4).joinToString(", ")
            val more = if (config.entities.size > 4) " (+${config.entities.size - 4} more)" else ""
            StubLine("entities (${config.entities.size})", preview + more)
        }
    }
}

@Composable
internal fun GaugeCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        config.entity?.let { StubLine("entity", it) }
    }
}

@Composable
internal fun GlanceCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        if (config.entities.isNotEmpty()) {
            StubLine("entities", "${config.entities.size}")
        }
    }
}

@Composable
internal fun GridCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        if (config.cardCount > 0) {
            StubLine("nested", "${config.cardCount} card${if (config.cardCount == 1) "" else "s"}")
        }
    }
}

@Composable
internal fun HistoryGraphCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        if (config.entities.isNotEmpty()) {
            StubLine("entities", "${config.entities.size}")
        }
    }
}

@Composable
internal fun HorizontalStackCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        if (config.cardCount > 0) {
            StubLine("nested", "${config.cardCount} card${if (config.cardCount == 1) "" else "s"}")
        }
    }
}

@Composable
internal fun IframeCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        val url = (config.raw?.get("url"))?.toString()?.trim('"')
        if (url != null) StubLine("url", url)
    }
}

@Composable
internal fun LightCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        config.entity?.let { StubLine("entity", it) }
    }
}

@Composable
internal fun LogbookCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        if (config.entities.isNotEmpty()) {
            StubLine("entities", "${config.entities.size}")
        }
    }
}

@Composable
internal fun MapCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        if (config.entities.isNotEmpty()) {
            StubLine("entities", "${config.entities.size}")
        }
    }
}

@Composable
internal fun MarkdownCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier)
}

@Composable
internal fun MediaControlCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        config.entity?.let { StubLine("entity", it) }
    }
}

@Composable
internal fun PictureElementsCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier)
}

@Composable
internal fun PictureGlanceCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        if (config.entities.isNotEmpty()) {
            StubLine("entities", "${config.entities.size}")
        }
    }
}

@Composable
internal fun PlantStatusCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        config.entity?.let { StubLine("entity", it) }
    }
}

@Composable
internal fun EntityCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        config.entity?.let { StubLine("entity", it) }
    }
}

@Composable
internal fun ShoppingListCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier)
}

@Composable
internal fun StatisticCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        config.entity?.let { StubLine("entity", it) }
    }
}

@Composable
internal fun StatisticsGraphCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        if (config.entities.isNotEmpty()) {
            StubLine("entities", "${config.entities.size}")
        }
    }
}

@Composable
internal fun ThermostatCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        config.entity?.let { StubLine("entity", it) }
    }
}

@Composable
internal fun TodoListCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        config.entity?.let { StubLine("entity", it) }
    }
}

@Composable
internal fun VerticalStackCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        if (config.cardCount > 0) {
            StubLine("nested", "${config.cardCount} card${if (config.cardCount == 1) "" else "s"}")
        }
    }
}

@Composable
internal fun WeatherForecastCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        config.entity?.let { StubLine("entity", it) }
    }
}

@Composable
internal fun UnknownCardStub(config: LovelaceCardConfig, modifier: Modifier = Modifier) {
    StubScaffold(config, modifier) {
        config.entity?.let { StubLine("entity", it) }
        if (config.entities.isNotEmpty()) {
            StubLine("entities", "${config.entities.size}")
        }
    }
}
