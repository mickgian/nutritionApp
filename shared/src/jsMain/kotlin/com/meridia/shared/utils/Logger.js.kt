package com.meridia.shared.utils

actual fun platformLog(level: Logger.Level, tag: String, message: String) {
    val fullMessage = "[$tag] $message"
    when (level) {
        Logger.Level.DEBUG -> console.log("🔍 DEBUG: $fullMessage")
        Logger.Level.INFO -> console.info("ℹ️ INFO: $fullMessage")
        Logger.Level.WARN -> console.warn("⚠️ WARN: $fullMessage")
        Logger.Level.ERROR -> console.error("❌ ERROR: $fullMessage")
    }
}