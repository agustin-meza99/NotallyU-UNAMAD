package com.example.notally.ui.today

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notally.data.DateRanges
import com.example.notally.R
import com.example.notally.ui.components.AvisoError
import com.example.notally.ui.components.BloqueError
import com.example.notally.ui.components.DayStrip
import com.example.notally.ui.components.DiaTira
import com.example.notally.ui.components.FilasCargando
import com.example.notally.ui.components.FilterChipRow
import com.example.notally.ui.components.FocalTaskCard
import com.example.notally.ui.components.OpcionFiltro
import com.example.notally.ui.components.SectionHeader
import com.example.notally.ui.components.SwipeTaskRow
import com.example.notally.ui.theme.EspaciadoNotally
import com.example.notally.ui.theme.margenHorizontalPantalla
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.theme.TipografiaNotally
import com.example.notally.ui.util.DateFormat
import com.example.notally.ui.util.textoEtiquetaFecha
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pantalla Hoy (§7.1–7.3, §7.7, §7.8, §7.10, §7.13).
 *
 * @param alCrearTarea abre el editor (la fase 4 conecta la navegación real).
 * @param alAbrirTarea recibe el id de la tarea para abrir el editor.
 * @param alCambiarVisibilidadCluster reporta al root si el cluster inferior
 * debe mostrarse según la dirección del scroll (§7.10).
 * @param alCambiarDisponibilidadAccion reporta al root si el estado permite
 * crear tarea: `false` solo en el vacío total, donde el CTA es la única
 * acción (§7.8).
 */
@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    alCrearTarea: () -> Unit = {},
    alAbrirTarea: (Long) -> Unit = {},
    diaSeleccionadoEpoch: Long? = null,
    alSeleccionarDia: (Long?) -> Unit = {},
    alCambiarVisibilidadCluster: (Boolean) -> Unit = {},
    alCambiarDisponibilidadAccion: (Boolean) -> Unit = {},
    viewModel: TodayViewModel = viewModel(
        factory = TodayViewModel.factory(LocalContext.current),
    ),
) {
    val estado by viewModel.estadoUi.collectAsStateWithLifecycle()
    val vacio = estado.vacio
    val estadoSnackbar = remember { SnackbarHostState() }
    val estadoLista = rememberLazyListState()
    val margen = margenHorizontalPantalla

    val mensajeCompletada = stringResource(R.string.task_completed)
    val mensajeEliminada = stringResource(R.string.task_deleted)
    val etiquetaDeshacer = stringResource(R.string.action_undo)
    val errorGuardado = stringResource(R.string.editor_saving_error)
    val etiquetaReintentar = stringResource(R.string.action_retry)
    val errorCargar = stringResource(R.string.error_load)
    val errorRefrescar = stringResource(R.string.error_refresh)
    val tituloPantalla = stringResource(R.string.nav_hoy)

    // Snackbars de 5 s exactos: Indefinite + timeout, con acción real.
    LaunchedEffect(
        viewModel,
        mensajeCompletada,
        mensajeEliminada,
        etiquetaDeshacer,
        errorGuardado,
        etiquetaReintentar,
    ) {
        viewModel.eventos.collectLatest { evento ->
            when (evento) {
                is EventoHoy.Completada -> {
                    val resultado = withTimeoutOrNull(MovimientoNotally.snackbarMs) {
                        estadoSnackbar.showSnackbar(
                            message = mensajeCompletada,
                            actionLabel = etiquetaDeshacer,
                            duration = SnackbarDuration.Indefinite,
                        )
                    }
                    if (resultado == SnackbarResult.ActionPerformed) {
                        viewModel.deshacer(evento.deshacer)
                    }
                }

                is EventoHoy.Eliminada -> {
                    val resultado = withTimeoutOrNull(MovimientoNotally.snackbarMs) {
                        estadoSnackbar.showSnackbar(
                            message = mensajeEliminada,
                            actionLabel = etiquetaDeshacer,
                            duration = SnackbarDuration.Indefinite,
                        )
                    }
                    if (resultado == SnackbarResult.ActionPerformed) {
                        viewModel.deshacer(evento.deshacer)
                    }
                }

                is EventoHoy.FalloGuardado -> {
                    val resultado = withTimeoutOrNull(MovimientoNotally.snackbarMs) {
                        estadoSnackbar.showSnackbar(
                            message = errorGuardado,
                            actionLabel = etiquetaReintentar,
                            duration = SnackbarDuration.Indefinite,
                        )
                    }
                    if (resultado == SnackbarResult.ActionPerformed) evento.reintentar()
                }
            }
        }
    }

    // Carga: solo si Room tarda más de 150 ms; antes no se pinta nada.
    var mostrarCarga by remember { mutableStateOf(false) }
    LaunchedEffect(estado.cargando, estado.tieneDatosEnCache) {
        if (estado.cargando && !estado.tieneDatosEnCache) {
            delay(150)
            mostrarCarga = true
        } else {
            mostrarCarga = false
        }
    }

    val fechaHoy = remember(estado.ahora) {
        DateRanges.fechaLocal(estado.ahora)
    }
    val diasTira = remember(estado.diasSemana, fechaHoy) {
        diasTiraHoy(estado, fechaHoy)
    }
    val mostrarTira = mostrarTiraHoy(estado, diasTira)

    // La selección de la tira vive en la raíz de pestañas con
    // `rememberSaveable`: sobrevive a la recreación de la Activity y se limpia
    // al cambiar de pestaña (§7.1.1). Nunca altera secciones, conteos ni resumen.
    val fechaSeleccionada = diaSeleccionadoEpoch?.let(LocalDate::ofEpochDay)

    val focalElegible = estado.filtro != FiltroHoy.COMPLETADAS && !mostrarCarga &&
        !(estado.error && !estado.tieneDatosEnCache) && estado.vacio == null
    val focal = if (focalElegible) estado.focal else null
    val mostrarFocal = focal != null
    val indiceBaseSecciones = 1 +
        (if (estado.error && estado.tieneDatosEnCache) 1 else 0) +
        (if (mostrarTira) 1 else 0) +
        (if (!estado.estaVacioTotal) 1 else 0) +
        (if (mostrarFocal) 1 else 0)

    // Destino de la tira: se guarda la intención y el efecto hace el salto
    // inmediato cuando la sección destino ya está compuesta.
    var saltoPendiente by remember { mutableStateOf<SaltoDia?>(null) }
    var topeViewport by remember { mutableStateOf(0f) }
    val coordenadasFila = remember { mutableMapOf<Long, LayoutCoordinates>() }

    LaunchedEffect(saltoPendiente, estado.secciones, indiceBaseSecciones, focal?.tareaId) {
        val salto = saltoPendiente ?: return@LaunchedEffect
        if (salto.expandirProximas) {
            val proximas = estado.secciones.firstOrNull {
                it.tipo == TipoSeccionHoy.PROXIMAS
            }
            if (proximas != null && !proximas.expandida) {
                viewModel.alternarSeccion(TipoSeccionHoy.PROXIMAS)
                return@LaunchedEffect
            }
        }

        fun idElementoSeccion(tipo: TipoSeccionHoy): Long? =
            estado.secciones.firstOrNull { it.tipo == tipo }?.elementos
                ?.firstOrNull { elemento ->
                    elemento.tarea.entregaEn?.let {
                        DateFormat.fechaLocal(it) == salto.fechaEntrega
                    } == true
                }
                ?.tarea
                ?.id

        var seccionDestino: TipoSeccionHoy? = salto.seccion
        var idTareaDestino = idElementoSeccion(salto.seccion)
        if (salto.seccion == TipoSeccionHoy.HOY && idTareaDestino == null) {
            // "Hoy" sin filas propias baja a la primera de "Sin hora" (§7.1.1).
            seccionDestino = TipoSeccionHoy.SIN_HORA
            idTareaDestino = estado.secciones
                .firstOrNull { it.tipo == TipoSeccionHoy.SIN_HORA }
                ?.elementos
                ?.firstOrNull()
                ?.tarea
                ?.id
        }
        val focalCoincide = focal?.let { elemento ->
            (elemento.grupo == seccionDestino || elemento.grupo == salto.seccion) &&
                elemento.elemento.tarea.entregaEn?.let {
                    DateFormat.fechaLocal(it) == salto.fechaEntrega
                } == true
        } == true
        if (idTareaDestino == null && focalCoincide) {
            seccionDestino = null
            idTareaDestino = focal?.tareaId
        }
        val indiceSeccion = seccionDestino?.let { tipo ->
            val posicion = estado.secciones.indexOfFirst { it.tipo == tipo }
            if (posicion >= 0) indiceBaseSecciones + posicion else null
        }
        val indice = when {
            seccionDestino == null -> if (mostrarFocal) indiceBaseSecciones - 1 else 0
            indiceSeccion != null -> indiceSeccion
            // Sin sección ni fila destino: el encabezado (§7.1.1).
            else -> 0
        }

        estadoLista.scrollToItem(indice)
        withFrameNanos { }
        withFrameNanos { }
        val coordenadas = idTareaDestino?.let(coordenadasFila::get)
        if (coordenadas != null) {
            val delta = coordenadas.positionInRoot().y - topeViewport
            if (delta != 0f) estadoLista.scrollBy(delta)
        }
        saltoPendiente = null
    }

    // Visibilidad del cluster (§7.10): se oculta al bajar y reaparece al subir.
    // El estado vive en la raíz de pestañas; aquí solo se reportan los cambios.
    val reportarVisibilidadCluster by rememberUpdatedState(alCambiarVisibilidadCluster)
    LaunchedEffect(estadoLista) {
        var indicePrevio = 0
        var desplazamientoPrevio = 0
        var visible = true
        snapshotFlow {
            Triple(
                estadoLista.firstVisibleItemIndex,
                estadoLista.firstVisibleItemScrollOffset,
                estadoLista.isScrollInProgress,
            )
        }.collect { (indice, desplazamiento, desplazando) ->
            if (desplazando) {
                val bajando = indice > indicePrevio ||
                    (indice == indicePrevio && desplazamiento > desplazamientoPrevio)
                val subiendo = indice < indicePrevio ||
                    (indice == indicePrevio && desplazamiento < desplazamientoPrevio)
                val nuevo = when {
                    bajando -> false
                    subiendo -> true
                    else -> visible
                }
                if (nuevo != visible) {
                    visible = nuevo
                    reportarVisibilidadCluster(nuevo)
                }
            }
            indicePrevio = indice
            desplazamientoPrevio = desplazamiento
        }
    }

    // En el vacío total el CTA del estado vacío es la única acción (§7.8):
    // el root oculta el FAB mientras este predicado sea cierto.
    val accionDisponible = vacio != VacioHoy.Total
    val reportarDisponibilidadAccion by rememberUpdatedState(alCambiarDisponibilidadAccion)
    LaunchedEffect(accionDisponible) {
        reportarDisponibilidadAccion(accionDisponible)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .semantics { paneTitle = tituloPantalla },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(estadoSnackbar) },
    ) { rellenoInterior ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(rellenoInterior),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                state = estadoLista,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = EspaciadoNotally.anchoMaximoContenido)
                    .fillMaxWidth()
                    .onGloballyPositioned { topeViewport = it.positionInRoot().y },
                contentPadding = PaddingValues(
                    start = margen,
                    end = margen,
                    top = 24.dp,
                    bottom = EspaciadoNotally.rellenoInferiorLista,
                ),
            ) {
                item(key = "header") {
                    EncabezadoHoy(
                        antetitulo = estado.antetitulo,
                        resumen = estado.resumen,
                    )
                }

                if (estado.error && estado.tieneDatosEnCache) {
                    item(key = "error_banner") {
                        AvisoError(
                            mensaje = errorRefrescar,
                            etiquetaReintentar = etiquetaReintentar,
                            alReintentar = viewModel::reintentar,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }

                if (mostrarTira) {
                    item(key = "day_strip") {
                        TiraDiasHoy(
                            dias = diasTira,
                            fechaSeleccionada = fechaSeleccionada,
                            alElegirDia = { dia ->
                                alSeleccionarDia(dia.fecha.toEpochDay())
                                saltoPendiente = resolverSaltoDia(dia, fechaHoy)
                            },
                        )
                    }
                }

                if (!estado.estaVacioTotal) {
                    item(key = "filters") {
                        FiltrosHoy(
                            opciones = opcionesFiltro(estado.conteosFiltro),
                            idSeleccionado = estado.filtro.id,
                            alSeleccionar = { id ->
                                viewModel.cambiarFiltro(FiltroHoy.desdeId(id))
                            },
                        )
                    }
                }

                elementosHoy(
                    focal = focal,
                    estado = estado,
                    mostrarCarga = mostrarCarga,
                    errorCargar = errorCargar,
                    etiquetaReintentar = etiquetaReintentar,
                    vacio = vacio,
                    alCrearTarea = alCrearTarea,
                    alAbrirTarea = alAbrirTarea,
                    viewModel = viewModel,
                    coordenadasFila = coordenadasFila,
                )

            }
        }
    }
}

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
