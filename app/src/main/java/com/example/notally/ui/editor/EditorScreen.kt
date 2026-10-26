package com.example.notally.ui.editor

import android.text.format.DateFormat as AndroidDateFormat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notally.R
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Curso
import com.example.notally.data.model.Prioridad
import com.example.notally.ui.components.BloqueError
import com.example.notally.ui.components.FilasCargando
import com.example.notally.ui.components.anilloFoco
import com.example.notally.ui.theme.EspaciadoNotally
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.theme.TipografiaNotally
import com.example.notally.ui.theme.margenHorizontalPantalla
import com.example.notally.ui.util.DateFormat
import com.example.notally.ui.util.textoEtiquetaFecha
import java.time.ZoneId
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
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
                BarraSuperiorEditor(
                    alVolver = {
                        gestorFoco.clearFocus()
                        viewModel.volcar()
                        alVolver()
                    },
                    alEliminar = { mostrarDialogoEliminar = true },
                    mostrarMenu = !estado.cargando && !estado.errorCarga,
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
                            ContenidoEditor(
                                estado = estado,
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
                BarraHerramientasEditor(
                    alPulsarSubtarea = {
                        focoSubtarea.requestFocus()
                    },
                    alPulsarEntrega = { hoja = HojaEditor.ENTREGA },
                    alPulsarPrioridad = { hoja = HojaEditor.PRIORIDAD },
                )
            }
        }
    }

    AnfitrionEditorSheets(
        hoja = hoja,
        borrador = estado,
        cursos = cursos,
        colorCursoPorDefecto = viewModel.colorCursoPorDefecto(),
        errorNuevoCurso = errorNuevoCurso,
        alDescartar = { hoja = null },
        alSeleccionarCurso = viewModel::cambiarCurso,
        alCrearCurso = { nombre, color ->
            viewModel.crearCursoYSeleccionar(nombre, color) { hoja = null }
        },
        alLimpiarErrorCurso = viewModel::limpiarErrorNuevoCurso,
        alCambiarEntrega = viewModel::cambiarEntrega,
        alCambiarPrioridad = viewModel::cambiarPrioridad,
        alCambiarEstado = viewModel::cambiarEstado,
    )

    if (mostrarDialogoEliminar) {
        DialogoEliminarTarea(
            titulo = EditorRules.tituloVisible(estado.titulo, sinTitulo),
            cantidadSubtareas = estado.subtareas.size,
            alDescartar = { mostrarDialogoEliminar = false },
            alConfirmar = {
                mostrarDialogoEliminar = false
                viewModel.eliminarTarea {
                    viewModel.volcar()
                    alVolver()
                }
            },
        )
    }
}

@Composable
private fun BarraSuperiorEditor(
    alVolver: () -> Unit,
    alEliminar: () -> Unit,
    mostrarMenu: Boolean,
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
private fun ContenidoEditor(
    estado: BorradorTarea,
    cursos: List<Curso>,
    nuevaSubtarea: String,
    alCambiarNuevaSubtarea: (String) -> Unit,
    subtareaEnfocada: Boolean,
    focoSubtarea: FocusRequester,
    alCambiarFocoSubtarea: (Boolean) -> Unit,
    alAgregarSubtarea: () -> Unit,
    alAlternarSubtarea: (SubtareaEditor) -> Unit,
    alEliminarSubtarea: (SubtareaEditor) -> Unit,
    alPulsarAgregar: () -> Unit,
    alCambiarTitulo: (String) -> Unit,
    alCambiarNotas: (String) -> Unit,
    alAbrirPropiedad: (HojaEditor) -> Unit,
    margen: androidx.compose.ui.unit.Dp,
    imeVisible: Boolean,
) {
    val colores = MaterialTheme.colorScheme
    val contexto = LocalContext.current
    val zona = remember { ZoneId.systemDefault() }
    val es24Horas = remember(contexto) { AndroidDateFormat.is24HourFormat(contexto) }
    val gestorFoco = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current
    val etiquetaTitulo = stringResource(R.string.editor_title_placeholder)
    val etiquetaNotas = stringResource(R.string.editor_notes_placeholder)

    val nombreCurso = cursos.firstOrNull { it.id == estado.cursoId }?.nombre
        ?: stringResource(R.string.task_noSubject)
    val textoEntrega = estado.entregaEn?.let { entregaEn ->
        val fecha = textoEtiquetaFecha(entregaEn, zona = zona)
        if (DateFormat.tieneHora(entregaEn, zona)) {
            "$fecha, ${DateFormat.formatearHora(entregaEn, es24Horas, zona)}"
        } else {
            fecha
        }
    } ?: stringResource(R.string.task_noDate)
    val textoPrioridad = stringResource(
        when (estado.prioridad) {
            Prioridad.ALTA -> R.string.priority_high
            Prioridad.MEDIA -> R.string.priority_medium
            Prioridad.BAJA -> R.string.priority_low
        },
    )
    val textoEstado = stringResource(
        when (estado.estado) {
            Estado.PENDIENTE -> R.string.state_pending
            Estado.EN_CURSO -> R.string.state_doing
            Estado.COMPLETADA -> R.string.state_done
        },
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = EspaciadoNotally.anchoMaximoContenido)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = margen)
                .padding(bottom = if (imeVisible) 112.dp else 48.dp),
        ) {
            BasicTextField(
                value = estado.titulo,
                onValueChange = alCambiarTitulo,
                textStyle = TipografiaNotally.tituloEditor.copy(color = colores.onSurface),
                cursorBrush = SolidColor(colores.tertiary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = etiquetaTitulo },
                decorationBox = { campoInterno ->
                    Box {
                        if (estado.titulo.isEmpty()) {
                            Text(
                                text = stringResource(R.string.editor_title_placeholder),
                                style = TipografiaNotally.tituloEditor,
                                color = colores.onSurfaceVariant,
                            )
                        }
                        campoInterno()
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            FilaPropiedadEditor(
                etiqueta = stringResource(R.string.editor_property_subject),
                valor = nombreCurso,
                alPulsar = { alAbrirPropiedad(HojaEditor.CURSO) },
            )
            FilaPropiedadEditor(
                etiqueta = stringResource(R.string.editor_property_due),
                valor = textoEntrega,
                alPulsar = { alAbrirPropiedad(HojaEditor.ENTREGA) },
            )
            FilaPropiedadEditor(
                etiqueta = stringResource(R.string.editor_property_priority),
                valor = textoPrioridad,
                mostrarPuntoError = estado.prioridad == Prioridad.ALTA,
                alPulsar = { alAbrirPropiedad(HojaEditor.PRIORIDAD) },
            )
            FilaPropiedadEditor(
                etiqueta = stringResource(R.string.editor_property_state),
                valor = textoEstado,
                alPulsar = { alAbrirPropiedad(HojaEditor.ESTADO) },
            )

            Spacer(Modifier.height(24.dp))
            BasicTextField(
                value = estado.notas,
                onValueChange = alCambiarNotas,
                textStyle = TipografiaNotally.cuerpoEditor.copy(color = colores.onSurface),
                cursorBrush = SolidColor(colores.tertiary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .semantics { contentDescription = etiquetaNotas },
                decorationBox = { campoInterno ->
                    Box(modifier = Modifier.heightIn(min = 120.dp)) {
                        if (estado.notas.isEmpty()) {
                            Text(
                                text = stringResource(R.string.editor_notes_placeholder),
                                style = TipografiaNotally.cuerpoEditor,
                                color = colores.onSurfaceVariant,
                            )
                        }
                        campoInterno()
                    }
                },
            )

            Spacer(Modifier.height(24.dp))
            SeccionSubtareas(
                subtareas = estado.subtareas,
                nuevaSubtarea = nuevaSubtarea,
                alCambiarNuevaSubtarea = alCambiarNuevaSubtarea,
                subtareaEnfocada = subtareaEnfocada,
                focoSubtarea = focoSubtarea,
                alCambiarFocoSubtarea = alCambiarFocoSubtarea,
                alPulsarAgregar = alPulsarAgregar,
                alAlternar = alAlternarSubtarea,
                alEliminar = alEliminarSubtarea,
                alTerminar = {
                    if (nuevaSubtarea.isBlank()) {
                        gestorFoco.clearFocus()
                        teclado?.hide()
                    } else {
                        alAgregarSubtarea()
                        focoSubtarea.requestFocus()
                    }
                },
            )
        }
    }
}

@Composable
private fun FilaPropiedadEditor(
    etiqueta: String,
    valor: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarPuntoError: Boolean = false,
) {
    val colores = MaterialTheme.colorScheme
    val compacto = LocalConfiguration.current.fontScale >= 1.3f
    val interaccion = remember { MutableInteractionSource() }
    val pulsada by interaccion.collectIsPressedAsState()
    var enfocada by remember { mutableStateOf(false) }
    val fondo by animateColorAsState(
        targetValue = if (pulsada) {
            colores.surfaceContainerLow
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        },
        animationSpec = tween(MovimientoNotally.colorMs),
        label = "editorPropertyBackground",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .anilloFoco(visible = enfocada, shape = FormaNotally.s)
            .clip(FormaNotally.s)
            .background(fondo)
            .onFocusChanged { enfocada = it.isFocused }
            .clickable(
                interactionSource = interaccion,
                indication = null,
                role = Role.Button,
                onClick = alPulsar,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (compacto) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = etiqueta,
                    style = MaterialTheme.typography.titleSmall,
                    color = colores.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = valor,
                        style = MaterialTheme.typography.titleSmall,
                        color = colores.onSurface,
                    )
                    if (mostrarPuntoError) {
                        Spacer(Modifier.width(6.dp))
                        PuntoError()
                    }
                }
            }
        } else {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.titleSmall,
                color = colores.onSurfaceVariant,
                modifier = Modifier.width(96.dp),
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = valor,
                    style = MaterialTheme.typography.titleSmall,
                    color = colores.onSurface,
                )
                if (mostrarPuntoError) {
                    Spacer(Modifier.width(6.dp))
                    PuntoError()
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = colores.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun PuntoError() {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error),
    )
}

@Composable
private fun SeccionSubtareas(
    subtareas: List<SubtareaEditor>,
    nuevaSubtarea: String,
    alCambiarNuevaSubtarea: (String) -> Unit,
    subtareaEnfocada: Boolean,
    focoSubtarea: FocusRequester,
    alCambiarFocoSubtarea: (Boolean) -> Unit,
    alPulsarAgregar: () -> Unit,
    alAlternar: (SubtareaEditor) -> Unit,
    alEliminar: (SubtareaEditor) -> Unit,
    alTerminar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.editor_subtasks),
            style = MaterialTheme.typography.titleSmall,
            color = colores.onSurface,
        )
        if (subtareas.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    R.string.editor_subtasks_progress,
                    subtareas.count { it.hecha },
                    subtareas.size,
                ),
                style = TipografiaNotally.numerico,
                color = colores.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        val etiquetaAgregar = stringResource(R.string.editor_subtasks_add)
        TextButton(
            onClick = alPulsarAgregar,
            modifier = Modifier.semantics { contentDescription = etiquetaAgregar },
        ) {
            Text(stringResource(R.string.action_add))
        }
    }

    subtareas.forEach { subtarea ->
        FilaSubtarea(
            subtarea = subtarea,
            alAlternar = { alAlternar(subtarea) },
            alEliminar = { alEliminar(subtarea) },
        )
    }

    BasicTextField(
        value = nuevaSubtarea,
        onValueChange = alCambiarNuevaSubtarea,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colores.onSurface),
        cursorBrush = SolidColor(colores.tertiary),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { alTerminar() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focoSubtarea)
            .onFocusChanged { alCambiarFocoSubtarea(it.isFocused) }
            .drawBehind {
                if (subtareaEnfocada) {
                    val trazo = 1.dp.toPx()
                    drawLine(
                        color = colores.outlineVariant,
                        start = Offset(0f, size.height - trazo / 2f),
                        end = Offset(size.width, size.height - trazo / 2f),
                        strokeWidth = trazo,
                    )
                }
            },
        decorationBox = { campoInterno ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (nuevaSubtarea.isEmpty()) {
                    Text(
                        text = stringResource(R.string.editor_subtasks_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colores.onSurfaceVariant,
                    )
                }
                campoInterno()
            }
        },
    )
}

@Composable
private fun FilaSubtarea(
    subtarea: SubtareaEditor,
    alAlternar: () -> Unit,
    alEliminar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val etiqueta = stringResource(
        if (subtarea.hecha) R.string.a11y_uncompleteTask else R.string.a11y_completeTask,
        subtarea.titulo,
    )
    val etiquetaEstado = stringResource(
        if (subtarea.hecha) R.string.state_done else R.string.state_pending,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    role = Role.Checkbox,
                    onClickLabel = etiqueta,
                    onClick = alAlternar,
                )
                .semantics {
                    contentDescription = etiqueta
                    stateDescription = etiquetaEstado
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (subtarea.hecha) colores.primary
                        else androidx.compose.ui.graphics.Color.Transparent,
                    )
                    .border(
                        width = 2.dp,
                        color = if (subtarea.hecha) colores.primary else colores.outline,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (subtarea.hecha) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = colores.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = subtarea.titulo,
            style = MaterialTheme.typography.bodyMedium,
            color = if (subtarea.hecha) colores.onSurfaceVariant else colores.onSurface,
            textDecoration = if (subtarea.hecha) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = alEliminar,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.a11y_deleteSubtask),
                tint = colores.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun BarraHerramientasEditor(
    alPulsarSubtarea: () -> Unit,
    alPulsarEntrega: () -> Unit,
    alPulsarPrioridad: () -> Unit,
) {
    val compacto = LocalConfiguration.current.fontScale >= 1.5f
    Surface(
        shape = FormaNotally.l,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.height(56.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccionBarra(
                recursoIcono = R.drawable.ic_checklist,
                etiqueta = stringResource(R.string.editor_toolbar_subtask),
                compacto = compacto,
                alPulsar = alPulsarSubtarea,
            )
            AccionBarra(
                recursoIcono = R.drawable.ic_calendar,
                etiqueta = stringResource(R.string.editor_toolbar_date),
                compacto = compacto,
                alPulsar = alPulsarEntrega,
            )
            AccionBarra(
                recursoIcono = R.drawable.ic_flag,
                etiqueta = stringResource(R.string.editor_property_priority),
                compacto = compacto,
                alPulsar = alPulsarPrioridad,
            )
        }
    }
}

@Composable
private fun AccionBarra(
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

@Composable
private fun DialogoEliminarTarea(
    titulo: String,
    cantidadSubtareas: Int,
    alDescartar: () -> Unit,
    alConfirmar: () -> Unit,
) {
    val colores = MaterialTheme.colorScheme
    val focoCancelar = remember { FocusRequester() }
    LaunchedEffect(Unit) { focoCancelar.requestFocus() }
    AlertDialog(
        onDismissRequest = alDescartar,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        shape = FormaNotally.xl,
        containerColor = colores.surfaceContainerLowest,
        title = {
            Text(
                text = stringResource(R.string.confirm_deleteTask_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colores.onSurface,
            )
        },
        text = {
            Text(
                text = if (cantidadSubtareas == 0) {
                    stringResource(R.string.confirm_deleteTask_bodyNoSub, titulo)
                } else {
                    pluralStringResource(
                        R.plurals.confirm_deleteTask_body,
                        cantidadSubtareas,
                        titulo,
                        cantidadSubtareas,
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colores.onSurfaceVariant,
            )
        },
        dismissButton = {
            TextButton(
                onClick = alDescartar,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .focusRequester(focoCancelar),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = alConfirmar,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = colores.error),
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
    )
}
