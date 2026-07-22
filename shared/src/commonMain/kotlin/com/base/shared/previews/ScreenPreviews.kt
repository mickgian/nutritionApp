package com.base.shared.previews

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.base.shared.models.ChatMessage
import com.base.shared.models.ChatUiState
import com.base.shared.screens.ChatScreenContentPreview
import com.base.shared.screens.RegistrationScreenContentPreview
import com.base.shared.viewModels.RegistrationViewModel

/**
 * Comprehensive preview gallery for all screen components.
 * 
 * This file provides a centralized way to preview all screens in different states.
 * Use this for:
 * - Android: In androidApp module with @Preview annotations
 * - Desktop: Create preview windows with these functions
 * - Web: Use in development builds for component testing
 * - iOS: Reference for SwiftUI previews
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenPreviewGallery() {
    var selectedScreen by remember { mutableStateOf("Chat") }
    var selectedState by remember { mutableStateOf("Idle") }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Screen Previews") }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Navigation panel
            Card(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Screens",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    listOf("Chat", "Registration", "Login").forEach { screen ->
                        TextButton(
                            onClick = { selectedScreen = screen },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                screen,
                                color = if (selectedScreen == screen) 
                                    MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        "States",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    getStatesForScreen(selectedScreen).forEach { state ->
                        TextButton(
                            onClick = { selectedState = state },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                state,
                                color = if (selectedState == state) 
                                    MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            
            // Preview panel
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (selectedScreen) {
                        "Chat" -> ChatPreviewForState(selectedState)
                        "Registration" -> RegistrationPreviewForState(selectedState)
                        "Login" -> LoginPreviewForState(selectedState)
                        else -> Text("Screen not implemented")
                    }
                }
            }
        }
    }
}

@Composable
private fun getStatesForScreen(screen: String): List<String> {
    return when (screen) {
        "Chat" -> listOf("Idle", "Loading", "Error", "With Messages")
        "Registration" -> listOf("Idle", "Loading", "Success", "Error", "Password Validation")
        "Login" -> listOf("Idle", "Loading", "Error")
        else -> listOf("Idle")
    }
}

@Composable
private fun ChatPreviewForState(state: String) {
    when (state) {
        "Idle" -> ChatScreenContentPreview(ChatUiState.Idle)
        "Loading" -> ChatScreenContentPreview(ChatUiState.Loading)
        "Error" -> ChatScreenContentPreview(
            ChatUiState.Error("Network connection failed. Please try again.")
        )
        "With Messages" -> {
            val sampleMessages = listOf(
                ChatMessage(text = "Hello! How can I help you today?", fromUser = false),
                ChatMessage(text = "I need help with my project setup", fromUser = true),
                ChatMessage(text = "I'd be happy to help! What specific part of the setup are you having trouble with?", fromUser = false),
                ChatMessage(text = "I'm trying to configure the database connection", fromUser = true),
                ChatMessage(text = "Great! Let me guide you through the database configuration process...", fromUser = false)
            )
            ChatScreenContentPreview(ChatUiState.History(sampleMessages))
        }
        else -> ChatScreenContentPreview(ChatUiState.Idle)
    }
}

@Composable
private fun RegistrationPreviewForState(state: String) {
    when (state) {
        "Idle" -> RegistrationScreenContentPreview(RegistrationViewModel.State.Idle)
        "Loading" -> RegistrationScreenContentPreview(RegistrationViewModel.State.Loading)
        "Success" -> RegistrationScreenContentPreview(RegistrationViewModel.State.Success)
        "Error" -> RegistrationScreenContentPreview(
            RegistrationViewModel.State.Error("This email is already registered. Please use a different email or try logging in.")
        )
        "Password Validation" -> RegistrationScreenContentPreview(RegistrationViewModel.State.Idle)
        else -> RegistrationScreenContentPreview(RegistrationViewModel.State.Idle)
    }
}

@Composable
private fun LoginPreviewForState(state: String) {
    // Placeholder for LoginScreen previews when implemented
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Login screen preview for state: $state")
    }
}

// Platform-specific preview functions
object PlatformPreviews {
    
    /**
     * For Android: Use this with @Preview annotation
     */
    @Composable
    fun AndroidChatPreview() = ChatScreenContentPreview(
        ChatUiState.History(listOf(
            ChatMessage(text = "Hello!", fromUser = false),
            ChatMessage(text = "Hi there!", fromUser = true)
        ))
    )
    
    @Composable
    fun AndroidRegistrationPreview() = RegistrationScreenContentPreview(
        RegistrationViewModel.State.Idle
    )
    
    /**
     * For Desktop: Create preview window
     */
    @Composable
    fun DesktopPreviewWindow() = ScreenPreviewGallery()
    
    /**
     * For Web: Component testing in browser
     */
    @Composable
    fun WebComponentTest(
        screenName: String = "Chat",
        stateName: String = "With Messages"
    ) {
        MaterialTheme {
            Surface {
                when (screenName) {
                    "Chat" -> ChatPreviewForState(stateName)
                    "Registration" -> RegistrationPreviewForState(stateName)
                    else -> Text("Unknown screen: $screenName")
                }
            }
        }
    }
}

// Quick preview functions for specific use cases
object QuickPreviews {
    
    @Composable
    fun ChatWithMessages() = ChatScreenContentPreview(
        ChatUiState.History(listOf(
            ChatMessage(text = "What's the weather like today?", fromUser = true),
            ChatMessage(text = "I'd be happy to help you with weather information! However, I don't have access to real-time weather data. You might want to check a weather app or website for current conditions in your area.", fromUser = false),
            ChatMessage(text = "Thanks for letting me know!", fromUser = true)
        ))
    )
    
    @Composable
    fun RegistrationWithValidation() = RegistrationScreenContentPreview(
        RegistrationViewModel.State.Idle
    )
    
    @Composable
    fun RegistrationError() = RegistrationScreenContentPreview(
        RegistrationViewModel.State.Error("Password must be at least 8 characters long and at least one number.")
    )
}