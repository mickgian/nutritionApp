package com.base.shared.utils

import platform.Foundation.NSLog

actual fun platformLog(level: Logger.Level, tag: String, message: String) {
    NSLog("[$tag] $message")
}