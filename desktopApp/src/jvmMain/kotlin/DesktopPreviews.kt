import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.base.shared.TravelAppTheme
import com.base.shared.previews.PlatformPreviews
import com.base.shared.previews.QuickPreviews
import com.base.shared.previews.ScreenPreviewGallery

/**
 * Desktop preview implementation for Compose Multiplatform screens.
 * 
 * Run this file to open preview windows for your screens.
 * Use this for rapid UI development and testing on desktop.
 */

fun main() = application {
    var showGallery by remember { mutableStateOf(true) }
    var showChatPreview by remember { mutableStateOf(false) }
    var showRegistrationPreview by remember { mutableStateOf(false) }
    
    // Main preview selector window
    Window(
        onCloseRequest = ::exitApplication,
        title = "Screen Previews - Base KMP AI Agent",
        state = WindowState(width = 400.dp, height = 300.dp)
    ) {
        TravelAppTheme {
            Surface {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Screen Previews",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = { showGallery = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Preview Gallery")
                    }
                    
                    OutlinedButton(
                        onClick = { showChatPreview = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Chat Screen Preview")
                    }
                    
                    OutlinedButton(
                        onClick = { showRegistrationPreview = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Registration Screen Preview")
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        "Choose a preview option above to see your screens in action.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
    
    // Preview Gallery Window
    if (showGallery) {
        Window(
            onCloseRequest = { showGallery = false },
            title = "Preview Gallery",
            state = WindowState(width = 1200.dp, height = 800.dp)
        ) {
            TravelAppTheme {
                Surface {
                    ScreenPreviewGallery()
                }
            }
        }
    }
    
    // Chat Screen Preview Window
    if (showChatPreview) {
        Window(
            onCloseRequest = { showChatPreview = false },
            title = "Chat Screen Preview",
            state = WindowState(width = 400.dp, height = 700.dp)
        ) {
            TravelAppTheme {
                Surface {
                    QuickPreviews.ChatWithMessages()
                }
            }
        }
    }
    
    // Registration Screen Preview Window
    if (showRegistrationPreview) {
        Window(
            onCloseRequest = { showRegistrationPreview = false },
            title = "Registration Screen Preview",
            state = WindowState(width = 400.dp, height = 700.dp)
        ) {
            TravelAppTheme {
                Surface {
                    QuickPreviews.RegistrationWithValidation()
                }
            }
        }
    }
}

/**
 * Alternative main function for running specific previews directly.
 * Uncomment the preview you want to see and comment out the main() function above.
 */

/*
// Run this to see the full preview gallery
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Preview Gallery",
        state = WindowState(width = 1200.dp, height = 800.dp)
    ) {
        TravelAppTheme {
            Surface {
                PlatformPreviews.DesktopPreviewWindow()
            }
        }
    }
}
*/

/*
// Run this to see just the chat screen with messages
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Chat Screen Preview",
        state = WindowState(width = 400.dp, height = 700.dp)
    ) {
        TravelAppTheme {
            Surface {
                QuickPreviews.ChatWithMessages()
            }
        }
    }
}
*/

/*
// Run this to see just the registration screen
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Registration Screen Preview",
        state = WindowState(width = 400.dp, height = 700.dp)
    ) {
        TravelAppTheme {
            Surface {
                QuickPreviews.RegistrationWithValidation()
            }
        }
    }
}
*/