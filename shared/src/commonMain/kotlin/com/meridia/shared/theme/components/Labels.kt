package com.meridia.shared.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.PillShape

/**
 * Small uppercase kicker above a section title — demo `.eyebrow`
 * (11px / 700 / .14em tracking, forest green at 75% opacity).
 */
@Composable
fun EyebrowLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = MeridiaTheme.colors.verde.copy(alpha = 0.75f),
        style = MeridiaTheme.typography.labelSmall,
    )
}

/**
 * A single macronutrient pill (e.g. "P 32g") — demo `.macro span`:
 * sage fill, forest text, 11px / 700, pill radius.
 */
@Composable
fun MacroBadge(text: String, modifier: Modifier = Modifier) {
    val colors = MeridiaTheme.colors
    Text(
        text = text,
        modifier = modifier
            .clip(PillShape)
            .background(colors.salvia)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = colors.verde,
        style = MeridiaTheme.typography.labelMedium,
    )
}
