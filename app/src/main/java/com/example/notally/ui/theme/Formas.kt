package com.example.notally.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formas del DESIGN.md §4.2.
 *
 * Mapeo a los slots de Material 3: extraSmall = 4, small = 8, medium = 12,
 * large = 16, extraLarge = 28. `full` no existe como slot y se usa CircleShape.
 */
val FormasNotally = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Acceso directo a los tokens de forma, para componentes que no usan slots. */
object FormaNotally {
    val xs = RoundedCornerShape(4.dp)
    val s = RoundedCornerShape(8.dp)
    val m = RoundedCornerShape(12.dp)
    val l = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(28.dp)
    val full = CircleShape

    /** Radio de fila: nunca 28 dp en una fila de lista. */
    val row = RoundedCornerShape(12.dp)
}
