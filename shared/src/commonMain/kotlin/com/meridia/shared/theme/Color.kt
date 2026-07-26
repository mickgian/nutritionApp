package com.meridia.shared.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Meridia design tokens, transcribed from the HTML demo (docs/demo/nutriodemo.html):
 *
 *   --verde       #17402F   primary / forest
 *   --verde-scuro #0E2B1F
 *   --salvia      #E4EEE4   tinted surfaces
 *   --lime        #C9E265   badges / highlights
 *   --panna       #F7F8F3   app background
 *   --bianco      #FFFFFF
 *   --inchiostro  #1A241E   ink / text
 *   --grigio      #6B7A70   secondary text
 *   --linea       #DDE6DD   hairlines / borders
 *   --arancio     #D96C3B   warnings / deadlines only
 */
object MeridiaPalette {
    val Verde = Color(0xFF17402F)
    val VerdeScuro = Color(0xFF0E2B1F)
    val Salvia = Color(0xFFE4EEE4)
    val Lime = Color(0xFFC9E265)
    val Panna = Color(0xFFF7F8F3)
    val Bianco = Color(0xFFFFFFFF)
    val Inchiostro = Color(0xFF1A241E)
    val Grigio = Color(0xFF6B7A70)
    val Linea = Color(0xFFDDE6DD)
    val Arancio = Color(0xFFD96C3B)

    // Warning surface pair (demo .chip.warn / .deadline)
    val WarnBg = Color(0xFFFBE3D6)
    val WarnFg = Color(0xFF9C4A21)
}

/**
 * Extended palette exposed through [com.meridia.shared.theme.MeridiaTheme.colors].
 *
 * Material's [androidx.compose.material3.ColorScheme] can't hold every brand token
 * (lime, salvia, arancio, the warn pair), so we carry them here and surface them via
 * a CompositionLocal — the idiomatic Compose pattern for a design system on top of M3.
 */
@Immutable
data class MeridiaColors(
    val verde: Color,
    val verdeScuro: Color,
    val salvia: Color,
    val lime: Color,
    val panna: Color,
    val bianco: Color,
    val inchiostro: Color,
    val grigio: Color,
    val linea: Color,
    val arancio: Color,
    val warnBg: Color,
    val warnFg: Color,
)

val meridiaLightColors = MeridiaColors(
    verde = MeridiaPalette.Verde,
    verdeScuro = MeridiaPalette.VerdeScuro,
    salvia = MeridiaPalette.Salvia,
    lime = MeridiaPalette.Lime,
    panna = MeridiaPalette.Panna,
    bianco = MeridiaPalette.Bianco,
    inchiostro = MeridiaPalette.Inchiostro,
    grigio = MeridiaPalette.Grigio,
    linea = MeridiaPalette.Linea,
    arancio = MeridiaPalette.Arancio,
    warnBg = MeridiaPalette.WarnBg,
    warnFg = MeridiaPalette.WarnFg,
)

val LocalMeridiaColors = staticCompositionLocalOf { meridiaLightColors }

/**
 * The Material 3 [androidx.compose.material3.ColorScheme] mapping, so stock M3
 * components (TextField, CircularProgressIndicator, …) already pick up the brand.
 */
val meridiaColorScheme = lightColorScheme(
    primary = MeridiaPalette.Verde,
    onPrimary = MeridiaPalette.Bianco,
    primaryContainer = MeridiaPalette.Salvia,
    onPrimaryContainer = MeridiaPalette.Verde,
    secondary = MeridiaPalette.Lime,
    onSecondary = MeridiaPalette.VerdeScuro,
    secondaryContainer = MeridiaPalette.Lime,
    onSecondaryContainer = MeridiaPalette.VerdeScuro,
    background = MeridiaPalette.Panna,
    onBackground = MeridiaPalette.Inchiostro,
    surface = MeridiaPalette.Bianco,
    onSurface = MeridiaPalette.Inchiostro,
    surfaceVariant = MeridiaPalette.Salvia,
    onSurfaceVariant = MeridiaPalette.Grigio,
    outline = MeridiaPalette.Linea,
    outlineVariant = MeridiaPalette.Linea,
    error = MeridiaPalette.Arancio,
    onError = MeridiaPalette.Bianco,
)
