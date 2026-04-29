package org.example.project

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val LocalWindowDecorationInsets = compositionLocalOf { PaddingValues(0.dp) }

val LocalHeaderDragModifier = compositionLocalOf<Modifier> { Modifier }
