package com.example.notally.ui.today

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.ui.components.BloqueError
import com.example.notally.ui.components.DiaTira
import com.example.notally.ui.components.EmptyState
import com.example.notally.ui.components.FilasCargando
import com.example.notally.ui.components.FocalTaskCard
import com.example.notally.ui.components.OpcionFiltro
import com.example.notally.ui.components.SectionHeader
import com.example.notally.ui.components.SwipeTaskRow
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.utils.textoEtiquetaFecha
import java.time.LocalDate

@Composable
internal fun EmptyStateHoy(
    vacio: VacioHoy,
    alCrearTarea: () -> Unit,
    alMostrarTodas: () -> Unit,
    alMostrarPendientes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (vacio) {
        VacioHoy.Total -> EmptyState(
            titulo = stringResource(R.string.today_empty_title),
            cuerpo = stringResource(R.string.today_empty_body),
            etiquetaAccion = stringResource(R.string.today_empty_cta),
            alAccionar = alCrearTarea,
            modifier = modifier,
        )

        VacioHoy.FiltroPendientes -> EmptyState(
            titulo = stringResource(R.string.filter_empty_pending_title),
            cuerpo = stringResource(R.string.filter_empty_pending_body),
            etiquetaAccion = stringResource(R.string.filter_empty_pending_cta),
            alAccionar = alMostrarTodas,
            modifier = modifier,
        )

        VacioHoy.FiltroCompletadas -> EmptyState(
            titulo = stringResource(R.string.filter_empty_done_title),
            cuerpo = stringResource(R.string.filter_empty_done_body),
            etiquetaAccion = stringResource(R.string.filter_empty_done_cta),
            alAccionar = alMostrarPendientes,
            modifier = modifier,
        )
    }
}

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
        TipoSeccionHoy.NO_ENTREGADAS -> R.string.today_section_not_delivered
        TipoSeccionHoy.ATRASADAS -> R.string.today_section_overdue
        TipoSeccionHoy.HOY -> R.string.today_section_today
        TipoSeccionHoy.SIN_HORA -> R.string.today_section_anytime
        TipoSeccionHoy.ENTREGADAS -> R.string.today_section_delivered
        TipoSeccionHoy.PROXIMAS -> R.string.today_section_upcoming
        TipoSeccionHoy.COMPLETADAS -> R.string.today_section_done
    },
)

/** Filas de la lista de Hoy: foco, carga/error, vacío, completadas y secciones. */
internal fun LazyListScope.elementosHoy(
    focal: TareaFocalHoy?,
    estado: EstadoUiHoy,
    mostrarCarga: Boolean,
    errorCargar: String,
    etiquetaReintentar: String,
    vacio: VacioHoy?,
    alCrearTarea: () -> Unit,
    alAbrirTarea: (Long) -> Unit,
    viewModel: TodayViewModel,
    coordenadasFila: MutableMap<Long, LayoutCoordinates>,
) {
    if (focal != null) {
        item(key = "focal_task") {
            AnimatedContent(
                targetState = focal,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = MovimientoNotally.fundidoMs / 2,
                            delayMillis = MovimientoNotally.fundidoMs / 2,
                            easing = MovimientoNotally.fundidoEasing,
                        ),
                    ) togetherWith fadeOut(
                        animationSpec = tween(
                            durationMillis = MovimientoNotally.fundidoMs / 2,
                            easing = MovimientoNotally.fundidoEasing,
                        ),
                    )
                },
                label = "focalTask",
                modifier = Modifier.padding(top = 24.dp),
            ) { objetivo ->
                val tarea = objetivo.elemento.tarea
                FocalTaskCard(
                    titulo = tarea.titulo,
                    antetitulo = if (objetivo.grupo == TipoSeccionHoy.NO_ENTREGADAS) {
                        stringResource(R.string.task_not_delivered)
                    } else {
                        tituloSeccion(objetivo.grupo)
                    },
                    textoCurso = objetivo.elemento.curso?.nombre
                        ?: stringResource(R.string.task_no_course),
                    textoEntrega = tarea.entregaEn?.let {
                        textoEtiquetaFecha(it, estado.ahora)
                    } ?: stringResource(R.string.task_noDate),
                    prioridad = tarea.prioridad,
                    modifier = Modifier.onGloballyPositioned {
                        coordenadasFila[objetivo.tareaId] = it
                    },
                    alPulsar = { alAbrirTarea(tarea.id) },
                )
            }
        }
    }

    when {
        mostrarCarga -> {
            item(key = "loading") {
                FilasCargando(modifier = Modifier.padding(top = 24.dp))
            }
        }

        estado.error && !estado.tieneDatosEnCache -> {
            item(key = "error_block") {
                BloqueError(
                    titulo = errorCargar,
                    etiquetaReintentar = etiquetaReintentar,
                    alReintentar = viewModel::reintentar,
                    modifier = Modifier.padding(top = 48.dp),
                )
            }
        }

        vacio != null -> {
            item(key = "empty") {
                EmptyStateHoy(
                    vacio = vacio,
                    alCrearTarea = alCrearTarea,
                    alMostrarTodas = {
                        viewModel.cambiarFiltro(FiltroHoy.TODAS)
                    },
                    alMostrarPendientes = {
                        viewModel.cambiarFiltro(FiltroHoy.PENDIENTES)
                    },
                    modifier = Modifier.padding(top = 48.dp),
                )
            }
        }

        estado.filtro == FiltroHoy.COMPLETADAS -> {
            item(key = "flat_completed") {
                Column(
                    modifier = Modifier
                        .animateItem(
                            placementSpec = tween(
                                MovimientoNotally.movimientoElementoMs,
                            ),
                        )
                        .animateContentSize(
                            MovimientoNotally.espacialNormal(),
                        )
                        .padding(top = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    estado.completadasPlanas.forEach { elemento ->
                        ElementoTarea(
                            elemento = elemento,
                            ahora = estado.ahora,
                            viewModel = viewModel,
                            alAbrirTarea = alAbrirTarea,
                            modifier = Modifier.onGloballyPositioned {
                                coordenadasFila[elemento.tarea.id] = it
                            },
                        )
                    }
                }
            }
        }

        else -> {
            estado.secciones.forEach { seccion ->
                item(key = "section_${seccion.tipo.name}") {
                    Column(
                        modifier = Modifier
                            .animateItem(
                                placementSpec = tween(
                                    MovimientoNotally.movimientoElementoMs,
                                ),
                            )
                            .animateContentSize(
                                MovimientoNotally.espacialNormal(),
                            )
                            .padding(top = 24.dp),
                    ) {
                        SectionHeader(
                            titulo = tituloSeccion(seccion.tipo),
                            cantidad = seccion.cantidad,
                            contraible = seccion.contraible,
                            expandida = seccion.expandida,
                            descripcionAccesibilidad =
                                if (seccion.tipo == TipoSeccionHoy.ENTREGADAS) {
                                    stringResource(R.string.a11y_section_delivered)
                                } else {
                                    null
                                },
                            alAlternar = {
                                viewModel.alternarSeccion(seccion.tipo)
                            },
                        )
                        if (seccion.expandida) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                seccion.elementos.forEach { elemento ->
                                    ElementoTarea(
                                        elemento = elemento,
                                        ahora = estado.ahora,
                                        viewModel = viewModel,
                                        alAbrirTarea = alAbrirTarea,
                                        modifier = Modifier.onGloballyPositioned {
                                            coordenadasFila[elemento.tarea.id] = it
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ElementoTarea(
    elemento: ElementoHoy,
    ahora: Long,
    viewModel: TodayViewModel,
    alAbrirTarea: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    SwipeTaskRow(
        tarea = elemento.tarea,
        nombreCurso = elemento.curso?.nombre,
        ahora = ahora,
        modifier = modifier,
        alPulsar = { alAbrirTarea(elemento.tarea.id) },
        alAlternarCompletada = { viewModel.alternarCompletada(elemento.tarea) },
        alCompletar = { viewModel.completar(elemento.tarea) },
        alPosponer = { viewModel.posponerParaManana(elemento.tarea) },
        alEliminar = { viewModel.eliminar(elemento.tarea) },
    )
}
