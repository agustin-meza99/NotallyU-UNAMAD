package com.example.notally.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.notally.R
import com.example.notally.ui.theme.FormaNotally

/**
 * Confirmación de borrado compartida por tareas y cursos.
 *
 * Mismo foco inicial en "Cancelar", mismo orden de acciones y mismo peso visual
 * que los diálogos locales que sustituye.
 */
@Composable
fun DialogoConfirmarEliminacion(
    titulo: String,
    cuerpo: String,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val focoCancelar = remember { FocusRequester() }
    LaunchedEffect(Unit) { focoCancelar.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        shape = FormaNotally.xl,
        containerColor = colores.surfaceContainerLowest,
        title = {
            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineSmall,
                color = colores.onSurface,
            )
        },
        text = {
            Text(
                text = cuerpo,
                style = MaterialTheme.typography.bodyMedium,
                color = colores.onSurfaceVariant,
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .focusRequester(focoCancelar),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = colores.error),
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
    )
}
