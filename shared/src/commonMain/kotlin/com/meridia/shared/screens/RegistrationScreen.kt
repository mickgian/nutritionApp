package com.meridia.shared.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.meridia.shared.utils.PasswordValidator
import com.meridia.shared.viewModels.RegistrationViewModel

@Composable
fun RegistrationScreen(
    vm: RegistrationViewModel = remember { RegistrationViewModel() },
    onRegistered: () -> Unit,
    onSwitchToLogin: () -> Unit,
) {
    val state by vm.state.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Crea account", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        var fullName by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        val emailFocusRequester = remember { FocusRequester() }
        val passwordFocusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Nome completo") },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { emailFocusRequester.requestFocus() }),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.focusRequester(emailFocusRequester),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.focusRequester(passwordFocusRequester),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    val valid = PasswordValidator.validatePassword(password).isValid
                    if (state is RegistrationViewModel.State.Idle &&
                        fullName.isNotBlank() && email.isNotBlank() && valid
                    ) {
                        vm.register(email, password, fullName)
                    }
                },
            ),
            singleLine = true,
        )

        if (password.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            PasswordRequirements(password)
        }

        Spacer(Modifier.height(16.dp))

        // Keep the button in a fixed position across states to avoid layout shift.
        when (state) {
            RegistrationViewModel.State.Loading -> CircularProgressIndicator()
            RegistrationViewModel.State.Success -> LaunchedEffect(Unit) { onRegistered() }
            else -> {
                val valid = PasswordValidator.validatePassword(password).isValid
                Button(
                    onClick = { vm.register(email, password, fullName) },
                    enabled = state !is RegistrationViewModel.State.Loading &&
                        fullName.isNotBlank() && email.isNotBlank() && valid,
                ) {
                    Text("Registrati")
                }
            }
        }

        if (state is RegistrationViewModel.State.Error) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = (state as RegistrationViewModel.State.Error).message,
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

/** Live checklist of the password rules — shared by the screen and its previews. */
@Composable
fun PasswordRequirements(password: String) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            "Requisiti della password:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(4.dp))
        PasswordValidator.getPasswordRequirements().forEach { requirement ->
            val isMet = PasswordValidator.checkRequirement(password, requirement)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp),
            ) {
                Icon(
                    imageVector = if (isMet) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = if (isMet) "Requisito soddisfatto" else "Requisito non soddisfatto",
                    tint = if (isMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.width(16.dp).height(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = requirement,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isMet) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    },
                )
            }
        }
    }
}
