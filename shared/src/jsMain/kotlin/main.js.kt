import androidx.compose.runtime.*
import com.base.shared.CommonView
import com.base.shared.auth.AuthModule
import kotlinx.coroutines.launch


@Composable
fun MainViewWeb() {
    val coroutineScope = rememberCoroutineScope()
    var isInitialized by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                println("Web: Initializing AuthModule...")
                AuthModule.initialize(null) // Web doesn't need platform context
                println("Web: Starting AuthManager...")
                AuthModule.startAuthManager()
                println("Web: AuthModule initialization complete")
                isInitialized = true
            } catch (e: Exception) {
                println("Web: Failed to initialize AuthModule: ${e.message}")
                console.error("Web AuthModule Error:", e)
                isInitialized = true // Show UI anyway
            }
        }
    }
    
    if (isInitialized) {
        CommonView()
    }
}


