package com.example.notally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.data.model.Priority
import com.example.notally.ui.theme.NotallyShape
import com.example.notally.ui.util.DateFormat

/**
 * Tarea focal de Hoy (§7.3.1).
 *
 * Único bloque invertido de la pantalla: `inverseSurface` con texto
 * `inverseOnSurface`, sin borde, sin sombra y sin checkbox. Toda la tarjeta
 * abre el editor y se anuncia como un solo nodo.
 *
 * @param eyebrow bucket ya resuelto ("Atrasadas", "Para hoy", "Sin hora",
 *   "Próximas"); la tarjeta aplica uppercase.
 * @param dueText etiqueta de entrega ("Hoy", "Vie 31 oct", "Sin fecha").
 */
@Composable
fun FocalTaskCard(
    title: String,
    eyebrow: String,
    subjectText: String,
    dueText: String,
    priority: Priority,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }

    val priorityLabel = stringResource(
        when (priority) {
            Priority.HIGH -> R.string.a11y_priority_high
            Priority.MEDIUM -> R.string.a11y_priority_medium
            Priority.LOW -> R.string.a11y_priority_low
        },
    )
    val separator = stringResource(R.string.task_meta_separator)
    val description = stringResource(
        R.string.a11y_featured_task,
        title,
        subjectText,
        dueText,
        priorityLabel,
    )
    val openLabel = stringResource(R.string.a11y_openTask)

    Row(
        modifier = modifier
            .heightIn(min = 96.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusRing(visible = focused, shape = NotallyShape.l)
            .clip(NotallyShape.l)
            .background(colors.inverseSurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.inverseOnSurface),
                role = Role.Button,
                onClickLabel = openLabel,
                onClick = onClick,
            )
            .semantics { contentDescription = description }
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Hueco de 20 dp reservado siempre: Alta y Media cambian de altura,
        // Baja queda transparente sin mover el texto.
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(20.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            val barHeight = when (priority) {
                Priority.HIGH -> 20.dp
                Priority.MEDIUM -> 12.dp
                Priority.LOW -> 0.dp
            }
            if (barHeight > 0.dp) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(barHeight)
                        .clip(CircleShape)
                        .background(colors.inverseOnSurface),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = eyebrow.uppercase(DateFormat.SpanishLocale),
                style = MaterialTheme.typography.labelSmall,
                color = colors.inverseOnSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.inverseOnSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subjectText + separator + dueText,
                style = MaterialTheme.typography.bodySmall,
                color = colors.inverseOnSurface,
            )
        }
    }
}
