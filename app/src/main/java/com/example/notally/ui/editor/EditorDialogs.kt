package com.example.notally.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.notally.R
import com.example.notally.ui.components.DialogoConfirmarEliminacion

/** Confirmación de borrado de la tarea abierta en el editor. */
@Composable
internal fun DeleteTaskDialog(
    tituloVisible: String,
    cantidadSubtareas: Int,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit,
) {
    DialogoConfirmarEliminacion(
        titulo = stringResource(R.string.confirm_deleteTask_title),
        cuerpo = if (cantidadSubtareas == 0) {
            stringResource(R.string.confirm_deleteTask_bodyNoSub, tituloVisible)
        } else {
            pluralStringResource(
                R.plurals.confirm_deleteTask_body,
                cantidadSubtareas,
                tituloVisible,
                cantidadSubtareas,
            )
        },
        onConfirmar = onConfirmar,
        onDismiss = onDismiss,
    )
}
