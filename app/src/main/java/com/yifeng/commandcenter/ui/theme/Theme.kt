package com.yifeng.commandcenter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val HevyntDarkColorScheme = darkColorScheme(
    primary = HevyntAccent,
    onPrimary = HevyntCanvas,
    primaryContainer = HevyntAccentContainer,
    onPrimaryContainer = HevyntTextPrimary,
    background = HevyntCanvas,
    onBackground = HevyntTextPrimary,
    surface = HevyntSurface,
    onSurface = HevyntTextPrimary,
    surfaceVariant = HevyntSurfaceRaised,
    onSurfaceVariant = HevyntTextSecondary,
    outline = HevyntOutline,
    outlineVariant = HevyntDivider,
    error = HevyntError,
    onError = HevyntCanvas,
)

private val HevyntShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun HevyntTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HevyntDarkColorScheme,
        typography = HevyntTypography,
        shapes = HevyntShapes,
        content = content
    )
}

@Composable
fun CommandcenterTheme(content: @Composable () -> Unit) = HevyntTheme(content)