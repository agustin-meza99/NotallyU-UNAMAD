package com.example.notally.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.notally.data.model.Prioridad

/** Sangría que reserva el riel a la izquierda del texto: 3 dp + 12 dp de aire. */
val anchoPriorityRail = 15.dp

/**
 * Riel de prioridad de §7.2 y §7.3.1.
 *
 * Se dibuja dentro del padding inicial que el llamador reserva con
 * `padding(start = anchoPriorityRail)`. El alto sigue al bloque de texto
 * (título + meta), así que no depende del número de líneas ni de la escala de
 * fuente; el ancho reservado es fijo y el texto nunca se desplaza.
 *
 * Alta cubre el 100 % del bloque; media, el 50 % centrado; baja y hechas no
 * dibujan nada. La diferencia de cobertura distingue alta de media sin depender
 * del color (§2.1). Los extremos son redondeados (radio 1.5 dp) para que no se
 * lea como un carácter suelto.
 */
fun Modifier.rielPrioridad(
    prioridad: Prioridad,
    hecha: Boolean,
    color: Color,
): Modifier = drawBehind {
    if (hecha || prioridad == Prioridad.BAJA) return@drawBehind
    val grosor = 3.dp.toPx()
    val aire = 12.dp.toPx()
    val cobertura = if (prioridad == Prioridad.ALTA) 1f else 0.5f
    val alto = size.height * cobertura
    drawRoundRect(
        color = color,
        topLeft = Offset(x = -(grosor + aire), y = (size.height - alto) / 2f),
        size = Size(width = grosor, height = alto),
        cornerRadius = CornerRadius(grosor / 2f),
    )
}
