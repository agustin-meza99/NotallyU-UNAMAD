package com.example.notally.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.notally.data.model.Prioridad
import com.example.notally.R
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.utils.DateFormat
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Un día de la tira semanal (§7.1.1).
 *
 * [recursoEtiquetaSeccion] es el nombre de la sección destino ("Atrasadas",
 * "Para hoy", "Próximas"); `null` cuando el día no tiene pendientes y por tanto
 * no es clicable.
 */
data class DiaTira(
    val fecha: LocalDate,
    val pendientes: Int,
    val esHoy: Boolean,
    val recursoEtiquetaSeccion: Int?,
)

/**
 * Tira de siete días de la semana en curso (§7.1.1).
 *
 * Celdas de 48 × 64 dp mínimo que crecen con la fuente, gap de 8 dp y scroll
 * horizontal sin barra. Cada celda con pendientes es un botón independiente;
 * la selección nunca se comunica solo con `primaryContainer`: añade borde y
 * cambia el color del texto.
 */
@Composable
fun DayStrip(
    dias: List<DiaTira>,
    fechaSeleccionada: LocalDate?,
    alElegirDia: (DiaTira) -> Unit,
    modifier: Modifier = Modifier,
) {
    val estadoScroll = rememberScrollState()
    var tiraX by remember { mutableStateOf(0f) }
    var anchoTira by remember { mutableStateOf(0) }
    var hoyX by remember { mutableStateOf<Float?>(null) }
    var anchoHoy by remember { mutableStateOf(0) }
    var hoyCentrado by remember { mutableStateOf(false) }

    // La celda de hoy se lleva al centro en la primera composición, sin
    // animación (§7.1.1).
    LaunchedEffect(dias, anchoTira, hoyX, estadoScroll.maxValue) {
        val celdaX = hoyX ?: return@LaunchedEffect
        if (hoyCentrado || anchoTira <= 0) return@LaunchedEffect
        val centroCelda = celdaX - tiraX + anchoHoy / 2f
        val destino = (centroCelda - anchoTira / 2f).roundToInt()
            .coerceIn(0, estadoScroll.maxValue)
        estadoScroll.scrollTo(destino)
        hoyCentrado = true
    }

    Row(
        modifier = modifier
            .onGloballyPositioned {
                tiraX = it.positionInRoot().x
                anchoTira = it.size.width
            }
            .horizontalScroll(estadoScroll),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        dias.forEach { dia ->
            DayCell(
                dia = dia,
                seleccionado = dia.fecha == fechaSeleccionada,
                alPulsar = { alElegirDia(dia) },
                modifier = if (dia.esHoy) {
                    Modifier.onGloballyPositioned {
                        hoyX = it.positionInRoot().x
                        anchoHoy = it.size.width
                    }
                } else {
                    Modifier
                },
            )
        }
    }
}

@Composable
private fun DayCell(
    dia: DiaTira,
    seleccionado: Boolean,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    var enfocado by remember { mutableStateOf(false) }

    val contenedor by animateColorAsState(
        targetValue = if (seleccionado) colores.primaryContainer else Color.Transparent,
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "dayCellContainer",
    )
    val colorBorde by animateColorAsState(
        targetValue = if (seleccionado) colores.onSurfaceVariant else Color.Transparent,
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "dayCellBorder",
    )
    val colorDiaSemana by animateColorAsState(
        targetValue = if (seleccionado) {
            colores.onPrimaryContainer
        } else {
            colores.onSurfaceVariant
        },
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "dayCellWeekday",
    )
    val colorNumero by animateColorAsState(
        targetValue = when {
            seleccionado -> colores.onPrimaryContainer
            dia.recursoEtiquetaSeccion == null -> colores.onSurfaceVariant
            else -> colores.onSurface
        },
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "dayCellNumber",
    )
    val colorBarra by animateColorAsState(
        targetValue = if (seleccionado) colores.onPrimaryContainer else colores.outline,
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "dayCellBar",
    )

    val textoPendientes = if (dia.pendientes == 0) {
        stringResource(R.string.today_pending_none_label)
    } else {
        pluralStringResource(
            R.plurals.today_pending_count,
            dia.pendientes,
            dia.pendientes,
        )
    }
    val etiqueta = stringResource(
        R.string.a11y_week_day,
        DateFormat.nombreDia(dia.fecha),
        dia.fecha.dayOfMonth,
        DateFormat.nombreMes(dia.fecha),
        textoPendientes,
    )
    val etiquetaAccion = dia.recursoEtiquetaSeccion?.let { recursoSeccion ->
        stringResource(R.string.a11y_week_open, stringResource(recursoSeccion))
    }
    val estadoSeleccionado = stringResource(R.string.a11y_week_selected)

    val modificadorClic = if (etiquetaAccion != null) {
        Modifier.clickable(
            role = Role.Button,
            onClickLabel = etiquetaAccion,
            onClick = alPulsar,
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .widthIn(min = 48.dp)
            .heightIn(min = 64.dp)
            .onFocusChanged { enfocado = it.isFocused }
            .anilloFoco(visible = enfocado, shape = FormaNotally.m)
            .clip(FormaNotally.m)
            .background(contenedor)
            .border(1.dp, colorBorde, FormaNotally.m)
            .then(modificadorClic)
            .semantics(mergeDescendants = true) {
                contentDescription = etiqueta
                if (seleccionado) stateDescription = estadoSeleccionado
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = DateFormat.abreviaturaDia(dia.fecha),
                style = MaterialTheme.typography.labelSmall,
                color = colorDiaSemana,
                maxLines = 1,
            )
            if (dia.esHoy) {
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(colores.tertiary),
                )
            }
        }
        Text(
            text = dia.fecha.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = colorNumero,
            maxLines = 1,
        )
        // Área inferior de 8 dp reservada siempre: mostrar u ocultar la barra
        // no cambia la altura de la celda.
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (dia.pendientes > 0) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(colorBarra),
                )
            }
        }
    }
}

/**
 * Tarea focal de Hoy (§7.3.1).
 *
 * Único bloque invertido de la pantalla: `inverseSurface` con texto
 * `inverseOnSurface`, sin borde, sin sombra y sin checkbox. Toda la tarjeta
 * abre el editor y se anuncia como un solo nodo.
 *
 * @param antetitulo grupo ya resuelto ("Atrasadas", "Para hoy", "Sin hora",
 *   "Próximas"); la tarjeta aplica uppercase.
 * @param textoEntrega etiqueta de entrega ("Hoy", "Vie 31 oct", "Sin fecha").
 */
@Composable
fun FocalTaskCard(
    titulo: String,
    antetitulo: String,
    textoCurso: String,
    textoEntrega: String,
    prioridad: Prioridad,
    modifier: Modifier = Modifier,
    alPulsar: () -> Unit = {},
) {
    val colores = MaterialTheme.colorScheme
    var enfocada by remember { mutableStateOf(false) }

    val etiquetaPrioridad = priorityLabelForA11y(prioridad)
    val separador = stringResource(R.string.task_meta_separator)
    val descripcion = stringResource(
        R.string.a11y_featured_task,
        titulo,
        textoCurso,
        textoEntrega,
        etiquetaPrioridad,
    )
    val etiquetaAbrir = stringResource(R.string.a11y_openTask)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .onFocusChanged { enfocada = it.isFocused }
            .anilloFoco(visible = enfocada, shape = FormaNotally.xl)
            .clip(FormaNotally.xl)
            .background(colores.inverseSurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colores.inverseOnSurface),
                role = Role.Button,
                onClickLabel = etiquetaAbrir,
                onClick = alPulsar,
            )
            .semantics { contentDescription = descripcion }
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = antetitulo.uppercase(DateFormat.LocaleEspanol),
            style = MaterialTheme.typography.labelSmall,
            color = colores.inverseOnSurface,
        )
        Spacer(Modifier.height(8.dp))
        Column {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                color = colores.inverseOnSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = textoCurso + separador + textoEntrega,
                style = MaterialTheme.typography.bodySmall,
                color = colores.inverseOnSurface,
            )
        }
    }
}
