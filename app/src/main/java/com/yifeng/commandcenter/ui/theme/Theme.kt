package com.yifeng.commandcenter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = LiftBlue,
    onPrimary = LiftOnSurface,
    primaryContainer = LiftBlueDark,
    background = LiftBackground,
    onBackground = LiftOnSurface,
    surface = LiftSurface,
    onSurface = LiftOnSurface,
    surfaceVariant = LiftSurfaceRaised,
    onSurfaceVariant = LiftMuted,
    outline = LiftBorder,
    outlineVariant = LiftDivider,
    error = LiftRed,
    onError = LiftOnSurface,
)

private val BalancedShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp),
)

@Composable
fun CommandcenterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = BalancedShapes,
        content = content
    )
}