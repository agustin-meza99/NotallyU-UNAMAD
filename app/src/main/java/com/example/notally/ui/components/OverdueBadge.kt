package com.example.notally.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.notally.ui.util.DateFormat
import com.example.notally.ui.util.overdueText

/**
 * Etiqueta "Atrasada · 2 días" / "Atrasada · ayer" en `error`.
 *
 * La palabra y los días llevan el estado; el color es un refuerzo, nunca el
 * único portador (§9.1). No se muestra si la tarea no está atrasada.
 */
@Composable
fun OverdueBadge(
    dueAt: Long,
    now: Long = System.currentTimeMillis(),
    modifier: Modifier = Modifier,
) {
    val label = overdueText(DateFormat.overdueDays(dueAt, now)) ?: return
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier.fillMaxWidth(),
    )
}
