package com.example.notally.ui.editor

import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.example.notally.data.model.Curso
import com.example.notally.data.model.Prioridad
import com.example.notally.R
import com.example.notally.ui.components.anilloFoco
import com.example.notally.ui.components.priorityLabel
import com.example.notally.ui.components.statusLabel
import com.example.notally.ui.theme.EspaciadoNotally
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.theme.TipografiaNotally
import com.example.notally.ui.utils.DateFormat
import com.example.notally.ui.utils.textoEtiquetaFecha
import java.time.ZoneId

@Composable
internal fun EditorContent(
    estado: BorradorTarea,
    cursos: List<Curso>,
    nuevaSubtarea: String,
    alCambiarNuevaSubtarea: (String) -> Unit,
    subtareaEnfocada: Boolean,
    focoSubtarea: FocusRequester,
    alCambiarFocoSubtarea: (Boolean) -> Unit,
    alAgregarSubtarea: () -> Unit,
    alAlternarSubtarea: (SubtareaEditor) -> Unit,
    alEliminarSubtarea: (SubtareaEditor) -> Unit,
    alPulsarAgregar: () -> Unit,
    alCambiarTitulo: (String) -> Unit,
    alCambiarNotas: (String) -> Unit,
    alAbrirPropiedad: (HojaEditor) -> Unit,
    margen: Dp,
    imeVisible: Boolean,
) {
    val colores = MaterialTheme.colorScheme
    val contexto = LocalContext.current
    val zona = remember { ZoneId.systemDefault() }
    val es24Horas = remember(contexto) { AndroidDateFormat.is24HourFormat(contexto) }
    val gestorFoco = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current
    val etiquetaTitulo = stringResource(R.string.editor_title_placeholder)
    val etiquetaNotas = stringResource(R.string.editor_notes_placeholder)

    val nombreCurso = cursos.firstOrNull { it.id == estado.cursoId }?.nombre
        ?: stringResource(R.string.task_no_course)
    val textoEntrega = estado.entregaEn?.let { entregaEn ->
        val fecha = textoEtiquetaFecha(entregaEn, zona = zona)
        if (DateFormat.tieneHora(entregaEn, zona)) {
            "$fecha, ${DateFormat.formatearHora(entregaEn, es24Horas, zona)}"
        } else {
            fecha
        }
    } ?: stringResource(R.string.task_noDate)
    val textoPrioridad = priorityLabel(estado.prioridad)
    val textoEstado = statusLabel(estado.estado)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = EspaciadoNotally.anchoMaximoContenido)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = margen)
                .padding(bottom = if (imeVisible) 112.dp else 48.dp),
        ) {
            BasicTextField(
                value = estado.titulo,
                onValueChange = alCambiarTitulo,
                textStyle = TipografiaNotally.tituloEditor.copy(color = colores.onSurface),
                cursorBrush = SolidColor(colores.tertiary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = etiquetaTitulo },
                decorationBox = { campoInterno ->
                    Box {
                        if (estado.titulo.isEmpty()) {
                            Text(
                                text = stringResource(R.string.editor_title_placeholder),
                                style = TipografiaNotally.tituloEditor,
                                color = colores.onSurfaceVariant,
                            )
                        }
                        campoInterno()
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            EditorPropertyRow(
                etiqueta = stringResource(R.string.editor_property_course),
                valor = nombreCurso,
                alPulsar = { alAbrirPropiedad(HojaEditor.CURSO) },
            )
            EditorPropertyRow(
                etiqueta = stringResource(R.string.editor_property_due),
                valor = textoEntrega,
                alPulsar = { alAbrirPropiedad(HojaEditor.ENTREGA) },
            )
            EditorPropertyRow(
                etiqueta = stringResource(R.string.editor_property_priority),
                valor = textoPrioridad,
                mostrarPuntoError = estado.prioridad == Prioridad.ALTA,
                alPulsar = { alAbrirPropiedad(HojaEditor.PRIORIDAD) },
            )
            EditorPropertyRow(
                etiqueta = stringResource(R.string.editor_property_state),
                valor = textoEstado,
                alPulsar = { alAbrirPropiedad(HojaEditor.ESTADO) },
            )
            if (estado.esGrupal) {
                EditorReadOnlyRow(texto = stringResource(R.string.task_group_yes))
            }
            if (estado.entregada) {
                EditorReadOnlyRow(texto = stringResource(R.string.task_delivered_yes))
            }

            Spacer(Modifier.height(24.dp))
            BasicTextField(
                value = estado.notas,
                onValueChange = alCambiarNotas,
                textStyle = TipografiaNotally.cuerpoEditor.copy(color = colores.onSurface),
                cursorBrush = SolidColor(colores.tertiary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .semantics { contentDescription = etiquetaNotas },
                decorationBox = { campoInterno ->
                    Box(modifier = Modifier.heightIn(min = 120.dp)) {
                        if (estado.notas.isEmpty()) {
                            Text(
                                text = stringResource(R.string.editor_notes_placeholder),
                                style = TipografiaNotally.cuerpoEditor,
                                color = colores.onSurfaceVariant,
                            )
                        }
                        campoInterno()
                    }
                },
            )

            Spacer(Modifier.height(24.dp))
            SubtasksSection(
                subtareas = estado.subtareas,
                nuevaSubtarea = nuevaSubtarea,
                alCambiarNuevaSubtarea = alCambiarNuevaSubtarea,
                subtareaEnfocada = subtareaEnfocada,
                focoSubtarea = focoSubtarea,
                alCambiarFocoSubtarea = alCambiarFocoSubtarea,
                alPulsarAgregar = alPulsarAgregar,
                alAlternar = alAlternarSubtarea,
                alEliminar = alEliminarSubtarea,
                alTerminar = {
                    if (nuevaSubtarea.isBlank()) {
                        gestorFoco.clearFocus()
                        teclado?.hide()
                    } else {
                        alAgregarSubtarea()
                        focoSubtarea.requestFocus()
                    }
                },
            )
        }
    }
}

@Composable
private fun EditorPropertyRow(
    etiqueta: String,
    valor: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarPuntoError: Boolean = false,
) {
    val colores = MaterialTheme.colorScheme
    val compacto = LocalConfiguration.current.fontScale >= 1.3f
    val interaccion = remember { MutableInteractionSource() }
    val pulsada by interaccion.collectIsPressedAsState()
    var enfocada by remember { mutableStateOf(false) }
    val fondo by animateColorAsState(
        targetValue = if (pulsada) {
            colores.surfaceContainerLow
        } else {
            Color.Transparent
        },
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "editorPropertyBackground",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .anilloFoco(visible = enfocada, shape = FormaNotally.s)
            .clip(FormaNotally.s)
            .background(fondo)
            .onFocusChanged { enfocada = it.isFocused }
            .clickable(
                interactionSource = interaccion,
                indication = null,
                role = Role.Button,
                onClick = alPulsar,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (compacto) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = etiqueta,
                    style = MaterialTheme.typography.titleSmall,
                    color = colores.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = valor,
                        style = MaterialTheme.typography.titleSmall,
                        color = colores.onSurface,
                    )
                    if (mostrarPuntoError) {
                        Spacer(Modifier.width(6.dp))
                        PuntoError()
                    }
                }
            }
        } else {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.titleSmall,
                color = colores.onSurfaceVariant,
                modifier = Modifier.width(96.dp),
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = valor,
                    style = MaterialTheme.typography.titleSmall,
                    color = colores.onSurface,
                )
                if (mostrarPuntoError) {
                    Spacer(Modifier.width(6.dp))
                    PuntoError()
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = colores.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Fila informativa sin acción, para datos que vienen del Aula Virtual. */
@Composable
private fun EditorReadOnlyRow(texto: String) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.titleSmall,
            color = colores.onSurfaceVariant,
        )
    }
}

@Composable
private fun PuntoError() {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error),
    )
}

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
