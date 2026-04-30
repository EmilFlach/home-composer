package org.example.project.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
private val CardRadius = 16.dp
private val InnerRadius = 4.dp

internal val LocalCardShape = compositionLocalOf<Shape> { RoundedCornerShape(CardRadius) }

internal enum class CardGroupPosition { ONLY, FIRST, MIDDLE, LAST }

internal val LocalCardGroupPosition = compositionLocalOf { CardGroupPosition.ONLY }

private fun cardGroupShape(index: Int, size: Int): Shape = when {
    index == 0 && size == 1 -> RoundedCornerShape(CardRadius)
    index == 0 -> RoundedCornerShape(
        topStart = CardRadius,
        topEnd = CardRadius,
        bottomStart = InnerRadius,
        bottomEnd = InnerRadius,
    )
    index == size - 1 -> RoundedCornerShape(
        topStart = InnerRadius,
        topEnd = InnerRadius,
        bottomStart = CardRadius,
        bottomEnd = CardRadius,
    )
    else -> RoundedCornerShape(InnerRadius)
}

@Composable
internal fun VerticalStackCard(
    config: LovelaceCardConfig,
    modifier: Modifier = Modifier,
) {
    val rawCards = (config.raw?.get("cards") as? JsonArray) ?: return
    if (rawCards.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        rawCards.forEachIndexed { index, cardElement ->
            val position = when {
                rawCards.size == 1 -> CardGroupPosition.ONLY
                index == 0 -> CardGroupPosition.FIRST
                index == rawCards.lastIndex -> CardGroupPosition.LAST
                else -> CardGroupPosition.MIDDLE
            }
            val shape = cardGroupShape(index, rawCards.size)
            CompositionLocalProvider(
                LocalCardShape provides shape,
                LocalCardGroupPosition provides position,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    LovelaceCard(card = cardElement)
                }
            }
        }
    }
}
