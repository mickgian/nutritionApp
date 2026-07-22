package com.base.shared.utils

enum class PlatformType {
    ANDROID,
    IOS,
    WEB,
    DESKTOP
}

expect fun getPlatform(): PlatformType

// Screen size categories for responsive design
enum class ScreenSize {
    COMPACT,   // < 600dp (phones)
    MEDIUM,    // 600dp - 840dp (tablets, small desktops)
    EXPANDED   // > 840dp (large tablets, desktops)
}

// Helper functions for responsive design
object ResponsiveUtils {
    fun isCompactWidth(windowWidthDp: Int): Boolean = windowWidthDp < 600
    fun isMediumWidth(windowWidthDp: Int): Boolean = windowWidthDp in 600..839
    fun isExpandedWidth(windowWidthDp: Int): Boolean = windowWidthDp >= 840
    
    fun getScreenSize(windowWidthDp: Int): ScreenSize = when {
        windowWidthDp < 600 -> ScreenSize.COMPACT
        windowWidthDp < 840 -> ScreenSize.MEDIUM
        else -> ScreenSize.EXPANDED
    }
}