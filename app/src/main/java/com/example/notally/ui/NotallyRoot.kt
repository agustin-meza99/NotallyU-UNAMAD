package com.example.notally.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedContent
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
import com.example.notally.ui.editor.EditorScreen
import com.example.notally.ui.settings.SettingsScreen
import com.example.notally.ui.subjects.SubjectsScreen
import com.example.notally.ui.theme.NotallyMotion
import com.example.notally.ui.today.TodayScreen

/** Destinos de la V1. El editor no lleva barra inferior. */
sealed interface Destination {
    val key: String

    data object Today : Destination {
        override val key: String = "today"
    }

    data object Subjects : Destination {
        override val key: String = "subjects"
    }

    data object Settings : Destination {
        override val key: String = "settings"
    }

    data class Editor(val taskId: Long?) : Destination {
        override val key: String = "editor:${taskId ?: "new"}"
    }
}

/**
 * Raíz de navegación (§6).
 *
 * Sin `navigation-compose`: la pila es una lista de [Destination] guardada en
 * un `String` con `rememberSaveable`. El editor hace su propio back predictivo
 * y las pestañas vuelven a Hoy con `BackHandler`. Los tabs se conservan con
 * [rememberSaveableStateHolder], así que volver del editor deja la pestaña de
 * origen con su scroll y su filtro.
 */
@Composable
fun NotallyRoot(modifier: Modifier = Modifier) {
    var stackEncoded by rememberSaveable {
        mutableStateOf(encodeDestination(Destination.Today))
    }
    val stack = remember(stackEncoded) { decodeStack(stackEncoded) }
    val current = stack.lastOrNull() ?: Destination.Today
    val editor = current as? Destination.Editor
    val currentTab = stack.lastOrNull { it !is Destination.Editor } ?: Destination.Today

    // Back del editor: lo gestiona `PredictiveBackHandler` dentro de
    // EditorScreen, que hace flush y permite el gesto predictivo. No se
    // intercepta aquí para no cancelarlo (§6, §10.5).
    //
    // Back en tabs: volver a Hoy es una navegación interna; se conserva un
    // BackHandler solo para pestañas secundarias. En Hoy el sistema sale de la
    // app sin nada pendiente que guardar.
    BackHandler(enabled = editor == null && currentTab != Destination.Today) {
        stackEncoded = encodeDestination(Destination.Today)
    }

    val holder = rememberSaveableStateHolder()

    AnimatedContent(
        targetState = editor,
        transitionSpec = {
            fadeIn(tween(NotallyMotion.fadeThroughMs)) togetherWith
                fadeOut(tween(NotallyMotion.fadeThroughMs))
        },
        label = "rootTransition",
        modifier = modifier.fillMaxSize(),
    ) { activeEditor ->
        if (activeEditor != null) {
            EditorRoute(
                taskId = activeEditor.taskId,
                onBack = {
                    stackEncoded = encodeStack(stack.dropLast(1))
                },
            )
        } else {
            TabShell(
                current = currentTab,
                onSelect = { destination ->
                    stackEncoded = encodeDestination(destination)
                },
                onOpenEditor = { taskId ->
                    stackEncoded = encodeStack(stack + Destination.Editor(taskId))
                },
                stateHolder = holder,
            )
        }
    }
}

@Composable
private fun TabShell(
    current: Destination,
    onSelect: (Destination) -> Unit,
    onOpenEditor: (Long?) -> Unit,
    stateHolder: androidx.compose.runtime.saveable.SaveableStateHolder,
) {
    val colors = MaterialTheme.colorScheme
    val items = remember {
        listOf(
            Destination.Today,
            Destination.Subjects,
            Destination.Settings,
        )
    }
    val itemColors = ShortNavigationBarItemDefaults.colors(
        selectedIconColor = colors.onPrimaryContainer,
        selectedTextColor = colors.onSurface,
        selectedIndicatorColor = colors.primaryContainer,
        unselectedIconColor = colors.onSurfaceVariant,
        unselectedTextColor = colors.onSurfaceVariant,
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            ShortNavigationBar(
                containerColor = colors.surface,
                contentColor = colors.onSurfaceVariant,
            ) {
                items.forEach { destination ->
                    val selected = current == destination
                    ShortNavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) onSelect(destination)
                        },
                        icon = {
                            Icon(
                                painter = painterResource(destination.iconRes()),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(destination.labelRes()),
                                maxLines = 2,
                                overflow = TextOverflow.Visible,
                                textAlign = TextAlign.Center,
                            )
                        },
                        colors = itemColors,
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            Crossfade(
                targetState = current,
                animationSpec = tween(NotallyMotion.tabFadeMs),
                label = "tabFade",
            ) { destination ->
                stateHolder.SaveableStateProvider(destination.key) {
                    when (destination) {
                        Destination.Today -> TodayScreen(
                            onCreateTask = { onOpenEditor(null) },
                            onOpenTask = { id -> onOpenEditor(id) },
                        )

                        Destination.Subjects -> SubjectsScreen()

                        Destination.Settings -> SettingsScreen()

                        is Destination.Editor -> Unit
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
private fun EditorRoute(
    taskId: Long?,
    onBack: () -> Unit,
) {
    val store = remember(taskId) { ViewModelStore() }
    val owner = remember(store) {
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
    }
    DisposableEffect(store) {
        onDispose { store.clear() }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        EditorScreen(taskId = taskId, onBack = onBack)
    }
}

private fun Destination.labelRes(): Int = when (this) {
    Destination.Today -> R.string.nav_hoy
    Destination.Subjects -> R.string.nav_materias
    Destination.Settings -> R.string.nav_ajustes
    is Destination.Editor -> R.string.nav_hoy
}

private fun Destination.iconRes(): Int = when (this) {
    Destination.Today -> R.drawable.ic_checklist
    Destination.Subjects -> R.drawable.ic_book
    Destination.Settings -> R.drawable.ic_settings
    is Destination.Editor -> R.drawable.ic_checklist
}

internal fun encodeDestination(destination: Destination): String = when (destination) {
    Destination.Today -> "today"
    Destination.Subjects -> "subjects"
    Destination.Settings -> "settings"
    is Destination.Editor -> "editor:${destination.taskId?.toString() ?: "new"}"
}

internal fun encodeStack(stack: List<Destination>): String =
    stack.joinToString("|") { encodeDestination(it) }

internal fun decodeStack(encoded: String): List<Destination> =
    encoded.split('|')
        .mapNotNull(::decodeDestination)
        .ifEmpty { listOf(Destination.Today) }

internal fun decodeDestination(encoded: String): Destination? = when {
    encoded == "today" -> Destination.Today
    encoded == "subjects" -> Destination.Subjects
    encoded == "settings" -> Destination.Settings
    encoded.startsWith("editor:") -> {
        val raw = encoded.removePrefix("editor:")
        Destination.Editor(if (raw == "new") null else raw.toLongOrNull())
    }

    else -> null
}
