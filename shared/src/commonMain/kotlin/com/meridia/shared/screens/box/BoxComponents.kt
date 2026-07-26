package com.meridia.shared.screens.box

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meridia.shared.models.BoxItemDto
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.MacroBadge
import com.meridia.shared.theme.components.MeridiaCard

private val WEEKDAYS = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")

@Composable
fun WeekdayStrip(selected: Int, onSelect: (Int) -> Unit) {
    val colors = MeridiaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WEEKDAYS.forEachIndexed { index, label ->
            val active = index == selected
            Text(
                label,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (active) colors.verde else colors.salvia)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = if (active) colors.bianco else colors.verde,
                style = MeridiaTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
fun DeadlineBanner() {
    val colors = MeridiaTheme.colors
    Text(
        "Ordina entro giovedì alle 23:59 · ritiro il lunedì mattina.",
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.lime)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        color = colors.verdeScuro,
        style = MeridiaTheme.typography.bodyMedium,
    )
}

@Composable
fun LockedBox() {
    val colors = MeridiaTheme.colors
    MeridiaCard {
        Text("🔒 Box bloccato", style = MeridiaTheme.typography.titleMedium, color = colors.inchiostro)
        Spacer(Modifier.height(8.dp))
        Text(
            "Il box settimanale si sblocca dopo la prima visita, quando lo studio ti " +
                "assegna un piano nutrizionale. Prenota una consulenza per iniziare.",
            style = MeridiaTheme.typography.bodyMedium,
            color = colors.grigio,
        )
    }
}

@Composable
fun DayMeals(items: List<BoxItemDto>, onMealClick: (Int) -> Unit) {
    val lunch = items.firstOrNull { it.slot == "lunch" }
    val dinner = items.firstOrNull { it.slot == "dinner" }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        lunch?.let { MealCard(label = "Pranzo", item = it, onMealClick = onMealClick) }
        dinner?.let { MealCard(label = "Cena", item = it, onMealClick = onMealClick) }
    }
}

@Composable
private fun MealCard(label: String, item: BoxItemDto, onMealClick: (Int) -> Unit) {
    val colors = MeridiaTheme.colors
    val meal = item.meal
    MeridiaCard(modifier = Modifier.clickable { onMealClick(meal.id) }) {
        Text(label.uppercase(), style = MeridiaTheme.typography.labelMedium, color = colors.grigio)
        Spacer(Modifier.height(4.dp))
        Text(
            "${meal.assetRef ?: ""} ${meal.title}".trim(),
            style = MeridiaTheme.typography.titleMedium,
            color = colors.inchiostro,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text("${meal.kcal} kcal", style = MeridiaTheme.typography.bodyMedium, color = colors.verde)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MacroBadge("P ${meal.proteinG}g")
            MacroBadge("C ${meal.carbG}g")
            MacroBadge("G ${meal.fatG}g")
        }
    }
}
