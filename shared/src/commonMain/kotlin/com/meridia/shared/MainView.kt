package com.meridia.shared

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
import com.meridia.shared.auth.AuthModule
import com.meridia.shared.auth.AuthState
import com.meridia.shared.theme.MeridiaTheme
import com.meridia.shared.models.SessionResponse
import com.meridia.shared.screens.ChatScreenWithHistory
import com.meridia.shared.screens.ConsulenzaScreen
import com.meridia.shared.screens.LoginScreen
import com.meridia.shared.screens.RegistrationScreen
import com.meridia.shared.screens.SessionListScreen
import com.meridia.shared.screens.admin.AdminScreen
import com.meridia.shared.screens.booking.BookingScreen
import com.meridia.shared.screens.box.BoxCheckoutScreen
import com.meridia.shared.screens.box.BoxScreen
import com.meridia.shared.screens.meal.MealDetailScreen
import com.meridia.shared.screens.notifications.NotificationsScreen
import com.meridia.shared.screens.profile.ProfileScreen
import com.meridia.shared.viewModels.RegistrationViewModel
import com.meridia.shared.viewModels.SessionViewModel
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
        is AuthState.Authenticated -> "Consulenza" // Meridia home (booking entry)
        is AuthState.Error -> "Login"
    }
    MeridiaTheme {
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
                        "Avvio…",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            return@MeridiaTheme
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
                        nav.navigate("Consulenza") { popUpTo("Login") { inclusive = true } }
                    },
                    onSwitchToRegister = { nav.navigate("Register") }
                )
            }

            /* ---------------- CONSULENZA (home) ---------------- */
            composable("Consulenza") {
                val scope = rememberCoroutineScope()
                ConsulenzaScreen(
                    onBook = { nav.navigate("Booking") },
                    onLogout = {
                        scope.launch { AuthModule.getAuthManager().logout() }
                        nav.navigate("Login") { popUpTo("Consulenza") { inclusive = true } }
                    },
                    onOpenBox = { nav.navigate("Box") },
                    onOpenProfile = { nav.navigate("Profile") },
                    onOpenNotifications = { nav.navigate("Notifications") },
                    onOpenAdmin = { nav.navigate("Admin") },
                )
            }

            /* ---------------- ADMIN (studio panel) ---------------- */
            composable("Admin") {
                AdminScreen(onClose = { nav.popBackStack() })
            }

            /* ---------------- BOOKING wizard ---------------- */
            composable("Booking") {
                BookingScreen(
                    onClose = { nav.popBackStack() },
                    onBooked = { nav.popBackStack() },
                )
            }

            /* ---------------- BOX (weekly meal box) ---------------- */
            composable("Box") {
                BoxScreen(
                    onClose = { nav.popBackStack() },
                    onOpenMeal = { mealId -> nav.navigate("MealDetail/$mealId") },
                    onOrder = { nav.navigate("BoxCheckout") },
                )
            }

            /* ---------------- BOX checkout ---------------- */
            composable("BoxCheckout") {
                BoxCheckoutScreen(
                    onClose = { nav.popBackStack() },
                    // Re-enter Box fresh so it reloads and shows the ordered state.
                    onOrdered = { nav.navigate("Box") { popUpTo("Box") { inclusive = true } } },
                )
            }

            /* ---------------- PROFILE ---------------- */
            composable("Profile") {
                ProfileScreen(
                    onClose = { nav.popBackStack() },
                    onBook = { nav.navigate("Booking") },
                )
            }

            /* ---------------- NOTIFICATIONS ---------------- */
            composable("Notifications") {
                NotificationsScreen(
                    // Re-enter Consulenza fresh so the unread dot refreshes after viewing.
                    onClose = { nav.navigate("Consulenza") { popUpTo("Consulenza") { inclusive = true } } },
                )
            }

            /* ---------------- MEAL detail ---------------- */
            composable("MealDetail/{mealId}") { backStackEntry ->
                val mealId = backStackEntry.arguments?.getString("mealId")?.toIntOrNull() ?: 0
                MealDetailScreen(mealId = mealId, onClose = { nav.popBackStack() })
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
