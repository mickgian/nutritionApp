package com.base.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Simplified Android previews that work without shared module dependencies.
 * 
 * These previews show the basic layout and structure of your screens
 * without needing access to ViewModels or complex shared dependencies.
 */

// Data classes for preview
data class PreviewChatMessage(
    val text: String,
    val fromUser: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Chat Screen - Empty", showBackground = true)
@Composable
fun ChatScreenEmptyPreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    title = { Text("Chat") },
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = { },
                            modifier = Modifier.weight(1f),
                            placeholder = { 
                                Text(
                                    "Message",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                ) 
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            maxLines = 4
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = { },
                            enabled = false,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Start a conversation",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Chat Screen - With Messages", showBackground = true)
@Composable
fun ChatScreenWithMessagesPreview() {
    val sampleMessages = listOf(
        PreviewChatMessage("Hello! How can I help you today?", false),
        PreviewChatMessage("I need help with my project setup", true),
        PreviewChatMessage("I'd be happy to help! What specific part of the setup are you having trouble with?", false),
        PreviewChatMessage("I'm trying to configure the database connection", true)
    )

    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    title = { Text("Chat") },
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = "Sample message",
                            onValueChange = { },
                            modifier = Modifier.weight(1f),
                            placeholder = { 
                                Text(
                                    "Message",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                ) 
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            maxLines = 4
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = { },
                            enabled = true,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 8.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sampleMessages) { msg ->
                    if (msg.fromUser) {
                        // User message - aligned to the right with blue background
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        // AI message - aligned to the left with avatar and gray background
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.Top
                        ) {
                            // AI Avatar
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "AI",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Message bubble
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Chat Screen - Loading", showBackground = true)
@Composable
fun ChatScreenLoadingPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Preview(name = "Registration Screen - Idle", showBackground = true)
@Composable
fun RegistrationScreenIdlePreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Register", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = "user@example.com",
                onValueChange = { },
                label = { Text("Email") }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = "SecurePass123!",
                onValueChange = { },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation()
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Password requirements preview
            Column(
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    "Password requirements:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                
                val requirements = listOf(
                    "At least 8 characters long" to true,
                    "At least one uppercase letter" to true,
                    "At least one lowercase letter" to true,
                    "At least one number" to true,
                    "At least one special character" to true
                )
                
                requirements.forEach { (requirement, isMet) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isMet) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = if (isMet) "Requirement met" else "Requirement not met",
                            tint = if (isMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = requirement,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { },
                enabled = true
            ) {
                Text("Register")
            }
            
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { }) {
                Text("Already have an account? Log in")
            }
        }
    }
}

@Preview(name = "Registration Screen - Loading", showBackground = true)
@Composable
fun RegistrationScreenLoadingPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Register", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = "user@example.com",
                onValueChange = { },
                label = { Text("Email") },
                enabled = false
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = "••••••••••••",
                onValueChange = { },
                label = { Text("Password") },
                enabled = false
            )
            
            Spacer(Modifier.height(16.dp))
            
            CircularProgressIndicator()
            
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { }, enabled = false) {
                Text("Already have an account? Log in")
            }
        }
    }
}

@Preview(name = "Registration Screen - Error", showBackground = true)
@Composable
fun RegistrationScreenErrorPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Register", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = "user@example.com",
                onValueChange = { },
                label = { Text("Email") }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = "weak",
                onValueChange = { },
                label = { Text("Password") },
                isError = true
            )
            
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { },
                enabled = false
            ) {
                Text("Register")
            }
            
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Password must be at least 8 characters long and at least one number.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { }) {
                Text("Already have an account? Log in")
            }
        }
    }
}

// Dark theme previews
@Preview(name = "Chat - Dark Theme", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ChatScreenDarkPreview() {
    ChatScreenWithMessagesPreview()
}

@Preview(name = "Registration - Dark Theme", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RegistrationScreenDarkPreview() {
    RegistrationScreenIdlePreview()
}