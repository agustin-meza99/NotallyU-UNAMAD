package com.example.notally.ui.cursos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.data.model.Curso
import com.example.notally.data.repository.ResultadoCurso
import com.example.notally.ui.components.CourseNameField
import com.example.notally.ui.components.DialogoConfirmarEliminacion
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.theme.LocalPaletaCursos
import com.example.notally.ui.theme.MuestrasCursos
import kotlinx.coroutines.launch

/** Qué muestra la hoja de curso: alta o edición de una tarjeta existente. */
sealed interface ObjetivoCursoSheet {
    data object Nueva : ObjetivoCursoSheet
    data class Editar(val tarjeta: TarjetaCursoUi) : ObjetivoCursoSheet
}

/**
 * Hoja "Editar curso" / "Nuevo curso" (§7.4, §7.9).
 *
 * La validación de vacío es local (deshabilita "Guardar"); el duplicado lo
 * detecta la base (`SQLiteConstraintException`) y se muestra como error
 * inline. La eliminación pide confirmación con el número real de tareas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CursoSheet(
    objetivo: ObjetivoCursoSheet,
    colorPorDefecto: Long,
    errorFormulario: ResultadoCurso?,
    alDescartar: () -> Unit,
    alGuardar: suspend (existente: Curso?, nombre: String, color: Long) ->
        ResultadoCurso,
    alGuardado: () -> Unit,
    alEliminar: (Curso) -> Unit,
    alLimpiarError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paleta = LocalPaletaCursos.current
    val edicion = objetivo as? ObjetivoCursoSheet.Editar
    val claveObjetivo = edicion?.tarjeta?.curso?.id?.toString() ?: "new"
    var nombre by rememberSaveable(claveObjetivo) {
        mutableStateOf(edicion?.tarjeta?.curso?.nombre.orEmpty())
    }
    // Se normaliza al swatch canónico de la paleta: un color viejo fuera de los
    // seis tokens se marca y se guarda como su equivalente más cercano.
    var color by rememberSaveable(claveObjetivo) {
        mutableStateOf(
            paleta.muestraCurso(
                edicion?.tarjeta?.curso?.color ?: colorPorDefecto,
            ).argb,
        )
    }
    var confirmarEliminar by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()

    LaunchedEffect(claveObjetivo) {
        guardando = false
    }

    val recursoTitulo = if (edicion == null) {
        R.string.courses_new
    } else {
        R.string.course_edit_title
    }
    ModalBottomSheet(
        onDismissRequest = alDescartar,
        modifier = modifier,
        shape = FormaNotally.xl,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            Text(
                text = stringResource(recursoTitulo),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            CourseNameField(
                valor = nombre,
                alCambiarValor = { nombre = it },
                resultado = errorFormulario,
                alLimpiarError = alLimpiarError,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.course_color),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            SwatchRow(
                colorSeleccionado = color,
                alSeleccionar = { color = it },
            )
            if (edicion != null) {
                Spacer(Modifier.height(24.dp))
                TextButton(
                    onClick = { confirmarEliminar = true },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.course_delete))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = alDescartar) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        if (!CursoFormRules.puedeEnviar(nombre, guardando)) {
                            return@TextButton
                        }
                        guardando = true
                        alcance.launch {
                            try {
                                val resultado = alGuardar(
                                    edicion?.tarjeta?.curso,
                                    nombre.trim(),
                                    paleta.muestraCurso(color).argb,
                                )
                                if (CursoFormRules.termino(resultado)) alGuardado()
                            } finally {
                                // Se reactiva siempre, también tras un segundo
                                // fallo con el mismo error.
                                guardando = false
                            }
                        }
                    },
                    enabled = CursoFormRules.puedeEnviar(nombre, guardando),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }

    if (confirmarEliminar && edicion != null) {
        val nombre = edicion.tarjeta.curso.nombre
        val cantidadTareas = edicion.tarjeta.totalTareas
        DialogoConfirmarEliminacion(
            titulo = stringResource(R.string.confirm_delete_course_title),
            cuerpo = if (cantidadTareas == 0) {
                stringResource(R.string.confirm_deleteTask_bodyNoSub, nombre)
            } else {
                pluralStringResource(
                    R.plurals.confirm_delete_course_body,
                    cantidadTareas,
                    nombre,
                    cantidadTareas,
                )
            },
            onConfirmar = {
                confirmarEliminar = false
                alEliminar(edicion.tarjeta.curso)
            },
            onDismiss = { confirmarEliminar = false },
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SwatchRow(
    colorSeleccionado: Long,
    alSeleccionar: (Long) -> Unit,
) {
    val paleta = LocalPaletaCursos.current
    val resuelto = paleta.muestraCurso(colorSeleccionado)
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MuestrasCursos.forEach { muestra ->
            val nombre = stringResource(muestra.recursoNombre)
            val seleccionada = muestra.argb == resuelto.argb
            val relleno = muestra.contenedorClaro
            val contenido = muestra.claro
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .selectable(
                        selected = seleccionada,
                        role = Role.RadioButton,
                        onClick = { alSeleccionar(muestra.argb) },
                    )
                    .semantics { contentDescription = nombre },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(relleno)
                        .then(
                            if (seleccionada) {
                                Modifier.border(2.dp, contenido, CircleShape)
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (seleccionada) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = contenido,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

