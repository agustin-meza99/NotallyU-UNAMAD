package com.example.notally.ui.components

import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Prioridad
import com.example.notally.data.model.Tarea
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.theme.colorMateria
import com.example.notally.ui.util.DateFormat
import com.example.notally.ui.util.textoAtraso
import com.example.notally.ui.util.textoEtiquetaFecha
import com.example.notally.ui.util.textoFraseEntrega

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
    nombreMateria: String?,
    colorMateriaArgb: Long?,
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

    val textoMateria = nombreMateria ?: stringResource(R.string.task_noSubject)
    val separador = stringResource(R.string.task_meta_separator)
    val sinFecha = stringResource(R.string.task_noDate)
    val textoFecha = when {
        tarea.entregaEn == null -> sinFecha
        hecha -> textoEtiquetaFecha(tarea.entregaEn, ahora)
        else -> textoAtraso(DateFormat.diasAtraso(tarea.entregaEn, ahora))
            ?: textoEtiquetaFecha(tarea.entregaEn, ahora)
    }
    val colorFecha = if (atrasada) colores.error else colores.onSurfaceVariant
    val colorPuntoMateria = colorMateriaArgb?.let { colorMateria(it) } ?: colores.outline

    val textoPrioridad = when (tarea.prioridad) {
        Prioridad.ALTA -> stringResource(R.string.a11y_priority_high)
        Prioridad.MEDIA -> stringResource(R.string.a11y_priority_medium)
        Prioridad.BAJA -> stringResource(R.string.a11y_priority_low)
    }
    val descripcionEntrega = tarea.entregaEn?.let {
        stringResource(R.string.a11y_task_due, textoFraseEntrega(it, ahora, es24Horas))
    }
    val descripcionFila =
        remember(tarea.titulo, textoMateria, descripcionEntrega, textoPrioridad, hecha) {
            buildList {
                add(tarea.titulo)
                add(textoMateria)
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
            .anilloFoco(visible = enfocada, shape = FormaNotally.row)
            .clip(FormaNotally.row)
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
            CasillaTarea(hecha = hecha)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            colorPuntoMateria,
                        ),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = buildAnnotatedString {
                        append(textoMateria)
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
}

/** Checkbox circular de 20 dp con confirmación de estado (§5, patrón 3). */
@Composable
private fun CasillaTarea(hecha: Boolean) {
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
