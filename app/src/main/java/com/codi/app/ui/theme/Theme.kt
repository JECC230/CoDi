package com.codi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * CoDi v1 soporta tema claro y oscuro real (interruptor en Ajustes ->
 * Apariencia). El valor efectivo lo decide [com.codi.app.data.ThemePreferences]
 * y se pasa aquí como [darkTheme]; de ahí se expone vía [LocalIsDarkTheme]
 * para que los tokens de `Color.kt` (`TextPrimary`, `SurfaceCard`, etc.), que
 * ya se usan en cada pantalla, resuelvan solos el color correcto sin que
 * ninguna pantalla tenga que cambiar.
 */
private val CoDiDarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFFF5F5F7),
    secondary = GradientPurple,
    onSecondary = Color(0xFFF5F5F7),
    tertiary = GradientPink,
    background = Color(0xFF0E0E14),
    onBackground = Color(0xFFF5F5F7),
    surface = Color(0xFF1C1C24),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF23232D),
    onSurfaceVariant = Color(0xFFB0B0BC),
    outline = Color(0x33FFFFFF),
    error = GradientPink
)

private val CoDiLightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFFFFFFFF),
    secondary = GradientPurple,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = GradientPink,
    background = Color(0xFFF6F6FA),
    onBackground = Color(0xFF1B1B20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B20),
    surfaceVariant = Color(0xFFEDEDF3),
    onSurfaceVariant = Color(0xFF54545F),
    outline = Color(0x1F000000),
    error = Color(0xFFB3261E)
)

@Composable
fun CoDiTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) CoDiDarkColorScheme else CoDiLightColorScheme,
            typography = CoDiTypography,
            shapes = CoDiShapes,
            content = content
        )
    }
}
