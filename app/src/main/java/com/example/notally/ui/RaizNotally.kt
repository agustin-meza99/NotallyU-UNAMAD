package com.example.notally.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.notally.R
import com.example.notally.ui.editor.PantallaEditor
import com.example.notally.ui.settings.PantallaAjustes
import com.example.notally.ui.subjects.PantallaMaterias
import com.example.notally.ui.theme.MovimientoNotally
import com.example.notally.ui.today.PantallaHoy

/** Destinos de la V1. El editor no lleva barra inferior. */
sealed interface Destino {
    val clave: String

    data object Hoy : Destino {
        override val clave: String = "today"
    }

    data object Materias : Destino {
        override val clave: String = "subjects"
    }

    data object Ajustes : Destino {
        override val clave: String = "settings"
    }

    data class Editor(val tareaId: Long?) : Destino {
        override val clave: String = "editor:${tareaId ?: "new"}"
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
fun RaizNotally(modifier: Modifier = Modifier) {
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
    val elementos = remember {
        listOf(
            Destino.Hoy,
            Destino.Materias,
            Destino.Ajustes,
        )
    }
    val coloresElemento = ShortNavigationBarItemDefaults.colors(
        selectedIconColor = colores.onPrimaryContainer,
        selectedTextColor = colores.onSurface,
        selectedIndicatorColor = colores.primaryContainer,
        unselectedIconColor = colores.onSurfaceVariant,
        unselectedTextColor = colores.onSurfaceVariant,
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colores.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            ShortNavigationBar(
                containerColor = colores.surface,
                contentColor = colores.onSurfaceVariant,
            ) {
                elementos.forEach { destino ->
                    val seleccionado = actual == destino
                    ShortNavigationBarItem(
                        selected = seleccionado,
                        onClick = {
                            if (!seleccionado) onSeleccionar(destino)
                        },
                        icon = {
                            Icon(
                                painter = painterResource(destino.recursoIcono()),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(destino.recursoEtiqueta()),
                                maxLines = 2,
                                overflow = TextOverflow.Visible,
                                textAlign = TextAlign.Center,
                            )
                        },
                        colors = coloresElemento,
                    )
                }
            }
        },
    ) { rellenoInterior ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(rellenoInterior)
                .consumeWindowInsets(rellenoInterior),
        ) {
            Crossfade(
                targetState = actual,
                animationSpec = tween(MovimientoNotally.fundidoPestanaMs),
                label = "tabFade",
            ) { destino ->
                conservadorEstado.SaveableStateProvider(destino.clave) {
                    when (destino) {
                        Destino.Hoy -> PantallaHoy(
                            alCrearTarea = { onAbrirEditor(null) },
                            alAbrirTarea = { id -> onAbrirEditor(id) },
                        )

                        Destino.Materias -> PantallaMaterias()

                        Destino.Ajustes -> PantallaAjustes()

                        is Destino.Editor -> Unit
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
        PantallaEditor(tareaId = tareaId, alVolver = onVolver)
    }
}

private fun Destino.recursoEtiqueta(): Int = when (this) {
    Destino.Hoy -> R.string.nav_hoy
    Destino.Materias -> R.string.nav_materias
    Destino.Ajustes -> R.string.nav_ajustes
    is Destino.Editor -> R.string.nav_hoy
}

private fun Destino.recursoIcono(): Int = when (this) {
    Destino.Hoy -> R.drawable.ic_checklist
    Destino.Materias -> R.drawable.ic_book
    Destino.Ajustes -> R.drawable.ic_settings
    is Destino.Editor -> R.drawable.ic_checklist
}

internal fun codificarDestino(destino: Destino): String = when (destino) {
    Destino.Hoy -> "today"
    Destino.Materias -> "subjects"
    Destino.Ajustes -> "settings"
    is Destino.Editor -> "editor:${destino.tareaId?.toString() ?: "new"}"
}

internal fun codificarPila(pila: List<Destino>): String =
    pila.joinToString("|") { codificarDestino(it) }

internal fun decodificarPila(codificada: String): List<Destino> =
    codificada.split('|')
        .mapNotNull(::decodificarDestino)
        .ifEmpty { listOf(Destino.Hoy) }

internal fun decodificarDestino(codificada: String): Destino? = when {
    codificada == "today" -> Destino.Hoy
    codificada == "subjects" -> Destino.Materias
    codificada == "settings" -> Destino.Ajustes
    codificada.startsWith("editor:") -> {
        val crudo = codificada.removePrefix("editor:")
        Destino.Editor(if (crudo == "new") null else crudo.toLongOrNull())
    }

    else -> null
}
