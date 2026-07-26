package com.meridia.shared.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meridia.shared.models.AdminSlotDto
import com.meridia.shared.screens.booking.slotDateTime
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.EyebrowLabel
import com.meridia.shared.theme.components.MeridiaCard
import com.meridia.shared.viewModels.AdminAvailabilityUiState
import com.meridia.shared.viewModels.AdminAvailabilityViewModel

/** Studio availability: open/close the upcoming booking slots (DEV-080 · DEV-020). */
@Composable
fun AdminAvailabilityPanel(vm: AdminAvailabilityViewModel) {
    val state by vm.state.collectAsState()
    val colors = MeridiaTheme.colors
    MeridiaCard {
        EyebrowLabel("Disponibilità")
        Text(
            "Tocca uno slot per aprirlo o chiuderlo alla prenotazione.",
            style = MeridiaTheme.typography.bodyMedium,
            color = colors.grigio,
        )
        Spacer(Modifier.height(12.dp))
        when (val s = state) {
            AdminAvailabilityUiState.Loading ->
                Box(Modifier.fillMaxWidth().height(60.dp), Alignment.Center) {
                    CircularProgressIndicator(color = colors.verde)
                }
            is AdminAvailabilityUiState.Error ->
                Text(s.message, style = MeridiaTheme.typography.bodyMedium, color = colors.arancio)
            is AdminAvailabilityUiState.Content -> AvailabilityContent(s, vm)
        }
    }
}

@Composable
private fun AvailabilityContent(state: AdminAvailabilityUiState.Content, vm: AdminAvailabilityViewModel) {
    val colors = MeridiaTheme.colors
    if (state.slots.isEmpty()) {
        Text(
            "Nessuno slot nelle prossime settimane.",
            style = MeridiaTheme.typography.bodyMedium,
            color = colors.grigio,
        )
        return
    }
    state.error?.let {
        Text(it, style = MeridiaTheme.typography.bodyMedium, color = colors.arancio)
        Spacer(Modifier.height(8.dp))
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        state.slots.forEach { slot -> SlotRow(slot, state.togglingSlotId == slot.id, vm) }
    }
}

@Composable
private fun SlotRow(slot: AdminSlotDto, toggling: Boolean, vm: AdminAvailabilityViewModel) {
    val colors = MeridiaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            slotDateTime(slot.startsAt),
            style = MeridiaTheme.typography.bodyMedium,
            color = colors.inchiostro,
        )
        if (toggling) {
            CircularProgressIndicator(color = colors.verde, modifier = Modifier.size(18.dp))
        } else {
            TextButton(onClick = { vm.toggle(slot) }) {
                Text(
                    if (slot.isOpen) "Aperto" else "Chiuso",
                    color = if (slot.isOpen) colors.verde else colors.grigio,
                )
            }
        }
    }
}
