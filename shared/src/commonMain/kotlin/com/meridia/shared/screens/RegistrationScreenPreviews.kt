package com.meridia.shared.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.meridia.shared.utils.PasswordValidator
import com.meridia.shared.viewModels.RegistrationViewModel

/**
 * Preview for [RegistrationScreen] that needs no ViewModel — renders the same
 * layout from a fixed [RegistrationViewModel.State].
 *
 * - Android: wrap with @Preview in the androidApp module.
 * - Desktop: host in a preview window.
 * - Web/iOS: use in development builds.
 */
@Composable
fun RegistrationScreenContentPreview(
    state: RegistrationViewModel.State = RegistrationViewModel.State.Idle,
    onSwitchToLogin: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Crea account", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        var email by remember { mutableStateOf("mario@example.com") }
        var password by remember { mutableStateOf("SecurePass123!") }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            singleLine = true,
        )

        if (password.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            PasswordRequirements(password)
        }

        Spacer(Modifier.height(16.dp))

        when (state) {
            RegistrationViewModel.State.Loading -> CircularProgressIndicator()
            RegistrationViewModel.State.Success -> Button(onClick = {}) {
                Text("Registrazione riuscita!")
            }
            else -> {
                val valid = PasswordValidator.validatePassword(password).isValid
                Button(onClick = {}, enabled = email.isNotBlank() && valid) {
                    Text("Registrati")
                }
            }
        }

        if (state is RegistrationViewModel.State.Error) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSwitchToLogin) {
            Text("Hai già un account? Accedi")
        }
    }
}

/** Named preview states for quick manual inspection. */
object RegistrationScreenPreviews {
    @Composable
    fun Idle() = RegistrationScreenContentPreview(RegistrationViewModel.State.Idle)

    @Composable
    fun Loading() = RegistrationScreenContentPreview(RegistrationViewModel.State.Loading)

    @Composable
    fun Success() = RegistrationScreenContentPreview(RegistrationViewModel.State.Success)

    @Composable
    fun Error() = RegistrationScreenContentPreview(
        RegistrationViewModel.State.Error("Email già registrata. Usa un'altra email o accedi."),
    )
}
