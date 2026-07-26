import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.meridia.shared.AppViewDesktop

fun main() = application {
    Window(title = "Meridia", onCloseRequest = ::exitApplication) {
        AppViewDesktop()
    }
}