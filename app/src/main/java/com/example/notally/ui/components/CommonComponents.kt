package com.example.notally.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.example.notally.data.repository.ResultadoCurso
import com.example.notally.R
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.theme.TipografiaNotally
import com.example.notally.ui.utils.DateFormat

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

/**
 * Esqueleto de estado vacío y de error sin datos (§7.8, §7.13): título,
 * cuerpo opcional y una acción real. Sin ilustración.
 *
 * [paddingAccion] conserva el padding propio de cada pantalla sobre el botón.
 */
@Composable
internal fun MessageWithAction(
    titulo: String,
    etiquetaAccion: String,
    alAccionar: () -> Unit,
    modifier: Modifier = Modifier,
    cuerpo: String? = null,
    paddingAccion: PaddingValues = ButtonDefaults.TextButtonContentPadding,
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
        if (cuerpo != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = cuerpo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))
        TextButton(
            onClick = alAccionar,
            contentPadding = paddingAccion,
        ) {
            Text(etiquetaAccion)
        }
    }
}

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

/**
 * Encabezado de sección (§7.3): `labelSmall` uppercase, contador opcional y
 * chevron de 20 dp con rotación de 180 ms cuando la sección es colapsable.
 */
@Composable
fun SectionHeader(
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
        append(titulo.uppercase(DateFormat.LocaleEspanol))
        if (cantidad != null) {
            append(stringResource(R.string.task_meta_separator))
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

/**
 * Fila de opción de 56 dp para hojas de selección (§7.9): control de selección
 * a la izquierda, texto `bodyLarge`, selección marcada con `tertiary`.
 *
 * El slot izquierdo mide siempre 20 dp para que el texto no se desplace al
 * cambiar la selección.
 */
@Composable
fun OptionRow(
    etiqueta: String,
    seleccionado: Boolean,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarPuntoError: Boolean = false,
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
fun FilterChipRow(
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
            FilterChip(
                opcion = opcion,
                seleccionado = opcion.id == idSeleccionado,
                alPulsar = { alSeleccionar(opcion.id) },
            )
        }
    }
}

@Composable
private fun FilterChip(
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

/**
 * Anillo de foco de 2 dp `tertiary` con offset de 2 dp (§4.3, §7.2, §9.4).
 *
 * El anillo se dibuja **fuera** de los límites del contenido, así que no altera
 * el layout: no añade padding, no desplaza filas y no recorta el texto. Si el
 * padre aplica `clip`, el anillo puede quedar recortado; en las filas de lista
 * se usa sin clip.
 */
@Composable
fun Modifier.anilloFoco(
    visible: Boolean,
    shape: Shape,
    color: Color = MaterialTheme.colorScheme.tertiary,
    width: Dp = 2.dp,
    offset: Dp = 2.dp,
): Modifier {
    if (!visible) return this
    return drawWithContent {
        drawContent()
        val trazo = width.toPx()
        val expansion = offset.toPx() + trazo / 2f
        val contorno = shape.createOutline(
            size = Size(size.width + expansion * 2f, size.height + expansion * 2f),
            layoutDirection = layoutDirection,
            density = this,
        )
        translate(-expansion, -expansion) {
            drawOutline(
                outline = contorno,
                color = color,
                style = Stroke(width = trazo),
            )
        }
    }
}

/**
 * Campo de nombre de curso compartido por la hoja de Cursos y la hoja de curso
 * del editor. Solo pinta el campo: el botón y su estado de envío los decide
 * cada flujo.
 */
@Composable
internal fun CourseNameField(
    valor: String,
    alCambiarValor: (String) -> Unit,
    resultado: ResultadoCurso?,
    alLimpiarError: () -> Unit,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = {
            alCambiarValor(it)
            if (resultado != null) alLimpiarError()
        },
        label = { Text(stringResource(R.string.course_name)) },
        singleLine = true,
        isError = resultado != null && !resultado.esExito,
        supportingText = when (resultado) {
            ResultadoCurso.Duplicado -> {
                { Text(stringResource(R.string.course_name_duplicate)) }
            }

            ResultadoCurso.Error -> {
                { Text(stringResource(R.string.course_save_error)) }
            }

            else -> null
        },
        shape = FormaNotally.s,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
