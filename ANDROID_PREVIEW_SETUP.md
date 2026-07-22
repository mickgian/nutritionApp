# Android Preview Setup Guide

## Current Status

The Android preview functionality requires additional setup due to dependency resolution issues between the Android app module and the shared Compose Multiplatform module.

## Issue Analysis

**Root Cause**: The Android app module doesn't have direct access to:
- Compose UI tooling dependencies (`@Preview`, `@Composable`)
- Shared theme (`TravelAppTheme`) 
- kotlinx.datetime classes used in `ChatMessage`

## Solution Options

### Option 1: Move Previews to Shared Module (Recommended)

Instead of Android-specific previews, use the existing preview system in the shared module:

1. **Desktop Previews** (Already Working ✅)
   ```bash
   ./gradlew desktopApp:run
   ```

2. **Shared Preview Gallery** (Already Working ✅)
   - Located in `shared/src/commonMain/kotlin/com/base/shared/previews/ScreenPreviews.kt`
   - Provides comprehensive preview functionality
   - Works across all platforms

### Option 2: Fix Android Dependencies (Complex)

To enable Android Studio @Preview annotations, you would need to:

1. **Add Missing Dependencies to androidApp/build.gradle.kts**:
   ```kotlin
   dependencies {
       // ... existing dependencies
       
       // For kotlinx.datetime support
       implementation(libs.kotlinx.datetime)
       
       // For Compose tooling
       implementation("androidx.compose.ui:ui-tooling-preview")
       debugImplementation("androidx.compose.ui:ui-tooling")
       
       // Ensure theme access
       api(project(":shared"))  // Change from implementation to api
   }
   ```

2. **Create Android-Specific Theme Wrapper**:
   ```kotlin
   // In androidApp module
   @Composable
   fun AndroidPreviewTheme(content: @Composable () -> Unit) {
       MaterialTheme {  // Use MaterialTheme instead of TravelAppTheme
           content()
       }
   }
   ```

### Option 3: Simplified Android Previews (Quick Fix)

Create basic Android previews without complex dependencies:

```kotlin
@Preview(showBackground = true)
@Composable
fun SimpleChatPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Chat Preview", style = MaterialTheme.typography.headlineMedium)
            Text("This would show chat interface", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

## Recommended Approach

**Use the existing Desktop Preview System** which provides:

✅ **Full Functionality**: All screen states and interactions  
✅ **No Dependencies Issues**: Works out of the box  
✅ **Cross-Platform**: Same previews work everywhere  
✅ **Interactive**: Can test button clicks and UI flows  
✅ **Hot Reload**: Instant updates during development  

### How to Use Desktop Previews

1. **Quick Start**:
   ```bash
   ./gradlew desktopApp:run
   ```

2. **Preview Gallery**: Choose different screens and states interactively

3. **Individual Previews**: Comment/uncomment different main functions in `DesktopPreviews.kt`

## Alternative Android Preview Implementation

If you specifically need Android Studio previews, here's a minimal working example:

```kotlin
// Create this file: androidApp/src/main/java/com/base/android/SimpleAndroidPreviews.kt
package com.base.android

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name = "Chat Layout", showBackground = true)
@Composable
fun ChatLayoutPreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(title = { Text("Chat") })
            },
            bottomBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = "Sample message",
                        onValueChange = { },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message") }
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { }) {
                        Text("Send")
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sample chat messages
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text("Hello! How can I help?", modifier = Modifier.padding(12.dp))
                }
                Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text("I need help with setup", modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

@Preview(name = "Registration Layout", showBackground = true)
@Composable
fun RegistrationLayoutPreview() {
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
                value = "password123",
                onValueChange = { },
                label = { Text("Password") }
            )
            Spacer(Modifier.height(16.dp))
            
            Button(onClick = { }) {
                Text("Register")
            }
        }
    }
}
```

## Conclusion

**Recommended**: Use the existing Desktop Preview system (`./gradlew desktopApp:run`) for comprehensive preview functionality.

**If Android Studio previews are essential**: Implement the simplified approach above with basic Material3 components instead of the full shared module dependencies.

The desktop preview system provides superior functionality and avoids the complexity of cross-module dependency resolution in Android Gradle builds.