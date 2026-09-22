package com.codi.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush

/** Gradiente de marca de CoDi: magenta -> púrpura -> azul. */
object CoDiGradients {

    /** Gradiente diagonal usado en tarjetas destacadas (Home). Constante: no depende del tema. */
    val Brand = Brush.linearGradient(
        colors = listOf(GradientPink, GradientPurple, GradientBlue)
    )

    /**
     * Variantes del gradiente de marca para los rectángulos de color de
     * Perfil: cada tarjeta usa un tramo distinto de la misma paleta para que
     * la pantalla se sienta de la misma familia que la tarjeta de Home.
     */
    val Warm = Brush.linearGradient(colors = listOf(GradientPink, GradientPurple))
    val Cool = Brush.linearGradient(colors = listOf(GradientPurple, GradientBlue))
    val Deep = Brush.linearGradient(colors = listOf(GradientBlue, GradientPurple))

    /**
     * Gradiente de fondo de la pantalla de Match: intenso arriba, oscureciendo
     * hacia el fondo de la app. Es `@Composable` porque el último stop usa
     * [BackgroundDark], que depende del tema claro/oscuro activo.
     */
    val MatchBackground: Brush
        @Composable get() = Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to GradientPink,
                0.35f to GradientPurple,
                0.7f to GradientBlue,
                1.0f to BackgroundDark
            )
        )
}
