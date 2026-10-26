package com.example.notally.ui.subjects

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.notally.data.model.Settings
import com.example.notally.data.model.Status
import com.example.notally.data.model.Subject
import com.example.notally.data.model.SubjectWithTaskCount
import com.example.notally.data.model.Task
import com.example.notally.data.repository.SettingsRepository
import com.example.notally.data.repository.SubjectRepository
import com.example.notally.data.repository.TaskRepository
import com.example.notally.notallyContainer
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Una materia con los conteos reales que muestra la tarjeta (§7.4). */
data class SubjectCardUi(
    val subject: Subject,
    val totalTasks: Int,
    val doneTasks: Int,
) {
    val pendingTasks: Int get() = (totalTasks - doneTasks).coerceAtLeast(0)
}

data class SubjectsUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val hasCachedData: Boolean = false,
    val subjects: List<SubjectCardUi> = emptyList(),
    val pendingTotal: Int = 0,
    val weeklyDeliveries: Int = 0,
)

/** Error de la hoja de materia que se muestra dentro del formulario. */
enum class SubjectFormError {
    DUPLICATE,
    GENERIC,
}

/** Resultado explícito de guardar una materia desde la hoja. */
enum class SubjectSaveResult {
    SAVED,
    DUPLICATE,
    GENERIC,
}

sealed interface SubjectsEvent {
    data class Failed(val retry: () -> Unit) : SubjectsEvent
}

/**
 * ViewModel de Materias (§7.4).
 *
 * Los conteos de cada tarjeta salen de `observeActiveTasks()` (no archivadas e
 * incluye completadas) y el total de pendientes del encabezado usa el conteo
 * de `SubjectDao`, que excluye completadas y archivadas. "Entregas esta
 * semana" se calcula con [WeekDeliveries] y respeta el ajuste de inicio de
 * semana.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SubjectsViewModel(
    private val subjectRepository: SubjectRepository,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private data class Raw(
        val subjects: List<SubjectWithTaskCount>,
        val tasks: List<Task>,
        val settings: Settings,
    )

    private sealed interface LoadState {
        data object Loading : LoadState
        data class Ready(val raw: Raw) : LoadState
        data object Error : LoadState
    }

    private data class Aggregate(
        val raw: Raw? = null,
        val loading: Boolean = true,
        val error: Boolean = false,
    )

    private val retryTrigger = MutableStateFlow(0)
    private val _formError = MutableStateFlow<SubjectFormError?>(null)
    val formError: StateFlow<SubjectFormError?> = _formError

    private val _events = MutableSharedFlow<SubjectsEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<SubjectsEvent> = _events.asSharedFlow()

    private val clockFlow: Flow<Long> = flow {
        while (true) {
            val now = clock()
            emit(now)
            delay(60_000L - (now % 60_000L))
        }
    }

    private val loadStates: Flow<LoadState> = retryTrigger.flatMapLatest {
        val ready: Flow<LoadState> = combine(
            subjectRepository.observeSubjectsWithTaskCount(),
            taskRepository.observeActiveTasks(),
            settingsRepository.settings,
        ) { subjects, tasks, settings ->
            LoadState.Ready(Raw(subjects, tasks, settings))
        }
        ready
            .onStart { emit(LoadState.Loading) }
            .catch { emit(LoadState.Error) }
    }

    private val aggregate: Flow<Aggregate> = loadStates.scan(Aggregate()) { acc, state ->
        when (state) {
            LoadState.Loading -> acc.copy(loading = true, error = false)
            is LoadState.Ready -> Aggregate(raw = state.raw, loading = false, error = false)
            LoadState.Error -> acc.copy(loading = false, error = true)
        }
    }

    val uiState: StateFlow<SubjectsUiState> = combine(
        aggregate,
        clockFlow,
    ) { state, now ->
        buildState(state, now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SubjectsUiState(),
    )

    fun retry() {
        retryTrigger.update { it + 1 }
    }

    fun clearFormError() {
        _formError.value = null
    }

    /**
     * Crea o renombra una materia. Devuelve el resultado para que la hoja
     * distinga éxito de error y pueda reactivar "Guardar" en `finally`.
     */
    suspend fun saveSubject(
        existing: Subject?,
        name: String,
        colorArgb: Long,
    ): SubjectSaveResult {
        val trimmed = name.trim()
        return try {
            if (existing == null) {
                subjectRepository.createSubject(name = trimmed, colorArgb = colorArgb)
            } else {
                subjectRepository.updateSubject(
                    existing.copy(name = trimmed, colorArgb = colorArgb),
                )
            }
            _formError.value = null
            SubjectSaveResult.SAVED
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SQLiteConstraintException) {
            _formError.value = SubjectFormError.DUPLICATE
            SubjectSaveResult.DUPLICATE
        } catch (_: Exception) {
            _formError.value = SubjectFormError.GENERIC
            SubjectSaveResult.GENERIC
        }
    }

    fun deleteSubject(subject: Subject, onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                subjectRepository.deleteSubject(subject.id)
                onDeleted()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _events.emit(SubjectsEvent.Failed { deleteSubject(subject, onDeleted) })
            }
        }
    }

    /** Color por defecto en alta: siguiente swatch en orden (§2.5). */
    fun defaultColorArgb(subjectCount: Int): Long {
        val swatches = com.example.notally.ui.theme.NotallySubjectSwatches
        return swatches[subjectCount % swatches.size].argb
    }

    private fun buildState(aggregate: Aggregate, now: Long): SubjectsUiState {
        val raw = aggregate.raw ?: return SubjectsUiState(
            loading = aggregate.loading,
            error = aggregate.error,
        )
        val tasksBySubject = raw.tasks.groupBy { it.subjectId }
        val cards = raw.subjects
            .sortedBy { it.subject.createdAt }
            .map { entry ->
                val tasks = tasksBySubject[entry.subject.id].orEmpty()
                SubjectCardUi(
                    subject = entry.subject,
                    totalTasks = tasks.size,
                    doneTasks = tasks.count { it.status == Status.DONE },
                )
            }
        return SubjectsUiState(
            loading = false,
            error = aggregate.error,
            hasCachedData = true,
            subjects = cards,
            pendingTotal = raw.subjects.sumOf { it.pendingTaskCount },
            weeklyDeliveries = WeekDeliveries.count(
                dueDates = raw.tasks.map { it.dueAt },
                now = now,
                weekStartsMonday = raw.settings.weekStartsMonday,
                zone = zone,
            ),
        )
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = context.notallyContainer
                SubjectsViewModel(
                    subjectRepository = container.subjectRepository,
                    taskRepository = container.taskRepository,
                    settingsRepository = container.settingsRepository,
                )
            }
        }
    }
}
