package com.meridia.shared.screens.admin

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.theme.components.EyebrowLabel
import com.meridia.shared.theme.components.MeridiaButton
import com.meridia.shared.theme.components.MeridiaCard
import com.meridia.shared.viewModels.AdminUiState

/** Studio promotion broadcast: push a message to every client (DEV-080). */
@Composable
fun AdminPromotionPanel(
    content: AdminUiState.Content,
    onSend: (title: String, body: String) -> Unit,
) {
    val colors = MeridiaTheme.colors
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    MeridiaCard {
        EyebrowLabel("Invia promozione")
        Text(
            "Una notifica push a tutti i clienti.",
            style = MeridiaTheme.typography.bodyMedium,
            color = colors.grigio,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titolo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Messaggio") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        MeridiaButton(
            "Invia",
            onClick = { onSend(title, body) },
            enabled = !content.promoSubmitting && title.isNotBlank() && body.isNotBlank(),
        )
        content.promoNotice?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MeridiaTheme.typography.bodyMedium, color = colors.verde)
        }
    }
}
