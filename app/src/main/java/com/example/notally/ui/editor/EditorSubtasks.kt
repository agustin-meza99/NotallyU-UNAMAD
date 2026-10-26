package com.example.notally.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.ui.theme.TipografiaNotally

@Composable
internal fun SubtasksSection(
    subtareas: List<SubtareaEditor>,
    nuevaSubtarea: String,
    alCambiarNuevaSubtarea: (String) -> Unit,
    subtareaEnfocada: Boolean,
    focoSubtarea: FocusRequester,
    alCambiarFocoSubtarea: (Boolean) -> Unit,
    alPulsarAgregar: () -> Unit,
    alAlternar: (SubtareaEditor) -> Unit,
    alEliminar: (SubtareaEditor) -> Unit,
    alTerminar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.editor_subtasks),
            style = MaterialTheme.typography.titleSmall,
            color = colores.onSurface,
        )
        if (subtareas.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    R.string.editor_subtasks_progress,
                    subtareas.count { it.hecha },
                    subtareas.size,
                ),
                style = TipografiaNotally.numerico,
                color = colores.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        val etiquetaAgregar = stringResource(R.string.editor_subtasks_add)
        TextButton(
            onClick = alPulsarAgregar,
            modifier = Modifier.semantics { contentDescription = etiquetaAgregar },
        ) {
            Text(stringResource(R.string.action_add))
        }
    }

    subtareas.forEach { subtarea ->
        SubtaskRow(
            subtarea = subtarea,
            alAlternar = { alAlternar(subtarea) },
            alEliminar = { alEliminar(subtarea) },
        )
    }

    BasicTextField(
        value = nuevaSubtarea,
        onValueChange = alCambiarNuevaSubtarea,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colores.onSurface),
        cursorBrush = SolidColor(colores.tertiary),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { alTerminar() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focoSubtarea)
            .onFocusChanged { alCambiarFocoSubtarea(it.isFocused) }
            .drawBehind {
                if (subtareaEnfocada) {
                    val trazo = 1.dp.toPx()
                    drawLine(
                        color = colores.outlineVariant,
                        start = Offset(0f, size.height - trazo / 2f),
                        end = Offset(size.width, size.height - trazo / 2f),
                        strokeWidth = trazo,
                    )
                }
            },
        decorationBox = { campoInterno ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (nuevaSubtarea.isEmpty()) {
                    Text(
                        text = stringResource(R.string.editor_subtasks_add),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colores.onSurfaceVariant,
                    )
                }
                campoInterno()
            }
        },
    )
}

@Composable
private fun SubtaskRow(
    subtarea: SubtareaEditor,
    alAlternar: () -> Unit,
    alEliminar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val etiqueta = stringResource(
        if (subtarea.hecha) R.string.a11y_uncompleteTask else R.string.a11y_completeTask,
        subtarea.titulo,
    )
    val etiquetaEstado = stringResource(
        if (subtarea.hecha) R.string.state_done else R.string.state_pending,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    role = Role.Checkbox,
                    onClickLabel = etiqueta,
                    onClick = alAlternar,
                )
                .semantics {
                    contentDescription = etiqueta
                    stateDescription = etiquetaEstado
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (subtarea.hecha) colores.primary else Color.Transparent,
                    )
                    .border(
                        width = 2.dp,
                        color = if (subtarea.hecha) colores.primary else colores.outline,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (subtarea.hecha) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = colores.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = subtarea.titulo,
            style = MaterialTheme.typography.bodyMedium,
            color = if (subtarea.hecha) colores.onSurfaceVariant else colores.onSurface,
            textDecoration = if (subtarea.hecha) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = alEliminar,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.a11y_deleteSubtask),
                tint = colores.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
