package com.example.notally.ui.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.notally.R
import com.example.notally.data.model.Materia
import com.example.notally.ui.theme.FormaNotally
import com.example.notally.ui.theme.LocalPaletaMaterias
import com.example.notally.ui.theme.MuestrasMaterias
import kotlinx.coroutines.launch

/** Qué muestra la hoja de materia: alta o edición de una tarjeta existente. */
sealed interface ObjetivoSubjectSheet {
    data object Nueva : ObjetivoSubjectSheet
    data class Editar(val tarjeta: TarjetaMateriaUi) : ObjetivoSubjectSheet
}

/**
 * Hoja "Editar materia" / "Nueva materia" (§7.4, §7.9).
 *
 * La validación de vacío es local (deshabilita "Guardar"); el duplicado lo
 * detecta la base (`SQLiteConstraintException`) y se muestra como error
 * inline. La eliminación pide confirmación con el número real de tareas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectSheet(
    objetivo: ObjetivoSubjectSheet,
    colorPorDefecto: Long,
    errorFormulario: ErrorFormularioMateria?,
    alDescartar: () -> Unit,
    alGuardar: suspend (existente: Materia?, nombre: String, color: Long) ->
        ResultadoGuardarMateria,
    alGuardado: () -> Unit,
    alEliminar: (Materia) -> Unit,
    alLimpiarError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val paleta = LocalPaletaMaterias.current
    val edicion = objetivo as? ObjetivoSubjectSheet.Editar
    val claveObjetivo = edicion?.tarjeta?.materia?.id?.toString() ?: "new"
    var nombre by rememberSaveable(claveObjetivo) {
        mutableStateOf(edicion?.tarjeta?.materia?.nombre.orEmpty())
    }
    // Se normaliza al swatch canónico del tema: un color viejo fuera de los
    // seis tokens se marca y se guarda como su equivalente más cercano.
    var color by rememberSaveable(claveObjetivo) {
        mutableStateOf(
            paleta.muestraMateria(
                edicion?.tarjeta?.materia?.color ?: colorPorDefecto,
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
        R.string.subjects_new
    } else {
        R.string.subject_edit_title
    }
    val errorDuplicado = errorFormulario == ErrorFormularioMateria.DUPLICADO

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
            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    nombre = it
                    if (errorFormulario != null) alLimpiarError()
                },
                label = { Text(stringResource(R.string.subject_name)) },
                singleLine = true,
                isError = errorDuplicado ||
                    errorFormulario == ErrorFormularioMateria.GENERICO,
                supportingText = when (errorFormulario) {
                    ErrorFormularioMateria.DUPLICADO -> {
                        { Text(stringResource(R.string.subject_name_duplicate)) }
                    }

                    ErrorFormularioMateria.GENERICO -> {
                        { Text(stringResource(R.string.subject_save_error)) }
                    }

                    null -> null
                },
                shape = FormaNotally.s,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.subject_color),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            FilaMuestras(
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
                    Text(stringResource(R.string.subject_delete))
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
                        if (!SubjectFormRules.puedeEnviar(nombre, guardando)) {
                            return@TextButton
                        }
                        guardando = true
                        alcance.launch {
                            try {
                                val resultado = alGuardar(
                                    edicion?.tarjeta?.materia,
                                    nombre.trim(),
                                    paleta.muestraMateria(color).argb,
                                )
                                if (SubjectFormRules.termino(resultado)) alGuardado()
                            } finally {
                                // Se reactiva siempre, también tras un segundo
                                // fallo con el mismo error.
                                guardando = false
                            }
                        }
                    },
                    enabled = SubjectFormRules.puedeEnviar(nombre, guardando),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }

    if (confirmarEliminar && edicion != null) {
        DialogoEliminarMateria(
            nombre = edicion.tarjeta.materia.nombre,
            cantidadTareas = edicion.tarjeta.totalTareas,
            alDescartar = { confirmarEliminar = false },
            alConfirmar = {
                confirmarEliminar = false
                alEliminar(edicion.tarjeta.materia)
            },
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FilaMuestras(
    colorSeleccionado: Long,
    alSeleccionar: (Long) -> Unit,
) {
    val paleta = LocalPaletaMaterias.current
    val resuelto = paleta.muestraMateria(colorSeleccionado)
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MuestrasMaterias.forEach { muestra ->
            val nombre = stringResource(muestra.recursoNombre)
            val seleccionada = muestra.argb == resuelto.argb
            val relleno = muestra.contenedor(paleta.esOscuro)
            val contenido = muestra.sobreContenedor(paleta.esOscuro)
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

@Composable
private fun DialogoEliminarMateria(
    nombre: String,
    cantidadTareas: Int,
    alDescartar: () -> Unit,
    alConfirmar: () -> Unit,
) {
    val focoCancelar = remember { FocusRequester() }
    LaunchedEffect(Unit) { focoCancelar.requestFocus() }
    AlertDialog(
        onDismissRequest = alDescartar,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        shape = FormaNotally.xl,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = {
            Text(
                text = stringResource(R.string.confirm_deleteSubject_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Text(
                text = if (cantidadTareas == 0) {
                    stringResource(R.string.confirm_deleteSubject_bodyNoTasks, nombre)
                } else {
                    pluralStringResource(
                        R.plurals.confirm_deleteSubject_body,
                        cantidadTareas,
                        nombre,
                        cantidadTareas,
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
    )
}
