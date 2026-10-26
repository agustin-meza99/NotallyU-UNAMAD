package com.example.notally.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notally.R
import com.example.notally.ui.components.EmptyState
import com.example.notally.ui.components.ErrorBanner
import com.example.notally.ui.components.ErrorBlock
import com.example.notally.ui.components.FilterChipRow
import com.example.notally.ui.components.FilterOption
import com.example.notally.ui.components.LoadingRows
import com.example.notally.ui.components.SectionHeader
import com.example.notally.ui.components.SwipeTaskRow
import com.example.notally.ui.theme.NotallyMotion
import com.example.notally.ui.theme.NotallyShape
import com.example.notally.ui.theme.NotallySpacing
import com.example.notally.ui.theme.NotallyType
import com.example.notally.ui.theme.screenHorizontalMargin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pantalla Hoy (§7.1–7.3, §7.7, §7.8, §7.10, §7.13).
 *
 * @param onCreateTask abre el editor (la fase 4 conecta la navegación real).
 * @param onOpenTask recibe el id de la tarea para abrir el editor.
 */
@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    onCreateTask: () -> Unit = {},
    onOpenTask: (Long) -> Unit = {},
    viewModel: TodayViewModel = viewModel(
        factory = TodayViewModel.factory(LocalContext.current),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val emptyState = state.empty
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val margin = screenHorizontalMargin

    val completedMessage = stringResource(R.string.task_completed)
    val deletedMessage = stringResource(R.string.task_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    val saveError = stringResource(R.string.editor_saving_error)
    val retryLabel = stringResource(R.string.action_retry)
    val errorLoad = stringResource(R.string.error_load)
    val errorRefresh = stringResource(R.string.error_refresh)

    // Snackbars de 5 s exactos: Indefinite + timeout, con acción real.
    LaunchedEffect(viewModel, completedMessage, deletedMessage, undoLabel, saveError, retryLabel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is TodayEvent.Completed -> {
                    val result = withTimeoutOrNull(NotallyMotion.snackbarMs) {
                        snackbarHostState.showSnackbar(
                            message = completedMessage,
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Indefinite,
                        )
                    }
                    if (result == SnackbarResult.ActionPerformed) viewModel.undo(event.undo)
                }

                is TodayEvent.Deleted -> {
                    val result = withTimeoutOrNull(NotallyMotion.snackbarMs) {
                        snackbarHostState.showSnackbar(
                            message = deletedMessage,
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Indefinite,
                        )
                    }
                    if (result == SnackbarResult.ActionPerformed) viewModel.undo(event.undo)
                }

                is TodayEvent.SaveFailed -> {
                    val result = withTimeoutOrNull(NotallyMotion.snackbarMs) {
                        snackbarHostState.showSnackbar(
                            message = saveError,
                            actionLabel = retryLabel,
                            duration = SnackbarDuration.Indefinite,
                        )
                    }
                    if (result == SnackbarResult.ActionPerformed) event.retry()
                }
            }
        }
    }

    // Carga: solo si Room tarda más de 150 ms; antes no se pinta nada.
    var showLoading by remember { mutableStateOf(false) }
    LaunchedEffect(state.loading, state.hasCachedData) {
        if (state.loading && !state.hasCachedData) {
            delay(150)
            showLoading = true
        } else {
            showLoading = false
        }
    }

    // FAB: se oculta al bajar y reaparece al subir (fade + escala 150 ms).
    var fabVisible by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        var previousIndex = 0
        var previousOffset = 0
        snapshotFlow {
            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                listState.isScrollInProgress,
            )
        }.collect { (index, offset, scrolling) ->
            if (scrolling) {
                val down = index > previousIndex ||
                    (index == previousIndex && offset > previousOffset)
                val up = index < previousIndex ||
                    (index == previousIndex && offset < previousOffset)
                if (down) fabVisible = false
                if (up) fabVisible = true
            }
            previousIndex = index
            previousOffset = offset
        }
    }

    val showFab = fabVisible && !state.isEmptyTotal && !state.loading &&
        !(state.error && !state.hasCachedData)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = fadeIn(tween(NotallyMotion.fabMs)) +
                    scaleIn(
                        animationSpec = tween(NotallyMotion.fabMs),
                        initialScale = 0.9f,
                    ),
                exit = fadeOut(tween(NotallyMotion.fabMs)) +
                    scaleOut(
                        animationSpec = tween(NotallyMotion.fabMs),
                        targetScale = 0.9f,
                    ),
            ) {
                FloatingActionButton(
                    onClick = onCreateTask,
                    modifier = Modifier.size(56.dp),
                    shape = NotallyShape.l,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 2.dp,
                        focusedElevation = 2.dp,
                        hoveredElevation = 2.dp,
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.a11y_newTask),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = NotallySpacing.maxContentWidth)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = margin,
                    end = margin,
                    top = 24.dp,
                    bottom = 96.dp,
                ),
            ) {
                item(key = "header") {
                    TodayHeader(
                        eyebrow = state.eyebrow,
                        summary = state.summary,
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

                if (!state.isEmptyTotal) {
                    item(key = "filters") {
                        FilterChipRow(
                            options = filterOptions(),
                            selectedId = state.filter.id,
                            onSelect = { id ->
                                viewModel.setFilter(
                                    when (id) {
                                        "pending" -> TodayFilter.PENDING
                                        "done" -> TodayFilter.DONE
                                        else -> TodayFilter.ALL
                                    },
                                )
                            },
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }

                when {
                    showLoading -> {
                        item(key = "loading") {
                            LoadingRows(modifier = Modifier.padding(top = 24.dp))
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

                    emptyState != null -> {
                        item(key = "empty") {
                            EmptyStateFor(
                                empty = emptyState,
                                onCreateTask = onCreateTask,
                                onShowAll = { viewModel.setFilter(TodayFilter.ALL) },
                                onShowPending = { viewModel.setFilter(TodayFilter.PENDING) },
                                modifier = Modifier.padding(top = 48.dp),
                            )
                        }
                    }

                    state.filter == TodayFilter.DONE -> {
                        item(key = "flat_completed") {
                            Column(
                                modifier = Modifier
                                    .animateItem(
                                        placementSpec = tween(NotallyMotion.itemMoveMs),
                                    )
                                    .animateContentSize(NotallyMotion.spatialDefault())
                                    .padding(top = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                state.flatCompleted.forEach { item ->
                                    TaskItem(
                                        item = item,
                                        now = state.now,
                                        viewModel = viewModel,
                                        onOpenTask = onOpenTask,
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        state.sections.forEachIndexed { index, section ->
                            item(key = "section_${section.type.name}") {
                                Column(
                                    modifier = Modifier
                                        .animateItem(
                                            placementSpec = tween(NotallyMotion.itemMoveMs),
                                        )
                                        .animateContentSize(NotallyMotion.spatialDefault())
                                        .padding(top = if (index == 0) 32.dp else 24.dp),
                                ) {
                                    SectionHeader(
                                        title = sectionTitle(section.type),
                                        count = section.count,
                                        collapsible = section.collapsible,
                                        expanded = section.expanded,
                                        onToggle = {
                                            viewModel.toggleSection(section.type)
                                        },
                                    )
                                    if (section.expanded) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            section.items.forEach { item ->
                                                TaskItem(
                                                    item = item,
                                                    now = state.now,
                                                    viewModel = viewModel,
                                                    onOpenTask = onOpenTask,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayHeader(
    eyebrow: String,
    summary: TodaySummary?,
) {
    val colors = MaterialTheme.colorScheme
    Column {
        if (eyebrow.isNotEmpty()) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = stringResource(R.string.a11y_today_heading),
            style = MaterialTheme.typography.displaySmall,
            color = colors.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        if (summary != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = pluralStringResource(
                    R.plurals.today_summary,
                    summary.total,
                    summary.completed,
                    summary.total,
                ),
                style = NotallyType.numeric,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TaskItem(
    item: TodayItem,
    now: Long,
    viewModel: TodayViewModel,
    onOpenTask: (Long) -> Unit,
) {
    SwipeTaskRow(
        task = item.task,
        subjectName = item.subject?.name,
        subjectColorArgb = item.subject?.colorArgb,
        now = now,
        onClick = { onOpenTask(item.task.id) },
        onToggleDone = { viewModel.toggleDone(item.task) },
        onComplete = { viewModel.complete(item.task) },
        onPostpone = { viewModel.postponeToTomorrow(item.task) },
        onDelete = { viewModel.delete(item.task) },
    )
}

@Composable
private fun EmptyStateFor(
    empty: TodayEmpty,
    onCreateTask: () -> Unit,
    onShowAll: () -> Unit,
    onShowPending: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (empty) {
        TodayEmpty.Total -> EmptyState(
            title = stringResource(R.string.today_empty_title),
            body = stringResource(R.string.today_empty_body),
            actionLabel = stringResource(R.string.today_empty_cta),
            onAction = onCreateTask,
            modifier = modifier,
        )

        TodayEmpty.PendingFilter -> EmptyState(
            title = stringResource(R.string.filter_empty_pending_title),
            body = stringResource(R.string.filter_empty_pending_body),
            actionLabel = stringResource(R.string.filter_empty_pending_cta),
            onAction = onShowAll,
            modifier = modifier,
        )

        TodayEmpty.DoneFilter -> EmptyState(
            title = stringResource(R.string.filter_empty_done_title),
            body = stringResource(R.string.filter_empty_done_body),
            actionLabel = stringResource(R.string.filter_empty_done_cta),
            onAction = onShowPending,
            modifier = modifier,
        )
    }
}

private val TodayFilter.id: String
    get() = when (this) {
        TodayFilter.ALL -> "all"
        TodayFilter.PENDING -> "pending"
        TodayFilter.DONE -> "done"
    }

@Composable
private fun filterOptions(): List<FilterOption> = listOf(
    FilterOption("all", stringResource(R.string.filter_all)),
    FilterOption("pending", stringResource(R.string.filter_pending)),
    FilterOption("done", stringResource(R.string.filter_done)),
)

@Composable
private fun sectionTitle(type: TodaySectionType): String = stringResource(
    when (type) {
        TodaySectionType.OVERDUE -> R.string.today_section_overdue
        TodaySectionType.TODAY -> R.string.today_section_today
        TodaySectionType.ANYTIME -> R.string.today_section_anytime
        TodaySectionType.UPCOMING -> R.string.today_section_upcoming
        TodaySectionType.COMPLETED -> R.string.today_section_done
    },
)
