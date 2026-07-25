package com.meridia.shared.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meridia.shared.theme.components.EyebrowLabel
import com.meridia.shared.theme.components.MacroBadge
import com.meridia.shared.theme.components.MeridiaButton
import com.meridia.shared.theme.components.MeridiaButtonStyle
import com.meridia.shared.theme.components.MeridiaCard
import com.meridia.shared.theme.components.MeridiaChip
import com.meridia.shared.theme.components.MeridiaChipStyle

/**
 * A living gallery of the design-system primitives. Not a shipped screen — host it in
 * a platform preview or a scratch route to eyeball the theme against the HTML demo on
 * each target. Wrap in [MeridiaTheme] at the call site.
 */
@Composable
fun MeridiaComponentGallery(modifier: Modifier = Modifier) {
    val colors = MeridiaTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.panna)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EyebrowLabel("Studio Meridia")
        Text("Il tuo piano", style = MeridiaTheme.typography.displaySmall, color = colors.inchiostro)

        MeridiaCard {
            Text("Box settimanale", style = MeridiaTheme.typography.titleMedium, color = colors.inchiostro)
            Text(
                "14 pasti pronti, ritiro il lunedì.",
                style = MeridiaTheme.typography.bodyMedium,
                color = colors.grigio,
            )
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MacroBadge("P 32g")
                MacroBadge("C 45g")
                MacroBadge("G 12g")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeridiaChip("Risparmi 10€")
            MeridiaChip("Scade giovedì", style = MeridiaChipStyle.Warn, leadingEmoji = "⏰")
        }

        MeridiaButton("Ordina il box", onClick = {})
        MeridiaButton("Vedi il menù", onClick = {}, style = MeridiaButtonStyle.Ghost)
        MeridiaButton("Non disponibile", onClick = {}, enabled = false)
    }
}
