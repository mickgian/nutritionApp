package com.meridia.shared.theme.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meridia.shared.theme.MeridiaRadius
import com.meridia.shared.theme.MeridiaTheme

enum class MeridiaButtonStyle {
    /** Filled forest-green call to action — demo `.btn-primary`. */
    Primary,

    /** Tinted low-emphasis action on a sage surface — demo `.btn-ghost`. */
    Ghost,
}

/**
 * The primary full-width action button. Matches the demo `.btn` (radius 14, weight 700,
 * 15px label) with primary/ghost variants and a disabled state at 40% opacity.
 */
@Composable
fun MeridiaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MeridiaButtonStyle = MeridiaButtonStyle.Primary,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = MeridiaTheme.colors
    val container = if (style == MeridiaButtonStyle.Primary) colors.verde else colors.salvia
    val contentColor = if (style == MeridiaButtonStyle.Primary) colors.bianco else colors.verde
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(MeridiaRadius.Button),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = contentColor,
            disabledContainerColor = container.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.4f),
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MeridiaTheme.typography.labelLarge)
    }
}
