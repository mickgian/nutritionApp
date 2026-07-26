package com.meridia.shared.screens.box

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
import androidx.compose.ui.unit.dp
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.EyebrowLabel
import com.meridia.shared.theme.components.MeridiaButton
import com.meridia.shared.theme.components.MeridiaButtonStyle
import com.meridia.shared.viewModels.BoxUiState
import com.meridia.shared.viewModels.BoxViewModel

/**
 * The weekly meal box. Renders loading / locked (no plan) / error / content
 * (orderable) states from GET /api/v1/me/plan + /me/box (DEV-033). The "ordered"
 * in-preparation state and the real checkout arrive with Epic 4 (DEV-040/041).
 */
@Composable
fun BoxScreen(
    onClose: () -> Unit,
    vm: BoxViewModel = remember { BoxViewModel() },
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
            EyebrowLabel("Box settimanale")
            TextButton(onClick = onClose) { Text("Chiudi", color = colors.verde) }
        }
        Spacer(Modifier.height(10.dp))

        when (val s = state) {
            BoxUiState.Loading -> Centered { CircularProgressIndicator(color = colors.verde) }
            BoxUiState.Locked -> LockedBox()
            is BoxUiState.Error -> ErrorBlock(message = s.message, onRetry = vm::load)
            is BoxUiState.Content -> BoxContent(content = s, onSelectWeekday = vm::selectWeekday)
        }
    }
}

@Composable
private fun BoxContent(content: BoxUiState.Content, onSelectWeekday: (Int) -> Unit) {
    val colors = MeridiaTheme.colors
    var showOrderNote by remember { mutableStateOf(false) }
    Text(
        content.plan.name,
        style = MeridiaTheme.typography.displaySmall,
        color = colors.inchiostro,
    )
    Text(
        "${content.plan.dailyKcal} kcal al giorno · ${content.plan.weeks} settimane",
        style = MeridiaTheme.typography.bodyMedium,
        color = colors.grigio,
    )
    Spacer(Modifier.height(14.dp))
    DeadlineBanner()
    Spacer(Modifier.height(14.dp))
    WeekdayStrip(selected = content.selectedWeekday, onSelect = onSelectWeekday)
    Spacer(Modifier.height(14.dp))
    DayMeals(items = content.box.items.filter { it.weekday == content.selectedWeekday })
    Spacer(Modifier.height(18.dp))
    MeridiaButton("Ordina il box", onClick = { showOrderNote = true })
    if (showOrderNote) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Il pagamento del box sarà disponibile a breve.",
            style = MeridiaTheme.typography.bodyMedium,
            color = colors.arancio,
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
