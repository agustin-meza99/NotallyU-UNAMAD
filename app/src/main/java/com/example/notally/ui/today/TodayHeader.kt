package com.example.notally.ui.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.ui.components.DayStrip
import com.example.notally.ui.components.DiaTira
import com.example.notally.ui.components.FilterChipRow
import com.example.notally.ui.components.OpcionFiltro
import com.example.notally.ui.theme.TipografiaNotally
import java.time.LocalDate

/** Tira de días derivada del estado, con la etiqueta de sección de cada día. */
internal fun diasTiraHoy(
    estado: EstadoUiHoy,
    fechaHoy: LocalDate,
): List<DiaTira> = estado.diasSemana.map { dia ->
    val recursoSeccion = when {
        dia.pendientes == 0 -> null
        dia.fecha.isBefore(fechaHoy) -> R.string.today_section_overdue
        dia.fecha == fechaHoy -> R.string.today_section_today
        else -> R.string.today_section_upcoming
    }
    DiaTira(
        fecha = dia.fecha,
        pendientes = dia.pendientes,
        esHoy = dia.fecha == fechaHoy,
        recursoEtiquetaSeccion = recursoSeccion,
    )
}

/** La tira solo aparece con datos en caché, sin carga y fuera de "Completadas". */
internal fun mostrarTiraHoy(estado: EstadoUiHoy, dias: List<DiaTira>): Boolean =
    estado.tieneDatosEnCache && !estado.cargando &&
        estado.filtro != FiltroHoy.COMPLETADAS &&
        dias.any { it.pendientes > 0 }

@Composable
internal fun EncabezadoHoy(
    antetitulo: String,
    resumen: ResumenHoy?,
) {
    val colores = MaterialTheme.colorScheme
    Column {
        // Sin título visible (§7.1): la fecha orienta y el resumen informa.
        if (antetitulo.isNotEmpty()) {
            Text(
                text = antetitulo,
                style = MaterialTheme.typography.labelSmall,
                color = colores.onSurfaceVariant,
            )
        }
        if (resumen != null) {
            if (antetitulo.isNotEmpty()) Spacer(Modifier.height(4.dp))
            Text(
                text = pluralStringResource(
                    R.plurals.today_summary,
                    resumen.total,
                    resumen.completadas,
                    resumen.total,
                ),
                style = TipografiaNotally.numerico,
                color = colores.onSurface,
            )
        }
    }
}

@Composable
internal fun TiraDiasHoy(
    dias: List<DiaTira>,
    fechaSeleccionada: LocalDate?,
    alElegirDia: (DiaTira) -> Unit,
) {
    DayStrip(
        dias = dias,
        fechaSeleccionada = fechaSeleccionada,
        alElegirDia = alElegirDia,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
internal fun FiltrosHoy(
    opciones: List<OpcionFiltro>,
    idSeleccionado: String,
    alSeleccionar: (String) -> Unit,
) {
    FilterChipRow(
        opciones = opciones,
        idSeleccionado = idSeleccionado,
        alSeleccionar = alSeleccionar,
        modifier = Modifier.padding(top = 16.dp),
    )
}
