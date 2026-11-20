package com.example.notally.ui.cursos

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notally.R
import com.example.notally.data.model.Curso
import com.example.notally.ui.components.AvisoError
import com.example.notally.ui.components.BloqueError
import com.example.notally.ui.components.EmptyState
import com.example.notally.ui.components.FilasCargando
import com.example.notally.ui.components.anilloFoco
import com.example.notally.ui.theme.EspaciadoNotally
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.theme.TipografiaNotally
import com.example.notally.ui.theme.margenHorizontalPantalla
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pantalla Cursos (§7.4, §7.8, §7.13).
 *
 * La tarjeta es la única acción: abre la hoja "Editar curso". No hay ruta de
 * detalle de curso en V1 (§0.5). Los conteos salen de Room; "entregas esta
 * semana" se recalcula con el ajuste de inicio de semana.
 */
@Composable
fun CursosScreen(
    modifier: Modifier = Modifier,
    alAbrirTareasCurso: (Long) -> Unit = {},
    solicitudNuevoCursoPendiente: Boolean = false,
    alConsumirSolicitudNuevoCurso: () -> Unit = {},
    viewModel: CursosViewModel = viewModel(
        factory = CursosViewModel.factory(LocalContext.current),
    ),
) {
    val estado by viewModel.estadoUi.collectAsStateWithLifecycle()
    val errorFormulario by viewModel.errorFormulario.collectAsStateWithLifecycle()
    val estadoSnackbar = remember { SnackbarHostState() }
    val margen = margenHorizontalPantalla

    var objetivoHoja by remember { mutableStateOf<ObjetivoCursoSheet?>(null) }

    // El FAB vive en el cluster del root. La señal es de un solo uso: se abre
    // la hoja y se consume; sin pendiente, volver a la pestaña no la reabre.
    LaunchedEffect(solicitudNuevoCursoPendiente) {
        if (solicitudNuevoCursoPendiente) {
            viewModel.limpiarErrorFormulario()
            objetivoHoja = ObjetivoCursoSheet.Nueva
            alConsumirSolicitudNuevoCurso()
        }
    }

    val errorEliminar = stringResource(R.string.course_delete_error)
    val etiquetaReintentar = stringResource(R.string.action_retry)
    val errorCargar = stringResource(R.string.error_load_courses)
    val errorRefrescar = stringResource(R.string.error_refresh)
    val tituloPantalla = stringResource(R.string.nav_cursos)

    LaunchedEffect(viewModel, errorEliminar, etiquetaReintentar) {
        viewModel.eventos.collectLatest { evento ->
            when (evento) {
                is EventoCursos.Fallo -> {
                    val resultado = withTimeoutOrNull(MovimientoNotally.snackbarMs) {
                        estadoSnackbar.showSnackbar(
                            message = errorEliminar,
                            actionLabel = etiquetaReintentar,
                            duration = SnackbarDuration.Indefinite,
                        )
                    }
                    if (resultado == SnackbarResult.ActionPerformed) evento.reintentar()
                }
            }
        }
    }

    // Carga: solo si Room tarda más de 150 ms (§7.13). Antes de ese umbral no
    // se pinta nada, así el estado vacío no parpadea.
    val mostrarCarga by produceState(
        initialValue = false,
        key1 = estado.cargando,
        key2 = estado.tieneDatosEnCache,
    ) {
        value = if (estado.cargando && !estado.tieneDatosEnCache) {
            delay(150)
            true
        } else {
            false
        }
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
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = EspaciadoNotally.anchoMaximoContenido)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = margen,
                    end = margen,
                    top = 24.dp,
                    // Deja la última tarjeta por encima del FAB y del cluster
                    // inferior, con el mismo criterio que Hoy (§4.1, §6).
                    bottom = EspaciadoNotally.rellenoInferiorLista,
                ),
            ) {
                item(key = "header") {
                    EncabezadoCursos(
                        cantidadCursos = estado.cursos.size,
                        totalPendientes = estado.totalPendientes,
                        entregasSemanales = estado.entregasSemanales,
                        mostrarConteos = estado.tieneDatosEnCache &&
                            estado.cursos.isNotEmpty(),
                        modifier = Modifier.padding(bottom = 16.dp),
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

                when {
                    estado.cargando && !estado.tieneDatosEnCache -> {
                        if (mostrarCarga) {
                            item(key = "loading") {
                                FilasCargando(modifier = Modifier.padding(top = 24.dp))
                            }
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

                    estado.cursos.isEmpty() -> {
                        item(key = "empty") {
                            EmptyState(
                                titulo = stringResource(R.string.courses_empty_title),
                                cuerpo = stringResource(R.string.courses_empty_body),
                                etiquetaAccion = stringResource(R.string.courses_new),
                                alAccionar = {
                                    viewModel.limpiarErrorFormulario()
                                    objetivoHoja = ObjetivoCursoSheet.Nueva
                                },
                                modifier = Modifier.padding(top = 48.dp),
                            )
                        }
                    }

                    else -> {
                        items(
                            items = estado.cursos,
                            key = { it.curso.id },
                        ) { tarjeta ->
                            TarjetaCurso(
                                tarjeta = tarjeta,
                                alPulsar = {
                                    viewModel.limpiarErrorFormulario()
                                    objetivoHoja = ObjetivoCursoSheet.Editar(tarjeta)
                                },
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    val objetivo = objetivoHoja
    if (objetivo != null) {
        CursoSheet(
            objetivo = objetivo,
            errorFormulario = errorFormulario,
            alDescartar = {
                viewModel.limpiarErrorFormulario()
                objetivoHoja = null
            },
            alGuardar = { existente: Curso?, nombre: String ->
                viewModel.guardarCurso(existente, nombre)
            },
            alGuardado = {
                viewModel.limpiarErrorFormulario()
                objetivoHoja = null
            },
            alEliminar = { curso ->
                viewModel.eliminarCurso(curso) {
                    viewModel.limpiarErrorFormulario()
                    objetivoHoja = null
                }
            },
            alVerTareas = {
                val curso = (objetivo as? ObjetivoCursoSheet.Editar)?.tarjeta?.curso
                if (curso != null) {
                    viewModel.limpiarErrorFormulario()
                    objetivoHoja = null
                    alAbrirTareasCurso(curso.id)
                }
            },
            alLimpiarError = viewModel::limpiarErrorFormulario,
        )
    }
}

@Composable
private fun EncabezadoCursos(
    cantidadCursos: Int,
    totalPendientes: Int,
    entregasSemanales: Int,
    mostrarConteos: Boolean,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    // Sin título visible (§7.4): el primer dato es el conteo real. El botón
    // "+" vive ahora en el FAB inferior.
    Column(modifier = modifier.fillMaxWidth()) {
        if (mostrarConteos) {
            val textoPendientes = pluralStringResource(
                R.plurals.courses_pending,
                totalPendientes,
                totalPendientes,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.courses_summary,
                    cantidadCursos,
                    cantidadCursos,
                    textoPendientes,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colores.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = pluralStringResource(
                    R.plurals.courses_week,
                    entregasSemanales,
                    entregasSemanales,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TarjetaCurso(
    tarjeta: TarjetaCursoUi,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val pulsada by fuenteInteraccion.collectIsPressedAsState()
    var enfocada by remember { mutableStateOf(false) }

    val contenedor by animateColorAsState(
        targetValue = if (pulsada) colores.surfaceContainerHigh else colores.surfaceContainerLow,
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "cursoCardContainer",
    )

    val completa = tarjeta.totalTareas > 0 &&
        tarjeta.tareasAvanzadas == tarjeta.totalTareas
    val textoCompleta = if (completa && !tarjeta.esAula) {
        pluralStringResource(
            R.plurals.course_progress_complete,
            tarjeta.totalTareas,
            tarjeta.tareasAvanzadas,
            tarjeta.totalTareas,
        )
    } else {
        null
    }
    val estadisticas = when {
        tarjeta.totalTareas == 0 -> stringResource(R.string.course_noTasks)
        tarjeta.esAula -> {
            val tareas = pluralStringResource(
                R.plurals.course_tasks,
                tarjeta.totalTareas,
                tarjeta.totalTareas,
            )
            val entregadas = pluralStringResource(
                R.plurals.course_delivered,
                tarjeta.tareasEntregadas,
                tarjeta.tareasEntregadas,
            )
            "$tareas${stringResource(R.string.task_meta_separator)}$entregadas"
        }
        textoCompleta != null -> textoCompleta
        else -> {
            val tareas = pluralStringResource(
                R.plurals.course_tasks,
                tarjeta.totalTareas,
                tarjeta.totalTareas,
            )
            val hechas = pluralStringResource(
                R.plurals.course_done,
                tarjeta.tareasHechas,
                tarjeta.tareasHechas,
            )
            "$tareas${stringResource(R.string.task_meta_separator)}$hechas"
        }
    }
    val descripcionProgreso = when {
        tarjeta.totalTareas == 0 -> stringResource(R.string.course_noTasks)
        textoCompleta != null -> textoCompleta
        else -> pluralStringResource(
            R.plurals.course_progress,
            tarjeta.totalTareas,
            tarjeta.tareasAvanzadas,
            tarjeta.totalTareas,
        )
    }
    val etiquetaAbrir = stringResource(R.string.a11y_edit_course)
    val descripcion = "${tarjeta.curso.nombre}. $descripcionProgreso."

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .anilloFoco(visible = enfocada, shape = FormaNotally.xl)
            .onFocusChanged { enfocada = it.isFocused }
            .clip(FormaNotally.xl)
            .clickable(
                interactionSource = fuenteInteraccion,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = etiquetaAbrir,
                onClick = alPulsar,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = descripcion
            },
        shape = FormaNotally.xl,
        color = contenedor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tarjeta.curso.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    color = colores.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = estadisticas,
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant,
                )
            }
            if (tarjeta.totalTareas > 0) {
                Spacer(Modifier.width(12.dp))
                val fraccion = (tarjeta.tareasAvanzadas.toFloat() / tarjeta.totalTareas).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        progress = { fraccion },
                        modifier = Modifier.fillMaxSize(),
                        color = colores.primary,
                        trackColor = colores.surfaceContainerHighest,
                        strokeWidth = 4.dp,
                    )
                    Text(
                        text = stringResource(
                            R.string.course_progress_ratio,
                            tarjeta.tareasAvanzadas,
                            tarjeta.totalTareas,
                        ),
                        style = TipografiaNotally.numerico,
                        color = colores.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun BarraProgreso(
    fraccion: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(2.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant),
    ) {
        if (fraccion > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraccion.coerceIn(0f, 1f))
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
