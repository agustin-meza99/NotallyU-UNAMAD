package com.example.notally.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.util.FormatoFecha

/**
 * Encabezado de sección (§7.3): `labelSmall` uppercase, contador opcional y
 * chevron de 20 dp con rotación de 180 ms cuando la sección es colapsable.
 */
@Composable
fun EncabezadoSeccion(
    titulo: String,
    modifier: Modifier = Modifier,
    cantidad: Int? = null,
    contraible: Boolean = false,
    expandida: Boolean = true,
    alAlternar: (() -> Unit)? = null,
) {
    val colores = MaterialTheme.colorScheme
    val rotacion by animateFloatAsState(
        targetValue = if (expandida) 180f else 0f,
        animationSpec = tween(MovimientoNotally.chevronMs),
        label = "sectionChevron",
    )
    val etiqueta = buildString {
        append(titulo.uppercase(FormatoFecha.LocaleEspanol))
        if (cantidad != null) {
            append(" · ")
            append(cantidad)
        }
    }
    val alternable = contraible && alAlternar != null
    val estadoExpandida = stringResource(R.string.section_expanded)
    val estadoContraida = stringResource(R.string.section_collapsed)
    val etiquetaExpandir = stringResource(R.string.a11y_expand_section)
    val etiquetaContraer = stringResource(R.string.a11y_collapse_section)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(
                if (alternable) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClickLabel = if (expandida) etiquetaContraer else etiquetaExpandir,
                        onClick = alAlternar,
                    )
                } else {
                    Modifier
                },
            )
            .semantics {
                heading()
                if (alternable) {
                    stateDescription = if (expandida) estadoExpandida else estadoContraida
                }
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = colores.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (contraible) {
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = colores.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotacion),
            )
        }
    }
}
