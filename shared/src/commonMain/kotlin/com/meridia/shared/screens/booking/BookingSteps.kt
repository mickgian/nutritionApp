package com.meridia.shared.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.meridia.shared.models.VisitTypeUi
import com.meridia.shared.screens.payment.PaymentPanel
import com.meridia.shared.theme.MeridiaRadius
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.EyebrowLabel
import com.meridia.shared.theme.components.MeridiaCard
import com.meridia.shared.viewModels.BookingUiState
import com.meridia.shared.viewModels.BookingViewModel

@Composable
private fun SelectableRow(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MeridiaTheme.colors
    val shape = RoundedCornerShape(MeridiaRadius.Button)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.salvia else colors.bianco)
            .border(1.5.dp, if (selected) colors.verde else colors.linea, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        content = content,
    )
}

@Composable
fun SlotStep(content: BookingUiState.Content, vm: BookingViewModel) {
    val colors = MeridiaTheme.colors
    EyebrowLabel("Scegli data e ora")
    Spacer(Modifier.height(8.dp))
    content.slots.forEach { slot ->
        SelectableRow(
            selected = slot.id == content.selectedSlot?.id,
            onClick = { vm.selectSlot(slot) },
        ) {
            Text(slotDateTime(slot.startsAt), style = MeridiaTheme.typography.titleMedium, color = colors.inchiostro)
            Text("${slot.durationMin} min", style = MeridiaTheme.typography.bodySmall, color = colors.grigio)
        }
        Spacer(Modifier.height(8.dp))
    }
    Spacer(Modifier.height(8.dp))
    EyebrowLabel("Tipo di visita")
    Spacer(Modifier.height(8.dp))
    VisitTypeUi.entries.forEach { visitType ->
        SelectableRow(
            selected = visitType == content.visitType,
            onClick = { vm.setVisitType(visitType) },
        ) {
            Text(visitType.label, style = MeridiaTheme.typography.titleMedium, color = colors.inchiostro)
            Text(euro(visitType.priceCents), style = MeridiaTheme.typography.bodyMedium, color = colors.verde)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun RiepilogoStep(content: BookingUiState.Content) {
    val colors = MeridiaTheme.colors
    EyebrowLabel("Riepilogo")
    Spacer(Modifier.height(8.dp))
    MeridiaCard {
        LabeledRow("Visita", content.visitType.label)
        LabeledRow("Data e ora", slotDateTime(content.selectedSlot?.startsAt ?: "—"))
        LabeledRow("Prezzo", euro(content.visitType.priceCents))
    }
    Text(
        "Puoi spostare o disdire gratuitamente fino a 48 ore prima dell'appuntamento.",
        style = MeridiaTheme.typography.bodyMedium,
        color = colors.grigio,
    )
}

@Composable
fun PagamentoStep(content: BookingUiState.Content, vm: BookingViewModel) {
    PaymentPanel(
        amountCents = content.visitType.priceCents,
        processing = content.submitting,
        error = content.submitError,
        onPay = vm::pay,
    )
}
