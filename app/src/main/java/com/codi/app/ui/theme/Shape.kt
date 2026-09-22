package com.codi.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radios de esquina del sistema de diseño (aprox. Figma: 10 / 12 / 16 / 20 / 24dp, chips en pill). */
val CoDiShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/** Esquinas totalmente redondeadas para chips tipo "pill". */
val PillShape = RoundedCornerShape(percent = 50)
