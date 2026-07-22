import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.base.shared.AppViewDesktop

fun main() = application {
    Window(title = "Base AI", onCloseRequest = ::exitApplication) {
        AppViewDesktop()
    }
}