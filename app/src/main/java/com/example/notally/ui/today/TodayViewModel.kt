package com.example.notally.ui.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.notally.notallyContainer
import com.example.notally.data.DateRanges
import com.example.notally.data.model.Status
import com.example.notally.data.model.Subject
import com.example.notally.data.model.Task
import com.example.notally.data.repository.SubjectRepository
import com.example.notally.data.repository.SettingsRepository
import com.example.notally.data.repository.TaskRepository
import com.example.notally.ui.util.DateFormat
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Resumen del día: "3 de 7 completadas". */
data class TodaySummary(
    val completed: Int,
    val total: Int,
)

/** Estado vacío que corresponde pintar. */
sealed interface TodayEmpty {
    data object Total : TodayEmpty
    data object PendingFilter : TodayEmpty
    data object DoneFilter : TodayEmpty
}

data class TodayUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val hasCachedData: Boolean = false,
    val filter: TodayFilter = TodayFilter.ALL,
    val eyebrow: String = "",
    val now: Long = System.currentTimeMillis(),
    val sections: List<TodaySectionUi> = emptyList(),
    val flatCompleted: List<TodayItem> = emptyList(),
    val weekDays: List<TodayWeekDay> = emptyList(),
    val focal: TodayFocalTask? = null,
    val filterCounts: TodayFilterCounts = TodayFilterCounts(),
    val summary: TodaySummary? = null,
    val empty: TodayEmpty? = null,
    val isEmptyTotal: Boolean = true,
)

/** Acción reversible que el snackbar puede deshacer. */
sealed interface TodayUndo {
    data class Completion(val task: Task, val previousStatus: Status) : TodayUndo
    data class Deletion(val task: Task, val subtaskTitles: List<String>) : TodayUndo
}

/** Mensajes de una sola vez para la pantalla. */
sealed interface TodayEvent {
    data class Completed(val undo: TodayUndo) : TodayEvent
    data class Deleted(val undo: TodayUndo) : TodayEvent
    data class SaveFailed(val retry: () -> Unit) : TodayEvent
}

/**
 * ViewModel de Hoy (§7.1–7.3, §7.13).
 *
 * Combina las consultas de Room con un reloj de un minuto: el rango de fechas
 * solo se recalcula cuando cambia el día, mientras que las etiquetas "Hoy" /
 * "Atrasada" se refrescan cada minuto. No usa Hilt.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val taskRepository: TaskRepository,
    private val subjectRepository: SubjectRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private data class RawData(
        val day: List<Task>,
        val upcoming: List<Task>,
        val active: List<Task>,
        val subjects: List<Subject>,
        val weekStartsMonday: Boolean,
    )

    private sealed interface LoadState {
        data object Loading : LoadState
        data class Ready(val raw: RawData) : LoadState
        data object Error : LoadState
    }

    private data class Aggregate(
        val raw: RawData? = null,
        val loading: Boolean = true,
        val error: Boolean = false,
    )

    private val refreshTrigger = MutableStateFlow(0)
    private val filterState = MutableStateFlow(TodayFilter.ALL)
    private val expansionOverrides =
        MutableStateFlow<Map<TodaySectionType, Boolean>>(emptyMap())

    private val _events = MutableSharedFlow<TodayEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<TodayEvent> = _events.asSharedFlow()

    private val clockFlow: Flow<Long> = flow {
        while (true) {
            val now = clock()
            emit(now)
            delay(60_000L - (now % 60_000L))
        }
    }

    private val todayStartFlow: Flow<Long> = clockFlow
        .map { DateRanges.dayRange(it, zone).first }
        .distinctUntilChanged()

    private val loadStates: Flow<LoadState> = refreshTrigger.flatMapLatest {
        todayStartFlow.flatMapLatest { start ->
            val today = Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
            val dayEnd = DateRanges.dayRange(today, zone).last
            // Último milisegundo de hoy: la consulta de futuro usa `>` y así
            // incluye todo lo que empieza mañana a las 00:00, sin tope superior.
            val tomorrowStart = DateRanges.dayRange(today.plusDays(1), zone).first
            val ready: Flow<LoadState> = combine(
                taskRepository.observeTasksForDay(start, dayEnd),
                taskRepository.observeFutureTasks(tomorrowStart - 1),
                taskRepository.observeActiveTasks(),
                subjectRepository.observeSubjects(),
                settingsRepository.settings,
            ) { day, upcoming, active, subjects, settings ->
                LoadState.Ready(
                    RawData(
                        day = day,
                        upcoming = upcoming,
                        active = active,
                        subjects = subjects,
                        weekStartsMonday = settings.weekStartsMonday,
                    ),
                )
            }
            ready
                .onStart { emit(LoadState.Loading) }
                .catch { emit(LoadState.Error) }
        }
    }

    private val aggregate: Flow<Aggregate> = loadStates.scan(Aggregate()) { acc, state ->
        when (state) {
            LoadState.Loading -> acc.copy(loading = true, error = false)
            is LoadState.Ready -> Aggregate(raw = state.raw, loading = false, error = false)
            LoadState.Error -> acc.copy(loading = false, error = true)
        }
    }

    val uiState: StateFlow<TodayUiState> = combine(
        aggregate,
        clockFlow,
        filterState,
        expansionOverrides,
    ) { aggregate, now, filter, overrides ->
        buildState(aggregate, now, filter, overrides)
    }
        .onEach { state ->
            // Con cero tareas no tiene sentido conservar "Pendientes" o
            // "Completadas"; se vuelve a "Todas" sin tocar el estado durante
            // la carga inicial (donde el vacío es solo transitorio).
            if (state.hasCachedData && state.isEmptyTotal &&
                filterState.value != TodayFilter.ALL
            ) {
                filterState.value = TodayFilter.ALL
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TodayUiState(),
        )

    fun setFilter(filter: TodayFilter) {
        filterState.value = filter
    }

    fun toggleSection(type: TodaySectionType) {
        val current = uiState.value.sections.firstOrNull { it.type == type }?.expanded ?: return
        expansionOverrides.update { it + (type to !current) }
    }

    fun retry() {
        refreshTrigger.update { it + 1 }
    }

    fun toggleDone(task: Task) {
        val previous = task.status
        val completing = previous != Status.DONE
        viewModelScope.launch {
            try {
                taskRepository.toggleDone(task.id, clock())
                if (completing) {
                    _events.emit(TodayEvent.Completed(TodayUndo.Completion(task, previous)))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _events.emit(TodayEvent.SaveFailed { toggleDone(task) })
            }
        }
    }

    /** Swipe "Completar": no reabre una tarea ya completada. */
    fun complete(task: Task) {
        if (task.status != Status.DONE) toggleDone(task)
    }

    fun postponeToTomorrow(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.updateTask(
                    task.copy(dueAt = postponeDueAt(task.dueAt, clock(), zone)),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _events.emit(TodayEvent.SaveFailed { postponeToTomorrow(task) })
            }
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            try {
                val snapshot = taskRepository.getTaskWithSubtasks(task.id)
                val titles = snapshot?.subtasks
                    ?.sortedBy { it.position }
                    ?.map { it.title }
                    .orEmpty()
                taskRepository.deleteTask(task.id)
                _events.emit(TodayEvent.Deleted(TodayUndo.Deletion(task, titles)))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _events.emit(TodayEvent.SaveFailed { delete(task) })
            }
        }
    }

    fun undo(undo: TodayUndo) {
        viewModelScope.launch {
            try {
                when (undo) {
                    is TodayUndo.Completion ->
                        taskRepository.setStatus(undo.task.id, undo.previousStatus, clock())
                    is TodayUndo.Deletion ->
                        taskRepository.createTask(undo.task.copy(id = 0L), undo.subtaskTitles)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _events.emit(TodayEvent.SaveFailed { undo(undo) })
            }
        }
    }

    private fun buildState(
        aggregate: Aggregate,
        now: Long,
        filter: TodayFilter,
        overrides: Map<TodaySectionType, Boolean>,
    ): TodayUiState {
        val eyebrow = DateFormat.eyebrow(now, zone)
        val raw = aggregate.raw
        if (raw == null) {
            return TodayUiState(
                loading = aggregate.loading,
                error = aggregate.error,
                eyebrow = eyebrow,
                now = now,
                isEmptyTotal = true,
            )
        }

        val subjectsById = raw.subjects.associateBy { it.id }
        val groups = groupToday(
            now = now,
            zone = zone,
            dayTasks = raw.day,
            upcomingTasks = raw.upcoming,
            activeTasks = raw.active,
            subjectsById = subjectsById,
            weekStartsMonday = raw.weekStartsMonday,
        )

        val sections = if (filter == TodayFilter.DONE) {
            emptyList()
        } else {
            buildTodaySections(groups, filter, overrides)
        }

        val empty = when {
            aggregate.loading -> null
            // Sin ninguna tarea manda el vacío total, aunque el filtro activo
            // sea "Pendientes" o "Completadas" (no hay nada que completar).
            groups.isEmpty -> TodayEmpty.Total
            filter == TodayFilter.DONE ->
                if (groups.completedHistory.isEmpty()) TodayEmpty.DoneFilter else null
            filter == TodayFilter.PENDING ->
                if (!groups.hasPending) TodayEmpty.PendingFilter else null
            else -> null
        }

        return TodayUiState(
            loading = false,
            error = aggregate.error,
            hasCachedData = true,
            filter = filter,
            eyebrow = eyebrow,
            now = now,
            sections = sections,
            flatCompleted = if (filter == TodayFilter.DONE) {
                groups.completedHistory
            } else {
                emptyList()
            },
            weekDays = groups.weekDays,
            focal = if (filter == TodayFilter.DONE) null else groups.focal,
            filterCounts = groups.filterCounts,
            summary = if (groups.summaryTotal > 0) {
                TodaySummary(
                    completed = groups.summaryCompleted,
                    total = groups.summaryTotal,
                )
            } else {
                null
            },
            empty = empty,
            isEmptyTotal = groups.isEmpty,
        )
    }

    companion object {
        /** Factory sin Hilt: lee el contenedor manual de la aplicación. */
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.notallyContainer
                TodayViewModel(
                    taskRepository = container.taskRepository,
                    subjectRepository = container.subjectRepository,
                    settingsRepository = container.settingsRepository,
                )
            }
        }
    }
}
