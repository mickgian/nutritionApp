package com.base.shared.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.base.shared.models.ChatMessage
import com.base.shared.models.ChatUiState
import com.base.shared.models.SessionResponse
import com.base.shared.network.auth.SessionRepositoryImpl
import com.base.shared.network.chat.ChatRepositoryImpl
import com.base.shared.utils.getPlatform
import com.base.shared.utils.PlatformType
import com.base.shared.utils.ResponsiveUtils
import com.base.shared.viewModels.ChatViewModel
import com.base.shared.viewModels.SessionViewModel
import kotlinx.coroutines.launch

// Utility function to decode HTML entities
private fun decodeHtmlEntities(text: String): String {
    return text
        .replace("&amp;#x27;", "'")
        .replace("&#x27;", "'")
        .replace("&amp;quot;", "\"")
        .replace("&quot;", "\"")
        .replace("&amp;lt;", "<")
        .replace("&lt;", "<")
        .replace("&amp;gt;", ">")
        .replace("&gt;", ">")
        .replace("&amp;amp;", "&")
        .replace("&amp;", "&")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    vm: ChatViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {

    val uiState by vm.state.collectAsState()
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            // in ChatScreen top bar
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                title = { Text("Chat") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                input = input,
                onInputChange = { input = it },
                onSendMessage = {
                    scope.launch {
                        vm.sendMessage(input.trim())
                        input = ""
                    }
                },
                enabled = input.isNotBlank()
            )
        }
    ) { padding ->

        when (uiState) {
            ChatUiState.Loading ->
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator()
                }

            is ChatUiState.Error ->
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Text((uiState as ChatUiState.Error).message)
                }

            is ChatUiState.History -> {
                val history = (uiState as ChatUiState.History).messages
                val listState = rememberLazyListState()
                
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding() + 8.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(history) { msg ->
                        ChatMessageBubble(message = msg)
                    }
                }
                
                // Auto-scroll to bottom when new messages arrive - more gentle scrolling
                LaunchedEffect(history.size) {
                    if (history.isNotEmpty()) {
                        // Use scrollToItem to avoid aggressive animation that can push content too far
                        listState.scrollToItem(history.size - 1)
                    }
                }
            }

            ChatUiState.Idle -> { /* empty */ }
        }
    }
}

// Preview Composables for development and testing

/**
 * Preview function that demonstrates the ChatScreen with different UI states.
 * 
 * For Android: Use @Preview annotation in androidApp module
 * For Desktop: Create preview window with this function
 * For Web/iOS: Use in development builds for testing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenContentPreview(
    state: ChatUiState = ChatUiState.Idle,
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var input by remember { mutableStateOf("Sample message") }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                title = { Text("Chat") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                input = input,
                onInputChange = { input = it },
                onSendMessage = { /* No-op for preview */ },
                enabled = input.isNotBlank()
            )
        }
    ) { padding ->
        when (state) {
            ChatUiState.Loading ->
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator()
                }

            is ChatUiState.Error ->
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Text(state.message)
                }

            is ChatUiState.History -> {
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
                    items(state.messages) { msg ->
                        ChatMessageBubble(message = msg)
                    }
                }
            }

            ChatUiState.Idle -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
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
}

// Specific preview states for easy testing
object ChatScreenPreviews {
    @Composable
    fun Idle() = ChatScreenContentPreview(ChatUiState.Idle)
    
    @Composable
    fun Loading() = ChatScreenContentPreview(ChatUiState.Loading)
    
    @Composable
    fun Error() = ChatScreenContentPreview(
        ChatUiState.Error("Network connection failed. Please try again.")
    )
    
    @Composable
    fun WithMessages() {
        val sampleMessages = listOf(
            ChatMessage(text = "Hello! How can I help you today?", fromUser = false),
            ChatMessage(text = "I need help with my project setup", fromUser = true),
            ChatMessage(text = "I'd be happy to help! What specific part of the setup are you having trouble with?", fromUser = false),
            ChatMessage(text = "I'm trying to configure the database connection", fromUser = true),
            ChatMessage(text = "Great! Let me guide you through the database configuration process...", fromUser = false)
        )
        ChatScreenContentPreview(ChatUiState.History(sampleMessages))
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    if (message.fromUser) {
        // User message - aligned to the right with blue background
        Row(
            modifier = modifier.fillMaxWidth(),
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
                    text = message.text,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    } else {
        // AI message - aligned to the left with avatar and gray background
        Row(
            modifier = modifier.fillMaxWidth(),
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
                    fontWeight = FontWeight.Bold
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
                    text = message.text,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Surface(
        modifier = modifier,
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
                value = input,
                onValueChange = onInputChange,
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
                maxLines = 4,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (enabled) {
                            onSendMessage()
                            keyboardController?.hide()
                        }
                    }
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Send button
            IconButton(
                onClick = onSendMessage,
                enabled = enabled,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Send message",
                    tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenWithHistory(
    token: String,
    onLogout: () -> Unit
) {
    // Session management
    val sessionVm = remember { SessionViewModel(token = token) }
    val sessionState by sessionVm.state.collectAsState()
    
    // Current active session state
    var activeSession by remember { mutableStateOf<SessionResponse?>(null) }
    
    // Update activeSession when sessions are renamed
    LaunchedEffect(sessionState, activeSession?.sessionId) {
        val currentState = sessionState
        val currentActiveSession = activeSession
        if (currentState is SessionViewModel.State.Ready && currentActiveSession != null) {
            // Find the updated session data for the current active session
            val updatedSession = currentState.sessions.find { it.sessionId == currentActiveSession.sessionId }
            if (updatedSession != null && updatedSession != currentActiveSession) {
                activeSession = updatedSession
            }
        }
    }
    
    // Chat state for active session
    val chatVm = remember(activeSession) {
        activeSession?.let { session ->
            ChatViewModel(
                repo = ChatRepositoryImpl(session.token.accessToken),
                sessionRepo = SessionRepositoryImpl(token), // Keep user token for session management
                initialSession = session,
                onSessionUpdated = { sessionVm.reload() } // Refresh session list when session is updated
            )
        }
    }
    
    val chatState by (chatVm?.state?.collectAsState() ?: mutableStateOf(ChatUiState.Idle))
    
    // Input state
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    // Platform and responsive behavior
    val platform = getPlatform()
    var sidebarCollapsed by remember { mutableStateOf(false) }
    
    // Check if we have multiple sessions, named sessions, or current chat has history to show sidebar
    val hasSessions = sessionState is SessionViewModel.State.Ready && 
                     ((sessionState as SessionViewModel.State.Ready).sessions.run {
                         size > 1 || any { it.name.isNotBlank() }
                     } || chatState is ChatUiState.History) // Show sidebar if we have chat history loaded (even if empty)
    
    if (hasSessions) {
        when (platform) {
            PlatformType.WEB -> {
                // Web: Use persistent sidebar with collapse functionality
                ResponsiveWebChatLayout(
                    sessionState = sessionState,
                    activeSession = activeSession,
                    chatState = chatState,
                    input = input,
                    sidebarCollapsed = sidebarCollapsed,
                    onInputChange = { input = it },
                    onSessionSelect = { session -> activeSession = session },
                    onNewChat = {
                        scope.launch {
                            val newSession = sessionVm.createAndUseNewSession()
                            activeSession = newSession
                        }
                    },
                    onSendMessage = {
                        scope.launch {
                            // Create session on-demand if none exists (prevents empty sessions)
                            val currentSession = if (activeSession == null) {
                                val newSession = sessionVm.createAndUseNewSession()
                                activeSession = newSession
                                newSession
                            } else {
                                activeSession
                            }
                            
                            // Create ChatViewModel directly if needed to avoid race condition
                            val currentChatVm = chatVm ?: currentSession?.let { session ->
                                ChatViewModel(
                                    repo = ChatRepositoryImpl(session.token.accessToken),
                                    sessionRepo = SessionRepositoryImpl(token),
                                    initialSession = session,
                                    onSessionUpdated = { sessionVm.reload() }
                                )
                            }
                            
                            currentChatVm?.sendMessage(input.trim())
                            input = ""
                        }
                    },
                    onToggleSidebar = { sidebarCollapsed = !sidebarCollapsed },
                    onLogout = onLogout,
                    sessionVm = sessionVm
                )
            }
            
            else -> {
                // Mobile/Desktop: Use drawer-based layout
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ChatHistorySidebar(
                            sessionState = sessionState,
                            activeSession = activeSession,
                            chatState = chatState,
                            onSessionSelect = { session ->
                                activeSession = session
                                scope.launch { drawerState.close() }
                            },
                            onNewChat = {
                                scope.launch {
                                    val newSession = sessionVm.createAndUseNewSession()
                                    activeSession = newSession
                                    drawerState.close()
                                }
                            },
                            onLogout = onLogout,
                            sessionVm = sessionVm
                        )
                    }
                ) {
                    ChatContent(
                        chatState = chatState,
                        activeSession = activeSession,
                        input = input,
                        onInputChange = { input = it },
                        onSendMessage = {
                            scope.launch {
                                // Create session on-demand if none exists (prevents empty sessions)
                                val currentSession = if (activeSession == null) {
                                    val newSession = sessionVm.createAndUseNewSession()
                                    activeSession = newSession
                                    newSession
                                } else {
                                    activeSession
                                }
                                
                                // Create ChatViewModel directly if needed to avoid race condition
                                val currentChatVm = chatVm ?: currentSession?.let { session ->
                                    ChatViewModel(
                                        repo = ChatRepositoryImpl(session.token.accessToken),
                                        sessionRepo = SessionRepositoryImpl(token),
                                        initialSession = session,
                                        onSessionUpdated = { sessionVm.reload() }
                                    )
                                }
                                
                                currentChatVm?.sendMessage(input.trim())
                                input = ""
                            }
                        },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onLogout = onLogout,
                        showMenuButton = true
                    )
                }
            }
        }
    } else {
        // Show without sidebar when no sessions or only one session
        ChatContent(
            chatState = chatState,
            activeSession = activeSession,
            input = input,
            onInputChange = { input = it },
            onSendMessage = {
                scope.launch {
                    // Create session on-demand if none exists (prevents empty sessions)
                    if (activeSession == null) {
                        val newSession = sessionVm.createAndUseNewSession()
                        activeSession = newSession
                    }
                    chatVm?.sendMessage(input.trim())
                    input = ""
                }
            },
            onMenuClick = { },
            onLogout = onLogout,
            showMenuButton = false
        )
    }
}

@Composable
private fun ChatHistorySidebar(
    sessionState: SessionViewModel.State,
    activeSession: SessionResponse?,
    chatState: ChatUiState,
    onSessionSelect: (SessionResponse) -> Unit,
    onNewChat: () -> Unit,
    onLogout: () -> Unit,
    sessionVm: SessionViewModel
) {
    val scope = rememberCoroutineScope()
    
    // Track which sessions have been checked for messages and their message counts
    var sessionMessageCounts by remember { mutableStateOf(mapOf<String, Int>()) }
    ModalDrawerSheet(
        modifier = Modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Chat History",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // New Chat Button
            OutlinedButton(
                onClick = onNewChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Chat")
            }
            
            HorizontalDivider()
            
            // Sessions List
            when (sessionState) {
                is SessionViewModel.State.Ready -> {
                    // Load message counts for sessions to filter out empty ones
                    LaunchedEffect(sessionState.sessions) {
                        sessionState.sessions.forEach { session ->
                            if (!sessionMessageCounts.containsKey(session.sessionId)) {
                                // Load message count for this session
                                try {
                                    val chatRepo = ChatRepositoryImpl(session.token.accessToken)
                                    val messages = chatRepo.loadChatHistory(session.sessionId)
                                    sessionMessageCounts = sessionMessageCounts + (session.sessionId to messages.size)
                                    println("KMP_LOG: [DEBUG] Session ${session.sessionId.take(8)} has ${messages.size} messages")
                                } catch (e: Exception) {
                                    println("KMP_LOG: [DEBUG] Failed to load messages for session ${session.sessionId.take(8)}: ${e.message}")
                                    // Mark as 0 messages if failed to load
                                    sessionMessageCounts = sessionMessageCounts + (session.sessionId to 0)
                                }
                            }
                        }
                    }
                    
                    // Filter sessions to only show those with messages (ignore custom names if no messages)
                    val sessionsWithContent = sessionState.sessions.filter { session ->
                        val messageCount = sessionMessageCounts[session.sessionId] ?: -1 // -1 means not loaded yet
                        messageCount > 0 // Only show sessions that actually have messages
                    }
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(sessionsWithContent, key = { it.sessionId }) { session ->
                            val messageCount = sessionMessageCounts[session.sessionId] ?: -1
                            SessionListItem(
                                session = session,
                                isActive = session.sessionId == activeSession?.sessionId,
                                onSessionSelect = { onSessionSelect(session) },
                                onRenameSession = { newName ->
                                    scope.launch {
                                        println("KMP_LOG: [DEBUG] Renaming session ${session.sessionId.take(8)} to '$newName'")
                                        sessionVm.renameSession(session.sessionId, newName, session.token.accessToken)
                                        println("KMP_LOG: [DEBUG] Rename completed, sidebar should stay open")
                                        // Don't automatically select the renamed session or close the drawer
                                    }
                                },
                                messageCount = messageCount
                            )
                        }
                    }
                }
                SessionViewModel.State.Loading -> {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is SessionViewModel.State.Error -> {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            sessionState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatContent(
    chatState: ChatUiState,
    activeSession: SessionResponse?,
    input: String,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onMenuClick: () -> Unit,
    onLogout: () -> Unit,
    showMenuButton: Boolean
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    if (showMenuButton) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    }
                },
                title = { 
                    Text(
                        activeSession?.name?.ifBlank { "New Chat" } ?: "Chat"
                    ) 
                },
                actions = {
                    if (!showMenuButton) {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                input = input,
                onInputChange = onInputChange,
                onSendMessage = onSendMessage,
                enabled = input.isNotBlank() // Session will be created on-demand
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        when (chatState) {
            ChatUiState.Loading ->
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading chat...")
                    }
                }
            
            is ChatUiState.Error ->
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            chatState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Please check your connection and try again.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            
            is ChatUiState.History -> {
                val history = chatState.messages
                val listState = rememberLazyListState()
                
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding() + 8.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(history) { msg ->
                        ChatMessageBubble(message = msg)
                    }
                }
                
                // Auto-scroll to bottom when new messages arrive - more gentle scrolling
                LaunchedEffect(history.size) {
                    if (history.isNotEmpty()) {
                        // Use scrollToItem to avoid aggressive animation that can push content too far
                        listState.scrollToItem(history.size - 1)
                    }
                }
            }
            
            ChatUiState.Idle -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
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
}

// Session List Item with Edit Functionality
@Composable
private fun SessionListItem(
    session: SessionResponse,
    isActive: Boolean,
    onSessionSelect: () -> Unit,
    onRenameSession: (String) -> Unit,
    messageCount: Int = -1, // -1 means not loaded yet
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(decodeHtmlEntities(session.name)) }
    
    // Decode HTML entities in session name for display
    val displayName = decodeHtmlEntities(session.name)
    
    ListItem(
        headlineContent = {
            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier
                            .weight(1f)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.key == Key.Enter) {
                                    val trimmedText = editText.trim()
                                    if (trimmedText.isNotBlank()) {
                                        onRenameSession(trimmedText)
                                        isEditing = false
                                    }
                                    // If empty, don't save and don't exit edit mode
                                    true
                                } else {
                                    false
                                }
                            },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Confirm button
                    IconButton(
                        onClick = {
                            val trimmedText = editText.trim()
                            if (trimmedText.isNotBlank()) {
                                onRenameSession(trimmedText)
                                isEditing = false
                            }
                            // If empty, don't save and don't exit edit mode
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
                            modifier = Modifier.size(16.dp),
                            tint = if (editText.trim().isNotBlank()) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    }
                    
                    // Cancel button
                    IconButton(
                        onClick = {
                            editText = decodeHtmlEntities(session.name)
                            isEditing = false
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName.ifBlank { "Chat" },
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Edit button (always show for all sessions)
                    IconButton(
                        onClick = {
                            editText = decodeHtmlEntities(session.name)
                            isEditing = true
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit title",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        },
        modifier = modifier
            .clickable {
                if (!isEditing) {
                    onSessionSelect()
                }
            }
            .then(
                if (isActive) {
                    Modifier.background(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                } else Modifier
            )
    )
}

// Responsive Web Chat Layout with Persistent Sidebar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResponsiveWebChatLayout(
    sessionState: SessionViewModel.State,
    activeSession: SessionResponse?,
    chatState: ChatUiState,
    input: String,
    sidebarCollapsed: Boolean,
    onInputChange: (String) -> Unit,
    onSessionSelect: (SessionResponse) -> Unit,
    onNewChat: () -> Unit,
    onSendMessage: () -> Unit,
    onToggleSidebar: () -> Unit,
    onLogout: () -> Unit,
    sessionVm: SessionViewModel
) {
    val scope = rememberCoroutineScope()
    
    // Track which sessions have been checked for messages and their message counts
    var sessionMessageCounts by remember { mutableStateOf(mapOf<String, Int>()) }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Persistent Sidebar
        if (!sidebarCollapsed) {
            Surface(
                modifier = Modifier.width(280.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header with collapse button
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Chat History",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Row {
                                // Collapse sidebar button
                                IconButton(onClick = onToggleSidebar) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Hide sidebar",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                IconButton(onClick = onLogout) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "Logout",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    
                    // New Chat Button
                    OutlinedButton(
                        onClick = onNewChat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Chat")
                    }
                    
                    HorizontalDivider()
                    
                    // Sessions List
                    when (sessionState) {
                        is SessionViewModel.State.Ready -> {
                            // Load message counts for sessions to filter out empty ones
                            LaunchedEffect(sessionState.sessions) {
                                sessionState.sessions.forEach { session ->
                                    if (!sessionMessageCounts.containsKey(session.sessionId)) {
                                        // Load message count for this session
                                        try {
                                            val chatRepo = ChatRepositoryImpl(session.token.accessToken)
                                            val messages = chatRepo.loadChatHistory(session.sessionId)
                                            sessionMessageCounts = sessionMessageCounts + (session.sessionId to messages.size)
                                        } catch (e: Exception) {
                                            // Mark as 0 messages if failed to load
                                            sessionMessageCounts = sessionMessageCounts + (session.sessionId to 0)
                                        }
                                    }
                                }
                            }
                            
                            // Filter sessions to only show those with messages
                            val sessionsWithContent = sessionState.sessions.filter { session ->
                                val messageCount = sessionMessageCounts[session.sessionId] ?: -1
                                messageCount > 0
                            }
                            
                            LazyColumn(
                                modifier = Modifier.weight(1f)
                            ) {
                                items(sessionsWithContent, key = { it.sessionId }) { session ->
                                    val messageCount = sessionMessageCounts[session.sessionId] ?: -1
                                    SessionListItem(
                                        session = session,
                                        isActive = session.sessionId == activeSession?.sessionId,
                                        onSessionSelect = { onSessionSelect(session) },
                                        onRenameSession = { newName ->
                                            scope.launch {
                                                sessionVm.renameSession(session.sessionId, newName, session.token.accessToken)
                                            }
                                        },
                                        messageCount = messageCount
                                    )
                                }
                            }
                        }
                        SessionViewModel.State.Loading -> {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is SessionViewModel.State.Error -> {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    sessionState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Main Chat Area
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Top bar with show sidebar button when collapsed
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (sidebarCollapsed) {
                        IconButton(onClick = onToggleSidebar) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Show sidebar"
                            )
                        }
                    }
                    
                    Text(
                        text = activeSession?.name?.ifBlank { "New Chat" } ?: "Chat",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (sidebarCollapsed) {
                        IconButton(onClick = onLogout) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout"
                            )
                        }
                    }
                }
            }
            
            // Chat Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (chatState) {
                    ChatUiState.Loading ->
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Loading chat...")
                            }
                        }
                    
                    is ChatUiState.Error ->
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    chatState.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Please check your connection and try again.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    
                    is ChatUiState.History -> {
                        val history = chatState.messages
                        val listState = rememberLazyListState()
                        
                        // Centered content area for better readability on wide screens
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(
                                    top = 16.dp,
                                    bottom = 16.dp,
                                    start = 16.dp,
                                    end = 16.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .widthIn(max = 800.dp) // Max width for readability
                                    .fillMaxHeight()
                            ) {
                                items(history) { msg ->
                                    WebChatMessageBubble(message = msg)
                                }
                            }
                        }
                        
                        // Auto-scroll to bottom when new messages arrive
                        LaunchedEffect(history.size) {
                            if (history.isNotEmpty()) {
                                listState.scrollToItem(history.size - 1)
                            }
                        }
                    }
                    
                    ChatUiState.Idle -> {
                        Box(
                            Modifier.fillMaxSize(),
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
            
            // Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ChatInputBar(
                        input = input,
                        onInputChange = onInputChange,
                        onSendMessage = onSendMessage,
                        enabled = input.isNotBlank(),
                        modifier = Modifier
                            .widthIn(max = 800.dp) // Match the max width of chat content
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

// Web-optimized chat message bubble with better spacing
@Composable
private fun WebChatMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    if (message.fromUser) {
        // User message - aligned to the right with blue background
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .widthIn(max = 480.dp) // Wider max width for web
            ) {
                Text(
                    text = message.text,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    } else {
        // AI message - aligned to the left with avatar and gray background
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // AI Avatar
            Box(
                modifier = Modifier
                    .size(40.dp) // Slightly larger for web
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Message bubble
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .widthIn(max = 480.dp) // Wider max width for web
            ) {
                Text(
                    text = message.text,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
