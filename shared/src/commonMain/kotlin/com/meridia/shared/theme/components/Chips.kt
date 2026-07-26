package com.meridia.shared.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.PillShape

enum class MeridiaChipStyle {
    /** Lime highlight — demo `.chip`. */
    Highlight,

    /** Warning / deadline chip — demo `.chip.warn`. */
    Warn,
}

/**
 * A pill badge for short labels (savings, deadlines, tags). Demo `.chip`:
 * pill radius, 11.5px / 700 label, optional leading emoji.
 */
@Composable
fun MeridiaChip(
    text: String,
    modifier: Modifier = Modifier,
    style: MeridiaChipStyle = MeridiaChipStyle.Highlight,
    leadingEmoji: String? = null,
) {
    val colors = MeridiaTheme.colors
    val background = if (style == MeridiaChipStyle.Highlight) colors.lime else colors.warnBg
    val foreground = if (style == MeridiaChipStyle.Highlight) colors.verdeScuro else colors.warnFg
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingEmoji != null) {
            Text(text = leadingEmoji, style = MeridiaTheme.typography.labelMedium)
        }
        Text(text = text, color = foreground, style = MeridiaTheme.typography.labelMedium)
    }
}
