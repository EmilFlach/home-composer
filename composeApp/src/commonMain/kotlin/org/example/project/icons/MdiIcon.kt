package org.example.project.icons

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import home_composer.composeapp.generated.resources.Res
import home_composer.composeapp.generated.resources.materialdesignicons_webfont
import org.jetbrains.compose.resources.Font

internal data class HaIcon(val codepoint: Int)

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
    val family = rememberMdiFontFamily()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        val sp = with(LocalDensity.current) { (size * 0.9f).toSp() }
        Text(
            text = surrogateString(icon.codepoint),
            fontFamily = family,
            fontSize = sp,
            lineHeight = sp,
            style = TextStyle(
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                )
            ),
            color = tint,
            maxLines = 1,
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

private val FALLBACK_ICON = HaIcon(MDI_CODEPOINTS["help-circle-outline"]!!)

/** Look up an MDI icon by bare name (no "mdi:" prefix). Falls back to help-circle-outline if unknown. */
internal fun mdiIconByName(name: String): HaIcon {
    val codepoint = MDI_CODEPOINTS[name]
    return if (codepoint != null) HaIcon(codepoint) else FALLBACK_ICON
}

/** Resolve an "mdi:name" string to an HaIcon, using [fallback] when the string is null or unknown. */
internal fun mdiStringToHaIcon(mdiString: String?, fallback: HaIcon): HaIcon {
    val name = mdiString?.removePrefix("mdi:") ?: return fallback
    val codepoint = MDI_CODEPOINTS[name]
    return if (codepoint != null) HaIcon(codepoint) else fallback
}
