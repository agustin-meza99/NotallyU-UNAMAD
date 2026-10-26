package com.example.notally.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Esqueleto de estado vacío y de error sin datos (§7.8, §7.13): título,
 * cuerpo opcional y una acción real. Sin ilustración.
 *
 * [paddingAccion] conserva el padding propio de cada pantalla sobre el botón.
 */
@Composable
internal fun MessageWithAction(
    titulo: String,
    etiquetaAccion: String,
    alAccionar: () -> Unit,
    modifier: Modifier = Modifier,
    cuerpo: String? = null,
    paddingAccion: PaddingValues = ButtonDefaults.TextButtonContentPadding,
) {
    Column(
        modifier = modifier.widthIn(max = 360.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        if (cuerpo != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = cuerpo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        TextButton(
            onClick = alAccionar,
            contentPadding = paddingAccion,
        ) {
            Text(etiquetaAccion)
        }
    }
}
