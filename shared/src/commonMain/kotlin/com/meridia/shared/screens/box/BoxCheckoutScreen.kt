package com.meridia.shared.screens.box

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meridia.shared.screens.payment.PaymentPanel
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.EyebrowLabel
import com.meridia.shared.theme.components.MeridiaButton
import com.meridia.shared.viewModels.BoxCheckoutState
import com.meridia.shared.viewModels.BoxCheckoutViewModel
import com.meridia.shared.viewModels.OrderFormula
import com.meridia.shared.viewModels.PickupSlot

/**
 * The box checkout sheet: pick a formula (box singolo / abbonamento) and a pickup
 * slot, place the order (POST /api/v1/orders), then pay it (POST /api/v1/payments,
 * DEV-071). On payment the box is ordered/paid.
 */
@Composable
fun BoxCheckoutScreen(
    onClose: () -> Unit,
    onOrdered: () -> Unit,
    vm: BoxCheckoutViewModel = remember { BoxCheckoutViewModel() },
) {
    val state by vm.state.collectAsState()
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
            EyebrowLabel("Ordina il box")
            TextButton(onClick = onClose) { Text("Chiudi", color = colors.verde) }
        }
        Spacer(Modifier.height(10.dp))

        when {
            state.paid -> SuccessBody(state = state, onOrdered = onOrdered)
            state.placedOrder != null -> PaymentStep(state = state, vm = vm)
            else -> CheckoutForm(state = state, vm = vm)
        }
    }
}

@Composable
private fun PaymentStep(state: BoxCheckoutState, vm: BoxCheckoutViewModel) {
    val colors = MeridiaTheme.colors
    Text(
        "${formulaName(state.formula)} · ritiro ${pickupName(state.pickup).lowercase()} (${state.pickup.window})",
        style = MeridiaTheme.typography.bodyMedium,
        color = colors.grigio,
    )
    Spacer(Modifier.height(14.dp))
    PaymentPanel(
        amountCents = state.formula.priceEur * 100,
        processing = state.processing,
        error = state.paymentError,
        onPay = vm::pay,
    )
}

@Composable
private fun CheckoutForm(state: BoxCheckoutState, vm: BoxCheckoutViewModel) {
    val colors = MeridiaTheme.colors

    EyebrowLabel("Scegli la formula")
    Spacer(Modifier.height(8.dp))
    OrderFormula.entries.forEach { formula ->
        SelectableCard(selected = formula == state.formula, onClick = { vm.selectFormula(formula) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(formulaName(formula), style = MeridiaTheme.typography.titleMedium, color = colors.inchiostro)
                    if (formula == OrderFormula.Subscription) {
                        Text("Risparmi € 40/mese", style = MeridiaTheme.typography.labelMedium, color = colors.verde)
                    }
                }
                Text("€ ${formula.priceEur}", style = MeridiaTheme.typography.titleMedium, color = colors.verde, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    Spacer(Modifier.height(6.dp))
    EyebrowLabel("Ritiro · lunedì in studio")
    Spacer(Modifier.height(8.dp))
    PickupSlot.entries.forEach { slot ->
        SelectableCard(selected = slot == state.pickup, onClick = { vm.selectPickup(slot) }) {
            Text(pickupName(slot), style = MeridiaTheme.typography.titleMedium, color = colors.inchiostro)
            Text(slot.window, style = MeridiaTheme.typography.bodyMedium, color = colors.grigio)
        }
        Spacer(Modifier.height(8.dp))
    }

    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Totale oggi", style = MeridiaTheme.typography.titleMedium, color = colors.inchiostro)
        Text("€ ${state.formula.priceEur}", style = MeridiaTheme.typography.displaySmall, color = colors.verde)
    }

    state.error?.let {
        Spacer(Modifier.height(10.dp))
        Text(it, color = colors.arancio, style = MeridiaTheme.typography.bodyMedium)
    }

    Spacer(Modifier.height(14.dp))
    MeridiaButton(
        if (state.submitting) "Invio in corso…" else "Vai al pagamento",
        onClick = vm::place,
        enabled = !state.submitting,
    )
}

@Composable
private fun SuccessBody(state: BoxCheckoutState, onOrdered: () -> Unit) {
    val colors = MeridiaTheme.colors
    Text("Box pagato ✓", style = MeridiaTheme.typography.displaySmall, color = colors.inchiostro)
    Spacer(Modifier.height(10.dp))
    val formulaLine =
        if (state.formula == OrderFormula.Subscription) "Abbonamento mensile attivato" else "Box singolo acquistato"
    Text(
        "$formulaLine · ritiro lunedì in studio, fascia ${pickupName(state.pickup).lowercase()} " +
            "(${state.pickup.window}). Riceverai la ricevuta via email e ti avvisiamo lunedì " +
            "mattina quando il box è pronto.",
        style = MeridiaTheme.typography.bodyMedium,
        color = colors.grigio,
    )
    Spacer(Modifier.height(16.dp))
    MeridiaButton("Torna al box", onClick = onOrdered)
}

@Composable
private fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = MeridiaTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) colors.lime else colors.bianco)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.verde else colors.linea,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        content()
    }
}

private fun formulaName(formula: OrderFormula): String = when (formula) {
    OrderFormula.Subscription -> "Abbonamento mensile"
    OrderFormula.Single -> "Box singolo"
}

private fun pickupName(slot: PickupSlot): String = when (slot) {
    PickupSlot.Mattina -> "Mattina"
    PickupSlot.Pomeriggio -> "Pomeriggio"
}
