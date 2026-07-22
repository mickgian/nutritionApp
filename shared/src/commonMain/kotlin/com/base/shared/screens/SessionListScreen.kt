package com.base.shared.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.base.shared.models.SessionResponse
import com.base.shared.viewModels.SessionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    vm: SessionViewModel,
    onPick: (SessionResponse) -> Unit,
    onLogout: () -> Unit,
) {
    val uiState by vm.state.collectAsState()
    val scope   = rememberCoroutineScope()

    /* If there is NO session yet → transparently create one */
    LaunchedEffect(uiState) {
        if (uiState is SessionViewModel.State.Ready &&
            (uiState as SessionViewModel.State.Ready).sessions.isEmpty()
        ) {
            val first = vm.createAndUseNewSession()
            onPick(first)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title   = { Text("Your chats") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    val newSession = vm.createAndUseNewSession()
                    /*  guarantee state-first, navigation-second  */
                    onPick(newSession)
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "New chat")
            }
        }
    ) { pad ->

        when (uiState) {
            SessionViewModel.State.Loading ->
                Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) {
                    CircularProgressIndicator()
                }

            is SessionViewModel.State.Error ->
                Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) {
                    Text((uiState as SessionViewModel.State.Error).message)
                }

            is SessionViewModel.State.Ready -> {
                val sessions = (uiState as SessionViewModel.State.Ready).sessions
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(pad)
                ) {
                    items(sessions, key = { it.sessionId }) { s ->
                        ListItem(
                            headlineContent   = { Text(s.name.ifBlank { "Untitled chat" }) },
                            supportingContent = { Text("id: ${s.sessionId}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    /* same guarantee as FAB above                        */
                                    scope.launch { onPick(s) }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

