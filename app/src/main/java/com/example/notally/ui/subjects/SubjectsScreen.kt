package com.example.notally.ui.subjects

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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notally.R
import com.example.notally.data.model.Materia
import com.example.notally.ui.components.AvisoError
import com.example.notally.ui.components.BloqueError
import com.example.notally.ui.components.EmptyState
import com.example.notally.ui.components.FilasCargando
import com.example.notally.ui.components.anilloFoco
import com.example.notally.ui.theme.EspaciadoNotally
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.theme.TipografiaNotally
import com.example.notally.ui.theme.colorMateria
import com.example.notally.ui.theme.margenHorizontalPantalla
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pantalla Materias (§7.4, §7.8, §7.13).
 *
 * La tarjeta es la única acción: abre la hoja "Editar materia". No hay ruta de
 * detalle de materia en V1 (§0.5). Los conteos salen de Room; "entregas esta
 * semana" se recalcula con el ajuste de inicio de semana.
 */
@Composable
fun SubjectsScreen(
    modifier: Modifier = Modifier,
    viewModel: SubjectsViewModel = viewModel(
        factory = SubjectsViewModel.factory(LocalContext.current),
    ),
) {
    val estado by viewModel.estadoUi.collectAsStateWithLifecycle()
    val errorFormulario by viewModel.errorFormulario.collectAsStateWithLifecycle()
    val estadoSnackbar = remember { SnackbarHostState() }
    val margen = margenHorizontalPantalla

    var objetivoHoja by remember { mutableStateOf<ObjetivoSubjectSheet?>(null) }

    val errorEliminar = stringResource(R.string.subject_delete_error)
    val etiquetaReintentar = stringResource(R.string.action_retry)
    val errorCargar = stringResource(R.string.error_loadSubjects)
    val errorRefrescar = stringResource(R.string.error_refresh)

    LaunchedEffect(viewModel, errorEliminar, etiquetaReintentar) {
        viewModel.eventos.collectLatest { evento ->
            when (evento) {
                is EventoMaterias.Fallo -> {
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
        modifier = modifier.fillMaxSize(),
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
                    bottom = 24.dp,
                ),
            ) {
                item(key = "header") {
                    EncabezadoMaterias(
                        cantidadMaterias = estado.materias.size,
                        totalPendientes = estado.totalPendientes,
                        entregasSemanales = estado.entregasSemanales,
                        mostrarConteos = estado.tieneDatosEnCache &&
                            estado.materias.isNotEmpty(),
                        mostrarAgregar = estado.materias.isNotEmpty(),
                        alAgregar = {
                            viewModel.limpiarErrorFormulario()
                            objetivoHoja = ObjetivoSubjectSheet.Nueva
                        },
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

                    estado.materias.isEmpty() -> {
                        item(key = "empty") {
                            EmptyState(
                                titulo = stringResource(R.string.subjects_empty_title),
                                cuerpo = stringResource(R.string.subjects_empty_body),
                                etiquetaAccion = stringResource(R.string.subjects_empty_cta),
                                alAccionar = {
                                    viewModel.limpiarErrorFormulario()
                                    objetivoHoja = ObjetivoSubjectSheet.Nueva
                                },
                                modifier = Modifier.padding(top = 48.dp),
                            )
                        }
                    }

                    else -> {
                        items(
                            items = estado.materias,
                            key = { it.materia.id },
                        ) { tarjeta ->
                            TarjetaMateria(
                                tarjeta = tarjeta,
                                alPulsar = {
                                    viewModel.limpiarErrorFormulario()
                                    objetivoHoja = ObjetivoSubjectSheet.Editar(tarjeta)
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
        SubjectSheet(
            objetivo = objetivo,
            colorPorDefecto = viewModel.colorPorDefecto(estado.materias.size),
            errorFormulario = errorFormulario,
            alDescartar = {
                viewModel.limpiarErrorFormulario()
                objetivoHoja = null
            },
            alGuardar = { existente: Materia?, nombre: String, color: Long ->
                viewModel.guardarMateria(existente, nombre, color)
            },
            alGuardado = {
                viewModel.limpiarErrorFormulario()
                objetivoHoja = null
            },
            alEliminar = { materia ->
                viewModel.eliminarMateria(materia) {
                    viewModel.limpiarErrorFormulario()
                    objetivoHoja = null
                }
            },
            alLimpiarError = viewModel::limpiarErrorFormulario,
        )
    }
}

@Composable
private fun EncabezadoMaterias(
    cantidadMaterias: Int,
    totalPendientes: Int,
    entregasSemanales: Int,
    mostrarConteos: Boolean,
    mostrarAgregar: Boolean,
    alAgregar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.nav_materias),
                style = MaterialTheme.typography.displaySmall,
                color = colores.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            if (mostrarConteos) {
                val textoPendientes = pluralStringResource(
                    R.plurals.subjects_pending,
                    totalPendientes,
                    totalPendientes,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.subjects_summary,
                        cantidadMaterias,
                        cantidadMaterias,
                        textoPendientes,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.subjects_week,
                        entregasSemanales,
                        entregasSemanales,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant,
                )
            }
        }
        if (mostrarAgregar) {
            BotonAgregarMateria(alPulsar = alAgregar)
        }
    }
}

/** "+" de Materias: 52 dp circular, `primary`/`onPrimary`, sin sombra (§7.10). */
@Composable
private fun BotonAgregarMateria(alPulsar: () -> Unit) {
    val colores = MaterialTheme.colorScheme
    var enfocado by remember { mutableStateOf(false) }
    val descripcion = stringResource(R.string.subjects_new)
    Box(
        modifier = Modifier
            .size(52.dp)
            .onFocusChanged { enfocado = it.isFocused }
            .anilloFoco(visible = enfocado, shape = CircleShape)
            .clip(CircleShape)
            .background(colores.primary)
            .clickable(role = Role.Button, onClick = alPulsar)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            tint = colores.onPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun TarjetaMateria(
    tarjeta: TarjetaMateriaUi,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    val acento = colorMateria(tarjeta.materia.color)
    val fuenteInteraccion = remember { MutableInteractionSource() }
    val pulsada by fuenteInteraccion.collectIsPressedAsState()
    var enfocada by remember { mutableStateOf(false) }

    val contenedor by animateColorAsState(
        targetValue = if (pulsada) colores.surfaceContainerLow else colores.surface,
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "subjectCardContainer",
    )
    val colorBorde by animateColorAsState(
        targetValue = if (pulsada) colores.outline else colores.outlineVariant,
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "subjectCardBorder",
    )

    val completa = tarjeta.totalTareas > 0 && tarjeta.tareasHechas == tarjeta.totalTareas
    val textoCompleta = if (completa) {
        pluralStringResource(
            R.plurals.subject_progress_complete,
            tarjeta.totalTareas,
            tarjeta.tareasHechas,
            tarjeta.totalTareas,
        )
    } else {
        null
    }
    val estadisticas = when {
        tarjeta.totalTareas == 0 -> stringResource(R.string.subject_noTasks)
        textoCompleta != null -> textoCompleta
        else -> {
            val tareas = pluralStringResource(
                R.plurals.subject_tasks,
                tarjeta.totalTareas,
                tarjeta.totalTareas,
            )
            val hechas = pluralStringResource(
                R.plurals.subject_done,
                tarjeta.tareasHechas,
                tarjeta.tareasHechas,
            )
            "$tareas${stringResource(R.string.task_meta_separator)}$hechas"
        }
    }
    val descripcionProgreso = when {
        tarjeta.totalTareas == 0 -> stringResource(R.string.subject_noTasks)
        textoCompleta != null -> textoCompleta
        else -> pluralStringResource(
            R.plurals.subject_progress,
            tarjeta.totalTareas,
            tarjeta.tareasHechas,
            tarjeta.totalTareas,
        )
    }
    val etiquetaAbrir = stringResource(R.string.a11y_editSubject)
    val descripcion = "${tarjeta.materia.nombre}. $descripcionProgreso."

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .anilloFoco(visible = enfocada, shape = FormaNotally.l)
            .onFocusChanged { enfocada = it.isFocused }
            .clip(FormaNotally.l)
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
        shape = FormaNotally.l,
        color = contenedor,
        border = BorderStroke(1.dp, colorBorde),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(acento),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tarjeta.materia.nombre,
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
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BarraProgreso(
                        fraccion = if (tarjeta.totalTareas == 0) {
                            0f
                        } else {
                            tarjeta.tareasHechas.toFloat() / tarjeta.totalTareas
                        },
                        color = acento,
                        modifier = Modifier.weight(1f),
                    )
                    if (tarjeta.totalTareas > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                R.string.subject_progress_ratio,
                                tarjeta.tareasHechas,
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
}

@Composable
private fun BarraProgreso(
    fraccion: Float,
    color: Color,
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
                    .background(color),
            )
        }
    }
}
