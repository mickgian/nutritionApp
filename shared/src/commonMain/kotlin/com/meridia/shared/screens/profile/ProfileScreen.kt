package com.meridia.shared.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meridia.shared.models.AppointmentDto
import com.meridia.shared.models.CreditDto
import com.meridia.shared.models.PlanDto
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.EyebrowLabel
import com.meridia.shared.theme.components.MeridiaButton
import com.meridia.shared.theme.components.MeridiaButtonStyle
import com.meridia.shared.theme.components.MeridiaCard
import com.meridia.shared.viewModels.ProfileUiState
import com.meridia.shared.viewModels.ProfileViewModel

private const val POLICY =
    "Spostamento o cancellazione gratuiti fino a 48 ore prima: ricevi un credito valido 6 mesi. " +
        "Con meno di 48 ore di preavviso l'importo non è rimborsabile."

/**
 * The profilo tab (DEV-051): upcoming appointment (with cancel + 48h messaging),
 * active credits, plan/box summary, and studio info. Renders loading / error /
 * content states from /me/* (aggregated by [ProfileViewModel]).
 */
@Composable
fun ProfileScreen(
    onClose: () -> Unit,
    onBook: () -> Unit,
    onAccountDeleted: () -> Unit,
    vm: ProfileViewModel = remember { ProfileViewModel() },
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
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
            EyebrowLabel("Profilo")
            TextButton(onClick = onClose) { Text("Chiudi", color = colors.verde) }
        }
        Spacer(Modifier.height(10.dp))

        when (val s = state) {
            ProfileUiState.Loading -> Centered { CircularProgressIndicator(color = colors.verde) }
            is ProfileUiState.Error -> ErrorBlock(message = s.message, onRetry = { vm.load() })
            is ProfileUiState.Content -> {
                ProfileContent(content = s, onBook = onBook, onCancel = vm::cancel)
                Spacer(Modifier.height(12.dp))
                ProfilePrivacyCard(
                    busy = s.privacyBusy,
                    notice = s.privacyNotice,
                    onExport = vm::exportData,
                    onDelete = { vm.deleteAccount(onAccountDeleted) },
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    content: ProfileUiState.Content,
    onBook: () -> Unit,
    onCancel: (Int) -> Unit,
) {
    content.notice?.let {
        NoticeBanner(it)
        Spacer(Modifier.height(12.dp))
    }
    AppointmentCard(
        appointment = content.upcomingAppointment,
        cancelling = content.cancelling,
        onBook = onBook,
        onCancel = onCancel,
    )
    if (content.credits.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        CreditsCard(content.credits)
    }
    Spacer(Modifier.height(12.dp))
    PercorsoCard(plan = content.plan, boxCount = content.boxCount, subscription = content.subscriptionActive)
    Spacer(Modifier.height(12.dp))
    StudioCard()
}

@Composable
private fun AppointmentCard(
    appointment: AppointmentDto?,
    cancelling: Boolean,
    onBook: () -> Unit,
    onCancel: (Int) -> Unit,
) {
    val colors = MeridiaTheme.colors
    var confirming by remember { mutableStateOf(false) }
    MeridiaCard {
        EyebrowLabel("Appuntamento")
        Spacer(Modifier.height(8.dp))
        if (appointment == null) {
            Text("Nessun appuntamento in programma.", style = MeridiaTheme.typography.bodyMedium, color = colors.grigio)
            Spacer(Modifier.height(12.dp))
            MeridiaButton("Prenota una visita", onClick = onBook, style = MeridiaButtonStyle.Ghost)
            return@MeridiaCard
        }
        Text(visitLabel(appointment.visitType), style = MeridiaTheme.typography.titleMedium, color = colors.inchiostro, fontWeight = FontWeight.SemiBold)
        Text(whenLabel(appointment.scheduledAt), style = MeridiaTheme.typography.bodyMedium, color = colors.grigio)
        Spacer(Modifier.height(10.dp))
        Text(POLICY, style = MeridiaTheme.typography.labelMedium, color = colors.grigio)
        Spacer(Modifier.height(12.dp))
        if (!confirming) {
            MeridiaButton("Cancella appuntamento", onClick = { confirming = true }, style = MeridiaButtonStyle.Ghost)
        } else {
            Text("Vuoi davvero cancellare?", style = MeridiaTheme.typography.bodyMedium, color = colors.inchiostro)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MeridiaButton(
                    if (cancelling) "Attendi…" else "Conferma",
                    onClick = { onCancel(appointment.id) },
                    enabled = !cancelling,
                    modifier = Modifier.widthIn(max = 160.dp),
                )
                MeridiaButton("Annulla", onClick = { confirming = false }, style = MeridiaButtonStyle.Ghost, modifier = Modifier.widthIn(max = 160.dp))
            }
        }
    }
}

@Composable
private fun CreditsCard(credits: List<CreditDto>) {
    val colors = MeridiaTheme.colors
    val total = credits.sumOf { it.amountCents }
    val soonest = credits.minByOrNull { it.expiresAt }?.expiresAt?.substringBefore('T')
    MeridiaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EyebrowLabel("Credito disponibile")
            Text("€ ${total / 100}", style = MeridiaTheme.typography.displaySmall, color = colors.verde)
        }
        soonest?.let {
            Spacer(Modifier.height(6.dp))
            Text("Da una cancellazione · utilizzabile entro il $it.", style = MeridiaTheme.typography.bodyMedium, color = colors.grigio)
        }
    }
}

@Composable
private fun PercorsoCard(plan: PlanDto?, boxCount: Int, subscription: Boolean) {
    val colors = MeridiaTheme.colors
    MeridiaCard {
        EyebrowLabel("Il mio percorso")
        Spacer(Modifier.height(10.dp))
        SpreadRow("Piano attivo", plan?.name ?: "—")
        Spacer(Modifier.height(8.dp))
        SpreadRow("Box ordinati", boxCount.toString())
        Spacer(Modifier.height(8.dp))
        SpreadRow("Abbonamento", if (subscription) "Attivo · € 79/box" else "Non attivo")
    }
}

@Composable
private fun SpreadRow(label: String, value: String) {
    val colors = MeridiaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MeridiaTheme.typography.bodyMedium, color = colors.grigio)
        Text(value, style = MeridiaTheme.typography.bodyMedium, color = colors.inchiostro, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NoticeBanner(message: String) {
    val colors = MeridiaTheme.colors
    Text(
        message,
        modifier = Modifier.fillMaxWidth(),
        style = MeridiaTheme.typography.bodyMedium,
        color = colors.verde,
    )
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    val colors = MeridiaTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, color = colors.arancio, style = MeridiaTheme.typography.bodyLarge)
        MeridiaButton("Riprova", onClick = onRetry, modifier = Modifier.widthIn(max = 200.dp), style = MeridiaButtonStyle.Ghost)
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center, content = { content() })
}

private fun visitLabel(wire: String): String = when (wire) {
    "prima" -> "Prima visita"
    "controllo" -> "Visita di controllo"
    else -> wire
}

private fun whenLabel(iso: String): String {
    val date = iso.substringBefore('T')
    val time = iso.substringAfter('T', "").take(5)
    return if (time.isBlank()) date else "$date · ore $time"
}
