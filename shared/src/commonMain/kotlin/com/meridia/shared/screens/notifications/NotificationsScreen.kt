package com.meridia.shared.screens.notifications

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
import com.meridia.shared.models.NotificationDto
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.EyebrowLabel
import com.meridia.shared.theme.components.MeridiaButton
import com.meridia.shared.theme.components.MeridiaButtonStyle
import com.meridia.shared.theme.components.MeridiaCard
import com.meridia.shared.viewModels.NotificationsUiState
import com.meridia.shared.viewModels.NotificationsViewModel

/**
 * The notifications list (DEV-061): behavior-profiled messages from
 * GET /api/v1/me/notifications, newest first. Unread items carry a dot; opening
 * the screen marks them read. Renders loading / error / empty / content states.
 */
@Composable
fun NotificationsScreen(
    onClose: () -> Unit,
    vm: NotificationsViewModel = remember { NotificationsViewModel() },
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.load(markRead = true) }
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
            EyebrowLabel("Notifiche")
            TextButton(onClick = onClose) { Text("Chiudi", color = colors.verde) }
        }
        Spacer(Modifier.height(10.dp))

        when (val s = state) {
            NotificationsUiState.Loading ->
                Centered { CircularProgressIndicator(color = colors.verde) }
            is NotificationsUiState.Error -> ErrorBlock(message = s.message, onRetry = { vm.load(markRead = true) })
            is NotificationsUiState.Content ->
                if (s.notifications.isEmpty()) EmptyState() else NotificationsList(s.notifications)
        }
    }
}

@Composable
private fun NotificationsList(notifications: List<NotificationDto>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        notifications.forEach { NotificationRow(it) }
    }
}

@Composable
private fun NotificationRow(notification: NotificationDto) {
    val colors = MeridiaTheme.colors
    MeridiaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(notification.icon, style = MeridiaTheme.typography.titleMedium)
            Spacer(Modifier.size(10.dp))
            Text(
                notification.title,
                style = MeridiaTheme.typography.titleMedium,
                color = colors.inchiostro,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.widthIn(max = 260.dp),
            )
            if (!notification.isRead) {
                Spacer(Modifier.size(8.dp))
                Box(Modifier.size(8.dp).clip(CircleShape).background(colors.arancio))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(notification.body, style = MeridiaTheme.typography.bodyMedium, color = colors.grigio)
    }
}

@Composable
private fun EmptyState() {
    val colors = MeridiaTheme.colors
    MeridiaCard {
        Text("Nessuna notifica", style = MeridiaTheme.typography.titleMedium, color = colors.inchiostro)
        Spacer(Modifier.height(6.dp))
        Text(
            "Qui troverai promemoria, scadenze del box e novità dallo studio.",
            style = MeridiaTheme.typography.bodyMedium,
            color = colors.grigio,
        )
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    val colors = MeridiaTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, color = colors.arancio, style = MeridiaTheme.typography.bodyLarge)
        MeridiaButton(
            "Riprova",
            onClick = onRetry,
            modifier = Modifier.widthIn(max = 200.dp),
            style = MeridiaButtonStyle.Ghost,
        )
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}
