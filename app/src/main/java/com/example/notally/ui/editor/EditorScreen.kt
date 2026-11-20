package com.example.notally.ui.editor

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notally.R
import com.example.notally.ui.components.BloqueError
import com.example.notally.ui.components.FilasCargando
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.theme.margenHorizontalPantalla
import com.example.notally.ui.theme.MovimientoNotally
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Editor / detalle de tarea (§7.5, §7.6).
 *
 * Sin botón "Guardar": el título y las notas se guardan con debounce y hay
 * flush al volver y al ir a segundo plano. La pantalla sí intercepta el gesto
 * de back con `PredictiveBackHandler`: consume su progreso y solo guarda y
 * navega cuando el gesto se confirma; un gesto cancelado no toca Room ni sale.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    tareaId: Long?,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current
    val viewModel: EditorViewModel = viewModel(
        key = if (tareaId == null) "editor-new" else "editor-$tareaId",
        factory = EditorViewModel.factory(contexto, tareaId),
    )
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val cursos by viewModel.cursos.collectAsStateWithLifecycle()
    val errorNuevoCurso by viewModel.errorNuevoCurso.collectAsStateWithLifecycle()

    val estadoSnackbar = remember { SnackbarHostState() }
    val margen = margenHorizontalPantalla
    val gestorFoco = LocalFocusManager.current

    var hoja by remember { mutableStateOf<HojaEditor?>(null) }
    var editandoNotas by remember { mutableStateOf(false) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var nuevaSubtarea by rememberSaveable { mutableStateOf("") }
    var subtareaEnfocada by remember { mutableStateOf(false) }
    val focoSubtarea = remember { FocusRequester() }

    val errorGuardado = stringResource(R.string.editor_saving_error)
    val etiquetaReintentar = stringResource(R.string.action_retry)
    val subtareaEliminada = stringResource(R.string.subtask_deleted)
    val etiquetaDeshacer = stringResource(R.string.action_undo)
    val sinTitulo = stringResource(R.string.editor_untitled)

    // El progreso del gesto se consume hasta el final (la animación la dibuja
    // el sistema). `collect` solo retorna normalmente cuando el gesto se
    // confirma; si se cancela, el job se cancela y `completado` queda en false.
    // Solo entonces se guarda, se espera la escritura y se navega.
    PredictiveBackHandler { progreso ->
        var completado = false
        try {
            progreso.collect { }
            completado = true
        } finally {
            if (completado) {
                gestorFoco.clearFocus()
                // El ViewModelStore se destruye al volver: drena la escritura
                // pendiente (con timeout) antes de navegar, sin bloquear la UI.
                withContext(NonCancellable) {
                    viewModel.volcarYEsperar()
                    alVolver()
                }
            }
        }
    }

    // Flush al ir a segundo plano y al salir de la pantalla.
    val propietarioCiclo = LocalLifecycleOwner.current
    DisposableEffect(propietarioCiclo, viewModel) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_STOP) viewModel.volcar()
        }
        propietarioCiclo.lifecycle.addObserver(observador)
        onDispose {
            propietarioCiclo.lifecycle.removeObserver(observador)
            viewModel.volcar()
        }
    }

    LaunchedEffect(viewModel, subtareaEliminada, etiquetaDeshacer) {
        viewModel.eventos.collectLatest { evento ->
            when (evento) {
                is EventoEditor.SubtareaEliminada -> {
                    val resultado = withTimeoutOrNull(MovimientoNotally.snackbarMs) {
                        estadoSnackbar.showSnackbar(
                            message = subtareaEliminada,
                            actionLabel = etiquetaDeshacer,
                            duration = SnackbarDuration.Indefinite,
                        )
                    }
                    if (resultado == SnackbarResult.ActionPerformed) {
                        viewModel.deshacerEliminarSubtarea(evento)
                    }
                }
            }
        }
    }

    LaunchedEffect(estado.errorGuardado, errorGuardado, etiquetaReintentar) {
        if (!estado.errorGuardado) return@LaunchedEffect
        val resultado = withTimeoutOrNull(MovimientoNotally.snackbarMs) {
            estadoSnackbar.showSnackbar(
                message = errorGuardado,
                actionLabel = etiquetaReintentar,
                duration = SnackbarDuration.Indefinite,
            )
        }
        if (resultado == SnackbarResult.ActionPerformed) viewModel.reintentarGuardado()
    }

    // §7.13: antes de 150 ms no se pinta nada (el fondo es `surface`), así no
    // parpadea el bloque de carga cuando Room responde al instante.
    val mostrarCarga by produceState(initialValue = false, key1 = estado.cargando) {
        value = if (estado.cargando) {
            delay(150)
            true
        } else {
            false
        }
    }

    val imeVisible = WindowInsets.isImeVisible

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = {
            SnackbarHost(
                hostState = estadoSnackbar,
                modifier = Modifier.imePadding(),
            )
        },
    ) { rellenoInterior ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(rellenoInterior),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                EditorTopBar(
                    alVolver = {
                        gestorFoco.clearFocus()
                        viewModel.volcar()
                        alVolver()
                    },
                    alEliminar = { mostrarDialogoEliminar = true },
                    mostrarMenu = !estado.cargando && !estado.errorCarga,
                    editandoNotas = editandoNotas,
                    alAlternarEditarNotas = { editandoNotas = !editandoNotas },
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    when {
                        estado.cargando && !mostrarCarga -> Unit

                        mostrarCarga -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = margen, vertical = 16.dp),
                            ) {
                                FilasCargando(filas = 4)
                            }
                        }

                        estado.errorCarga -> {
                            BloqueError(
                                titulo = stringResource(R.string.editor_load_error),
                                etiquetaReintentar = etiquetaReintentar,
                                alReintentar = viewModel::reintentarCarga,
                                modifier = Modifier.padding(horizontal = margen, vertical = 48.dp),
                            )
                        }

                        else -> {
                            EditorContent(
                                estado = estado,
                                editandoNotas = editandoNotas,
                                cursos = cursos,
                                nuevaSubtarea = nuevaSubtarea,
                                alCambiarNuevaSubtarea = { nuevaSubtarea = it },
                                subtareaEnfocada = subtareaEnfocada,
                                focoSubtarea = focoSubtarea,
                                alCambiarFocoSubtarea = { subtareaEnfocada = it },
                                alAgregarSubtarea = {
                                    val pendiente = nuevaSubtarea
                                    // Solo se limpia cuando el alta se confirma;
                                    // si falla, el texto permanece para reintentar.
                                    viewModel.agregarSubtarea(pendiente) {
                                        if (nuevaSubtarea == pendiente) nuevaSubtarea = ""
                                    }
                                },
                                alAlternarSubtarea = viewModel::alternarSubtarea,
                                alEliminarSubtarea = viewModel::eliminarSubtarea,
                                alPulsarAgregar = { focoSubtarea.requestFocus() },
                                alCambiarTitulo = viewModel::cambiarTitulo,
                                alCambiarNotas = viewModel::cambiarNotas,
                                alAbrirPropiedad = { hoja = it },
                                margen = margen,
                                imeVisible = imeVisible,
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = imeVisible && !estado.cargando && !estado.errorCarga,
                enter = fadeIn(tween(MovimientoNotally.fabMs)),
                exit = fadeOut(tween(MovimientoNotally.fabMs)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .padding(12.dp),
            ) {
                EditorToolbar(
                    alPulsarSubtarea = {
                        focoSubtarea.requestFocus()
                    },
                    alPulsarEntrega = { hoja = HojaEditor.ENTREGA },
                    alPulsarPrioridad = { hoja = HojaEditor.PRIORIDAD },
                )
            }
        }
    }

    EditorSheetsHost(
        hoja = hoja,
        borrador = estado,
        cursos = cursos,
        errorNuevoCurso = errorNuevoCurso,
        alDescartar = { hoja = null },
        alSeleccionarCurso = viewModel::cambiarCurso,
        alCrearCurso = { nombre ->
            viewModel.crearCursoYSeleccionar(nombre) { hoja = null }
        },
        alLimpiarErrorCurso = viewModel::limpiarErrorNuevoCurso,
        alCambiarEntrega = viewModel::cambiarEntrega,
        alCambiarPrioridad = viewModel::cambiarPrioridad,
        alCambiarEstado = viewModel::cambiarEstado,
    )

    if (mostrarDialogoEliminar) {
        DeleteTaskDialog(
            tituloVisible = EditorRules.tituloVisible(estado.titulo, sinTitulo),
            cantidadSubtareas = estado.subtareas.size,
            onConfirmar = {
                mostrarDialogoEliminar = false
                viewModel.eliminarTarea {
                    viewModel.volcar()
                    alVolver()
                }
            },
            onDismiss = { mostrarDialogoEliminar = false },
        )
    }
}

@Composable
private fun EditorTopBar(
    alVolver: () -> Unit,
    alEliminar: () -> Unit,
    mostrarMenu: Boolean,
    editandoNotas: Boolean,
    alAlternarEditarNotas: () -> Unit,
) {
    var menuAbierto by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = alVolver,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.a11y_back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        if (mostrarMenu) {
            Box {
                IconButton(
                    onClick = { menuAbierto = true },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_horiz),
                        contentDescription = stringResource(R.string.a11y_more),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuAbierto,
                    onDismissRequest = { menuAbierto = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(if (editandoNotas) "Terminar edición" else "Editar descripción")
                        },
                        onClick = {
                            menuAbierto = false
                            alAlternarEditarNotas()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.editor_delete)) },
                        onClick = {
                            menuAbierto = false
                            alEliminar()
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
internal fun EditorToolbar(
    alPulsarSubtarea: () -> Unit,
    alPulsarEntrega: () -> Unit,
    alPulsarPrioridad: () -> Unit,
) {
    val compacto = LocalConfiguration.current.fontScale >= 1.5f
    Surface(
        shape = FormaNotally.l,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.height(56.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarAction(
                recursoIcono = R.drawable.ic_checklist,
                etiqueta = stringResource(R.string.editor_toolbar_subtask),
                compacto = compacto,
                alPulsar = alPulsarSubtarea,
            )
            ToolbarAction(
                recursoIcono = R.drawable.ic_calendar,
                etiqueta = stringResource(R.string.editor_toolbar_date),
                compacto = compacto,
                alPulsar = alPulsarEntrega,
            )
            ToolbarAction(
                recursoIcono = R.drawable.ic_flag,
                etiqueta = stringResource(R.string.editor_property_priority),
                compacto = compacto,
                alPulsar = alPulsarPrioridad,
            )
        }
    }
}

@Composable
private fun ToolbarAction(
    recursoIcono: Int,
    etiqueta: String,
    compacto: Boolean,
    alPulsar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(FormaNotally.s)
            .clickable(role = Role.Button, onClick = alPulsar)
            .then(
                if (compacto) {
                    Modifier.semantics { contentDescription = etiqueta }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(recursoIcono),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        if (!compacto) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
