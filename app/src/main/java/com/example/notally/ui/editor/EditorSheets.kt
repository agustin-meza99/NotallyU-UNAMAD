package com.example.notally.ui.editor

import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.notally.R
import com.example.notally.data.model.Priority
import com.example.notally.data.model.Status
import com.example.notally.data.model.Subject
import com.example.notally.ui.components.OptionRow
import com.example.notally.ui.theme.NotallyShape
import com.example.notally.ui.util.DateFormat
import com.example.notally.ui.util.dateLabelText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Hoja abierta desde las propiedades del editor. */
enum class EditorSheet {
    SUBJECT,
    DUE,
    PRIORITY,
    STATUS,
}

/**
 * Hojas de propiedad del editor (§7.5, §7.9).
 *
 * Cada hoja se cierra al elegir una opción, salvo la de Entrega: tras fijar la
 * fecha se queda abierta para permitir la hora opcional, y se cierra al
 * terminar o al tocar fuera.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSheetsHost(
    sheet: EditorSheet?,
    draft: EditorDraft,
    subjects: List<Subject>,
    defaultSubjectColor: Long,
    newSubjectError: EditorSubjectError?,
    onDismiss: () -> Unit,
    onSelectSubject: (Long?) -> Unit,
    onCreateSubject: (String, Long) -> Unit,
    onClearSubjectError: () -> Unit,
    onSetDue: (Long?) -> Unit,
    onSetPriority: (Priority) -> Unit,
    onSetStatus: (Status) -> Unit,
) {
    val current = sheet ?: return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = NotallyShape.xl,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
    ) {
        when (current) {
            EditorSheet.SUBJECT -> SubjectPickerContent(
                subjects = subjects,
                selectedId = draft.subjectId,
                defaultColor = defaultSubjectColor,
                error = newSubjectError,
                onSelect = {
                    onSelectSubject(it)
                    onDismiss()
                },
                onCreate = onCreateSubject,
                onClearError = onClearSubjectError,
            )

            EditorSheet.DUE -> DueContent(
                dueAt = draft.dueAt,
                onSetDue = onSetDue,
                onClose = onDismiss,
            )

            EditorSheet.PRIORITY -> {
                SheetTitle(stringResource(R.string.editor_property_priority))
                Priority.entries.forEach { priority ->
                    OptionRow(
                        label = priorityLabel(priority),
                        selected = draft.priority == priority,
                        onClick = {
                            onSetPriority(priority)
                            onDismiss()
                        },
                        showErrorDot = priority == Priority.HIGH,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            EditorSheet.STATUS -> {
                SheetTitle(stringResource(R.string.editor_property_state))
                Status.entries.forEach { status ->
                    OptionRow(
                        label = statusLabel(status),
                        selected = draft.status == status,
                        onClick = {
                            onSetStatus(status)
                            onDismiss()
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun SubjectPickerContent(
    subjects: List<Subject>,
    selectedId: Long?,
    defaultColor: Long,
    error: EditorSubjectError?,
    onSelect: (Long?) -> Unit,
    onCreate: (String, Long) -> Unit,
    onClearError: () -> Unit,
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }

    SheetTitle(stringResource(R.string.editor_property_subject))
    OptionRow(
        label = stringResource(R.string.task_noSubject),
        selected = selectedId == null,
        onClick = { onSelect(null) },
    )
    subjects.forEach { subject ->
        OptionRow(
            label = subject.name,
            selected = selectedId == subject.id,
            onClick = { onSelect(subject.id) },
        )
    }
    if (!creating) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = {
                    onClearError()
                    creating = true
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.subjects_new))
            }
        }
    } else {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (error != null) onClearError()
                },
                label = { Text(stringResource(R.string.subject_name)) },
                singleLine = true,
                isError = error != null,
                supportingText = when (error) {
                    EditorSubjectError.DUPLICATE -> {
                        { Text(stringResource(R.string.subject_name_duplicate)) }
                    }
                    EditorSubjectError.GENERIC -> {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { creating = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
                TextButton(
                    onClick = { onCreate(name.trim(), defaultColor) },
                    enabled = name.isNotBlank(),
                ) {
                    Text(stringResource(R.string.action_add))
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueContent(
    dueAt: Long?,
    onSetDue: (Long?) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val is24Hour = remember(context) { AndroidDateFormat.is24HourFormat(context) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateText = dueAt?.let { dateLabelText(it, zone = zone) }
    val timeText = dueAt?.let { DateFormat.formatTime(it, is24Hour, zone) }
    val hasTime = dueAt != null && DateFormat.hasTimeOfDay(dueAt, zone)

    SheetTitle(stringResource(R.string.editor_property_due))
    Text(
        text = when {
            dueAt == null -> stringResource(R.string.task_noDate)
            timeText != null -> "$dateText · $timeText"
            else -> dateText.orEmpty()
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
    )
    OptionRow(
        label = stringResource(R.string.task_noDate),
        selected = dueAt == null,
        onClick = {
            onSetDue(null)
            onClose()
        },
    )
    OptionRow(
        label = stringResource(R.string.editor_due_pick_date),
        selected = false,
        onClick = { showDatePicker = true },
    )
    if (dueAt != null) {
        OptionRow(
            label = stringResource(R.string.editor_due_pick_time),
            selected = false,
            onClick = { showTimePicker = true },
        )
        if (hasTime) {
            OptionRow(
                label = stringResource(R.string.editor_due_clear_time),
                selected = false,
                onClick = {
                    val date = Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDate()
                    onSetDue(
                        date.atStartOfDay(zone).toInstant().toEpochMilli(),
                    )
                },
            )
        }
    }
    Spacer(Modifier.height(8.dp))

    if (showDatePicker) {
        val initial = dueAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = state.selectedDateMillis
                        if (selected != null) {
                            val date = Instant.ofEpochMilli(selected)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onSetDue(mergeDateAndTime(dueAt, date, zone))
                        }
                        showDatePicker = false
                    },
                    enabled = state.selectedDateMillis != null,
                ) {
                    Text(stringResource(R.string.action_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val zoned = dueAt?.let { Instant.ofEpochMilli(it).atZone(zone) }
        val timeState = rememberTimePickerState(
            initialHour = zoned?.hour ?: 9,
            initialMinute = zoned?.minute ?: 0,
            is24Hour = is24Hour,
        )
        Dialog(
            onDismissRequest = { showTimePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                shape = NotallyShape.xl,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.editor_time_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = timeState)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        TextButton(
                            onClick = {
                                val baseDue = dueAt ?: return@TextButton
                                val date = Instant.ofEpochMilli(baseDue)
                                    .atZone(zone)
                                    .toLocalDate()
                                val withTime = date.atTime(timeState.hour, timeState.minute)
                                    .atZone(zone)
                                    .toInstant()
                                    .toEpochMilli()
                                onSetDue(withTime)
                                showTimePicker = false
                            },
                        ) {
                            Text(stringResource(R.string.action_accept))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun priorityLabel(priority: Priority): String = stringResource(
    when (priority) {
        Priority.HIGH -> R.string.priority_high
        Priority.MEDIUM -> R.string.priority_medium
        Priority.LOW -> R.string.priority_low
    },
)

@Composable
private fun statusLabel(status: Status): String = stringResource(
    when (status) {
        Status.PENDING -> R.string.state_pending
        Status.IN_PROGRESS -> R.string.state_doing
        Status.DONE -> R.string.state_done
    },
)

/**
 * Combina la fecha elegida con la hora que ya tuviera la tarea; si no había
 * hora, la medianoche local (la UI muestra solo la fecha).
 */
internal fun mergeDateAndTime(existingDueAt: Long?, date: LocalDate, zone: ZoneId): Long {
    val time = existingDueAt?.let {
        val zoned = Instant.ofEpochMilli(it).atZone(zone)
        LocalTime.of(zoned.hour, zoned.minute)
    } ?: LocalTime.MIDNIGHT
    return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
}
