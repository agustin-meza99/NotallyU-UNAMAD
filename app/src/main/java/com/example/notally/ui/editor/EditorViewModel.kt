package com.example.notally.ui.editor

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.notally.R
import com.example.notally.data.model.Priority
import com.example.notally.data.model.Status
import com.example.notally.data.model.Subject
import com.example.notally.data.model.Subtask
import com.example.notally.data.model.Task
import com.example.notally.data.repository.SubjectRepository
import com.example.notally.data.repository.TaskRepository
import com.example.notally.notallyContainer
import com.example.notally.ui.theme.NotallySubjectSwatches
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** Una subtarea tal como la pinta el editor. */
data class EditorSubtask(
    val id: Long,
    val title: String,
    val isDone: Boolean,
)

/** Todo el contenido editable del editor, en un solo estado. */
data class EditorDraft(
    val taskId: Long = 0L,
    val title: String = "",
    val notes: String = "",
    val subjectId: Long? = null,
    val dueAt: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val status: Status = Status.PENDING,
    val subtasks: List<EditorSubtask> = emptyList(),
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val saveError: Boolean = false,
)

/** Error al crear una materia desde la hoja del editor. */
enum class EditorSubjectError {
    DUPLICATE,
    GENERIC,
}

sealed interface EditorEvent {
    data class SubtaskDeleted(
        val title: String,
        val wasDone: Boolean,
    ) : EditorEvent
}

/**
 * ViewModel del editor de tarea (§7.5, §7.6).
 *
 * Autosave: debounce de 500 ms y como máximo una escritura por segundo. Se
 * hace flush al salir y al ir a segundo plano; no hay botón "Guardar".
 *
 * Las escrituras corren en [persistScope], un scope propio que no muere con
 * `viewModelScope`: así el flush lanzado desde `onDispose`/`onCleared` termina
 * incluso cuando la pantalla ya se retiró del composition. Las escrituras se
 * serializan con un [Mutex] y se evitan si no cambió nada.
 */
class EditorViewModel(
    private val taskId: Long?,
    private val taskRepository: TaskRepository,
    private val subjectRepository: SubjectRepository,
    private val untitledLabel: String,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow(
        EditorDraft(loading = taskId != null, loadError = false),
    )
    val state: StateFlow<EditorDraft> = _state.asStateFlow()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _newSubjectError = MutableStateFlow<EditorSubjectError?>(null)
    val newSubjectError: StateFlow<EditorSubjectError?> = _newSubjectError.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val persistMutex = Mutex()
    private val opsMutex = Mutex()

    private var loadedTask: Task? = null
    private var revision = 0L
    private var savedRevision = 0L
    private var lastWriteAt = 0L
    private var debounceJob: Job? = null
    private var deleted = false

    init {
        observeSubjects()
        if (taskId != null) load(taskId) else _state.update { it.copy(loading = false) }
    }

    // ---------------------------------------------------------------------
    // Carga
    // ---------------------------------------------------------------------

    private fun load(id: Long) {
        viewModelScope.launch {
            try {
                applySnapshot(taskRepository.getTaskWithSubtasks(id))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(loading = false, loadError = true) }
            }
        }
    }

    private fun applySnapshot(snapshot: com.example.notally.data.model.TaskWithSubtasks?) {
        if (snapshot == null) {
            _state.update { it.copy(loading = false, loadError = true) }
            return
        }
        loadedTask = snapshot.task
        _state.value = EditorDraft(
            taskId = snapshot.task.id,
            title = snapshot.task.title,
            notes = snapshot.task.notes,
            subjectId = snapshot.task.subjectId,
            dueAt = snapshot.task.dueAt,
            priority = snapshot.task.priority,
            status = snapshot.task.status,
            subtasks = snapshot.subtasks.sortedBy { it.position }.map { it.toRow() },
            loading = false,
        )
    }

    fun retryLoad() {
        val id = taskId ?: return
        _state.update { it.copy(loading = true, loadError = false) }
        load(id)
    }

    private fun observeSubjects() {
        viewModelScope.launch {
            subjectRepository.observeSubjects()
                .catch { }
                .collect { _subjects.value = it }
        }
    }

    // ---------------------------------------------------------------------
    // Cambios y autosave
    // ---------------------------------------------------------------------

    fun onTitleChange(value: String) = update { it.copy(title = value) }

    fun onNotesChange(value: String) = update { it.copy(notes = value) }

    fun setSubject(subjectId: Long?) = update { it.copy(subjectId = subjectId) }

    fun setDueAt(dueAt: Long?) = update { it.copy(dueAt = dueAt) }

    fun setPriority(priority: Priority) = update { it.copy(priority = priority) }

    fun setStatus(status: Status) = update { it.copy(status = status) }

    private fun update(transform: (EditorDraft) -> EditorDraft) {
        _state.update(transform)
        revision += 1
        scheduleSave()
    }

    private fun scheduleSave() {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            val elapsed = clock() - lastWriteAt
            if (elapsed in 0 until MIN_WRITE_INTERVAL_MS) {
                delay(MIN_WRITE_INTERVAL_MS - elapsed)
            }
            persist()
        }
    }

    /** Flush silencioso al volver, al ir a segundo plano o al destruirse. */
    fun flush() {
        debounceJob?.cancel()
        persistScope.launch { persist() }
    }

    /**
     * Drena la escritura pendiente y espera a que termine, con un máximo de
     * [timeoutMillis]. Se usa desde el back confirmado: el `ViewModelStore` se
     * destruye al volver, así que este es el último punto donde se puede
     * garantizar que el borrador llegue a Room. No bloquea el hilo de UI.
     */
    suspend fun flushAndWait(timeoutMillis: Long = FLUSH_TIMEOUT_MS) {
        debounceJob?.cancel()
        withTimeoutOrNull(timeoutMillis) {
            persistMutex.withLock { persistLocked() }
        }
    }

    fun retrySave() {
        _state.update { it.copy(saveError = false) }
        persistScope.launch { persist() }
    }

    private suspend fun persist() {
        persistMutex.withLock { persistLocked() }
    }

    private suspend fun persistLocked() {
        if (deleted) return
        val draft = _state.value
        if (draft.loading || draft.loadError) return
        val revisionAtStart = revision
        try {
            if (draft.taskId == 0L) {
                if (!EditorRules.hasContent(draft.title, draft.notes, draft.subtasks.size)) return
                val created = taskRepository.createTask(
                    Task(
                        title = EditorRules.displayTitle(draft.title, untitledLabel),
                        notes = draft.notes,
                        subjectId = draft.subjectId,
                        dueAt = draft.dueAt,
                        priority = draft.priority,
                        status = draft.status,
                    ),
                )
                loadedTask = taskRepository.getTask(created)
                _state.update { it.copy(taskId = created, saveError = false) }
            } else {
                if (revisionAtStart == savedRevision) return
                val base = loadedTask
                    ?: taskRepository.getTask(draft.taskId)?.also { loadedTask = it }
                    ?: return
                val updated = base.copy(
                    title = EditorRules.displayTitle(draft.title, untitledLabel),
                    notes = draft.notes,
                    subjectId = draft.subjectId,
                    dueAt = draft.dueAt,
                    priority = draft.priority,
                    status = draft.status,
                )
                taskRepository.updateTask(updated)
                loadedTask = updated
                _state.update { it.copy(saveError = false) }
            }
            savedRevision = revisionAtStart
            lastWriteAt = clock()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            _state.update { it.copy(saveError = true) }
        }
    }

    /**
     * Garantiza que exista una fila en la base antes de tocar subtareas. Si el
     * borrador está vacío, el título se guarda como "Sin título" porque la
     * subtarea que se está agregando ya es contenido.
     */
    private suspend fun ensureTaskId(): Long? = persistMutex.withLock {
        val current = _state.value
        current.taskId.takeIf { it != 0L }?.let { return@withLock it }
        if (deleted) return@withLock null
        val revisionAtStart = revision
        return@withLock try {
            val id = taskRepository.createTask(
                Task(
                    title = EditorRules.displayTitle(current.title, untitledLabel),
                    notes = current.notes,
                    subjectId = current.subjectId,
                    dueAt = current.dueAt,
                    priority = current.priority,
                    status = current.status,
                ),
            )
            loadedTask = taskRepository.getTask(id)
            _state.update { it.copy(taskId = id) }
            savedRevision = revisionAtStart
            lastWriteAt = clock()
            id
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            _state.update { it.copy(saveError = true) }
            null
        }
    }

    // ---------------------------------------------------------------------
    // Subtareas
    // ---------------------------------------------------------------------

    /**
     * Agrega una subtarea y solo entonces confirma con [onAdded]. Si la
     * escritura falla, el campo conserva el texto y se muestra el error de
     * guardado; nunca se limpia antes de tener la fila persistida.
     */
    fun addSubtask(title: String, onAdded: () -> Unit = {}) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            opsMutex.withLock {
                val id = ensureTaskId() ?: return@withLock
                try {
                    taskRepository.addSubtask(id, trimmed)
                    syncSubtasks(id)
                    onAdded()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _state.update { it.copy(saveError = true) }
                }
            }
        }
    }

    fun toggleSubtask(row: EditorSubtask) {
        if (row.id <= 0L) return
        viewModelScope.launch {
            opsMutex.withLock {
                try {
                    taskRepository.toggleSubtask(row.id)
                    syncSubtasks(_state.value.taskId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _state.update { it.copy(saveError = true) }
                }
            }
        }
    }

    fun deleteSubtask(row: EditorSubtask) {
        if (row.id <= 0L) return
        viewModelScope.launch {
            opsMutex.withLock {
                try {
                    taskRepository.deleteSubtask(row.id)
                    syncSubtasks(_state.value.taskId)
                    _events.emit(EditorEvent.SubtaskDeleted(row.title, row.isDone))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _state.update { it.copy(saveError = true) }
                }
            }
        }
    }

    fun undoDeleteSubtask(event: EditorEvent.SubtaskDeleted) {
        viewModelScope.launch {
            opsMutex.withLock {
                val id = ensureTaskId() ?: return@withLock
                try {
                    val newId = taskRepository.addSubtask(id, event.title)
                    if (event.wasDone) taskRepository.setSubtaskDone(newId, true)
                    syncSubtasks(id)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _state.update { it.copy(saveError = true) }
                }
            }
        }
    }

    private suspend fun syncSubtasks(taskId: Long) {
        if (taskId == 0L) return
        val rows = taskRepository.getTaskWithSubtasks(taskId)
            ?.subtasks
            ?.sortedBy { it.position }
            ?.map { it.toRow() }
            .orEmpty()
        _state.update { it.copy(subtasks = rows) }
    }

    // ---------------------------------------------------------------------
    // Materias y eliminación
    // ---------------------------------------------------------------------

    fun defaultSubjectColor(): Long {
        val swatches = NotallySubjectSwatches
        return swatches[_subjects.value.size % swatches.size].argb
    }

    fun clearNewSubjectError() {
        _newSubjectError.value = null
    }

    /** Crea una materia y la selecciona. [onCreated] se ejecuta en el hilo principal. */
    fun createSubjectAndSelect(
        name: String,
        colorArgb: Long,
        onCreated: () -> Unit,
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            try {
                val id = subjectRepository.createSubject(
                    name = trimmed,
                    colorArgb = colorArgb,
                )
                _newSubjectError.value = null
                update { it.copy(subjectId = id) }
                onCreated()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SQLiteConstraintException) {
                _newSubjectError.value = EditorSubjectError.DUPLICATE
            } catch (_: Exception) {
                _newSubjectError.value = EditorSubjectError.GENERIC
            }
        }
    }

    fun deleteTask(onDeleted: () -> Unit) {
        debounceJob?.cancel()
        viewModelScope.launch {
            try {
                persistMutex.withLock {
                    deleted = true
                    val id = _state.value.taskId
                    if (id != 0L) taskRepository.deleteTask(id)
                }
                onDeleted()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                deleted = false
                _state.update { it.copy(saveError = true) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        persistScope.launch { persist() }
    }

    companion object {
        private const val DEBOUNCE_MS = 500L
        private const val MIN_WRITE_INTERVAL_MS = 1_000L
        private const val FLUSH_TIMEOUT_MS = 800L

        fun factory(context: Context, taskId: Long?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val container = context.notallyContainer
                    EditorViewModel(
                        taskId = taskId,
                        taskRepository = container.taskRepository,
                        subjectRepository = container.subjectRepository,
                        untitledLabel = context.getString(R.string.editor_untitled),
                    )
                }
            }
    }
}

private fun Subtask.toRow(): EditorSubtask =
    EditorSubtask(id = id, title = title, isDone = isDone)
