package com.example.notally.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Anillo de foco de 2 dp `tertiary` con offset de 2 dp (§4.3, §7.2, §9.4).
 *
 * El anillo se dibuja **fuera** de los límites del contenido, así que no altera
 * el layout: no añade padding, no desplaza filas y no recorta el texto. Si el
 * padre aplica `clip`, el anillo puede quedar recortado; en las filas de lista
 * se usa sin clip.
 */
@Composable
fun Modifier.focusRing(
    visible: Boolean,
    shape: Shape,
    color: Color = MaterialTheme.colorScheme.tertiary,
    width: Dp = 2.dp,
    offset: Dp = 2.dp,
): Modifier {
    if (!visible) return this
    return drawWithContent {
        drawContent()
        val stroke = width.toPx()
        val expansion = offset.toPx() + stroke / 2f
        val outline = shape.createOutline(
            size = Size(size.width + expansion * 2f, size.height + expansion * 2f),
            layoutDirection = layoutDirection,
            density = this,
        )
        translate(-expansion, -expansion) {
            drawOutline(
                outline = outline,
                color = color,
                style = Stroke(width = stroke),
            )
        }
    }
}
