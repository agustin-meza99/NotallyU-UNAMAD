package com.example.notally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.notally.R

/**
 * Fila de opción de 56 dp para hojas de selección (§7.9): control de selección
 * a la izquierda, texto `bodyLarge`, selección marcada con `tertiary`.
 *
 * El slot izquierdo mide siempre 20 dp para que el texto no se desplace al
 * cambiar la selección.
 */
@Composable
fun FilaOpcion(
    etiqueta: String,
    seleccionado: Boolean,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarPuntoError: Boolean = false,
    inicial: @Composable (() -> Unit)? = null,
) {
    val colores = MaterialTheme.colorScheme
    val colorContenido = if (seleccionado) colores.tertiary else colores.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(
                selected = seleccionado,
                role = Role.RadioButton,
                onClick = alPulsar,
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (seleccionado) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = colores.tertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        if (inicial != null) {
            inicial()
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyLarge,
            color = colorContenido,
            modifier = Modifier.weight(1f),
        )
        if (mostrarPuntoError) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colores.error),
            )
        }
    }
}
