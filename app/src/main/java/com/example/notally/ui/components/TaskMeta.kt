package com.example.notally.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.notally.R
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Prioridad

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
