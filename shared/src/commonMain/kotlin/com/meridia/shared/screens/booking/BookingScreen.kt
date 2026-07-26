package com.meridia.shared.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.MeridiaButton
import com.meridia.shared.viewModels.BookingStep
import com.meridia.shared.viewModels.BookingUiState
import com.meridia.shared.viewModels.BookingViewModel

@Composable
fun BookingScreen(
    onClose: () -> Unit,
    onBooked: () -> Unit,
    vm: BookingViewModel = remember { BookingViewModel() },
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
    val colors = MeridiaTheme.colors

    Column(modifier = Modifier.fillMaxSize().background(colors.panna)) {
        BookingTopBar(state = state, onClose = onClose, onBack = vm::back)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val s = state) {
                BookingUiState.Loading -> Centered {
                    CircularProgressIndicator(color = colors.verde)
                }

                BookingUiState.Empty -> Centered {
                    Text(
                        "Nessuna disponibilità al momento. Riprova più tardi.",
                        style = MeridiaTheme.typography.bodyLarge,
                        color = colors.grigio,
                    )
                }

                is BookingUiState.Error -> Centered {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(s.message, color = colors.arancio, style = MeridiaTheme.typography.bodyLarge)
                        MeridiaButton(
                            "Riprova",
                            onClick = vm::load,
                            modifier = Modifier.widthIn(max = 200.dp),
                        )
                    }
                }

                is BookingUiState.Content ->
                    BookingStepBody(content = s, vm = vm, modifier = Modifier.fillMaxSize())

                is BookingUiState.Confirmed ->
                    BookingConfirmation(s.appointment, onDone = onBooked, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun BookingTopBar(state: BookingUiState, onClose: () -> Unit, onBack: () -> Unit) {
    val colors = MeridiaTheme.colors
    val content = state as? BookingUiState.Content
    val canGoBack = content != null && content.step != BookingStep.Slot
    Row(
        modifier = Modifier.fillMaxWidth().background(colors.bianco).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { if (canGoBack) onBack() else onClose() }) {
            Text(if (canGoBack) "Indietro" else "Chiudi", color = colors.verde)
        }
        Text(
            "Prenota una visita",
            style = MeridiaTheme.typography.titleLarge,
            color = colors.inchiostro,
            modifier = Modifier.weight(1f),
        )
        if (content != null) StepDots(content.step)
    }
}

@Composable
private fun StepDots(step: BookingStep) {
    val colors = MeridiaTheme.colors
    val active = when (step) {
        BookingStep.Slot -> 1
        BookingStep.Riepilogo -> 2
        BookingStep.Pagamento -> 3
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(end = 8.dp)) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (index < active) colors.verde else colors.linea),
            )
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}
