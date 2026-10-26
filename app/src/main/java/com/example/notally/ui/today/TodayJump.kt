package com.example.notally.ui.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.notally.R
import com.example.notally.ui.components.DiaTira
import com.example.notally.ui.components.OpcionFiltro
import java.time.LocalDate

/** Destino de la tira de días: sección existente, no un filtro (§7.1.1). */
internal data class SaltoDia(
    val seccion: TipoSeccionHoy,
    val fechaEntrega: LocalDate,
    val expandirProximas: Boolean,
)

internal fun resolverSaltoDia(dia: DiaTira, hoy: LocalDate): SaltoDia = when {
    dia.fecha.isBefore(hoy) ->
        SaltoDia(TipoSeccionHoy.ATRASADAS, dia.fecha, expandirProximas = false)

    dia.fecha == hoy ->
        SaltoDia(TipoSeccionHoy.HOY, dia.fecha, expandirProximas = false)

    else ->
        SaltoDia(TipoSeccionHoy.PROXIMAS, dia.fecha, expandirProximas = true)
}

@Composable
internal fun opcionesFiltro(conteos: ConteosFiltroHoy): List<OpcionFiltro> =
    FiltroHoy.entries.map { filtro ->
        OpcionFiltro(
            id = filtro.id,
            etiqueta = stringResource(filtro.recursoEtiqueta()),
            cantidad = when (filtro) {
                FiltroHoy.TODAS -> conteos.todas
                FiltroHoy.PENDIENTES -> conteos.pendientes
                FiltroHoy.COMPLETADAS -> conteos.completadas
            },
        )
    }

private fun FiltroHoy.recursoEtiqueta(): Int = when (this) {
    FiltroHoy.TODAS -> R.string.filter_all
    FiltroHoy.PENDIENTES -> R.string.filter_pending
    FiltroHoy.COMPLETADAS -> R.string.filter_done
}

@Composable
internal fun tituloSeccion(tipo: TipoSeccionHoy): String = stringResource(
    when (tipo) {
        TipoSeccionHoy.ATRASADAS -> R.string.today_section_overdue
        TipoSeccionHoy.HOY -> R.string.today_section_today
        TipoSeccionHoy.SIN_HORA -> R.string.today_section_anytime
        TipoSeccionHoy.PROXIMAS -> R.string.today_section_upcoming
        TipoSeccionHoy.COMPLETADAS -> R.string.today_section_done
    },
)
