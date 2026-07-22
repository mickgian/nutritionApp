package com.base.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.base.shared.auth.AuthModule
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    // Initialize AuthModule before creating the UI
    println("iOS: Starting AuthModule initialization...")
    try {
        AuthModule.initialize(null) // iOS doesn't need platform context
        println("iOS: AuthModule initialized successfully")
    } catch (e: Exception) {
        println("iOS: AuthModule initialization failed: ${e.message}")
        e.printStackTrace()
    }
    
    return ComposeUIViewController {
        AppViewIos()
    }
}
