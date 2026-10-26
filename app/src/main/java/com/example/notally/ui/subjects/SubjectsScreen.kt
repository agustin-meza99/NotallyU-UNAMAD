package com.example.notally.ui.subjects

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notally.R
import com.example.notally.data.model.Subject
import com.example.notally.ui.components.EmptyState
import com.example.notally.ui.components.ErrorBanner
import com.example.notally.ui.components.ErrorBlock
import com.example.notally.ui.components.LoadingRows
import com.example.notally.ui.components.focusRing
import com.example.notally.ui.theme.NotallyMotion
import com.example.notally.ui.theme.NotallyShape
import com.example.notally.ui.theme.NotallySpacing
import com.example.notally.ui.theme.NotallyType
import com.example.notally.ui.theme.screenHorizontalMargin
import com.example.notally.ui.theme.subjectColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pantalla Materias (§7.4, §7.8, §7.13).
 *
 * La tarjeta es la única acción: abre la hoja "Editar materia". No hay ruta de
 * detalle de materia en V1 (§0.5). Los conteos salen de Room; "entregas esta
 * semana" se recalcula con el ajuste de inicio de semana.
 */
@Composable
fun SubjectsScreen(
    modifier: Modifier = Modifier,
    viewModel: SubjectsViewModel = viewModel(
        factory = SubjectsViewModel.factory(LocalContext.current),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val formError by viewModel.formError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val margin = screenHorizontalMargin

    var sheetTarget by remember { mutableStateOf<SubjectSheetTarget?>(null) }

    val deleteError = stringResource(R.string.subject_delete_error)
    val retryLabel = stringResource(R.string.action_retry)
    val errorLoad = stringResource(R.string.error_loadSubjects)
    val errorRefresh = stringResource(R.string.error_refresh)

    LaunchedEffect(viewModel, deleteError, retryLabel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SubjectsEvent.Failed -> {
                    val result = withTimeoutOrNull(NotallyMotion.snackbarMs) {
                        snackbarHostState.showSnackbar(
                            message = deleteError,
                            actionLabel = retryLabel,
                            duration = SnackbarDuration.Indefinite,
                        )
                    }
                    if (result == SnackbarResult.ActionPerformed) event.retry()
                }
            }
        }
    }

    // Carga: solo si Room tarda más de 150 ms (§7.13). Antes de ese umbral no
    // se pinta nada, así el estado vacío no parpadea.
    val showLoading by produceState(
        initialValue = false,
        key1 = state.loading,
        key2 = state.hasCachedData,
    ) {
        value = if (state.loading && !state.hasCachedData) {
            delay(150)
            true
        } else {
            false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = NotallySpacing.maxContentWidth)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = margin,
                    end = margin,
                    top = 24.dp,
                    bottom = 24.dp,
                ),
            ) {
                item(key = "header") {
                    SubjectsHeader(
                        subjectCount = state.subjects.size,
                        pendingTotal = state.pendingTotal,
                        weeklyDeliveries = state.weeklyDeliveries,
                        showCounts = state.hasCachedData && state.subjects.isNotEmpty(),
                        showAdd = state.subjects.isNotEmpty(),
                        onAdd = {
                            viewModel.clearFormError()
                            sheetTarget = SubjectSheetTarget.New
                        },
                    )
                }

                if (state.error && state.hasCachedData) {
                    item(key = "error_banner") {
                        ErrorBanner(
                            message = errorRefresh,
                            retryLabel = retryLabel,
                            onRetry = viewModel::retry,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }

                when {
                    state.loading && !state.hasCachedData -> {
                        if (showLoading) {
                            item(key = "loading") {
                                LoadingRows(modifier = Modifier.padding(top = 24.dp))
                            }
                        }
                    }

                    state.error && !state.hasCachedData -> {
                        item(key = "error_block") {
                            ErrorBlock(
                                title = errorLoad,
                                retryLabel = retryLabel,
                                onRetry = viewModel::retry,
                                modifier = Modifier.padding(top = 48.dp),
                            )
                        }
                    }

                    state.subjects.isEmpty() -> {
                        item(key = "empty") {
                            EmptyState(
                                title = stringResource(R.string.subjects_empty_title),
                                body = stringResource(R.string.subjects_empty_body),
                                actionLabel = stringResource(R.string.subjects_empty_cta),
                                onAction = {
                                    viewModel.clearFormError()
                                    sheetTarget = SubjectSheetTarget.New
                                },
                                modifier = Modifier.padding(top = 48.dp),
                            )
                        }
                    }

                    else -> {
                        items(
                            items = state.subjects,
                            key = { it.subject.id },
                        ) { card ->
                            SubjectCard(
                                card = card,
                                onClick = {
                                    viewModel.clearFormError()
                                    sheetTarget = SubjectSheetTarget.Edit(card)
                                },
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    val target = sheetTarget
    if (target != null) {
        SubjectSheet(
            target = target,
            defaultColorArgb = viewModel.defaultColorArgb(state.subjects.size),
            formError = formError,
            onDismiss = {
                viewModel.clearFormError()
                sheetTarget = null
            },
            onSave = { existing: Subject?, name: String, colorArgb: Long ->
                viewModel.saveSubject(existing, name, colorArgb)
            },
            onSaved = {
                viewModel.clearFormError()
                sheetTarget = null
            },
            onDelete = { subject ->
                viewModel.deleteSubject(subject) {
                    viewModel.clearFormError()
                    sheetTarget = null
                }
            },
            onClearError = viewModel::clearFormError,
        )
    }
}

@Composable
private fun SubjectsHeader(
    subjectCount: Int,
    pendingTotal: Int,
    weeklyDeliveries: Int,
    showCounts: Boolean,
    showAdd: Boolean,
    onAdd: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.nav_materias),
                style = MaterialTheme.typography.displaySmall,
                color = colors.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            if (showCounts) {
                val pendingText = pluralStringResource(
                    R.plurals.subjects_pending,
                    pendingTotal,
                    pendingTotal,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.subjects_summary,
                        subjectCount,
                        subjectCount,
                        pendingText,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.subjects_week,
                        weeklyDeliveries,
                        weeklyDeliveries,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        if (showAdd) {
            IconButton(onClick = onAdd) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.subjects_new),
                    tint = colors.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun SubjectCard(
    card: SubjectCardUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val accent = subjectColor(card.subject.colorArgb)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }

    val container by animateColorAsState(
        targetValue = if (pressed) colors.surfaceContainerLow else colors.surface,
        animationSpec = tween(NotallyMotion.colorMs),
        label = "subjectCardContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = if (pressed) colors.outline else colors.outlineVariant,
        animationSpec = tween(NotallyMotion.colorMs),
        label = "subjectCardBorder",
    )

    val complete = card.totalTasks > 0 && card.doneTasks == card.totalTasks
    val completeText = if (complete) {
        pluralStringResource(
            R.plurals.subject_progress_complete,
            card.totalTasks,
            card.doneTasks,
            card.totalTasks,
        )
    } else {
        null
    }
    val stats = when {
        card.totalTasks == 0 -> stringResource(R.string.subject_noTasks)
        completeText != null -> completeText
        else -> {
            val tasks = pluralStringResource(
                R.plurals.subject_tasks,
                card.totalTasks,
                card.totalTasks,
            )
            val done = pluralStringResource(
                R.plurals.subject_done,
                card.doneTasks,
                card.doneTasks,
            )
            "$tasks${stringResource(R.string.task_meta_separator)}$done"
        }
    }
    val progressDescription = when {
        card.totalTasks == 0 -> stringResource(R.string.subject_noTasks)
        completeText != null -> completeText
        else -> pluralStringResource(
            R.plurals.subject_progress,
            card.totalTasks,
            card.doneTasks,
            card.totalTasks,
        )
    }
    val openLabel = stringResource(R.string.a11y_editSubject)
    val description = "${card.subject.name}. $progressDescription."

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .focusRing(visible = focused, shape = NotallyShape.l)
            .onFocusChanged { focused = it.isFocused }
            .clip(NotallyShape.l)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = openLabel,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = description
            },
        shape = NotallyShape.l,
        color = container,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stats,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressBar(
                        fraction = if (card.totalTasks == 0) {
                            0f
                        } else {
                            card.doneTasks.toFloat() / card.totalTasks
                        },
                        color = accent,
                        modifier = Modifier.weight(1f),
                    )
                    if (card.totalTasks > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                R.string.subject_progress_ratio,
                                card.doneTasks,
                                card.totalTasks,
                            ),
                            style = NotallyType.numeric,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(2.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
