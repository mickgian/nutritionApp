package com.meridia.shared.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meridia.shared.models.AdminClientDto
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.EyebrowLabel
import com.meridia.shared.theme.components.MeridiaCard
import com.meridia.shared.viewModels.AdminUiState

private data class PlanPreset(val name: String, val kcal: Int, val weeks: Int)

private val PRESET_PLANS = listOf(
    PlanPreset("Dimagrimento 1400 kcal", 1400, 4),
    PlanPreset("Mediterranea 1800 kcal", 1800, 4),
    PlanPreset("Sportivo 2200 kcal", 2200, 4),
)

/** Studio client directory with plan assignment (DEV-080 · consumes DEV-030). */
@Composable
fun AdminClientsPanel(
    content: AdminUiState.Content,
    onAssign: (clientId: Int, name: String, kcal: Int, weeks: Int) -> Unit,
) {
    val colors = MeridiaTheme.colors
    MeridiaCard {
        EyebrowLabel("Clienti · Assegna piano")
        Spacer(Modifier.height(8.dp))
        if (content.clients.isEmpty()) {
            Text(
                "Nessun cliente registrato.",
                style = MeridiaTheme.typography.bodyMedium,
                color = colors.grigio,
            )
            return@MeridiaCard
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            content.clients.forEach { client ->
                ClientRow(
                    client = client,
                    assigning = content.assigningClientId == client.id,
                    onAssign = onAssign,
                )
            }
        }
    }
}

@Composable
private fun ClientRow(
    client: AdminClientDto,
    assigning: Boolean,
    onAssign: (Int, String, Int, Int) -> Unit,
) {
    val colors = MeridiaTheme.colors
    Column {
        Text(
            client.fullName,
            style = MeridiaTheme.typography.bodyLarge,
            color = colors.inchiostro,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Piano attuale: ${client.activePlan ?: "nessuno"}",
            style = MeridiaTheme.typography.bodyMedium,
            color = colors.grigio,
        )
        Spacer(Modifier.height(6.dp))
        if (assigning) {
            Box(Modifier.fillMaxWidth().height(36.dp), Alignment.CenterStart) {
                CircularProgressIndicator(color = colors.verde, modifier = Modifier.size(18.dp))
            }
        } else {
            PRESET_PLANS.forEach { preset ->
                TextButton(onClick = { onAssign(client.id, preset.name, preset.kcal, preset.weeks) }) {
                    Text("Assegna «${preset.name}»", color = colors.verde)
                }
            }
        }
    }
}
