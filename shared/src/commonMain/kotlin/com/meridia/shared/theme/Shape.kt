package com.meridia.shared.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii from the demo tokens: --r-sm 10px, --r-md 16px, --r-lg 22px.
 * The button radius (14px) and the pill (99px) are recurring one-offs in the demo.
 */
object MeridiaRadius {
    val Sm = 10.dp
    val Md = 16.dp
    val Lg = 22.dp
    val Button = 14.dp
    val Pill = 99.dp
}

val PillShape = RoundedCornerShape(MeridiaRadius.Pill)

val MeridiaShapes = Shapes(
    extraSmall = RoundedCornerShape(MeridiaRadius.Sm),
    small = RoundedCornerShape(MeridiaRadius.Sm),
    medium = RoundedCornerShape(MeridiaRadius.Md),
    large = RoundedCornerShape(MeridiaRadius.Lg),
    extraLarge = RoundedCornerShape(MeridiaRadius.Lg),
)
