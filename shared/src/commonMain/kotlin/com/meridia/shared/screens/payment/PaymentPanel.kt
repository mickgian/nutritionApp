package com.meridia.shared.screens.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meridia.shared.models.PaymentMethodUi
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.EyebrowLabel
import com.meridia.shared.theme.components.MeridiaButton
import com.meridia.shared.theme.components.MeridiaButtonStyle
import com.meridia.shared.theme.components.MeridiaCard

/**
 * The reusable payment step (DEV-071): total, the three payment methods, and
 * processing / error feedback. Shared by the booking wizard and the box
 * checkout — the caller's ViewModel performs the POST /payments in [onPay].
 */
@Composable
fun PaymentPanel(
    amountCents: Int,
    processing: Boolean,
    error: String?,
    onPay: (PaymentMethodUi) -> Unit,
) {
    val colors = MeridiaTheme.colors
    EyebrowLabel("Pagamento")
    Spacer(Modifier.height(8.dp))
    MeridiaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Totale", style = MeridiaTheme.typography.titleMedium, color = colors.inchiostro)
            Text(euroLabel(amountCents), style = MeridiaTheme.typography.displaySmall, color = colors.verde)
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("Metodo di pagamento", style = MeridiaTheme.typography.titleMedium, color = colors.inchiostro)
    Spacer(Modifier.height(8.dp))
    PaymentMethodUi.entries.forEach { method ->
        MeridiaButton(
            method.label,
            onClick = { onPay(method) },
            style = MeridiaButtonStyle.Ghost,
            enabled = !processing,
        )
        Spacer(Modifier.height(8.dp))
    }
    if (processing) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = colors.verde, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Pagamento in corso…", style = MeridiaTheme.typography.bodyMedium, color = colors.grigio)
        }
    }
    error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = colors.arancio, style = MeridiaTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Pagamento sicuro. Riceverai la ricevuta via email.",
        style = MeridiaTheme.typography.bodySmall,
        color = colors.grigio,
    )
}

private fun euroLabel(cents: Int): String {
    val euros = cents / 100
    val rest = (cents % 100).toString().padStart(2, '0')
    return "€$euros,$rest"
}
