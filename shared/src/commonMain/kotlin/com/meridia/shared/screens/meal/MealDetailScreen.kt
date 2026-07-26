package com.meridia.shared.screens.meal

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meridia.shared.models.MealDto
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.EyebrowLabel
import com.meridia.shared.theme.components.MacroBadge
import com.meridia.shared.theme.components.MeridiaButton
import com.meridia.shared.theme.components.MeridiaButtonStyle
import com.meridia.shared.theme.components.MeridiaCard
import com.meridia.shared.viewModels.MealDetailUiState
import com.meridia.shared.viewModels.MealDetailViewModel

/**
 * Meal detail opened from the weekly box (DEV-032): valori nutrizionali,
 * conservazione, riscaldamento. Renders loading / error / content states.
 */
@Composable
fun MealDetailScreen(
    mealId: Int,
    onClose: () -> Unit,
    vm: MealDetailViewModel = remember { MealDetailViewModel() },
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(mealId) { vm.load(mealId) }
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
            EyebrowLabel("Pasto")
            TextButton(onClick = onClose) { Text("Chiudi", color = colors.verde) }
        }
        Spacer(Modifier.height(10.dp))

        when (val s = state) {
            MealDetailUiState.Loading -> Centered { CircularProgressIndicator(color = colors.verde) }
            is MealDetailUiState.Error -> ErrorBlock(message = s.message, onRetry = { vm.load(mealId) })
            is MealDetailUiState.Content -> MealBody(meal = s.meal)
        }
    }
}

@Composable
private fun MealBody(meal: MealDto) {
    val colors = MeridiaTheme.colors
    Text(
        "${meal.assetRef ?: ""} ${meal.title}".trim(),
        style = MeridiaTheme.typography.displaySmall,
        color = colors.inchiostro,
    )
    Text(slotLabel(meal.slot), style = MeridiaTheme.typography.bodyMedium, color = colors.grigio)
    Spacer(Modifier.height(14.dp))

    MeridiaCard {
        Text("Valori nutrizionali", style = MeridiaTheme.typography.titleMedium, color = colors.inchiostro)
        Spacer(Modifier.height(6.dp))
        Text("${meal.kcal} kcal", style = MeridiaTheme.typography.bodyLarge, color = colors.verde)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MacroBadge("Proteine ${meal.proteinG}g")
            MacroBadge("Carboidrati ${meal.carbG}g")
            MacroBadge("Grassi ${meal.fatG}g")
        }
    }

    meal.storageNote?.let {
        Spacer(Modifier.height(12.dp))
        InfoCard(title = "Conservazione", body = it)
    }
    meal.reheatNote?.let {
        Spacer(Modifier.height(12.dp))
        InfoCard(title = "Riscaldamento", body = it)
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    val colors = MeridiaTheme.colors
    MeridiaCard {
        Text(title, style = MeridiaTheme.typography.titleMedium, color = colors.inchiostro, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MeridiaTheme.typography.bodyMedium, color = colors.grigio)
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

private fun slotLabel(slot: String): String = when (slot) {
    "lunch" -> "Pranzo"
    "dinner" -> "Cena"
    else -> slot
}
