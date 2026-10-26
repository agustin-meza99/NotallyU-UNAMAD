package com.example.notally.ui.cursos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notally.R
import com.example.notally.ui.components.BloqueError
import com.example.notally.ui.components.EmptyState
import com.example.notally.ui.components.FilasCargando
import com.example.notally.ui.components.TaskRow
import com.example.notally.ui.theme.EspaciadoNotally
import com.example.notally.ui.theme.margenHorizontalPantalla

/**
 * Tareas de un curso, abiertas desde la hoja de curso.
 *
 * Comparte la fila de Hoy ([TaskRow]) para conservar sus etiquetas de entrega
 * (Entregada / No entregada / Atrasada) y su accesibilidad. La cabecera es
 * propia, sin `TopAppBar`: 48 dp de atrás y el nombre del curso en `titleLarge`.
 */
@Composable
fun TareasCursoScreen(
    cursoId: Long,
    alVolver: () -> Unit,
    alAbrirTarea: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TareasCursoViewModel = viewModel(
        key = "tareasCurso$cursoId",
        factory = TareasCursoViewModel.factory(LocalContext.current, cursoId),
    ),
) {
    val estado by viewModel.estadoUi.collectAsStateWithLifecycle()
    val estadoSnackbar = remember { SnackbarHostState() }
    val margen = margenHorizontalPantalla

    val mensajeError = stringResource(R.string.editor_saving_error)
    val errorCargar = stringResource(R.string.error_load)
    val etiquetaReintentar = stringResource(R.string.action_retry)
    val nombreCurso = estado.curso?.nombre ?: stringResource(R.string.nav_cursos)

    LaunchedEffect(viewModel, mensajeError) {
        viewModel.eventos.collect {
            estadoSnackbar.showSnackbar(mensajeError)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .semantics { paneTitle = nombreCurso },
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
                    .fillMaxSize()
                    .widthIn(max = EspaciadoNotally.anchoMaximoContenido)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = margen,
                    end = margen,
                    top = 8.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item(key = "header") {
                    EncabezadoCurso(
                        nombre = nombreCurso,
                        cantidadTareas = estado.tareas.size,
                        mostrarContador = !estado.cargando && !estado.error,
                        alVolver = alVolver,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                when {
                    estado.cargando -> {
                        item(key = "loading") {
                            FilasCargando(modifier = Modifier.padding(top = 16.dp))
                        }
                    }

                    estado.error -> {
                        item(key = "error") {
                            BloqueError(
                                titulo = errorCargar,
                                etiquetaReintentar = etiquetaReintentar,
                                alReintentar = viewModel::reintentar,
                                modifier = Modifier.padding(top = 48.dp),
                            )
                        }
                    }

                    estado.tareas.isEmpty() -> {
                        item(key = "empty") {
                            EmptyState(
                                titulo = stringResource(R.string.course_tasks_empty_title),
                                cuerpo = stringResource(R.string.course_tasks_empty_body),
                                etiquetaAccion = stringResource(R.string.course_tasks_empty_cta),
                                alAccionar = alVolver,
                                modifier = Modifier.padding(top = 48.dp),
                            )
                        }
                    }

                    else -> {
                        items(
                            items = estado.tareas,
                            key = { it.id },
                        ) { tarea ->
                            TaskRow(
                                tarea = tarea,
                                nombreCurso = estado.curso?.nombre,
                                ahora = estado.ahora,
                                alPulsar = { alAbrirTarea(tarea.id) },
                                alAlternarCompletada = {
                                    viewModel.alternarCompletada(tarea.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EncabezadoCurso(
    nombre: String,
    cantidadTareas: Int,
    mostrarContador: Boolean,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
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
        Spacer(Modifier.width(4.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = nombre,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (mostrarContador) {
                Text(
                    text = pluralStringResource(
                        R.plurals.course_tasks_count,
                        cantidadTareas,
                        cantidadTareas,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
