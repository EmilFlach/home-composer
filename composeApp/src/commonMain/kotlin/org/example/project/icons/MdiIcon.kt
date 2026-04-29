package org.example.project.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import home_composer.composeapp.generated.resources.Res
import home_composer.composeapp.generated.resources.materialdesignicons_webfont
import org.jetbrains.compose.resources.Font

internal sealed interface HaIcon {
    data class FontIcon(val codepoint: Int) : HaIcon
    data class VectorIcon(val vector: ImageVector) : HaIcon
}

@Composable
internal fun rememberMdiFontFamily(): FontFamily {
    val font = Font(Res.font.materialdesignicons_webfont)
    return remember(font) { FontFamily(font) }
}

@Composable
internal fun MdiIcon(
    icon: HaIcon,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
) {
    when (icon) {
        is HaIcon.FontIcon -> {
            val family = rememberMdiFontFamily()
            Text(
                text = surrogateString(icon.codepoint),
                fontFamily = family,
                fontSize = with(LocalDensity.current) { size.toSp() },
                color = tint,
                modifier = modifier,
                maxLines = 1,
            )
        }
        is HaIcon.VectorIcon -> Icon(
            imageVector = icon.vector,
            contentDescription = null,
            tint = tint,
            modifier = modifier,
        )
    }
}

/** Convert a supplementary Unicode codepoint (> U+FFFF) to a two-char surrogate-pair String. */
private fun surrogateString(codepoint: Int): String {
    val offset = codepoint - 0x10000
    val high = (0xD800 + (offset ushr 10)).toChar()
    val low = (0xDC00 + (offset and 0x3FF)).toChar()
    return charArrayOf(high, low).concatToString()
}

/** Look up an MDI icon by bare name (no "mdi:" prefix). Falls back to a help icon if unknown. */
internal fun mdiIconByName(name: String): HaIcon {
    val codepoint = MDI_CODEPOINTS[name]
    return if (codepoint != null) HaIcon.FontIcon(codepoint)
    else HaIcon.VectorIcon(Icons.AutoMirrored.Filled.HelpOutline)
}

/** Resolve an "mdi:name" string to an HaIcon, using [fallback] when the string is null or unknown. */
internal fun mdiStringToHaIcon(mdiString: String?, fallback: HaIcon): HaIcon {
    val name = mdiString?.removePrefix("mdi:") ?: return fallback
    val codepoint = MDI_CODEPOINTS[name]
    return if (codepoint != null) HaIcon.FontIcon(codepoint) else fallback
}
