package com.example.notally.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
import com.example.notally.ui.components.DayStrip
import com.example.notally.ui.components.DayStripDay
import com.example.notally.ui.components.EmptyState
import com.example.notally.ui.components.ErrorBanner
import com.example.notally.ui.components.ErrorBlock
import com.example.notally.ui.components.FilterChipRow
import com.example.notally.ui.components.FilterOption
import com.example.notally.ui.components.FocalTaskCard
import com.example.notally.ui.components.LoadingRows
import com.example.notally.ui.components.SectionHeader
import com.example.notally.ui.components.SwipeTaskRow
import com.example.notally.ui.components.focusRing
import com.example.notally.ui.theme.NotallyMotion
import com.example.notally.ui.theme.NotallySpacing
import com.example.notally.ui.theme.NotallyType
import com.example.notally.ui.theme.screenHorizontalMargin
import com.example.notally.ui.util.DateFormat
import com.example.notally.ui.util.dateLabelText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

    val todayDate = remember(state.now) {
        Instant.ofEpochMilli(state.now).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val stripDays = remember(state.weekDays, todayDate) {
        state.weekDays.map { day ->
            val sectionRes = when {
                day.pendingCount == 0 -> null
                day.date.isBefore(todayDate) -> R.string.today_section_overdue
                day.date == todayDate -> R.string.today_section_today
                else -> R.string.today_section_upcoming
            }
            DayStripDay(
                date = day.date,
                pendingCount = day.pendingCount,
                isToday = day.date == todayDate,
                sectionLabelRes = sectionRes,
            )
        }
    }
    val showStrip = state.hasCachedData && !state.loading &&
        state.filter != TodayFilter.DONE &&
        stripDays.any { it.pendingCount > 0 }

    // La selección vive en la pantalla: se conserva hasta tocar otro día o
    // cambiar de pestaña, y nunca altera secciones, conteos ni resumen.
    var selectedDayEpoch by remember { mutableStateOf<Long?>(null) }
    val selectedDate = selectedDayEpoch?.let(LocalDate::ofEpochDay)

    val focalEligible = state.filter != TodayFilter.DONE && !showLoading &&
        !(state.error && !state.hasCachedData) && state.empty == null
    val focal = if (focalEligible) state.focal else null
    val showFocal = focal != null
    val sectionsBaseIndex = 1 +
        (if (state.error && state.hasCachedData) 1 else 0) +
        (if (showStrip) 1 else 0) +
        (if (!state.isEmptyTotal) 1 else 0) +
        (if (showFocal) 1 else 0)

    // Destino de la tira: se guarda la intención y el efecto hace el salto
    // inmediato cuando la sección destino ya está compuesta.
    var pendingJump by remember { mutableStateOf<DayJump?>(null) }
    var viewportTop by remember { mutableStateOf(0f) }
    val rowCoordinates = remember { mutableMapOf<Long, LayoutCoordinates>() }

    LaunchedEffect(pendingJump, state.sections, sectionsBaseIndex, focal?.taskId) {
        val jump = pendingJump ?: return@LaunchedEffect
        if (jump.expandUpcoming) {
            val upcoming = state.sections.firstOrNull {
                it.type == TodaySectionType.UPCOMING
            }
            if (upcoming != null && !upcoming.expanded) {
                viewModel.toggleSection(TodaySectionType.UPCOMING)
                return@LaunchedEffect
            }
        }

        fun sectionItemId(type: TodaySectionType): Long? =
            state.sections.firstOrNull { it.type == type }?.items
                ?.firstOrNull { item ->
                    item.task.dueAt?.let {
                        DateFormat.localDate(it) == jump.dueDate
                    } == true
                }
                ?.task
                ?.id

        var targetSection: TodaySectionType? = jump.section
        var targetTaskId = sectionItemId(jump.section)
        if (jump.section == TodaySectionType.TODAY && targetTaskId == null) {
            // "Hoy" sin filas propias baja a la primera de "Sin hora" (§7.1.1).
            targetSection = TodaySectionType.ANYTIME
            targetTaskId = state.sections
                .firstOrNull { it.type == TodaySectionType.ANYTIME }
                ?.items
                ?.firstOrNull()
                ?.task
                ?.id
        }
        val focalMatches = focal?.let { item ->
            (item.bucket == targetSection || item.bucket == jump.section) &&
                item.item.task.dueAt?.let {
                    DateFormat.localDate(it) == jump.dueDate
                } == true
        } == true
        if (targetTaskId == null && focalMatches) {
            targetSection = null
            targetTaskId = focal?.taskId
        }
        val sectionIndex = targetSection?.let { type ->
            val position = state.sections.indexOfFirst { it.type == type }
            if (position >= 0) sectionsBaseIndex + position else null
        }
        val index = when {
            targetSection == null -> if (showFocal) sectionsBaseIndex - 1 else 0
            sectionIndex != null -> sectionIndex
            // Sin sección ni fila destino: el encabezado (§7.1.1).
            else -> 0
        }

        listState.scrollToItem(index)
        withFrameNanos { }
        withFrameNanos { }
        val coordinates = targetTaskId?.let(rowCoordinates::get)
        if (coordinates != null) {
            val delta = coordinates.positionInRoot().y - viewportTop
            if (delta != 0f) listState.scrollBy(delta)
        }
        pendingJump = null
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

    val showFab = fabVisible && state.empty != TodayEmpty.Total && !state.loading &&
        !(state.error && !state.hasCachedData)
    var fabFocused by remember { mutableStateOf(false) }

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
                    modifier = Modifier
                        .size(52.dp)
                        .onFocusChanged { fabFocused = it.isFocused }
                        .focusRing(visible = fabFocused, shape = CircleShape),
                    shape = CircleShape,
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
                    .fillMaxWidth()
                    .onGloballyPositioned { viewportTop = it.positionInRoot().y },
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

                if (showStrip) {
                    item(key = "day_strip") {
                        DayStrip(
                            days = stripDays,
                            selectedDate = selectedDate,
                            onDayClick = { day ->
                                selectedDayEpoch = day.date.toEpochDay()
                                pendingJump = resolveDayJump(day, todayDate)
                            },
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }

                if (!state.isEmptyTotal) {
                    item(key = "filters") {
                        FilterChipRow(
                            options = filterOptions(state.filterCounts),
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

                if (focal != null) {
                    item(key = "focal_task") {
                        AnimatedContent(
                            targetState = focal,
                            transitionSpec = {
                                fadeIn(
                                    animationSpec = tween(
                                        durationMillis = NotallyMotion.fadeThroughMs / 2,
                                        delayMillis = NotallyMotion.fadeThroughMs / 2,
                                        easing = NotallyMotion.fadeThroughEasing,
                                    ),
                                ) togetherWith fadeOut(
                                    animationSpec = tween(
                                        durationMillis = NotallyMotion.fadeThroughMs / 2,
                                        easing = NotallyMotion.fadeThroughEasing,
                                    ),
                                )
                            },
                            label = "focalTask",
                            modifier = Modifier.padding(top = 24.dp),
                        ) { target ->
                            val task = target.item.task
                            FocalTaskCard(
                                title = task.title,
                                eyebrow = sectionTitle(target.bucket),
                                subjectText = target.item.subject?.name
                                    ?: stringResource(R.string.task_noSubject),
                                dueText = task.dueAt?.let {
                                    dateLabelText(it, state.now)
                                } ?: stringResource(R.string.task_noDate),
                                priority = task.priority,
                                modifier = Modifier.onGloballyPositioned {
                                    rowCoordinates[target.taskId] = it
                                },
                                onClick = { onOpenTask(task.id) },
                            )
                        }
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
                                        modifier = Modifier.onGloballyPositioned {
                                            rowCoordinates[item.task.id] = it
                                        },
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        state.sections.forEach { section ->
                            item(key = "section_${section.type.name}") {
                                Column(
                                    modifier = Modifier
                                        .animateItem(
                                            placementSpec = tween(NotallyMotion.itemMoveMs),
                                        )
                                        .animateContentSize(NotallyMotion.spatialDefault())
                                        .padding(top = 24.dp),
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
                                                    modifier = Modifier.onGloballyPositioned {
                                                        rowCoordinates[item.task.id] = it
                                                    },
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
    modifier: Modifier = Modifier,
) {
    SwipeTaskRow(
        task = item.task,
        subjectName = item.subject?.name,
        subjectColorArgb = item.subject?.colorArgb,
        now = now,
        modifier = modifier,
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
private fun filterOptions(counts: TodayFilterCounts): List<FilterOption> = listOf(
    FilterOption("all", stringResource(R.string.filter_all), counts.all),
    FilterOption("pending", stringResource(R.string.filter_pending), counts.pending),
    FilterOption("done", stringResource(R.string.filter_done), counts.done),
)

/** Destino de la tira de días: sección existente, no un filtro (§7.1.1). */
private data class DayJump(
    val section: TodaySectionType,
    val dueDate: LocalDate,
    val expandUpcoming: Boolean,
)

private fun resolveDayJump(day: DayStripDay, today: LocalDate): DayJump = when {
    day.date.isBefore(today) ->
        DayJump(TodaySectionType.OVERDUE, day.date, expandUpcoming = false)
    day.date == today ->
        DayJump(TodaySectionType.TODAY, day.date, expandUpcoming = false)
    else ->
        DayJump(TodaySectionType.UPCOMING, day.date, expandUpcoming = true)
}

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
