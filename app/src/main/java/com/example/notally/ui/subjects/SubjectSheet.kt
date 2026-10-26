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
import com.example.notally.data.model.Subject
import com.example.notally.ui.theme.LocalSubjectPalette
import com.example.notally.ui.theme.NotallyShape
import com.example.notally.ui.theme.NotallySubjectSwatches
import kotlinx.coroutines.launch

/** Qué muestra la hoja de materia: alta o edición de una tarjeta existente. */
sealed interface SubjectSheetTarget {
    data object New : SubjectSheetTarget
    data class Edit(val card: SubjectCardUi) : SubjectSheetTarget
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
    target: SubjectSheetTarget,
    defaultColorArgb: Long,
    formError: SubjectFormError?,
    onDismiss: () -> Unit,
    onSave: suspend (existing: Subject?, name: String, colorArgb: Long) -> SubjectSaveResult,
    onSaved: () -> Unit,
    onDelete: (Subject) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalSubjectPalette.current
    val editing = target as? SubjectSheetTarget.Edit
    val targetKey = editing?.card?.subject?.id?.toString() ?: "new"
    var name by rememberSaveable(targetKey) {
        mutableStateOf(editing?.card?.subject?.name.orEmpty())
    }
    // Se normaliza al swatch canónico del tema: un color viejo fuera de los
    // seis tokens se marca y se guarda como su equivalente más cercano.
    var colorArgb by rememberSaveable(targetKey) {
        mutableStateOf(
            palette.subjectSwatch(
                editing?.card?.subject?.colorArgb ?: defaultColorArgb,
            ).argb,
        )
    }
    var confirmDelete by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(targetKey) {
        saving = false
    }

    val titleRes = if (editing == null) R.string.subjects_new else R.string.subject_edit_title
    val duplicateError = formError == SubjectFormError.DUPLICATE

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = NotallyShape.xl,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (formError != null) onClearError()
                },
                label = { Text(stringResource(R.string.subject_name)) },
                singleLine = true,
                isError = duplicateError || formError == SubjectFormError.GENERIC,
                supportingText = when (formError) {
                    SubjectFormError.DUPLICATE -> {
                        { Text(stringResource(R.string.subject_name_duplicate)) }
                    }
                    SubjectFormError.GENERIC -> {
                        { Text(stringResource(R.string.subject_save_error)) }
                    }
                    null -> null
                },
                shape = NotallyShape.s,
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
            SwatchRow(
                selectedArgb = colorArgb,
                onSelect = { colorArgb = it },
            )
            if (editing != null) {
                Spacer(Modifier.height(24.dp))
                TextButton(
                    onClick = { confirmDelete = true },
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
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        if (!SubjectFormRules.canSubmit(name, saving)) return@TextButton
                        saving = true
                        scope.launch {
                            try {
                                val result = onSave(
                                    editing?.card?.subject,
                                    name.trim(),
                                    palette.subjectSwatch(colorArgb).argb,
                                )
                                if (SubjectFormRules.isFinished(result)) onSaved()
                            } finally {
                                // Se reactiva siempre, también tras un segundo
                                // fallo con el mismo error.
                                saving = false
                            }
                        }
                    },
                    enabled = SubjectFormRules.canSubmit(name, saving),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }

    if (confirmDelete && editing != null) {
        DeleteSubjectDialog(
            name = editing.card.subject.name,
            taskCount = editing.card.totalTasks,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete(editing.card.subject)
            },
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SwatchRow(
    selectedArgb: Long,
    onSelect: (Long) -> Unit,
) {
    val palette = LocalSubjectPalette.current
    val resolved = palette.subjectSwatch(selectedArgb)
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        NotallySubjectSwatches.forEach { swatch ->
            val name = stringResource(swatch.nameRes)
            val selected = swatch.argb == resolved.argb
            val fill = swatch.container(palette.isDark)
            val content = swatch.onContainer(palette.isDark)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onSelect(swatch.argb) },
                    )
                    .semantics { contentDescription = name },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(fill)
                        .then(
                            if (selected) {
                                Modifier.border(2.dp, content, CircleShape)
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = content,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteSubjectDialog(
    name: String,
    taskCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val cancelFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { cancelFocus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        shape = NotallyShape.xl,
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
                text = if (taskCount == 0) {
                    stringResource(R.string.confirm_deleteSubject_bodyNoTasks, name)
                } else {
                    pluralStringResource(
                        R.plurals.confirm_deleteSubject_body,
                        taskCount,
                        name,
                        taskCount,
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .focusRequester(cancelFocus),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
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
