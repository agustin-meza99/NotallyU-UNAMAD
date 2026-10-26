package com.example.notally.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Motion del DESIGN.md §5.
 *
 * En material3 1.4.0 `MotionScheme` es `internal`, así que los springs
 * expresivos se copian como constantes de la app (valores verificados en el
 * artefacto). No se fuerza duración: si el sistema desactiva animaciones,
 * Compose respeta `MotionDurationScale`.
 */
object MovimientoNotally {

    // Springs espaciales: tamaño, posición, expansión.
    fun <T> espacialNormal(): SpringSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 380f)

    fun <T> espacialRapida(): SpringSpec<T> =
        spring(dampingRatio = 0.6f, stiffness = 800f)

    // Springs de efectos: color, alpha, confirmación.
    fun <T> efectoNormal(): SpringSpec<T> =
        spring(dampingRatio = 1.0f, stiffness = 1600f)

    fun <T> efectoRapida(): SpringSpec<T> =
        spring(dampingRatio = 1.0f, stiffness = 3800f)

    // Duraciones puntuales (§5, excepciones permitidas).
    const val colorMs = 120
    const val dibujoMarcaMs = 160
    const val fabMs = 150
    const val chevronMs = 180
    const val movimientoElementoMs = 220
    const val fundidoMs = 180
    const val fundidoPestanaMs = 120
    const val snackbarMs = 5_000L

    val fundidoEasing = FastOutSlowInEasing

    /** Atajo para `animateContentSize`. */
    fun tamanoContenido(): FiniteAnimationSpec<androidx.compose.ui.unit.IntSize> = espacialNormal()
}
