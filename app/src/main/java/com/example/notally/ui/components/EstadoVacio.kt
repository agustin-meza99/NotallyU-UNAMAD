package com.example.notally.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
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
 * Estado vacío (§7.8): título, cuerpo y una acción real. Sin ilustración.
 *
 * Se alinea arriba; el margen superior de 48 dp desde el encabezado lo aporta
 * el contenedor de la pantalla.
 */
@Composable
fun EstadoVacio(
    titulo: String,
    cuerpo: String,
    etiquetaAccion: String,
    alAccionar: () -> Unit,
    modifier: Modifier = Modifier,
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
        Spacer(Modifier.height(8.dp))
        Text(
            text = cuerpo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(
            onClick = alAccionar,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 12.dp,
                vertical = 4.dp,
            ),
        ) {
            Text(etiquetaAccion)
        }
    }
}
