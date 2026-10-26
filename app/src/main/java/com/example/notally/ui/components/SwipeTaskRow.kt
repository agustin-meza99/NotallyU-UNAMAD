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
import com.example.notally.data.model.Task
import com.example.notally.ui.theme.NotallyShape

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
    task: Task,
    subjectName: String?,
    subjectColorArgb: Long?,
    modifier: Modifier = Modifier,
    now: Long = System.currentTimeMillis(),
    onClick: () -> Unit = {},
    onToggleDone: () -> Unit = {},
    onComplete: () -> Unit = onToggleDone,
    onPostpone: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val currentComplete by rememberUpdatedState(onComplete)
    val currentPostpone by rememberUpdatedState(onPostpone)

    val state = remember(density) {
        SwipeToDismissBoxState(
            initialValue = SwipeToDismissBoxValue.Settled,
            density = density,
            confirmValueChange = { value ->
                when (value) {
                    SwipeToDismissBoxValue.EndToStart -> currentComplete()
                    SwipeToDismissBoxValue.StartToEnd -> currentPostpone()
                    SwipeToDismissBoxValue.Settled -> Unit
                }
                // Siempre vuelve a reposo: la fila no se expulsa.
                false
            },
            positionalThreshold = { totalDistance -> totalDistance * 0.45f },
        )
    }

    var hapticArmed by remember { mutableStateOf(true) }
    LaunchedEffect(state) {
        snapshotFlow { state.progress }.collect { progress ->
            if (progress >= 0.45f && hapticArmed) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                hapticArmed = false
            } else if (progress < 0.30f && !hapticArmed) {
                hapticArmed = true
            }
        }
    }

    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = state.dismissDirection
            val isComplete = direction == SwipeToDismissBoxValue.EndToStart
            val container = if (isComplete) {
                colors.tertiaryContainer
            } else {
                colors.secondaryContainer
            }
            val content = if (isComplete) {
                colors.onTertiaryContainer
            } else {
                colors.onSecondaryContainer
            }
            // El fondo ocupa solo el área revelada (ancho de la etiqueta), no
            // toda la fila: así el color de acción no sangra bajo el texto.
            if (direction != SwipeToDismissBoxValue.Settled) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .align(
                                if (isComplete) Alignment.CenterEnd else Alignment.CenterStart,
                            )
                            .fillMaxHeight()
                            .clip(NotallyShape.row)
                            .background(container)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isComplete) R.drawable.ic_check else R.drawable.ic_calendar,
                            ),
                            contentDescription = null,
                            tint = content,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (isComplete) R.string.swipe_complete else R.string.swipe_tomorrow,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = content,
                        )
                    }
                }
            }
        },
    ) {
        TaskRow(
            task = task,
            subjectName = subjectName,
            subjectColorArgb = subjectColorArgb,
            now = now,
            onClick = onClick,
            onToggleDone = onToggleDone,
            onPostpone = onPostpone,
            onDelete = onDelete,
        )
    }
}
