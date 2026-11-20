package com.example.notally.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.notally.R
import com.example.notally.ui.components.FloatingNavBar
import com.example.notally.ui.components.FloatingNavItem
import com.example.notally.ui.components.NotallyFab
import com.example.notally.ui.editor.EditorScreen
import com.example.notally.ui.settings.SettingsScreen
import com.example.notally.ui.cursos.CursosScreen
import com.example.notally.ui.cursos.TareasCursoScreen
import com.example.notally.ui.theme.EspaciadoNotally
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.theme.margenHorizontalPantalla
import com.example.notally.ui.today.TodayScreen

private const val PREFIJO_EDITOR = "editor:"
private const val PREFIJO_CURSO_TAREAS = "cursoTareas:"
private const val ID_EDITOR_NUEVO = "new"

/** Alto de la píldora y del FAB (§6, §7.10). */
private val AltoCluster = 56.dp
private val AltoFab = 52.dp

/** Aire entre el FAB y el borde superior del cluster cuando el destino es Cursos. */
private val SeparacionFabCluster = 16.dp

/** Destinos en los que existe FAB; Ajustes no tiene acción principal. */
private enum class DestinoFab { Hoy, Cursos }

/** Destinos de la V1. El editor no lleva barra inferior. */
sealed interface Destino {
    val clave: String

    data object Hoy : Destino {
        override val clave: String = "today"
    }

    data object Cursos : Destino {
        override val clave: String = "cursos"
    }

    data object Settings : Destino {
        override val clave: String = "settings"
    }

    /** Tareas de un curso; se abre desde la hoja de curso y no lleva barra. */
    data class CursoTareas(val cursoId: Long) : Destino {
        override val clave: String = "$PREFIJO_CURSO_TAREAS$cursoId"
    }

    data class Editor(val tareaId: Long?) : Destino {
        override val clave: String = "$PREFIJO_EDITOR${tareaId ?: ID_EDITOR_NUEVO}"
    }
}

/** Pantallas que sustituyen por completo a las pestañas (sin barra inferior). */
private sealed interface NivelRaiz {
    data object Pestanas : NivelRaiz
    data class CursoTareas(val cursoId: Long) : NivelRaiz
    data class Editor(val tareaId: Long?) : NivelRaiz
}

/**
 * Raíz de navegación (§6).
 *
 * Sin `navigation-compose`: la pila es una lista de [Destino] guardada en
 * un `String` con `rememberSaveable`. El editor hace su propio back predictivo
 * y las pestañas vuelven a Hoy con `BackHandler`. Los tabs se conservan con
 * [rememberSaveableStateHolder], así que volver del editor deja la pestaña de
 * origen con su scroll y su filtro.
 */
@Composable
fun NotallyRoot(modifier: Modifier = Modifier) {
    var pilaCodificada by rememberSaveable {
        mutableStateOf(codificarDestino(Destino.Hoy))
    }
    val pila = remember(pilaCodificada) { decodificarPila(pilaCodificada) }
    val actual = pila.lastOrNull() ?: Destino.Hoy
    val editor = actual as? Destino.Editor
    val cursoTareas = actual as? Destino.CursoTareas
    val pestanaActual = pila.lastOrNull {
        it !is Destino.Editor && it !is Destino.CursoTareas
    } ?: Destino.Hoy
    val nivel = when {
        editor != null -> NivelRaiz.Editor(editor.tareaId)
        cursoTareas != null -> NivelRaiz.CursoTareas(cursoTareas.cursoId)
        else -> NivelRaiz.Pestanas
    }

    BackHandler(enabled = cursoTareas != null) {
        pilaCodificada = codificarPila(pila.dropLast(1))
    }
    BackHandler(
        enabled = editor == null && cursoTareas == null && pestanaActual != Destino.Hoy,
    ) {
        pilaCodificada = codificarDestino(Destino.Hoy)
    }

    val conservadorEstado = rememberSaveableStateHolder()

    AnimatedContent(
        targetState = nivel,
        transitionSpec = {
            fadeIn(tween(MovimientoNotally.fundidoMs)) togetherWith
                fadeOut(tween(MovimientoNotally.fundidoMs))
        },
        label = "rootTransition",
        modifier = modifier.fillMaxSize(),
    ) { nivelActivo ->
        when (nivelActivo) {
            is NivelRaiz.Editor -> RutaEditor(
                tareaId = nivelActivo.tareaId,
                onVolver = {
                    pilaCodificada = codificarPila(pila.dropLast(1))
                },
            )

            is NivelRaiz.CursoTareas -> {
                conservadorEstado.SaveableStateProvider(
                    "cursoTareas:${nivelActivo.cursoId}",
                ) {
                    TareasCursoScreen(
                        cursoId = nivelActivo.cursoId,
                        alVolver = {
                            pilaCodificada = codificarPila(pila.dropLast(1))
                        },
                        alAbrirTarea = { tareaId ->
                            pilaCodificada = codificarPila(pila + Destino.Editor(tareaId))
                        },
                    )
                }
            }

            NivelRaiz.Pestanas -> ContenedorPestanas(
                actual = pestanaActual,
                onSeleccionar = { destino ->
                    pilaCodificada = codificarDestino(destino)
                },
                onAbrirEditor = { tareaId ->
                    pilaCodificada = codificarPila(pila + Destino.Editor(tareaId))
                },
                onAbrirTareasCurso = { cursoId ->
                    pilaCodificada = codificarPila(pila + Destino.CursoTareas(cursoId))
                },
                conservadorEstado = conservadorEstado,
            )
        }
    }
}

@Composable
private fun ContenedorPestanas(
    actual: Destino,
    onSeleccionar: (Destino) -> Unit,
    onAbrirEditor: (Long?) -> Unit,
    onAbrirTareasCurso: (Long) -> Unit,
    conservadorEstado: androidx.compose.runtime.saveable.SaveableStateHolder,
) {
    val colores = MaterialTheme.colorScheme
    val margen = margenHorizontalPantalla
    val elementos = remember {
        listOf(
            Destino.Hoy,
            Destino.Cursos,
            Destino.Settings,
        )
    }
    var diaHoySeleccionado by rememberSaveable { mutableStateOf<Long?>(null) }
    var solicitudNuevoCursoPendiente by remember { mutableStateOf(false) }
    var clusterVisible by remember { mutableStateOf(true) }
    var accionDisponible by remember { mutableStateOf(true) }
    LaunchedEffect(actual) {
        clusterVisible = true
        accionDisponible = true
    }

    val abrirNuevaTarea: () -> Unit = { onAbrirEditor(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colores.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { rellenoInterior ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(rellenoInterior)
                .consumeWindowInsets(rellenoInterior),
        ) {
            val objetivoPestana = rememberUpdatedState(actual)
            AnimatedContent(
                targetState = actual,
                transitionSpec = {
                    val desde = initialState.indicePestana() ?: 0
                    val hasta = targetState.indicePestana() ?: 0
                    val avanza = hasta > desde
                    val recorrido = tween<IntOffset>(
                        durationMillis = MovimientoNotally.slidePestanaMs,
                        easing = MovimientoNotally.fundidoEasing,
                    )
                    val entrada = fadeIn(
                        tween(
                            durationMillis = MovimientoNotally.fundidoPestanaMs,
                            easing = MovimientoNotally.fundidoEasing,
                        ),
                    )
                    if (avanza) {
                        (slideInHorizontally(recorrido) { ancho -> ancho } + entrada)
                            .togetherWith(slideOutHorizontally(recorrido) { ancho -> -ancho })
                    } else {
                        (slideInHorizontally(recorrido) { ancho -> -ancho } + entrada)
                            .togetherWith(slideOutHorizontally(recorrido) { ancho -> ancho })
                    }
                },
                label = "tabSlide",
                modifier = Modifier.fillMaxSize(),
            ) { destino ->
                val saliente = destino != objetivoPestana.value
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (saliente) {
                                Modifier.clearAndSetSemantics { }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    conservadorEstado.SaveableStateProvider(destino.clave) {
                        when (destino) {
                            Destino.Hoy -> TodayScreen(
                                alCrearTarea = abrirNuevaTarea,
                                alAbrirTarea = { id -> onAbrirEditor(id) },
                                diaSeleccionadoEpoch = diaHoySeleccionado,
                                alSeleccionarDia = { diaHoySeleccionado = it },
                                alCambiarVisibilidadCluster = { clusterVisible = it },
                                alCambiarDisponibilidadAccion = { accionDisponible = it },
                            )

                            Destino.Cursos -> CursosScreen(
                                modifier = Modifier.padding(
                                    bottom = EspaciadoNotally.rellenoInferiorLista,
                                ),
                                alAbrirTareasCurso = onAbrirTareasCurso,
                                solicitudNuevoCursoPendiente = solicitudNuevoCursoPendiente,
                                alConsumirSolicitudNuevoCurso = {
                                    solicitudNuevoCursoPendiente = false
                                },
                            )

                            Destino.Settings -> SettingsScreen(
                                modifier = Modifier.padding(
                                    bottom = EspaciadoNotally.rellenoInferiorLista,
                                ),
                            )

                            is Destino.Editor -> Unit
                            is Destino.CursoTareas -> Unit
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = clusterVisible,
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = margen, end = margen, bottom = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AltoCluster),
                ) {
                    FloatingNavBar(
                        items = elementos.map { destino ->
                            val seleccionado = actual == destino
                            FloatingNavItem(
                                etiqueta = stringResource(destino.recursoEtiqueta()),
                                icono = painterResource(destino.recursoIcono()),
                                seleccionado = seleccionado,
                                alPulsar = {
                                    if (!seleccionado) {
                                        diaHoySeleccionado = null
                                        clusterVisible = true
                                        onSeleccionar(destino)
                                    }
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    val destinoFab = when (actual) {
                        Destino.Hoy -> if (accionDisponible) DestinoFab.Hoy else null
                        Destino.Cursos -> DestinoFab.Cursos
                        else -> null
                    }
                    var ultimoDestinoFab by remember { mutableStateOf(DestinoFab.Hoy) }
                    LaunchedEffect(destinoFab) {
                        if (destinoFab != null) ultimoDestinoFab = destinoFab
                    }
                    val destinoFabEfectivo = destinoFab ?: ultimoDestinoFab
                    val desplazamientoYObjetivo = when (destinoFabEfectivo) {
                        DestinoFab.Hoy -> 2.dp
                        DestinoFab.Cursos -> -(AltoFab + SeparacionFabCluster)
                    }

                    AnimatedVisibility(
                        visible = destinoFab != null,
                        enter = fadeIn(tween(MovimientoNotally.fabMs)),
                        exit = fadeOut(tween(MovimientoNotally.fabMs)),
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        val desplazamientoY by animateDpAsState(
                            targetValue = desplazamientoYObjetivo,
                            animationSpec = MovimientoNotally.espacialNormal(),
                            label = "fabDestino",
                        )
                        val accionFab: () -> Unit = when (destinoFabEfectivo) {
                            DestinoFab.Hoy -> abrirNuevaTarea
                            DestinoFab.Cursos -> {
                                { solicitudNuevoCursoPendiente = true }
                            }
                        }
                        val descripcionFab = when (destinoFabEfectivo) {
                            DestinoFab.Hoy -> stringResource(R.string.a11y_newTask)
                            DestinoFab.Cursos -> stringResource(R.string.courses_new)
                        }
                        NotallyFab(
                            alPulsar = accionFab,
                            descripcion = descripcionFab,
                            modifier = Modifier.offset(y = desplazamientoY),
                        )
                    }
                }
            }
        }
    }
}

/**
 * El editor se aísla en su propio [ViewModelStore] para que cada apertura
 * arranque con un borrador limpio y el ViewModel se libere al volver.
 */
@Composable
private fun RutaEditor(
    tareaId: Long?,
    onVolver: () -> Unit,
) {
    val almacen = remember(tareaId) { ViewModelStore() }
    val propietario = remember(almacen) {
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = almacen
        }
    }
    DisposableEffect(almacen) {
        onDispose { almacen.clear() }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides propietario) {
        EditorScreen(tareaId = tareaId, alVolver = onVolver)
    }
}

private fun Destino.recursoEtiqueta(): Int = when (this) {
    Destino.Hoy -> R.string.nav_hoy
    Destino.Cursos -> R.string.nav_cursos
    Destino.Settings -> R.string.nav_ajustes
    is Destino.Editor -> R.string.nav_hoy
    is Destino.CursoTareas -> R.string.nav_cursos
}

/**
 * Orden de navegación de la barra (§6): Hoy → Cursos → Ajustes.
 * Gobierna el signo del slide; el editor no participa (vive fuera de esta pila).
 */
private fun Destino.indicePestana(): Int? = when (this) {
    Destino.Hoy -> 0
    Destino.Cursos -> 1
    Destino.Settings -> 2
    is Destino.Editor -> null
    is Destino.CursoTareas -> null
}

private fun Destino.recursoIcono(): Int = when (this) {
    Destino.Hoy -> R.drawable.ic_checklist
    Destino.Cursos -> R.drawable.ic_book
    Destino.Settings -> R.drawable.ic_settings
    is Destino.Editor -> R.drawable.ic_checklist
    is Destino.CursoTareas -> R.drawable.ic_book
}

internal fun codificarDestino(destino: Destino): String = destino.clave

internal fun codificarPila(pila: List<Destino>): String =
    pila.joinToString("|") { codificarDestino(it) }

internal fun decodificarPila(codificada: String): List<Destino> =
    codificada.split('|')
        .mapNotNull(::decodificarDestino)
        .ifEmpty { listOf(Destino.Hoy) }

private val destinosFijos: Map<String, Destino> = listOf(
    Destino.Hoy,
    Destino.Cursos,
    Destino.Settings,
).associateBy { it.clave }

internal fun decodificarDestino(codificada: String): Destino? = when {
    codificada.startsWith(PREFIJO_CURSO_TAREAS) -> codificada
        .removePrefix(PREFIJO_CURSO_TAREAS)
        .toLongOrNull()
        ?.let(Destino::CursoTareas)

    codificada.startsWith(PREFIJO_EDITOR) -> Destino.Editor(
        codificada.removePrefix(PREFIJO_EDITOR)
            .takeUnless { it == ID_EDITOR_NUEVO }
            ?.toLongOrNull(),
    )

    else -> destinosFijos[codificada]
}
