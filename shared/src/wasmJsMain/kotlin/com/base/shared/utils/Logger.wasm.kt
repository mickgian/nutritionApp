package com.base.shared.utils

actual fun platformLog(level: Logger.Level, tag: String, message: String) {
    when (level) {
        Logger.Level.DEBUG -> println("DEBUG [$tag]: $message")
        Logger.Level.INFO -> println("INFO [$tag]: $message")
        Logger.Level.WARN -> println("WARN [$tag]: $message")
        Logger.Level.ERROR -> println("ERROR [$tag]: $message")
    }
}