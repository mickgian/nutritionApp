package com.meridia.shared.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.EyebrowLabel
import com.meridia.shared.theme.components.MeridiaButton
import com.meridia.shared.theme.components.MeridiaCard

/**
 * The Consulenza tab entry. Minimal for DEV-022 (nav entry + booking CTA); the
 * full professional profile and reviews arrive in DEV-023.
 */
@Composable
fun ConsulenzaScreen(onBook: () -> Unit, onLogout: () -> Unit) {
    val colors = MeridiaTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.panna)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Meridia", style = MeridiaTheme.typography.titleLarge, color = colors.verde)
            TextButton(onClick = onLogout) { Text("Esci", color = colors.verde) }
        }
        Spacer(Modifier.height(24.dp))
        EyebrowLabel("Consulenza")
        Text(
            "Prenota la tua visita nutrizionale",
            style = MeridiaTheme.typography.displaySmall,
            color = colors.inchiostro,
        )
        Spacer(Modifier.height(14.dp))
        MeridiaCard {
            Text("Dott.ssa Anna Serra", style = MeridiaTheme.typography.titleMedium, color = colors.inchiostro)
            Text("Biologa Nutrizionista", style = MeridiaTheme.typography.bodyMedium, color = colors.grigio)
            Spacer(Modifier.height(8.dp))
            Text(
                "Prima visita €90 · Visita di controllo €50",
                style = MeridiaTheme.typography.bodyMedium,
                color = colors.verde,
            )
        }
        Spacer(Modifier.height(8.dp))
        MeridiaButton("Prenota una visita", onClick = onBook)
    }
}
