package com.base.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.base.shared.auth.AuthModule
import com.base.shared.utils.Logger
import com.base.shared.view.AppViewAndroid

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the authentication module with Android context
        Logger.info("MainActivity: Initializing AuthModule", "BaseAppInit")
        try {
            AuthModule.initialize(this)
            Logger.info("MainActivity: AuthModule initialized successfully", "BaseAppInit")
            // Note: AuthModule.initialize() now automatically starts the AuthManager
        } catch (e: Exception) {
            Logger.error("MainActivity: Failed to initialize AuthModule", "BaseAppInit", e)
        }
        
        setContent {
            enableEdgeToEdge()
            AppViewAndroid()
        }
    }
}
