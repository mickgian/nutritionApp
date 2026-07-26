package com.meridia.shared.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meridia.shared.models.AppointmentDto
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.MeridiaButton

@Composable
fun BookingConfirmation(
    appointment: AppointmentDto,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MeridiaTheme.colors
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(74.dp).clip(CircleShape).background(colors.lime),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", style = MeridiaTheme.typography.displaySmall, color = colors.verdeScuro)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Prenotazione confermata",
            style = MeridiaTheme.typography.headlineMedium,
            color = colors.inchiostro,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            slotDateTime(appointment.scheduledAt),
            style = MeridiaTheme.typography.bodyLarge,
            color = colors.grigio,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "In attesa di pagamento",
            style = MeridiaTheme.typography.bodyMedium,
            color = colors.arancio,
        )
        Spacer(Modifier.height(24.dp))
        MeridiaButton("Fatto", onClick = onDone, modifier = Modifier.widthIn(max = 240.dp))
    }
}
