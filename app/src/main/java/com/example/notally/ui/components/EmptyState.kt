package com.example.notally.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Estado vacío (§7.8): título, cuerpo y una acción real. Sin ilustración.
 *
 * Se alinea arriba; el margen superior de 48 dp desde el encabezado lo aporta
 * el contenedor de la pantalla.
 */
@Composable
fun EmptyState(
    titulo: String,
    cuerpo: String,
    etiquetaAccion: String,
    alAccionar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageWithAction(
        titulo = titulo,
        etiquetaAccion = etiquetaAccion,
        alAccionar = alAccionar,
        modifier = modifier,
        cuerpo = cuerpo,
        paddingAccion = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    )
}
