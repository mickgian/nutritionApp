package com.meridia.shared.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.meridia.shared.theme.MeridiaShapes
import com.meridia.shared.theme.MeridiaTheme

/**
 * The standard content surface — demo `.card`: white fill, 1px hairline border,
 * large (22px) radius, 18px padding. Children are laid out in a [ColumnScope].
 */
@Composable
fun MeridiaCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MeridiaTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MeridiaShapes.large)
            .background(colors.bianco)
            .border(1.dp, colors.linea, MeridiaShapes.large)
            .padding(18.dp),
        content = content,
    )
}
