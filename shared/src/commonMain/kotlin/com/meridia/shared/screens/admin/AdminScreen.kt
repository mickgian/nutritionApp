package com.meridia.shared.screens.admin

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.EyebrowLabel
import com.meridia.shared.theme.components.MeridiaButton
import com.meridia.shared.theme.components.MeridiaCard
import com.meridia.shared.viewModels.AdminAvailabilityViewModel
import com.meridia.shared.viewModels.AdminUiState
import com.meridia.shared.viewModels.AdminViewModel

/**
 * The studio (admin) panel (DEV-080): availability, client directory with plan
 * assignment, box-order overview, and promotion broadcast. Reachable only when
 * the signed-in user is an admin (the entry point is hidden otherwise); the
 * server enforces the role on every endpoint. Renders loading / error / content.
 */
@Composable
fun AdminScreen(
    onClose: () -> Unit,
    vm: AdminViewModel = remember { AdminViewModel() },
    availabilityVm: AdminAvailabilityViewModel = remember { AdminAvailabilityViewModel() },
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) {
        vm.load()
        availabilityVm.load()
    }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                EyebrowLabel("Pannello studio")
                Spacer(Modifier.width(10.dp))
                AdminBadge()
            }
            TextButton(onClick = onClose) { Text("Chiudi", color = colors.verde) }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Gestisci lo studio",
            style = MeridiaTheme.typography.displaySmall,
            color = colors.inchiostro,
        )
        Spacer(Modifier.height(16.dp))

        when (val s = state) {
            AdminUiState.Loading -> LoadingCard()
            is AdminUiState.Error -> ErrorCard(message = s.message) {
                vm.load()
                availabilityVm.load()
            }
            is AdminUiState.Content -> {
                s.actionError?.let {
                    Text(it, style = MeridiaTheme.typography.bodyMedium, color = colors.arancio)
                    Spacer(Modifier.height(12.dp))
                }
                AdminAvailabilityPanel(vm = availabilityVm)
                Spacer(Modifier.height(14.dp))
                AdminClientsPanel(content = s, onAssign = vm::assignPlan)
                Spacer(Modifier.height(14.dp))
                AdminOrdersPanel(orders = s.orders)
                Spacer(Modifier.height(14.dp))
                AdminPromotionPanel(content = s, onSend = vm::sendPromotion)
            }
        }
    }
}

@Composable
private fun AdminBadge() {
    val colors = MeridiaTheme.colors
    Box(
        modifier = Modifier
            .background(colors.verdeScuro, RoundedCornerShape(99.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            "Admin",
            style = MeridiaTheme.typography.labelSmall,
            color = colors.lime,
            fontWeight = FontWeight.Bold,
        )
    }
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
