package com.example.notally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.notally.ui.theme.NotallyShape

/**
 * Error sin datos (§7.13): bloque alineado arriba, sin ilustración, con
 * "Reintentar".
 */
@Composable
fun ErrorBlock(
    title: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
) {
    Column(modifier = modifier.widthIn(max = 360.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text(retryLabel)
        }
    }
}

/**
 * Error con datos en caché (§7.13): banner de una línea, `errorContainer`,
 * radio 12, padding 12. No se cierra solo y no tapa el contenido.
 */
@Composable
fun ErrorBanner(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .background(colors.errorContainer, NotallyShape.m)
            .semantics { liveRegion = LiveRegionMode.Assertive }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onRetry,
            colors = ButtonDefaults.textButtonColors(
                contentColor = colors.onErrorContainer,
            ),
        ) {
            Text(retryLabel)
        }
    }
}

/** Bloque estático de carga (§7.13). Tres filas, sin shimmer. */
@Composable
fun LoadingRows(
    modifier: Modifier = Modifier,
    rows: Int = 3,
) {
    val color = MaterialTheme.colorScheme.surfaceContainerLow
    Column(modifier = modifier) {
        repeat(rows) {
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
