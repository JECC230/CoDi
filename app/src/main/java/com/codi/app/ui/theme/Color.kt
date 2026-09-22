package com.codi.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Expuesto por [CoDiTheme]: dice si el tema activo es oscuro o claro. Los
 * tokens de abajo lo leen para resolver su color sin que cada pantalla
 * tenga que saber en qué tema está — así el switch de Ajustes cambia toda
 * la app sin tocar ninguna pantalla.
 */
val LocalIsDarkTheme = compositionLocalOf { true }

// --- Colores de marca: se mantienen iguales en claro y oscuro a propósito,
// son la identidad visual de CoDi (gradiente, acento, estados). ---

val AccentBlue = Color(0xFF3D7BFF)
val AccentBlueDark = Color(0xFF2A5CD6)

val GradientPink = Color(0xFFFF2D78)
val GradientPurple = Color(0xFF8B2FD6)
val GradientBlue = Color(0xFF3B5BFE)

val ChipBackgroundSelected = AccentBlue
val UnreadDot = AccentBlue

/** Paleta de colores usada para los placeholders de foto (avatares, imágenes de perfil). */
val PlaceholderPalette = listOf(
    Color(0xFFFF6B9A),
    Color(0xFF9B7BFF),
    Color(0xFF5B8CFF),
    Color(0xFFFFA45B),
    Color(0xFF4CD9B0),
    Color(0xFFE85BD8)
)

fun placeholderColor(seed: Int): Color =
    PlaceholderPalette[seed.mod(PlaceholderPalette.size)]

// --- Tokens dependientes del tema: mismo nombre de siempre (para no tocar
// cada pantalla), pero ahora resuelven un valor distinto según el tema. ---

private val DarkBackground = Color(0xFF0E0E14)
private val LightBackground = Color(0xFFF6F6FA)

private val DarkSurfaceCard = Color(0xFF1C1C24)
private val LightSurfaceCard = Color(0xFFFFFFFF)

private val DarkSurfaceCardAlt = Color(0xFF23232D)
private val LightSurfaceCardAlt = Color(0xFFEDEDF3)

private val DarkStroke = Color(0x33FFFFFF)
private val LightStroke = Color(0x1F000000)

private val DarkTextPrimary = Color(0xFFF5F5F7)
private val LightTextPrimary = Color(0xFF1B1B20)

private val DarkTextSecondary = Color(0xFFB0B0BC)
private val LightTextSecondary = Color(0xFF54545F)

private val DarkTextMuted = Color(0xFF83838F)
private val LightTextMuted = Color(0xFF64646E)

private val DarkChipBackground = Color(0x29FFFFFF)
private val LightChipBackground = Color(0x1F000000)

private val DarkBubbleReceived = Color(0xFF23232D)
private val LightBubbleReceived = Color(0xFFECECF2)

// Verde "en línea"/confirmación: en modo claro se oscurece para no perder
// contraste sobre fondo casi blanco (el tono brillante de modo oscuro se ve
// lavado ahí).
private val DarkOnlineGreen = Color(0xFF4CD964)
private val LightOnlineGreen = Color(0xFF1E8E3E)

val OnlineGreen: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkOnlineGreen else LightOnlineGreen

/** Fondo general de pantalla. El nombre viene de cuando la app solo tenía tema oscuro. */
val BackgroundDark: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkBackground else LightBackground

val SurfaceCard: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkSurfaceCard else LightSurfaceCard

val SurfaceCardAlt: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkSurfaceCardAlt else LightSurfaceCardAlt

val StrokeSubtle: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkStroke else LightStroke

val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkTextPrimary else LightTextPrimary

val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkTextSecondary else LightTextSecondary

val TextMuted: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkTextMuted else LightTextMuted

val ChipBackground: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkChipBackground else LightChipBackground

val BubbleReceived: Color
    @Composable get() = if (LocalIsDarkTheme.current) DarkBubbleReceived else LightBubbleReceived
