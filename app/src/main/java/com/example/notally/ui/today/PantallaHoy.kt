package com.example.notally.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notally.R
import com.example.notally.ui.components.AvisoError
import com.example.notally.ui.components.BloqueError
import com.example.notally.ui.components.DiaTira
import com.example.notally.ui.components.EncabezadoSeccion
import com.example.notally.ui.components.EstadoVacio
import com.example.notally.ui.components.FilaChipsFiltro
import com.example.notally.ui.components.FilaTareaDeslizable
import com.example.notally.ui.components.FilasCargando
import com.example.notally.ui.components.OpcionFiltro
import com.example.notally.ui.components.TarjetaTareaFocal
import com.example.notally.ui.components.TiraDias
import com.example.notally.ui.components.anilloFoco
import com.example.notally.ui.theme.EspaciadoNotally
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.theme.TipografiaNotally
import com.example.notally.ui.theme.margenHorizontalPantalla
import com.example.notally.ui.util.FormatoFecha
import com.example.notally.ui.util.textoEtiquetaFecha
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pantalla Hoy (§7.1–7.3, §7.7, §7.8, §7.10, §7.13).
 *
 * @param alCrearTarea abre el editor (la fase 4 conecta la navegación real).
 * @param alAbrirTarea recibe el id de la tarea para abrir el editor.
 */
@Composable
fun PantallaHoy(
    modifier: Modifier = Modifier,
    alCrearTarea: () -> Unit = {},
    alAbrirTarea: (Long) -> Unit = {},
    viewModel: HoyViewModel = viewModel(
        factory = HoyViewModel.factory(LocalContext.current),
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
        Instant.ofEpochMilli(estado.ahora).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val diasTira = remember(estado.diasSemana, fechaHoy) {
        estado.diasSemana.map { dia ->
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
    }
    val mostrarTira = estado.tieneDatosEnCache && !estado.cargando &&
        estado.filtro != FiltroHoy.COMPLETADAS &&
        diasTira.any { it.pendientes > 0 }

    // La selección vive en la pantalla: se conserva hasta tocar otro día o
    // cambiar de pestaña, y nunca altera secciones, conteos ni resumen.
    var diaSeleccionadoEpoch by remember { mutableStateOf<Long?>(null) }
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
                        FormatoFecha.fechaLocal(it) == salto.fechaEntrega
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
                    FormatoFecha.fechaLocal(it) == salto.fechaEntrega
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

    // FAB: se oculta al bajar y reaparece al subir (fade + escala 150 ms).
    var fabVisible by remember { mutableStateOf(true) }
    LaunchedEffect(estadoLista) {
        var indicePrevio = 0
        var desplazamientoPrevio = 0
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
                if (bajando) fabVisible = false
                if (subiendo) fabVisible = true
            }
            indicePrevio = indice
            desplazamientoPrevio = desplazamiento
        }
    }

    val mostrarFab = fabVisible && estado.vacio != VacioHoy.Total && !estado.cargando &&
        !(estado.error && !estado.tieneDatosEnCache)
    var fabEnfocado by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(estadoSnackbar) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = mostrarFab,
                enter = fadeIn(tween(MovimientoNotally.fabMs)) +
                    scaleIn(
                        animationSpec = tween(MovimientoNotally.fabMs),
                        initialScale = 0.9f,
                    ),
                exit = fadeOut(tween(MovimientoNotally.fabMs)) +
                    scaleOut(
                        animationSpec = tween(MovimientoNotally.fabMs),
                        targetScale = 0.9f,
                    ),
            ) {
                FloatingActionButton(
                    onClick = alCrearTarea,
                    modifier = Modifier
                        .size(52.dp)
                        .onFocusChanged { fabEnfocado = it.isFocused }
                        .anilloFoco(visible = fabEnfocado, shape = CircleShape),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 2.dp,
                        focusedElevation = 2.dp,
                        hoveredElevation = 2.dp,
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.a11y_newTask),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
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
                    bottom = 96.dp,
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
                        TiraDias(
                            dias = diasTira,
                            fechaSeleccionada = fechaSeleccionada,
                            alElegirDia = { dia ->
                                diaSeleccionadoEpoch = dia.fecha.toEpochDay()
                                saltoPendiente = resolverSaltoDia(dia, fechaHoy)
                            },
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }

                if (!estado.estaVacioTotal) {
                    item(key = "filters") {
                        FilaChipsFiltro(
                            opciones = opcionesFiltro(estado.conteosFiltro),
                            idSeleccionado = estado.filtro.id,
                            alSeleccionar = { id ->
                                viewModel.cambiarFiltro(
                                    when (id) {
                                        "pending" -> FiltroHoy.PENDIENTES
                                        "done" -> FiltroHoy.COMPLETADAS
                                        else -> FiltroHoy.TODAS
                                    },
                                )
                            },
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }

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
                            TarjetaTareaFocal(
                                titulo = tarea.titulo,
                                antetitulo = tituloSeccion(objetivo.grupo),
                                textoMateria = objetivo.elemento.materia?.nombre
                                    ?: stringResource(R.string.task_noSubject),
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
                            EstadoVacioHoy(
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
                                    EncabezadoSeccion(
                                        titulo = tituloSeccion(seccion.tipo),
                                        cantidad = seccion.cantidad,
                                        contraible = seccion.contraible,
                                        expandida = seccion.expandida,
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
        }
    }
}

@Composable
private fun EncabezadoHoy(
    antetitulo: String,
    resumen: ResumenHoy?,
) {
    val colores = MaterialTheme.colorScheme
    Column {
        if (antetitulo.isNotEmpty()) {
            Text(
                text = antetitulo,
                style = MaterialTheme.typography.labelSmall,
                color = colores.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = stringResource(R.string.a11y_today_heading),
            style = MaterialTheme.typography.displaySmall,
            color = colores.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        if (resumen != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = pluralStringResource(
                    R.plurals.today_summary,
                    resumen.total,
                    resumen.completadas,
                    resumen.total,
                ),
                style = TipografiaNotally.numerico,
                color = colores.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ElementoTarea(
    elemento: ElementoHoy,
    ahora: Long,
    viewModel: HoyViewModel,
    alAbrirTarea: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilaTareaDeslizable(
        tarea = elemento.tarea,
        nombreMateria = elemento.materia?.nombre,
        colorMateriaArgb = elemento.materia?.color,
        ahora = ahora,
        modifier = modifier,
        alPulsar = { alAbrirTarea(elemento.tarea.id) },
        alAlternarCompletada = { viewModel.alternarCompletada(elemento.tarea) },
        alCompletar = { viewModel.completar(elemento.tarea) },
        alPosponer = { viewModel.posponerParaManana(elemento.tarea) },
        alEliminar = { viewModel.eliminar(elemento.tarea) },
    )
}

@Composable
private fun EstadoVacioHoy(
    vacio: VacioHoy,
    alCrearTarea: () -> Unit,
    alMostrarTodas: () -> Unit,
    alMostrarPendientes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (vacio) {
        VacioHoy.Total -> EstadoVacio(
            titulo = stringResource(R.string.today_empty_title),
            cuerpo = stringResource(R.string.today_empty_body),
            etiquetaAccion = stringResource(R.string.today_empty_cta),
            alAccionar = alCrearTarea,
            modifier = modifier,
        )

        VacioHoy.FiltroPendientes -> EstadoVacio(
            titulo = stringResource(R.string.filter_empty_pending_title),
            cuerpo = stringResource(R.string.filter_empty_pending_body),
            etiquetaAccion = stringResource(R.string.filter_empty_pending_cta),
            alAccionar = alMostrarTodas,
            modifier = modifier,
        )

        VacioHoy.FiltroCompletadas -> EstadoVacio(
            titulo = stringResource(R.string.filter_empty_done_title),
            cuerpo = stringResource(R.string.filter_empty_done_body),
            etiquetaAccion = stringResource(R.string.filter_empty_done_cta),
            alAccionar = alMostrarPendientes,
            modifier = modifier,
        )
    }
}

private val FiltroHoy.id: String
    get() = when (this) {
        FiltroHoy.TODAS -> "all"
        FiltroHoy.PENDIENTES -> "pending"
        FiltroHoy.COMPLETADAS -> "done"
    }

@Composable
private fun opcionesFiltro(conteos: ConteosFiltroHoy): List<OpcionFiltro> = listOf(
    OpcionFiltro("all", stringResource(R.string.filter_all), conteos.todas),
    OpcionFiltro("pending", stringResource(R.string.filter_pending), conteos.pendientes),
    OpcionFiltro("done", stringResource(R.string.filter_done), conteos.completadas),
)

/** Destino de la tira de días: sección existente, no un filtro (§7.1.1). */
private data class SaltoDia(
    val seccion: TipoSeccionHoy,
    val fechaEntrega: LocalDate,
    val expandirProximas: Boolean,
)

private fun resolverSaltoDia(dia: DiaTira, hoy: LocalDate): SaltoDia = when {
    dia.fecha.isBefore(hoy) ->
        SaltoDia(TipoSeccionHoy.ATRASADAS, dia.fecha, expandirProximas = false)

    dia.fecha == hoy ->
        SaltoDia(TipoSeccionHoy.HOY, dia.fecha, expandirProximas = false)

    else ->
        SaltoDia(TipoSeccionHoy.PROXIMAS, dia.fecha, expandirProximas = true)
}

@Composable
private fun tituloSeccion(tipo: TipoSeccionHoy): String = stringResource(
    when (tipo) {
        TipoSeccionHoy.ATRASADAS -> R.string.today_section_overdue
        TipoSeccionHoy.HOY -> R.string.today_section_today
        TipoSeccionHoy.SIN_HORA -> R.string.today_section_anytime
        TipoSeccionHoy.PROXIMAS -> R.string.today_section_upcoming
        TipoSeccionHoy.COMPLETADAS -> R.string.today_section_done
    },
)
