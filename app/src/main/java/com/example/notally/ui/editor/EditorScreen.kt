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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.example.notally.data.model.Priority
import com.example.notally.data.model.Status
import com.example.notally.ui.components.focusRing
import com.example.notally.ui.theme.NotallyMotion
import com.example.notally.ui.theme.NotallyShape
import com.example.notally.ui.theme.NotallySpacing
import com.example.notally.ui.theme.NotallyType
import com.example.notally.ui.theme.screenHorizontalMargin
import com.example.notally.ui.util.DateFormat
import com.example.notally.ui.util.dateLabelText
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
    taskId: Long?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: EditorViewModel = viewModel(
        key = if (taskId == null) "editor-new" else "editor-$taskId",
        factory = EditorViewModel.factory(context, taskId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val newSubjectError by viewModel.newSubjectError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val margin = screenHorizontalMargin
    val focusManager = LocalFocusManager.current

    var sheet by remember { mutableStateOf<EditorSheet?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var newSubtask by rememberSaveable { mutableStateOf("") }
    var subtaskFocused by remember { mutableStateOf(false) }
    val subtaskFocus = remember { FocusRequester() }

    val saveError = stringResource(R.string.editor_saving_error)
    val retryLabel = stringResource(R.string.action_retry)
    val subtaskDeleted = stringResource(R.string.subtask_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    val untitled = stringResource(R.string.editor_untitled)

    // El progreso del gesto se consume hasta el final (la animación la dibuja
    // el sistema). `collect` solo retorna normalmente cuando el gesto se
    // confirma; si se cancela, el job se cancela y `completed` queda en false.
    // Solo entonces se guarda, se espera la escritura y se navega.
    PredictiveBackHandler { progress ->
        var completed = false
        try {
            progress.collect { }
            completed = true
        } finally {
            if (completed) {
                focusManager.clearFocus()
                // El ViewModelStore se destruye al volver: drena la escritura
                // pendiente (con timeout) antes de navegar, sin bloquear la UI.
                withContext(NonCancellable) {
                    viewModel.flushAndWait()
                    onBack()
                }
            }
        }
    }

    // Flush al ir a segundo plano y al salir de la pantalla.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.flush()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.flush()
        }
    }

    LaunchedEffect(viewModel, subtaskDeleted, undoLabel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is EditorEvent.SubtaskDeleted -> {
                    val result = withTimeoutOrNull(NotallyMotion.snackbarMs) {
                        snackbarHostState.showSnackbar(
                            message = subtaskDeleted,
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Indefinite,
                        )
                    }
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDeleteSubtask(event)
                    }
                }
            }
        }
    }

    LaunchedEffect(state.saveError, saveError, retryLabel) {
        if (!state.saveError) return@LaunchedEffect
        val result = withTimeoutOrNull(NotallyMotion.snackbarMs) {
            snackbarHostState.showSnackbar(
                message = saveError,
                actionLabel = retryLabel,
                duration = SnackbarDuration.Indefinite,
            )
        }
        if (result == SnackbarResult.ActionPerformed) viewModel.retrySave()
    }

    // §7.13: antes de 150 ms no se pinta nada (el fondo es `surface`), así no
    // parpadea el bloque de carga cuando Room responde al instante.
    val showLoading by produceState(initialValue = false, key1 = state.loading) {
        value = if (state.loading) {
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
                hostState = snackbarHostState,
                modifier = Modifier.imePadding(),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                EditorTopBar(
                    onBack = {
                        focusManager.clearFocus()
                        viewModel.flush()
                        onBack()
                    },
                    onDelete = { showDeleteDialog = true },
                    showOverflow = !state.loading && !state.loadError,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    when {
                        state.loading && !showLoading -> Unit

                        showLoading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = margin, vertical = 16.dp),
                            ) {
                                com.example.notally.ui.components.LoadingRows(rows = 4)
                            }
                        }

                        state.loadError -> {
                            com.example.notally.ui.components.ErrorBlock(
                                title = stringResource(R.string.editor_load_error),
                                retryLabel = retryLabel,
                                onRetry = viewModel::retryLoad,
                                modifier = Modifier.padding(horizontal = margin, vertical = 48.dp),
                            )
                        }

                        else -> {
                            EditorContent(
                                state = state,
                                subjects = subjects,
                                newSubtask = newSubtask,
                                onNewSubtaskChange = { newSubtask = it },
                                subtaskFocused = subtaskFocused,
                                subtaskFocus = subtaskFocus,
                                onSubtaskFocusChange = { subtaskFocused = it },
                                onAddSubtask = {
                                    val pending = newSubtask
                                    // Solo se limpia cuando el alta se confirma;
                                    // si falla, el texto permanece para reintentar.
                                    viewModel.addSubtask(pending) {
                                        if (newSubtask == pending) newSubtask = ""
                                    }
                                },
                                onToggleSubtask = viewModel::toggleSubtask,
                                onDeleteSubtask = viewModel::deleteSubtask,
                                onAddClick = { subtaskFocus.requestFocus() },
                                onTitleChange = viewModel::onTitleChange,
                                onNotesChange = viewModel::onNotesChange,
                                onOpenProperty = { sheet = it },
                                margin = margin,
                                imeVisible = imeVisible,
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = imeVisible && !state.loading && !state.loadError,
                enter = fadeIn(tween(NotallyMotion.fabMs)),
                exit = fadeOut(tween(NotallyMotion.fabMs)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .padding(12.dp),
            ) {
                EditorToolbar(
                    onSubtask = {
                        subtaskFocus.requestFocus()
                    },
                    onDue = { sheet = EditorSheet.DUE },
                    onPriority = { sheet = EditorSheet.PRIORITY },
                )
            }
        }
    }

    EditorSheetsHost(
        sheet = sheet,
        draft = state,
        subjects = subjects,
        defaultSubjectColor = viewModel.defaultSubjectColor(),
        newSubjectError = newSubjectError,
        onDismiss = { sheet = null },
        onSelectSubject = viewModel::setSubject,
        onCreateSubject = { name, color ->
            viewModel.createSubjectAndSelect(name, color) { sheet = null }
        },
        onClearSubjectError = viewModel::clearNewSubjectError,
        onSetDue = viewModel::setDueAt,
        onSetPriority = viewModel::setPriority,
        onSetStatus = viewModel::setStatus,
    )

    if (showDeleteDialog) {
        DeleteTaskDialog(
            title = EditorRules.displayTitle(state.title, untitled),
            subtaskCount = state.subtasks.size,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteTask {
                    viewModel.flush()
                    onBack()
                }
            },
        )
    }
}

@Composable
private fun EditorTopBar(
    onBack: () -> Unit,
    onDelete: () -> Unit,
    showOverflow: Boolean,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
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
        if (showOverflow) {
            Box {
                IconButton(
                    onClick = { menuOpen = true },
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
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.editor_delete)) },
                        onClick = {
                            menuOpen = false
                            onDelete()
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
private fun EditorContent(
    state: EditorDraft,
    subjects: List<com.example.notally.data.model.Subject>,
    newSubtask: String,
    onNewSubtaskChange: (String) -> Unit,
    subtaskFocused: Boolean,
    subtaskFocus: FocusRequester,
    onSubtaskFocusChange: (Boolean) -> Unit,
    onAddSubtask: () -> Unit,
    onToggleSubtask: (EditorSubtask) -> Unit,
    onDeleteSubtask: (EditorSubtask) -> Unit,
    onAddClick: () -> Unit,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onOpenProperty: (EditorSheet) -> Unit,
    margin: androidx.compose.ui.unit.Dp,
    imeVisible: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val is24Hour = remember(context) { AndroidDateFormat.is24HourFormat(context) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val titleLabel = stringResource(R.string.editor_title_placeholder)
    val notesLabel = stringResource(R.string.editor_notes_placeholder)

    val subjectName = subjects.firstOrNull { it.id == state.subjectId }?.name
        ?: stringResource(R.string.task_noSubject)
    val dueText = state.dueAt?.let { dueAt ->
        val date = dateLabelText(dueAt, zone = zone)
        if (DateFormat.hasTimeOfDay(dueAt, zone)) {
            "$date, ${DateFormat.formatTime(dueAt, is24Hour, zone)}"
        } else {
            date
        }
    } ?: stringResource(R.string.task_noDate)
    val priorityText = stringResource(
        when (state.priority) {
            Priority.HIGH -> R.string.priority_high
            Priority.MEDIUM -> R.string.priority_medium
            Priority.LOW -> R.string.priority_low
        },
    )
    val statusText = stringResource(
        when (state.status) {
            Status.PENDING -> R.string.state_pending
            Status.IN_PROGRESS -> R.string.state_doing
            Status.DONE -> R.string.state_done
        },
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = NotallySpacing.maxContentWidth)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = margin)
                .padding(bottom = if (imeVisible) 112.dp else 48.dp),
        ) {
        BasicTextField(
            value = state.title,
            onValueChange = onTitleChange,
            textStyle = NotallyType.editorTitle.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.tertiary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Default,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = titleLabel },
            decorationBox = { innerTextField ->
                Box {
                    if (state.title.isEmpty()) {
                        Text(
                            text = stringResource(R.string.editor_title_placeholder),
                            style = NotallyType.editorTitle,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
            },
        )
        Spacer(Modifier.height(16.dp))
        EditorPropertyRow(
            label = stringResource(R.string.editor_property_subject),
            value = subjectName,
            onClick = { onOpenProperty(EditorSheet.SUBJECT) },
        )
        EditorPropertyRow(
            label = stringResource(R.string.editor_property_due),
            value = dueText,
            onClick = { onOpenProperty(EditorSheet.DUE) },
        )
        EditorPropertyRow(
            label = stringResource(R.string.editor_property_priority),
            value = priorityText,
            showErrorDot = state.priority == Priority.HIGH,
            onClick = { onOpenProperty(EditorSheet.PRIORITY) },
        )
        EditorPropertyRow(
            label = stringResource(R.string.editor_property_state),
            value = statusText,
            onClick = { onOpenProperty(EditorSheet.STATUS) },
        )

        Spacer(Modifier.height(24.dp))
        BasicTextField(
            value = state.notes,
            onValueChange = onNotesChange,
            textStyle = NotallyType.editorBody.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.tertiary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .semantics { contentDescription = notesLabel },
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.heightIn(min = 120.dp)) {
                    if (state.notes.isEmpty()) {
                        Text(
                            text = stringResource(R.string.editor_notes_placeholder),
                            style = NotallyType.editorBody,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
            },
        )

        Spacer(Modifier.height(24.dp))
        SubtasksSection(
            subtasks = state.subtasks,
            newSubtask = newSubtask,
            onNewSubtaskChange = onNewSubtaskChange,
            subtaskFocused = subtaskFocused,
            subtaskFocus = subtaskFocus,
            onSubtaskFocusChange = onSubtaskFocusChange,
            onAddClick = onAddClick,
            onToggle = onToggleSubtask,
            onDelete = onDeleteSubtask,
            onDone = {
                if (newSubtask.isBlank()) {
                    focusManager.clearFocus()
                    keyboard?.hide()
                } else {
                    onAddSubtask()
                    subtaskFocus.requestFocus()
                }
            },
        )
        }
    }
}

@Composable
private fun EditorPropertyRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showErrorDot: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val compact = LocalConfiguration.current.fontScale >= 1.3f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val background by animateColorAsState(
        targetValue = if (pressed) {
            colors.surfaceContainerLow
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        },
        animationSpec = tween(NotallyMotion.colorMs),
        label = "editorPropertyBackground",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .focusRing(visible = focused, shape = NotallyShape.s)
            .clip(NotallyShape.s)
            .background(background)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (compact) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                    )
                    if (showErrorDot) {
                        Spacer(Modifier.width(6.dp))
                        ErrorDot()
                    }
                }
            }
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.width(96.dp),
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                )
                if (showErrorDot) {
                    Spacer(Modifier.width(6.dp))
                    ErrorDot()
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun ErrorDot() {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error),
    )
}

@Composable
private fun SubtasksSection(
    subtasks: List<EditorSubtask>,
    newSubtask: String,
    onNewSubtaskChange: (String) -> Unit,
    subtaskFocused: Boolean,
    subtaskFocus: FocusRequester,
    onSubtaskFocusChange: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onToggle: (EditorSubtask) -> Unit,
    onDelete: (EditorSubtask) -> Unit,
    onDone: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.editor_subtasks),
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurface,
        )
        if (subtasks.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    R.string.editor_subtasks_progress,
                    subtasks.count { it.isDone },
                    subtasks.size,
                ),
                style = NotallyType.numeric,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        val addLabel = stringResource(R.string.editor_subtasks_add)
        TextButton(
            onClick = onAddClick,
            modifier = Modifier.semantics { contentDescription = addLabel },
        ) {
            Text(stringResource(R.string.action_add))
        }
    }

    subtasks.forEach { subtask ->
        SubtaskRow(
            subtask = subtask,
            onToggle = { onToggle(subtask) },
            onDelete = { onDelete(subtask) },
        )
    }

    BasicTextField(
        value = newSubtask,
        onValueChange = onNewSubtaskChange,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.tertiary),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(subtaskFocus)
            .onFocusChanged { onSubtaskFocusChange(it.isFocused) }
            .drawBehind {
                if (subtaskFocused) {
                    val stroke = 1.dp.toPx()
                    drawLine(
                        color = colors.outlineVariant,
                        start = Offset(0f, size.height - stroke / 2f),
                        end = Offset(size.width, size.height - stroke / 2f),
                        strokeWidth = stroke,
                    )
                }
            },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (newSubtask.isEmpty()) {
                    Text(
                        text = stringResource(R.string.editor_subtasks_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun SubtaskRow(
    subtask: EditorSubtask,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val label = stringResource(
        if (subtask.isDone) R.string.a11y_uncompleteTask else R.string.a11y_completeTask,
        subtask.title,
    )
    val stateLabel = stringResource(
        if (subtask.isDone) R.string.state_done else R.string.state_pending,
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
                    onClickLabel = label,
                    onClick = onToggle,
                )
                .semantics {
                    contentDescription = label
                    stateDescription = stateLabel
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (subtask.isDone) colors.primary else androidx.compose.ui.graphics.Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (subtask.isDone) colors.primary else colors.outline,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (subtask.isDone) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (subtask.isDone) colors.onSurfaceVariant else colors.onSurface,
            textDecoration = if (subtask.isDone) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.a11y_deleteSubtask),
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun EditorToolbar(
    onSubtask: () -> Unit,
    onDue: () -> Unit,
    onPriority: () -> Unit,
) {
    val compact = LocalConfiguration.current.fontScale >= 1.5f
    Surface(
        shape = NotallyShape.l,
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
            ToolbarAction(
                iconRes = R.drawable.ic_checklist,
                label = stringResource(R.string.editor_toolbar_subtask),
                compact = compact,
                onClick = onSubtask,
            )
            ToolbarAction(
                iconRes = R.drawable.ic_calendar,
                label = stringResource(R.string.editor_toolbar_date),
                compact = compact,
                onClick = onDue,
            )
            ToolbarAction(
                iconRes = R.drawable.ic_flag,
                label = stringResource(R.string.editor_property_priority),
                compact = compact,
                onClick = onPriority,
            )
        }
    }
}

@Composable
private fun ToolbarAction(
    iconRes: Int,
    label: String,
    compact: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(NotallyShape.s)
            .clickable(role = Role.Button, onClick = onClick)
            .then(
                if (compact) {
                    Modifier.semantics { contentDescription = label }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        if (!compact) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DeleteTaskDialog(
    title: String,
    subtaskCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val cancelFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { cancelFocus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
        shape = NotallyShape.xl,
        containerColor = colors.surfaceContainerLowest,
        title = {
            Text(
                text = stringResource(R.string.confirm_deleteTask_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
            )
        },
        text = {
            Text(
                text = if (subtaskCount == 0) {
                    stringResource(R.string.confirm_deleteTask_bodyNoSub, title)
                } else {
                    pluralStringResource(
                        R.plurals.confirm_deleteTask_body,
                        subtaskCount,
                        title,
                        subtaskCount,
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
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
                colors = ButtonDefaults.textButtonColors(contentColor = colors.error),
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
    )
}
