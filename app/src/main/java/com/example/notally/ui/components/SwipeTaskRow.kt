package com.example.notally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.data.model.Tarea
import com.example.notally.ui.theme.FormaNotally

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
    colorCursoArgb: Long?,
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
                            .clip(FormaNotally.row)
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
            colorCursoArgb = colorCursoArgb,
            ahora = ahora,
            alPulsar = alPulsar,
            alAlternarCompletada = alAlternarCompletada,
            alPosponer = alPosponer,
            alEliminar = alEliminar,
        )
    }
}
