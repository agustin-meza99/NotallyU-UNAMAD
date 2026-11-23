package com.example.notally.ui.cursos

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.example.notally.ui.components.NotallySearchBar
import com.example.notally.ui.components.anilloFoco
import com.example.notally.ui.theme.EspaciadoNotally
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.theme.TipografiaNotally
import com.example.notally.ui.theme.margenHorizontalPantalla
import com.example.notally.ui.theme.unamad
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pantalla Cursos renovada con identidad UNAMAD, panel de estadísticas
 * académicas, búsqueda instantánea y tarjetas interactivas con progreso animado.
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
    var consulta by rememberSaveable { mutableStateOf("") }

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

    val cursosFiltrados = remember(estado.cursos, consulta) {
        val q = consulta.trim().lowercase()
        if (q.isEmpty()) {
            estado.cursos
        } else {
            estado.cursos.filter { item ->
                item.curso.nombre.lowercase().contains(q) ||
                    (item.curso.codigo?.lowercase()?.contains(q) == true) ||
                    (item.curso.periodo?.lowercase()?.contains(q) == true)
            }
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
                    top = 16.dp,
                    bottom = EspaciadoNotally.rellenoInferiorLista,
                ),
            ) {
                item(key = "header") {
                    EncabezadoCursosUnamad(
                        cantidadCursos = estado.cursos.size,
                        totalPendientes = estado.totalPendientes,
                        entregasSemanales = estado.entregasSemanales,
                        mostrarConteos = estado.tieneDatosEnCache,
                        modifier = Modifier.padding(bottom = 14.dp),
                    )
                }

                if (estado.cursos.isNotEmpty()) {
                    item(key = "search_courses") {
                        NotallySearchBar(
                            consulta = consulta,
                            placeholder = stringResource(R.string.courses_search_placeholder),
                            alCambiarConsulta = { consulta = it },
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }
                }

                if (estado.error && estado.tieneDatosEnCache) {
                    item(key = "error_banner") {
                        AvisoError(
                            mensaje = errorRefrescar,
                            etiquetaReintentar = etiquetaReintentar,
                            alReintentar = viewModel::reintentar,
                            modifier = Modifier.padding(bottom = 14.dp),
                        )
                    }
                }

                when {
                    estado.cargando && !estado.tieneDatosEnCache -> {
                        if (mostrarCarga) {
                            item(key = "loading") {
                                FilasCargando(modifier = Modifier.padding(top = 12.dp))
                            }
                        }
                    }

                    estado.error && !estado.tieneDatosEnCache -> {
                        item(key = "error_block") {
                            BloqueError(
                                titulo = errorCargar,
                                etiquetaReintentar = etiquetaReintentar,
                                alReintentar = viewModel::reintentar,
                                modifier = Modifier.padding(top = 36.dp),
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
                                modifier = Modifier.padding(top = 36.dp),
                            )
                        }
                    }

                    cursosFiltrados.isEmpty() -> {
                        item(key = "search_empty") {
                            EmptyState(
                                titulo = stringResource(R.string.today_search_empty_title),
                                cuerpo = stringResource(R.string.today_search_empty_body, consulta.trim()),
                                etiquetaAccion = stringResource(R.string.today_search_empty_cta),
                                alAccionar = { consulta = "" },
                                modifier = Modifier.padding(top = 24.dp),
                            )
                        }
                    }

                    else -> {
                        items(
                            items = cursosFiltrados,
                            key = { it.curso.id },
                        ) { tarjeta ->
                            TarjetaCurso(
                                tarjeta = tarjeta,
                                alPulsar = {
                                    viewModel.limpiarErrorFormulario()
                                    objetivoHoja = ObjetivoCursoSheet.Editar(tarjeta)
                                },
                                modifier = Modifier
                                    .animateItem(
                                        placementSpec = tween(MovimientoNotally.movimientoElementoMs),
                                    )
                                    .padding(bottom = 12.dp),
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
private fun EncabezadoCursosUnamad(
    cantidadCursos: Int,
    totalPendientes: Int,
    entregasSemanales: Int,
    mostrarConteos: Boolean,
    modifier: Modifier = Modifier,
) {
    val unamadColors = MaterialTheme.unamad
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(FormaNotally.xl)
            .background(
                Brush.linearGradient(
                    colors = listOf(unamadColors.heroStart, unamadColors.heroEnd),
                ),
            )
            .border(
                width = 1.dp,
                color = unamadColors.goldAccent.copy(alpha = 0.35f),
                shape = FormaNotally.xl,
            )
            .padding(20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_book),
                    contentDescription = null,
                    tint = unamadColors.goldAccent,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.courses_header_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = unamadColors.heroOnContent,
                )
            }

            if (mostrarConteos) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CursoStatTile(
                        valor = cantidadCursos.toString(),
                        etiqueta = stringResource(R.string.courses_stat_courses),
                        modifier = Modifier.weight(1f),
                    )
                    CursoStatTile(
                        valor = totalPendientes.toString(),
                        etiqueta = stringResource(R.string.courses_stat_pending),
                        modifier = Modifier.weight(1f),
                    )
                    CursoStatTile(
                        valor = entregasSemanales.toString(),
                        etiqueta = stringResource(R.string.courses_stat_week),
                        destacado = entregasSemanales > 0,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CursoStatTile(
    valor: String,
    etiqueta: String,
    modifier: Modifier = Modifier,
    destacado: Boolean = false,
) {
    val unamadColors = MaterialTheme.unamad
    Column(
        modifier = modifier
            .clip(FormaNotally.m)
            .background(
                if (destacado) {
                    unamadColors.goldAccent.copy(alpha = 0.22f)
                } else {
                    unamadColors.heroOnContent.copy(alpha = 0.12f)
                },
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = valor,
            style = MaterialTheme.typography.titleMedium,
            color = if (destacado) unamadColors.heroBadgeContent else unamadColors.heroOnContent,
            maxLines = 1,
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = unamadColors.heroOnContent.copy(alpha = 0.80f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TarjetaCurso(
    tarjeta: TarjetaCursoUi,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val unamadColors = MaterialTheme.unamad
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val pulsada by fuenteInteraccion.collectIsPressedAsState()
    var enfocada by remember { mutableStateOf(false) }

    val contenedor by animateColorAsState(
        targetValue = if (pulsada) colores.surfaceContainerHigh else colores.surfaceContainerLowest,
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "cursoCardContainer",
    )
    val escala by animateFloatAsState(
        targetValue = if (pulsada) 0.985f else 1f,
        animationSpec = MovimientoNotally.espacialRapida(),
        label = "cursoCardScale",
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
    val separador = stringResource(R.string.task_meta_separator)
    val metaAcademica = remember(tarjeta.curso) {
        listOfNotNull(
            tarjeta.curso.codigo?.takeIf { it.isNotBlank() },
            tarjeta.curso.seccion?.takeIf { it.isNotBlank() }?.let { "Sec. $it" },
            tarjeta.curso.periodo?.takeIf { it.isNotBlank() },
        ).joinToString(separador)
    }

    val fraccionObjetivo = if (tarjeta.totalTareas > 0) {
        (tarjeta.tareasAvanzadas.toFloat() / tarjeta.totalTareas).coerceIn(0f, 1f)
    } else {
        0f
    }
    val fraccionAnimada by animateFloatAsState(
        targetValue = fraccionObjetivo,
        animationSpec = MovimientoNotally.espacialNormal(),
        label = "cursoProgress",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(escala)
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
        border = BorderStroke(1.dp, colores.outlineVariant.copy(alpha = 0.50f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 18.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Acento lateral: verde UNAMAD para Aula Virtual, azul río para cursos manuales
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(if (tarjeta.esAula) colores.primary else colores.tertiary),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(FormaNotally.s)
                            .background(
                                if (tarjeta.esAula) {
                                    unamadColors.successContainer
                                } else {
                                    colores.tertiaryContainer
                                },
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = stringResource(
                                if (tarjeta.esAula) {
                                    R.string.courses_badge_aula
                                } else {
                                    R.string.courses_badge_manual
                                },
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (tarjeta.esAula) {
                                unamadColors.onSuccessContainer
                            } else {
                                colores.onTertiaryContainer
                            },
                        )
                    }
                    if (metaAcademica.isNotEmpty()) {
                        Text(
                            text = metaAcademica,
                            style = MaterialTheme.typography.bodySmall,
                            color = colores.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
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
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        progress = { fraccionAnimada },
                        modifier = Modifier.fillMaxSize(),
                        color = if (completa) unamadColors.goldAccent else colores.primary,
                        trackColor = colores.surfaceContainerHighest,
                        strokeWidth = 4.5.dp,
                        strokeCap = StrokeCap.Round,
                    )
                    Text(
                        text = stringResource(
                            R.string.course_progress_ratio,
                            tarjeta.tareasAvanzadas,
                            tarjeta.totalTareas,
                        ),
                        style = TipografiaNotally.numerico,
                        color = colores.onSurface,
                    )
                }
            }
        }
    }
}
