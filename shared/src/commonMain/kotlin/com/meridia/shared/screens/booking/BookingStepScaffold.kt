package com.meridia.shared.screens.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.MeridiaButton
import com.meridia.shared.viewModels.BookingStep
import com.meridia.shared.viewModels.BookingUiState
import com.meridia.shared.viewModels.BookingViewModel

@Composable
fun BookingStepBody(
    content: BookingUiState.Content,
    vm: BookingViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(18.dp)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (content.step) {
                BookingStep.Slot -> SlotStep(content, vm)
                BookingStep.Riepilogo -> RiepilogoStep(content)
                BookingStep.Pagamento -> PagamentoStep(content)
            }
        }
        Spacer(Modifier.height(12.dp))
        StepFooter(content, vm)
    }
}

@Composable
private fun StepFooter(content: BookingUiState.Content, vm: BookingViewModel) {
    when (content.step) {
        BookingStep.Slot -> MeridiaButton(
            "Continua",
            onClick = vm::next,
            enabled = content.selectedSlot != null,
        )

        BookingStep.Riepilogo -> MeridiaButton("Vai al pagamento", onClick = vm::next)

        BookingStep.Pagamento -> MeridiaButton(
            if (content.submitting) "Attendere…" else "Conferma e paga ${euro(content.visitType.priceCents)}",
            onClick = vm::confirm,
            enabled = !content.submitting,
        )
    }
}

/** A label/value line (e.g. "Prezzo" … "€90,00"). */
@Composable
fun LabeledRow(label: String, value: String) {
    val colors = MeridiaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MeridiaTheme.typography.bodyMedium, color = colors.grigio)
        Text(value, style = MeridiaTheme.typography.titleMedium, color = colors.inchiostro)
    }
}

/** Integer cents → Italian euro string, e.g. 9000 → "€90,00". */
fun euro(cents: Int): String {
    val euros = cents / 100
    val rest = (cents % 100).toString().padStart(2, '0')
    return "€$euros,$rest"
}

/** ISO instant "2026-07-28T10:00:00…" → "28/07/2026 · 10:00" (falls back to raw input). */
fun slotDateTime(iso: String): String {
    if (!iso.contains('T')) return iso
    val (datePart, timePart) = iso.split('T', limit = 2)
    val date = datePart.split('-')
    val time = timePart.take(5)
    return if (date.size == 3) "${date[2]}/${date[1]}/${date[0]} · $time" else "$datePart · $time"
}
