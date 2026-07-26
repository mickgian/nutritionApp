package com.meridia.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.meridia.shared.auth.AuthModule
import com.meridia.shared.utils.Logger
import com.meridia.shared.view.AppViewAndroid

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the authentication module with Android context
        Logger.info("MainActivity: Initializing AuthModule", "MeridiaInit")
        try {
            AuthModule.initialize(this)
            Logger.info("MainActivity: AuthModule initialized successfully", "MeridiaInit")
            // Note: AuthModule.initialize() now automatically starts the AuthManager
        } catch (e: Exception) {
            Logger.error("MainActivity: Failed to initialize AuthModule", "MeridiaInit", e)
        }
        
        setContent {
            enableEdgeToEdge()
            AppViewAndroid()
        }
    }
}
