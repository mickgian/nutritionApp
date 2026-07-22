package com.base.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.base.shared.auth.AuthModule
import com.base.shared.auth.AuthState
import com.base.shared.models.SessionResponse
import com.base.shared.screens.ChatScreenWithHistory
import com.base.shared.screens.LoginScreen
import com.base.shared.screens.RegistrationScreen
import com.base.shared.screens.SessionListScreen
import com.base.shared.viewModels.RegistrationViewModel
import com.base.shared.viewModels.SessionViewModel
import kotlinx.coroutines.launch

@Composable
fun CommonView() {
    val nav = rememberNavController()

    /* ------------------------------------------------------------ */
    /*  Top-level app state                                          */
    /* ------------------------------------------------------------ */
    val authState by AuthModule.getAuthManager().authState.collectAsState()
    var session by remember { mutableStateOf<SessionResponse?>(null) }

    /* Decide which screen to start on */
    val startDestination = when (authState) {
        is AuthState.Loading -> "Login" // Show login while loading
        is AuthState.Unauthenticated -> "Login"
        is AuthState.Authenticated -> "Chat" // Always go directly to Chat
        is AuthState.Error -> "Login"
    }
    
    // Debug: Log the current auth state
    LaunchedEffect(authState) {
        println("DEBUG: AuthState changed to: $authState")
        println("DEBUG: StartDestination: $startDestination")
    }

    MaterialTheme {
        // Show loading screen if AuthState is still Loading
        if (authState is AuthState.Loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Initializing...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            return@MaterialTheme
        }

        NavHost(
            navController = nav,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {

            /* ---------------- REGISTER ---------------- */
            composable("Register") {
                RegistrationScreen(
                    vm = remember { RegistrationViewModel() },
                    onRegistered = {
                        nav.navigate("Login") {
                            popUpTo("Register") { inclusive = true }
                        }
                    },
                    onSwitchToLogin = { nav.navigate("Login") }
                )
            }

            /* ---------------- LOGIN ---------------- */
            composable("Login") {
                LoginScreen(
                    onSuccess = { newToken ->
                        nav.navigate("Chat") { popUpTo("Login") { inclusive = true } }
                    },
                    onSwitchToRegister = { nav.navigate("Register") }
                )
            }


            /* ------------- SESSION (pick / create) ------------- */
            composable("Session") {
                when (val currentAuthState = authState) {
                    is AuthState.Authenticated -> {
                        val bearer = currentAuthState.token.accessToken
                        val sessionVm = remember { SessionViewModel(token = bearer) }

                        val coroutineScope = rememberCoroutineScope()
                        
                        SessionListScreen(
                            vm = sessionVm,
                            onPick = { chosen ->
                                session = chosen
                                nav.navigate("Chat")
                            },
                            onLogout = {           // clear creds + bounce to login
                                session = null
                                coroutineScope.launch {
                                    AuthModule.getAuthManager().logout()
                                }
                                nav.navigate("Login") {
                                    popUpTo("Session") { inclusive = true }
                                }
                            }
                        )
                    }
                    else -> {
                        // User is not authenticated, bounce to Login
                        LaunchedEffect(Unit) {
                            nav.navigate("Login") { popUpTo("Session") { inclusive = true } }
                        }
                    }
                }
            }

            /* ---------------- CHAT -------------------- */
            composable("Chat") {
                when (val currentAuthState = authState) {
                    is AuthState.Authenticated -> {
                        val bearer = currentAuthState.token.accessToken
                        val coroutineScope = rememberCoroutineScope()

                        // New ChatGPT-style chat screen that manages sessions internally
                        ChatScreenWithHistory(
                            token = bearer,
                            onLogout = {
                                session = null
                                coroutineScope.launch {
                                    AuthModule.getAuthManager().logout()
                                }
                                nav.navigate("Login") {
                                    popUpTo("Chat") { inclusive = true }
                                }
                            }
                        )
                    }
                    else -> {
                        // User is not authenticated, bounce to Login
                        LaunchedEffect(Unit) {
                            nav.navigate("Login") { popUpTo("Chat") { inclusive = true } }
                        }
                    }
                }
            }

        }
    }
}
