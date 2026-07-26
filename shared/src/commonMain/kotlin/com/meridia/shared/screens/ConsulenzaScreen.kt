package com.meridia.shared.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meridia.shared.models.ProfessionalDto
import com.meridia.shared.models.ReviewDto
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.EyebrowLabel
import com.meridia.shared.theme.components.MeridiaButton
import com.meridia.shared.theme.components.MeridiaButtonStyle
import com.meridia.shared.theme.components.MeridiaCard
import com.meridia.shared.viewModels.ConsulenzaUiState
import com.meridia.shared.viewModels.ConsulenzaViewModel
import com.meridia.shared.viewModels.NotificationsUiState
import com.meridia.shared.viewModels.NotificationsViewModel
import kotlin.math.round

/**
 * The Consulenza tab: the studio nutritionist card + reviews (from
 * GET /api/v1/professional) with the "Prenota una visita" entry point. Renders
 * loading / error / content states (DEV-023).
 */
@Composable
fun ConsulenzaScreen(
    onBook: () -> Unit,
    onLogout: () -> Unit,
    onOpenBox: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAdmin: () -> Unit,
    vm: ConsulenzaViewModel = remember { ConsulenzaViewModel() },
    notificationsVm: NotificationsViewModel = remember { NotificationsViewModel() },
) {
    val state by vm.state.collectAsState()
    val notificationsState by notificationsVm.state.collectAsState()
    LaunchedEffect(Unit) {
        vm.load()
        notificationsVm.load()
    }
    val colors = MeridiaTheme.colors
    val unread = (notificationsState as? NotificationsUiState.Content)?.unreadCount ?: 0

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
            Text("Meridia", style = MeridiaTheme.typography.titleLarge, color = colors.verde)
            TextButton(onClick = onLogout) { Text("Esci", color = colors.verde) }
        }
        Spacer(Modifier.height(24.dp))
        EyebrowLabel("Consulenza")
        Text(
            "Prenota la tua visita nutrizionale",
            style = MeridiaTheme.typography.displaySmall,
            color = colors.inchiostro,
        )
        Spacer(Modifier.height(14.dp))

        when (val s = state) {
            ConsulenzaUiState.Loading -> LoadingCard()
            is ConsulenzaUiState.Error -> ErrorCard(message = s.message, onRetry = vm::load)
            is ConsulenzaUiState.Content -> {
                ProfessionalCard(professional = s.professional)
                Spacer(Modifier.height(8.dp))
                MeridiaButton("Prenota una visita", onClick = onBook)
                Spacer(Modifier.height(20.dp))
                ReviewsSection(reviews = s.professional.reviews)
            }
        }

        Spacer(Modifier.height(20.dp))
        MeridiaButton(
            "Il tuo box settimanale",
            onClick = onOpenBox,
            style = MeridiaButtonStyle.Ghost,
        )
        Spacer(Modifier.height(10.dp))
        MeridiaButton(
            "Il tuo profilo",
            onClick = onOpenProfile,
            style = MeridiaButtonStyle.Ghost,
        )
        Spacer(Modifier.height(10.dp))
        MeridiaButton(
            if (unread > 0) "Le tue notifiche ($unread)" else "Le tue notifiche",
            onClick = onOpenNotifications,
            style = MeridiaButtonStyle.Ghost,
            leadingIcon = if (unread > 0) {
                { UnreadDot() }
            } else {
                null
            },
        )
        if ((state as? ConsulenzaUiState.Content)?.isAdmin == true) {
            Spacer(Modifier.height(10.dp))
            MeridiaButton(
                "Apri il pannello dello studio",
                onClick = onOpenAdmin,
                style = MeridiaButtonStyle.Ghost,
            )
        }
    }
}

@Composable
private fun UnreadDot() {
    Box(Modifier.size(8.dp).clip(CircleShape).background(MeridiaTheme.colors.arancio))
}

@Composable
private fun LoadingCard() {
    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MeridiaTheme.colors.verde)
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    val colors = MeridiaTheme.colors
    MeridiaCard {
        Text(message, style = MeridiaTheme.typography.bodyLarge, color = colors.arancio)
        Spacer(Modifier.height(12.dp))
        MeridiaButton("Riprova", onClick = onRetry, modifier = Modifier.widthIn(max = 200.dp))
    }
}

@Composable
private fun ProfessionalCard(professional: ProfessionalDto) {
    val colors = MeridiaTheme.colors
    MeridiaCard {
        Text(
            professional.fullName,
            style = MeridiaTheme.typography.titleMedium,
            color = colors.inchiostro,
        )
        Text(professional.title, style = MeridiaTheme.typography.bodyMedium, color = colors.grigio)
        if (professional.reviewsCount > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                "★ ${oneDecimal(professional.rating)} · " +
                    "${professional.reviewsCount} ${reviewsWord(professional.reviewsCount)}",
                style = MeridiaTheme.typography.bodyMedium,
                color = colors.verde,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(professional.bio, style = MeridiaTheme.typography.bodyMedium, color = colors.inchiostro)
        Spacer(Modifier.height(10.dp))
        Text(
            "Prima visita €90 · Visita di controllo €50",
            style = MeridiaTheme.typography.bodyMedium,
            color = colors.verde,
        )
    }
}

@Composable
private fun ReviewsSection(reviews: List<ReviewDto>) {
    val colors = MeridiaTheme.colors
    Text("Recensioni", style = MeridiaTheme.typography.titleMedium, color = colors.inchiostro)
    Spacer(Modifier.height(10.dp))
    if (reviews.isEmpty()) {
        Text(
            "Ancora nessuna recensione.",
            style = MeridiaTheme.typography.bodyMedium,
            color = colors.grigio,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        reviews.forEach { ReviewCard(it) }
    }
}

@Composable
private fun ReviewCard(review: ReviewDto) {
    val colors = MeridiaTheme.colors
    MeridiaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                review.authorName,
                style = MeridiaTheme.typography.bodyLarge,
                color = colors.inchiostro,
                fontWeight = FontWeight.SemiBold,
            )
            Text(stars(review.rating), color = colors.lime, style = MeridiaTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(6.dp))
        Text(review.body, style = MeridiaTheme.typography.bodyMedium, color = colors.grigio)
    }
}

private fun reviewsWord(count: Int): String = if (count == 1) "recensione" else "recensioni"

private fun stars(rating: Int): String {
    val clamped = rating.coerceIn(0, 5)
    return "★".repeat(clamped) + "☆".repeat(5 - clamped)
}

private fun oneDecimal(value: Double): String {
    val scaled = round(value * 10).toInt()
    return "${scaled / 10}.${scaled % 10}"
}
