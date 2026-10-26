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
import com.example.notally.ui.theme.NotallyMotion
import com.example.notally.ui.theme.NotallyType

/** Opción de filtro; el componente no conoce el dominio. */
data class FilterOption(
    val id: String,
    val label: String,
    val count: Int? = null,
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
fun FilterChipRow(
    options: List<FilterOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            FilterChipItem(
                option = option,
                selected = option.id == selectedId,
                onClick = { onSelect(option.id) },
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    option: FilterOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val container by animateColorAsState(
        targetValue = when {
            selected -> colors.tertiaryContainer
            pressed -> colors.surfaceContainerLow
            else -> Color.Transparent
        },
        animationSpec = tween(NotallyMotion.colorMs),
        label = "filterChipContainer",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) colors.onTertiaryContainer else colors.onSurfaceVariant,
        animationSpec = tween(NotallyMotion.colorMs),
        label = "filterChipLabel",
    )
    val countColor by animateColorAsState(
        targetValue = if (selected) colors.onTertiaryContainer else colors.onSurfaceVariant,
        animationSpec = tween(NotallyMotion.colorMs),
        label = "filterChipCount",
    )
    val borderModifier = when {
        selected -> Modifier
        else -> Modifier.border(1.dp, colors.outlineVariant, CircleShape)
    }
    // El contador se anuncia dentro del mismo nodo del chip (§7.7).
    val countDescription = option.count?.let { count ->
        pluralStringResource(R.plurals.a11y_filter_count, count, option.label, count)
    }

    // El target de 48×48 y el gesto viven aquí, no en el dibujo de 32 dp.
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .onFocusChanged { focused = it.isFocused }
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                if (countDescription != null) contentDescription = countDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .focusRing(visible = focused, shape = CircleShape)
                .clip(CircleShape)
                .background(container)
                .then(borderModifier)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = colors.onTertiaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Row {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = labelColor,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline(),
                )
                if (option.count != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = option.count.toString(),
                        style = NotallyType.numeric,
                        color = countColor,
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
