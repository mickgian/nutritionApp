# Compose Multiplatform Preview Guide

This guide explains how to use preview functionality for your Compose Multiplatform screens across different platforms.

## 📱 Platform Support

| Platform | Preview Method | Live Updates | Interactive |
|----------|---------------|--------------|-------------|
| **Android** | `@Preview` annotations | ✅ | ✅ |
| **Desktop** | Preview windows | ✅ | ✅ |
| **iOS** | SwiftUI preview reference | ❌ | ❌ |
| **Web** | Development builds | ✅ | ✅ |

## 🎨 Available Previews

### Screen Previews
- **ChatScreen**: Idle, Loading, Error, With Messages states
- **RegistrationScreen**: Idle, Loading, Success, Error, Password Validation states
- **LoginScreen**: Ready for implementation

### Preview Files
```
shared/src/commonMain/kotlin/com/base/shared/
├── previews/ScreenPreviews.kt          # Common preview logic
├── screens/ChatScreen.kt               # Chat screen with previews
└── screens/RegistrationScreen.kt       # Registration screen with previews

androidApp/src/main/java/com/base/android/
└── previews/ScreenPreviews.kt          # Android @Preview implementations

desktopApp/src/jvmMain/kotlin/
└── DesktopPreviews.kt                  # Desktop preview windows
```

## 🤖 Android Previews

### Setup
1. Open Android Studio
2. Navigate to `androidApp/src/main/java/com/base/android/SimpleAndroidPreviews.kt`
3. The preview panel will show all available previews

### Available Previews
- **Chat Screen**: Empty, With Messages, Loading states
- **Registration Screen**: Idle, Loading, Error states
- **Dark Theme Variants**: All screens in dark mode
- **Simplified Dependencies**: Uses only Material3 components

### Usage
```kotlin
@Preview(name = "Chat - With Messages", showBackground = true)
@Composable
fun ChatScreenWithMessagesPreview() {
    MaterialTheme {
        // Simplified chat layout without ViewModel dependencies
    }
}
```

### Features
- **Live Updates**: Changes reflect immediately
- **No Complex Dependencies**: Works without shared module issues
- **Multiple States**: All major UI states covered
- **Theme Support**: Light and dark theme previews
- **Layout Focus**: Shows structure and styling without business logic

### Note
These are simplified previews that focus on layout and visual design. For full functionality testing with real ViewModels and data, use the **Desktop Preview System** (`./gradlew desktopApp:run`).

## 🖥️ Desktop Previews

### Setup
1. Navigate to `desktopApp/src/jvmMain/kotlin/DesktopPreviews.kt`
2. Run the main function to open preview windows

### Features
- **Multiple Windows**: Each screen in its own window
- **Interactive**: Full functionality testing
- **Preview Gallery**: All screens and states in one interface
- **Live Updates**: Hot reload when code changes

### Usage
```bash
# Run desktop previews
./gradlew desktopApp:run
```

### Window Options
- **Preview Gallery**: Interactive gallery with all screens and states
- **Individual Screens**: Dedicated windows for specific screens
- **Custom Configurations**: Modify window sizes and states

## 🕸️ Web Previews

### Setup
1. Use the shared preview components in your web build
2. Create dedicated preview pages for development

### Implementation Example
```kotlin
@Composable
fun WebPreviewPage() {
    MaterialTheme {
        Surface {
            PlatformPreviews.WebComponentTest(
                screenName = "Chat",
                stateName = "With Messages"
            )
        }
    }
}
```

### Usage
```bash
# Run web development build
./gradlew jsApp:jsBrowserDevelopmentRun
```

## 📱 iOS Previews

### Current Status
- iOS previews require SwiftUI wrapper implementation
- Use shared preview logic as reference for SwiftUI previews
- Consider creating UIHostingController wrappers for Compose content

### Recommended Approach
1. Create SwiftUI views that mirror the Compose previews
2. Use `ChatScreenContentPreview` as reference for layout
3. Implement preview states in SwiftUI

## 🎯 Quick Start

### 1. Android Studio
1. Open the project in Android Studio
2. Go to `androidApp/.../previews/ScreenPreviews.kt`
3. See previews in the preview panel (right side)

### 2. Desktop Development
```bash
# Run desktop preview gallery
./gradlew desktopApp:run
```

### 3. Add New Screen Preview
```kotlin
// 1. In your screen file, add preview composable
@Composable
fun YourScreenContentPreview(
    state: YourViewModel.State = YourViewModel.State.Idle
) {
    // Screen content without ViewModel dependencies
}

// 2. In ScreenPreviews.kt, add to gallery
@Composable
private fun YourScreenPreviewForState(state: String) {
    when (state) {
        "Idle" -> YourScreenContentPreview(YourViewModel.State.Idle)
        "Loading" -> YourScreenContentPreview(YourViewModel.State.Loading)
        // ... other states
    }
}

// 3. In Android previews, add @Preview functions
@Preview(name = "Your Screen - Idle", showBackground = true)
@Composable
fun YourScreenIdlePreview() {
    TravelAppTheme {
        YourScreenContentPreview(YourViewModel.State.Idle)
    }
}
```

## 🛠️ Development Workflow

### For UI Development
1. **Start with previews**: Create preview composables first
2. **Test all states**: Verify loading, error, and success states
3. **Cross-platform check**: Test on both Android and Desktop
4. **Theme variants**: Check light and dark themes

### For Testing
1. **State verification**: Ensure all UI states render correctly
2. **Layout testing**: Check different screen sizes
3. **Interaction testing**: Verify button states and user flows
4. **Visual regression**: Use previews to catch layout issues

## 🎨 Preview Best Practices

### DO
✅ Create preview composables that don't depend on ViewModels  
✅ Test all possible UI states  
✅ Use meaningful sample data  
✅ Include both light and dark theme previews  
✅ Test different screen sizes  
✅ Use descriptive preview names  

### DON'T
❌ Include network calls or side effects in previews  
❌ Use real ViewModels with dependencies  
❌ Forget to handle error states  
❌ Use empty or meaningless preview data  
❌ Skip edge cases in UI states  

## 🔧 Troubleshooting

### Android Studio Preview Issues
- **Not showing**: Check if `@Preview` annotations are present
- **Compilation errors**: Ensure no ViewModel dependencies
- **Outdated**: Refresh with ⟲ button in preview panel

### Desktop Preview Issues
- **Won't start**: Check Gradle configuration and dependencies
- **Blank window**: Verify theme and surface setup
- **Crashes**: Check for missing dependencies in desktop module

### Common Issues
- **ViewModel errors**: Use preview-specific composables without VMs
- **Missing dependencies**: Ensure all required imports are included
- **State issues**: Verify all preview states are valid

## 📚 Examples

Check these files for implementation examples:
- `shared/.../previews/ScreenPreviews.kt` - Complete preview gallery
- `androidApp/.../previews/ScreenPreviews.kt` - Android-specific previews
- `desktopApp/.../DesktopPreviews.kt` - Desktop preview windows

## 🚀 Next Steps

1. **Implement Login Screen previews** following the same pattern
2. **Add iOS SwiftUI previews** for complete platform coverage
3. **Create component-level previews** for reusable UI components
4. **Set up automated screenshot testing** using preview compositions

---

Happy previewing! 🎉