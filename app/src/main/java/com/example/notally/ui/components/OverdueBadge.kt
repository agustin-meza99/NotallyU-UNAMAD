package com.example.notally.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.notally.ui.util.DateFormat
import com.example.notally.ui.util.textoAtraso

/**
 * Etiqueta "Atrasada · 2 días" / "Atrasada · ayer" en `error`.
 *
 * La palabra y los días llevan el estado; el color es un refuerzo, nunca el
 * único portador (§9.1). No se muestra si la tarea no está atrasada.
 */
@Composable
fun OverdueBadge(
    entregaEn: Long,
    ahora: Long = System.currentTimeMillis(),
    modifier: Modifier = Modifier,
) {
    val etiqueta = textoAtraso(DateFormat.diasAtraso(entregaEn, ahora)) ?: return
    Text(
        text = etiqueta,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier.fillMaxWidth(),
    )
}
