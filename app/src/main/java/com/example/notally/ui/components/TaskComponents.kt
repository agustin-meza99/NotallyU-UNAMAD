package com.example.notally.ui.components

import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Prioridad
import com.example.notally.data.model.Tarea
import com.example.notally.R
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.utils.DateFormat
import com.example.notally.ui.utils.textoAtraso
import com.example.notally.ui.utils.textoEtiquetaFecha
import com.example.notally.ui.utils.textoFraseEntrega

/**
 * Fila de tarea (§7.2).
 *
 * El checkbox es un nodo de accesibilidad separado (nunca se fusiona con la
 * fila). La fila entera abre el editor y expone las acciones de accesibilidad
 * Completar / Posponer a mañana / Eliminar.
 *
 * @param ahora instante de referencia para las etiquetas de fecha; la pantalla
 *   lo actualiza periódicamente para que "Hoy"/"Atrasada" no se queden obsoletos.
 */
@Composable
fun TaskRow(
    tarea: Tarea,
    nombreCurso: String?,
    modifier: Modifier = Modifier,
    ahora: Long = System.currentTimeMillis(),
    alPulsar: () -> Unit = {},
    alAlternarCompletada: () -> Unit = {},
    alPosponer: () -> Unit = {},
    alEliminar: () -> Unit = {},
) {
    val colores = MaterialTheme.colorScheme
    val hecha = tarea.estado == Estado.COMPLETADA
    val atrasada = !hecha && tarea.entregaEn != null &&
        DateFormat.diasAtraso(tarea.entregaEn, ahora) > 0

    val contexto = LocalContext.current
    val es24Horas = remember(contexto) { AndroidDateFormat.is24HourFormat(contexto) }
    val haptica = LocalHapticFeedback.current

    val textoCurso = nombreCurso ?: stringResource(R.string.task_no_course)
    val separador = stringResource(R.string.task_meta_separator)
    val sinFecha = stringResource(R.string.task_noDate)
    val textoFecha = when {
        tarea.entregaEn == null -> sinFecha
        hecha -> textoEtiquetaFecha(tarea.entregaEn, ahora)
        else -> textoAtraso(DateFormat.diasAtraso(tarea.entregaEn, ahora))
            ?: textoEtiquetaFecha(tarea.entregaEn, ahora)
    }
    val colorFecha = if (atrasada) colores.error else colores.onSurfaceVariant

    val textoPrioridad = priorityLabelForA11y(tarea.prioridad)
    val descripcionEntrega = tarea.entregaEn?.let {
        stringResource(R.string.a11y_task_due, textoFraseEntrega(it, ahora, es24Horas))
    }
    val descripcionFila =
        remember(tarea.titulo, textoCurso, descripcionEntrega, textoPrioridad, hecha) {
            buildList {
                add(tarea.titulo)
                add(textoCurso)
                if (descripcionEntrega != null) add(descripcionEntrega)
                add(textoPrioridad)
            }.joinToString(". ") + "."
        }

    val etiquetaAbrir = stringResource(R.string.a11y_openTask)
    val etiquetaCompletar = stringResource(R.string.swipe_complete)
    val etiquetaPosponer = stringResource(R.string.task_postpone)
    val etiquetaEliminar = stringResource(R.string.action_delete)
    val etiquetaCasilla = stringResource(
        if (hecha) R.string.a11y_uncompleteTask else R.string.a11y_completeTask,
        tarea.titulo,
    )
    val estadoCasilla = stringResource(
        if (hecha) R.string.state_done else R.string.state_pending,
    )

    val fuenteInteraccion = remember { MutableInteractionSource() }
    val pulsada by fuenteInteraccion.collectIsPressedAsState()
    val fondo by animateColorAsState(
        targetValue = if (pulsada) colores.surfaceContainerLow else Color.Transparent,
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "taskRowBackground",
    )
    var enfocada by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .anilloFoco(visible = enfocada, shape = FormaNotally.m)
            .clip(FormaNotally.m)
            .background(fondo)
            .onFocusChanged { enfocada = it.isFocused }
            .clickable(
                interactionSource = fuenteInteraccion,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = etiquetaAbrir,
                onClick = alPulsar,
            )
            .semantics {
                contentDescription = descripcionFila
                customActions = listOf(
                    CustomAccessibilityAction(etiquetaCompletar) {
                        if (!hecha) alAlternarCompletada()
                        true
                    },
                    CustomAccessibilityAction(etiquetaPosponer) {
                        alPosponer()
                        true
                    },
                    CustomAccessibilityAction(etiquetaEliminar) {
                        alEliminar()
                        true
                    },
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Target 48×48; dibujo 20 dp.
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    role = Role.Checkbox,
                    onClickLabel = etiquetaCasilla,
                    onClick = {
                        if (!hecha) {
                            haptica.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        alAlternarCompletada()
                    },
                )
                .semantics {
                    contentDescription = etiquetaCasilla
                    stateDescription = estadoCasilla
                },
            contentAlignment = Alignment.Center,
        ) {
            TaskCheckbox(hecha = hecha)
        }

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = anchoPriorityRail, top = 12.dp, bottom = 12.dp)
                .rielPrioridad(
                    prioridad = tarea.prioridad,
                    hecha = hecha,
                    color = if (tarea.prioridad == Prioridad.ALTA) {
                        colores.error
                    } else {
                        colores.outline
                    },
                ),
        ) {
            Text(
                text = tarea.titulo,
                style = MaterialTheme.typography.titleMedium,
                color = if (hecha) colores.onSurfaceVariant else colores.onSurface,
                textDecoration = if (hecha) TextDecoration.LineThrough else null,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildAnnotatedString {
                    append(textoCurso)
                    append(separador)
                    withStyle(SpanStyle(color = colorFecha)) { append(textoFecha) }
                },
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Checkbox circular de 20 dp con confirmación de estado (§5, patrón 3). */
@Composable
private fun TaskCheckbox(hecha: Boolean) {
    val colores = MaterialTheme.colorScheme
    val relleno by animateColorAsState(
        targetValue = if (hecha) colores.primary else Color.Transparent,
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "checkboxFill",
    )
    val colorBorde by animateColorAsState(
        targetValue = if (hecha) colores.primary else colores.outline,
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "checkboxBorder",
    )
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(relleno)
            .border(2.dp, colorBorde, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = hecha,
            enter = fadeIn(tween(MovimientoNotally.colorMs)) +
                scaleIn(
                    animationSpec = MovimientoNotally.espacialRapida(),
                    initialScale = 0.9f,
                ),
            exit = fadeOut(tween(80)),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = colores.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/**
 * Fila de tarea con gestos de swipe (§7.2).
 *
 * End→start ejecuta "Completar" (`tertiaryContainer`); start→end ejecuta
 * "Mañana" (`secondaryContainer`). Umbral 45 % del ancho y haptic al cruzarlo.
 *
 * Nota de API: material3 1.4.0 eliminó `confirmValueChange` del constructor
 * principal de `SwipeToDismissBoxState` y añadió `onDismiss` + `reset()`. Se usa
 * el constructor (deprecado pero público en 1.4.0) con `confirmValueChange`
 * devolviendo `false`, que es exactamente el comportamiento pedido: ejecutar y
 * volver a reposo sin expulsar la fila. Con `onDismiss` la fila llegaba al
 * ancho completo antes de volver, un salto visible.
 */
@Suppress("DEPRECATION")
@Composable
fun SwipeTaskRow(
    tarea: Tarea,
    nombreCurso: String?,
    modifier: Modifier = Modifier,
    ahora: Long = System.currentTimeMillis(),
    alPulsar: () -> Unit = {},
    alAlternarCompletada: () -> Unit = {},
    alCompletar: () -> Unit = alAlternarCompletada,
    alPosponer: () -> Unit = {},
    alEliminar: () -> Unit = {},
) {
    val colores = MaterialTheme.colorScheme
    val haptica = LocalHapticFeedback.current
    val densidad = LocalDensity.current

    val completarActual by rememberUpdatedState(alCompletar)
    val posponerActual by rememberUpdatedState(alPosponer)

    val estado = remember(densidad) {
        SwipeToDismissBoxState(
            initialValue = SwipeToDismissBoxValue.Settled,
            density = densidad,
            confirmValueChange = { valor ->
                when (valor) {
                    SwipeToDismissBoxValue.EndToStart -> completarActual()
                    SwipeToDismissBoxValue.StartToEnd -> posponerActual()
                    SwipeToDismissBoxValue.Settled -> Unit
                }
                // Siempre vuelve a reposo: la fila no se expulsa.
                false
            },
            positionalThreshold = { distanciaTotal -> distanciaTotal * 0.45f },
        )
    }

    var hapticaArmada by remember { mutableStateOf(true) }
    LaunchedEffect(estado) {
        snapshotFlow { estado.progress }.collect { progreso ->
            if (progreso >= 0.45f && hapticaArmada) {
                haptica.performHapticFeedback(HapticFeedbackType.LongPress)
                hapticaArmada = false
            } else if (progreso < 0.30f && !hapticaArmada) {
                hapticaArmada = true
            }
        }
    }

    SwipeToDismissBox(
        state = estado,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direccion = estado.dismissDirection
            val esCompletar = direccion == SwipeToDismissBoxValue.EndToStart
            val contenedor = if (esCompletar) {
                colores.tertiaryContainer
            } else {
                colores.secondaryContainer
            }
            val contenido = if (esCompletar) {
                colores.onTertiaryContainer
            } else {
                colores.onSecondaryContainer
            }
            // El fondo ocupa solo el área revelada (ancho de la etiqueta), no
            // toda la fila: así el color de acción no sangra bajo el texto.
            if (direccion != SwipeToDismissBoxValue.Settled) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .align(
                                if (esCompletar) Alignment.CenterEnd
                                else Alignment.CenterStart,
                            )
                            .fillMaxHeight()
                            .clip(FormaNotally.m)
                            .background(contenedor)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(
                                if (esCompletar) R.drawable.ic_check
                                else R.drawable.ic_calendar,
                            ),
                            contentDescription = null,
                            tint = contenido,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (esCompletar) R.string.swipe_complete
                                else R.string.swipe_tomorrow,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = contenido,
                        )
                    }
                }
            }
        },
    ) {
        TaskRow(
            tarea = tarea,
            nombreCurso = nombreCurso,
            ahora = ahora,
            alPulsar = alPulsar,
            alAlternarCompletada = alAlternarCompletada,
            alPosponer = alPosponer,
            alEliminar = alEliminar,
        )
    }
}

/** "Prioridad alta/media/baja" para lectores de pantalla. */
@Composable
fun priorityLabelForA11y(prioridad: Prioridad): String = stringResource(
    when (prioridad) {
        Prioridad.ALTA -> R.string.a11y_priority_high
        Prioridad.MEDIA -> R.string.a11y_priority_medium
        Prioridad.BAJA -> R.string.a11y_priority_low
    },
)

/** "Alta/Media/Baja" para la UI visible. */
@Composable
fun priorityLabel(prioridad: Prioridad): String = stringResource(
    when (prioridad) {
        Prioridad.ALTA -> R.string.priority_high
        Prioridad.MEDIA -> R.string.priority_medium
        Prioridad.BAJA -> R.string.priority_low
    },
)

/** "Pendiente/En curso/Completada". */
@Composable
fun statusLabel(estado: Estado): String = stringResource(
    when (estado) {
        Estado.PENDIENTE -> R.string.state_pending
        Estado.EN_CURSO -> R.string.state_doing
        Estado.COMPLETADA -> R.string.state_done
    },
)

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
