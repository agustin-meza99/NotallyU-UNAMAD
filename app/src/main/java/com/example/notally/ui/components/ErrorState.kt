package com.example.notally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.notally.ui.theme.FormaNotally

/**
 * Error sin datos (§7.13): bloque alineado arriba, sin ilustración, con
 * "Reintentar".
 */
@Composable
fun BloqueError(
    titulo: String,
    etiquetaReintentar: String,
    alReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    cuerpo: String? = null,
) {
    MessageWithAction(
        titulo = titulo,
        etiquetaAccion = etiquetaReintentar,
        alAccionar = alReintentar,
        modifier = modifier,
        cuerpo = cuerpo,
    )
}

/**
 * Error con datos en caché (§7.13): banner de una línea, `errorContainer`,
 * radio 12, padding 12. No se cierra solo y no tapa el contenido.
 */
@Composable
fun AvisoError(
    mensaje: String,
    etiquetaReintentar: String,
    alReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .background(colores.errorContainer, FormaNotally.m)
            .semantics { liveRegion = LiveRegionMode.Assertive }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = colores.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = alReintentar,
            colors = ButtonDefaults.textButtonColors(
                contentColor = colores.onErrorContainer,
            ),
        ) {
            Text(etiquetaReintentar)
        }
    }
}

/** Bloque estático de carga (§7.13). Tres filas, sin shimmer. */
@Composable
fun FilasCargando(
    modifier: Modifier = Modifier,
    filas: Int = 3,
) {
    val color = MaterialTheme.colorScheme.surfaceContainerLow
    Column(modifier = modifier) {
        repeat(filas) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .height(56.dp)
                    .background(color, RoundedCornerShape(12.dp)),
            )
        }
    }
}
