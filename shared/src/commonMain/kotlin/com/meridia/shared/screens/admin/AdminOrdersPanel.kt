package com.meridia.shared.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meridia.shared.models.AdminOrderDto
import com.meridia.shared.screens.booking.euro
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.EyebrowLabel
import com.meridia.shared.theme.components.MeridiaCard

/** Studio overview of box orders across all clients (DEV-080). Read-only. */
@Composable
fun AdminOrdersPanel(orders: List<AdminOrderDto>) {
    val colors = MeridiaTheme.colors
    val toPrepare = orders.filter { it.status != "cancelled" }
    val subscriptions = toPrepare.count { it.formula == "subscription" }
    val singles = toPrepare.count { it.formula == "single" }
    MeridiaCard {
        EyebrowLabel("Ordini box")
        Spacer(Modifier.height(8.dp))
        Text(
            "${toPrepare.size} box da preparare",
            style = MeridiaTheme.typography.titleMedium,
            color = colors.inchiostro,
        )
        Text(
            "$subscriptions ${abbonamentoWord(subscriptions)} · $singles ${singoloWord(singles)}",
            style = MeridiaTheme.typography.bodyMedium,
            color = colors.grigio,
        )
        if (orders.isEmpty()) return@MeridiaCard
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            orders.forEach { OrderRow(it) }
        }
    }
}

@Composable
private fun OrderRow(order: AdminOrderDto) {
    val colors = MeridiaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                order.clientName,
                style = MeridiaTheme.typography.bodyLarge,
                color = colors.inchiostro,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${formulaLabel(order.formula)} · ${pickupLabel(order.pickup)} · ${statusLabel(order.status)}",
                style = MeridiaTheme.typography.bodyMedium,
                color = colors.grigio,
            )
        }
        Text(euro(order.priceCents), style = MeridiaTheme.typography.bodyLarge, color = colors.verde)
    }
}

private fun formulaLabel(formula: String): String =
    if (formula == "subscription") "Abbonamento" else "Box singolo"

private fun pickupLabel(pickup: String): String =
    if (pickup == "pomeriggio") "Ritiro pomeriggio" else "Ritiro mattina"

private fun statusLabel(status: String): String = when (status) {
    "paid" -> "Pagato"
    "cancelled" -> "Annullato"
    else -> "In attesa di pagamento"
}

private fun abbonamentoWord(count: Int): String = if (count == 1) "abbonamento" else "abbonamenti"

private fun singoloWord(count: Int): String = if (count == 1) "singolo" else "singoli"
