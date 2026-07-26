package com.meridia.shared.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The app-wide theme. Wraps Material 3 with Meridia's color scheme, typography and
 * shapes, and provides the extended brand palette via [LocalMeridiaColors].
 *
 * Usage:
 * ```
 * MeridiaTheme {
 *     // MeridiaTheme.colors.lime, MeridiaTheme.typography.displaySmall, …
 * }
 * ```
 */
@Composable
fun MeridiaTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMeridiaColors provides meridiaLightColors) {
        MaterialTheme(
            colorScheme = meridiaColorScheme,
            typography = MeridiaTypography,
            shapes = MeridiaShapes,
            content = content,
        )
    }
}

/**
 * Accessor object, mirroring Material 3's own `MaterialTheme` fun + object pairing.
 * `colors` exposes the brand tokens that don't fit the M3 [androidx.compose.material3.ColorScheme];
 * `typography` and `shapes` delegate to the underlying Material theme.
 */
object MeridiaTheme {
    val colors: MeridiaColors
        @Composable @ReadOnlyComposable get() = LocalMeridiaColors.current

    val typography: Typography
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable @ReadOnlyComposable get() = MaterialTheme.shapes
}
