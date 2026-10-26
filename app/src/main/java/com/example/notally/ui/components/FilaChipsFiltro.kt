package com.example.notally.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.theme.TipografiaNotally

/** Opción de filtro; el componente no conoce el dominio. */
data class OpcionFiltro(
    val id: String,
    val etiqueta: String,
    val cantidad: Int? = null,
)

/**
 * Chips de filtro (§7.7). Dibujo visual de 32 dp que crece con la fuente, target
 * real de 48×48 en el contenedor que recibe el tap, radio full.
 *
 * El check ocupa un slot fijo de 16 dp en ambos estados para que seleccionar no
 * cambie el ancho. A 200 % de fuente la fila tiene scroll horizontal y cada
 * etiqueta permanece en una línea; el chip crece en alto en vez de recortarse.
 */
@Composable
fun FilaChipsFiltro(
    opciones: List<OpcionFiltro>,
    idSeleccionado: String,
    alSeleccionar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        opciones.forEach { opcion ->
            ChipFiltro(
                opcion = opcion,
                seleccionado = opcion.id == idSeleccionado,
                alPulsar = { alSeleccionar(opcion.id) },
            )
        }
    }
}

@Composable
private fun ChipFiltro(
    opcion: OpcionFiltro,
    seleccionado: Boolean,
    alPulsar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    var enfocado by remember { mutableStateOf(false) }
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val pulsado by fuenteInteraccion.collectIsPressedAsState()

    val contenedor by animateColorAsState(
        targetValue = when {
            seleccionado -> colores.tertiaryContainer
            pulsado -> colores.surfaceContainerLow
            else -> Color.Transparent
        },
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "filterChipContainer",
    )
    val colorEtiqueta by animateColorAsState(
        targetValue = if (seleccionado) colores.onTertiaryContainer else colores.onSurfaceVariant,
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "filterChipLabel",
    )
    val colorCantidad by animateColorAsState(
        targetValue = if (seleccionado) colores.onTertiaryContainer else colores.onSurfaceVariant,
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "filterChipCount",
    )
    val modificadorBorde = when {
        seleccionado -> Modifier
        else -> Modifier.border(1.dp, colores.outlineVariant, CircleShape)
    }
    // El contador se anuncia dentro del mismo nodo del chip (§7.7).
    val descripcionCantidad = opcion.cantidad?.let { cantidad ->
        pluralStringResource(R.plurals.a11y_filter_count, cantidad, opcion.etiqueta, cantidad)
    }

    // El target de 48×48 y el gesto viven aquí, no en el dibujo de 32 dp.
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .onFocusChanged { enfocado = it.isFocused }
            .selectable(
                selected = seleccionado,
                interactionSource = fuenteInteraccion,
                role = Role.RadioButton,
                onClick = alPulsar,
            )
            .semantics(mergeDescendants = true) {
                if (descripcionCantidad != null) {
                    contentDescription = descripcionCantidad
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .anilloFoco(visible = enfocado, shape = CircleShape)
                .clip(CircleShape)
                .background(contenedor)
                .then(modificadorBorde)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (seleccionado) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = colores.onTertiaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Row {
                Text(
                    text = opcion.etiqueta,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorEtiqueta,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline(),
                )
                if (opcion.cantidad != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = opcion.cantidad.toString(),
                        style = TipografiaNotally.numerico,
                        color = colorCantidad,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .widthIn(min = 24.dp)
                            .alignByBaseline(),
                    )
                }
            }
        }
    }
}
