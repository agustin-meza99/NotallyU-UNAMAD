package com.example.notally.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.example.notally.ui.editor.EditorScreen
import com.example.notally.ui.settings.SettingsScreen
import com.example.notally.ui.cursos.CursosScreen
import com.example.notally.ui.theme.EspaciadoNotally
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.theme.margenHorizontalPantalla
import com.example.notally.ui.today.TodayScreen

private const val PREFIJO_EDITOR = "editor:"
private const val ID_EDITOR_NUEVO = "new"

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

    data class Editor(val tareaId: Long?) : Destino {
        override val clave: String = "$PREFIJO_EDITOR${tareaId ?: ID_EDITOR_NUEVO}"
    }
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
    val pestanaActual = pila.lastOrNull { it !is Destino.Editor } ?: Destino.Hoy

    // Back del editor: lo gestiona `PredictiveBackHandler` dentro de
    // EditorScreen, que hace flush y permite el gesto predictivo. No se
    // intercepta aquí para no cancelarlo (§6, §10.5).
    //
    // Back en tabs: volver a Hoy es una navegación interna; se conserva un
    // BackHandler solo para pestañas secundarias. En Hoy el sistema sale de la
    // app sin nada pendiente que guardar.
    BackHandler(enabled = editor == null && pestanaActual != Destino.Hoy) {
        pilaCodificada = codificarDestino(Destino.Hoy)
    }

    val conservadorEstado = rememberSaveableStateHolder()

    AnimatedContent(
        targetState = editor,
        transitionSpec = {
            fadeIn(tween(MovimientoNotally.fundidoMs)) togetherWith
                fadeOut(tween(MovimientoNotally.fundidoMs))
        },
        label = "rootTransition",
        modifier = modifier.fillMaxSize(),
    ) { editorActivo ->
        if (editorActivo != null) {
            RutaEditor(
                tareaId = editorActivo.tareaId,
                onVolver = {
                    pilaCodificada = codificarPila(pila.dropLast(1))
                },
            )
        } else {
            ContenedorPestanas(
                actual = pestanaActual,
                onSeleccionar = { destino ->
                    pilaCodificada = codificarDestino(destino)
                },
                onAbrirEditor = { tareaId ->
                    pilaCodificada = codificarPila(pila + Destino.Editor(tareaId))
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
    // Día elegido en la tira de Hoy. Vive sobre el contenido de la pestaña para
    // sobrevivir a la recreación de la Activity; el `rememberSaveable` de dentro
    // de la pestaña lo restauraría también al volver de otra pestaña, que es lo
    // que §7.1.1 prohíbe. Por eso se limpia al cambiar de pestaña.
    var diaHoySeleccionado by rememberSaveable { mutableStateOf<Long?>(null) }
    // El cluster se oculta al bajar en Hoy (§7.10). El estado vive aquí, en la
    // raíz, y TodayScreen solo reporta la dirección del scroll.
    var clusterVisible by remember { mutableStateOf(true) }
    // El FAB de Hoy se oculta en el vacío total, donde el CTA es la única
    // acción (§7.8). No se arrastra a otras pestañas.
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
                    // Orden de navegación: Hoy(0) → Cursos(1) → Ajustes(2).
                    // Avanzar (índice mayor) entra desde la derecha y la
                    // anterior sale hacia la izquierda; retroceder es lo
                    // inverso. Ambas pantallas se mueven en paralelo y, al
                    // recorrer el ancho completo, se reparten el ancho sin
                    // dejar hueco de fondo. El saliente no se desvanece: si lo
                    // hiciera, descubriría el papel antes de terminar el
                    // recorrido.
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
                // Al cambiar de pestaña, el contenido saliente sigue en el
                // árbol durante la transición. Se le quitan las semánticas para
                // que TalkBack no lea dos pantallas a la vez (§9.2). Se lee un
                // State actualizado, no el parámetro capturado, para que el
                // subárbol saliente se recomponga al cambiar el destino.
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

                            // El cluster se superpone al scroll: estas listas
                            // reservan su banda inferior (§4.1, §6).
                            Destino.Cursos -> CursosScreen(
                                modifier = Modifier.padding(
                                    bottom = EspaciadoNotally.rellenoInferiorLista,
                                ),
                            )

                            Destino.Settings -> SettingsScreen(
                                modifier = Modifier.padding(
                                    bottom = EspaciadoNotally.rellenoInferiorLista,
                                ),
                            )

                            is Destino.Editor -> Unit
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
                    accionPrincipal = if (actual == Destino.Hoy && accionDisponible) {
                        abrirNuevaTarea
                    } else {
                        null
                    },
                    descripcionAccionPrincipal = stringResource(R.string.a11y_newTask),
                )
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
}

private fun Destino.recursoIcono(): Int = when (this) {
    Destino.Hoy -> R.drawable.ic_checklist
    Destino.Cursos -> R.drawable.ic_book
    Destino.Settings -> R.drawable.ic_settings
    is Destino.Editor -> R.drawable.ic_checklist
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
    codificada.startsWith(PREFIJO_EDITOR) -> Destino.Editor(
        codificada.removePrefix(PREFIJO_EDITOR)
            .takeUnless { it == ID_EDITOR_NUEVO }
            ?.toLongOrNull(),
    )

    else -> destinosFijos[codificada]
}
