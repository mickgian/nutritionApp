package com.base.shared.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.base.shared.models.TokenResponse
import com.base.shared.viewModels.LoginViewModel

@Composable
fun LoginScreen(
    vm: LoginViewModel = remember { LoginViewModel() },
    onSuccess: (TokenResponse) -> Unit,
    onSwitchToRegister: () -> Unit
) {
    val state by vm.loginState.collectAsState()

    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    
    val passwordFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // iOS workaround: Request focus on email field when screen loads
    LaunchedEffect(Unit) {
        try {
            emailFocusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore focus errors on some platforms
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        when (state) {
            LoginViewModel.LoginState.Idle,
            is LoginViewModel.LoginState.Error -> {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {

                    /* --- fields --- */
                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text("Email") },
                        placeholder = { Text("Enter your email") },
                        modifier = Modifier.focusRequester(emailFocusRequester),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Email
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { passwordFocusRequester.requestFocus() }
                        ),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Password") },
                        placeholder = { Text("Enter your password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.focusRequester(passwordFocusRequester),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                if (user.isNotBlank() && pass.isNotBlank()) {
                                    vm.login(user, pass)
                                }
                            }
                        ),
                        singleLine = true
                    )
                    Spacer(Modifier.height(16.dp))

                    /* --- login button --- */
                    Button(
                        onClick = { vm.login(user, pass) },
                        enabled = user.isNotBlank() && pass.isNotBlank()
                    ) { 
                        Text("Login") 
                    }

                    if (state is LoginViewModel.LoginState.Error) {
                        Text(
                            (state as LoginViewModel.LoginState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    /* --- switch to register --- */
                    TextButton(
                        onClick = onSwitchToRegister,
                        modifier = Modifier.padding(top = 24.dp)
                    ) {
                        Text("Don't have an account? Register")
                    }
                }
            }

            LoginViewModel.LoginState.Loading -> CircularProgressIndicator()

            is LoginViewModel.LoginState.Success -> {
                val token = (state as LoginViewModel.LoginState.Success).token            // TokenResponse
                onSuccess(token)
            }


        }
    }
}
