import androidx.compose.runtime.*
import com.meridia.shared.CommonView
import com.meridia.shared.auth.AuthModule
import kotlinx.coroutines.launch


@Composable
fun MainViewWeb() {
    val coroutineScope = rememberCoroutineScope()
    var isInitialized by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                AuthModule.initialize(null) // WASM doesn't need platform context
                AuthModule.startAuthManager()
                isInitialized = true
            } catch (e: Exception) {
                println("Failed to initialize AuthModule: ${e.message}")
                isInitialized = true // Show UI anyway
            }
        }
    }
    
    if (isInitialized) {
        CommonView()
    }
}


