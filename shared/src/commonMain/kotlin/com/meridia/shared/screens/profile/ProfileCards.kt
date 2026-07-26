package com.meridia.shared.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.EyebrowLabel
import com.meridia.shared.theme.components.MeridiaButton
import com.meridia.shared.theme.components.MeridiaButtonStyle
import com.meridia.shared.theme.components.MeridiaCard

/** Studio contact card (moved here to keep ProfileScreen within the size limit). */
@Composable
fun StudioCard() {
    val colors = MeridiaTheme.colors
    MeridiaCard {
        EyebrowLabel("Studio")
        Spacer(Modifier.height(8.dp))
        Text(
            "Studio Meridia — Via dei Gelsomini 12, Pachino (SR)\nLun-Ven 8:30-19:00 · ☎ 0931 000000",
            style = MeridiaTheme.typography.bodyMedium,
            color = colors.grigio,
        )
    }
}

/**
 * GDPR self-service (DEV-093): export all of my data, or erase my account.
 * Erasure is guarded behind an explicit in-card confirmation; the caller wires
 * [onDelete] to the backend call and the subsequent logout.
 */
@Composable
fun ProfilePrivacyCard(
    busy: Boolean,
    notice: String?,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MeridiaTheme.colors
    var confirming by remember { mutableStateOf(false) }
    MeridiaCard {
        EyebrowLabel("Privacy e dati")
        Spacer(Modifier.height(8.dp))
        Text(
            "Scarica tutti i tuoi dati o elimina definitivamente il tuo account. " +
                "L'eliminazione è irreversibile.",
            style = MeridiaTheme.typography.bodyMedium,
            color = colors.grigio,
        )
        notice?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MeridiaTheme.typography.bodyMedium, color = colors.verde)
        }
        Spacer(Modifier.height(12.dp))
        MeridiaButton(
            if (busy) "Attendi…" else "Esporta i miei dati",
            onClick = onExport,
            enabled = !busy,
            style = MeridiaButtonStyle.Ghost,
        )
        Spacer(Modifier.height(8.dp))
        if (!confirming) {
            MeridiaButton(
                "Elimina account",
                onClick = { confirming = true },
                enabled = !busy,
                style = MeridiaButtonStyle.Ghost,
            )
        } else {
            Text(
                "Vuoi eliminare definitivamente l'account e tutti i dati?",
                style = MeridiaTheme.typography.bodyMedium,
                color = colors.inchiostro,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeridiaButton(
                    if (busy) "Attendi…" else "Conferma eliminazione",
                    onClick = onDelete,
                    enabled = !busy,
                    modifier = Modifier.widthIn(max = 200.dp),
                )
                MeridiaButton(
                    "Annulla",
                    onClick = { confirming = false },
                    enabled = !busy,
                    style = MeridiaButtonStyle.Ghost,
                    modifier = Modifier.widthIn(max = 140.dp),
                )
            }
        }
    }
}
