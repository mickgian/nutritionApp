package com.base.shared.utils

import android.util.Log

actual fun platformLog(level: Logger.Level, tag: String, message: String) {
    // Add a prefix to make our logs easily identifiable
    val prefixedTag = "BaseApp_$tag"
    val prefixedMessage = "KMP_LOG: $message"
    
    when (level) {
        Logger.Level.DEBUG -> Log.d(prefixedTag, prefixedMessage)
        Logger.Level.INFO -> Log.i(prefixedTag, prefixedMessage)
        Logger.Level.WARN -> Log.w(prefixedTag, prefixedMessage)
        Logger.Level.ERROR -> Log.e(prefixedTag, prefixedMessage)
    }
}