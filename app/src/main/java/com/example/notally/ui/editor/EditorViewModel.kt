package com.example.notally.ui.editor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.notally.R
import com.example.notally.contenedorNotally
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Curso
import com.example.notally.data.model.Prioridad
import com.example.notally.data.model.Subtarea
import com.example.notally.data.model.Tarea
import com.example.notally.data.model.TareaConSubtareas
import com.example.notally.data.repository.CursosRepository
import com.example.notally.data.repository.ResultadoCurso
import com.example.notally.data.repository.TareasRepository
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
data class SubtareaEditor(
    val id: Long,
    val titulo: String,
    val hecha: Boolean,
)

/** Todo el contenido editable del editor, en un solo estado. */
data class BorradorTarea(
    val tareaId: Long = 0L,
    val titulo: String = "",
    val notas: String = "",
    val cursoId: Long? = null,
    val entregaEn: Long? = null,
    val prioridad: Prioridad = Prioridad.MEDIA,
    val estado: Estado = Estado.PENDIENTE,
    val esGrupal: Boolean = false,
    val entregada: Boolean = false,
    val externalId: String? = null,
    val subtareas: List<SubtareaEditor> = emptyList(),
    val cargando: Boolean = true,
    val errorCarga: Boolean = false,
    val errorGuardado: Boolean = false,
)

sealed interface EventoEditor {
    data class SubtareaEliminada(
        val titulo: String,
        val estabaHecha: Boolean,
    ) : EventoEditor
}

/**
 * ViewModel del editor de tarea (§7.5, §7.6).
 *
 * Autosave: debounce de 500 ms y como máximo una escritura por segundo. Se
 * hace flush al salir y al ir a segundo plano; no hay botón "Guardar".
 *
 * Las escrituras corren en [alcancePersistencia], un scope propio que no muere
 * con `viewModelScope`: así el flush lanzado desde `onDispose`/`onCleared`
 * termina incluso cuando la pantalla ya se retiró del composition. Las
 * escrituras se serializan con un [Mutex] y se evitan si no cambió nada.
 */
class EditorViewModel(
    private val tareaId: Long?,
    private val repositorioTareas: TareasRepository,
    private val repositorioCursos: CursosRepository,
    private val etiquetaSinTitulo: String,
    private val reloj: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _estado = MutableStateFlow(
        BorradorTarea(cargando = tareaId != null, errorCarga = false),
    )
    val estado: StateFlow<BorradorTarea> = _estado.asStateFlow()

    private val _cursos = MutableStateFlow<List<Curso>>(emptyList())
    val cursos: StateFlow<List<Curso>> = _cursos.asStateFlow()

    private val _errorNuevoCurso = MutableStateFlow<ResultadoCurso?>(null)
    val errorNuevoCurso: StateFlow<ResultadoCurso?> = _errorNuevoCurso.asStateFlow()

    private val _eventos = MutableSharedFlow<EventoEditor>(extraBufferCapacity = 8)
    val eventos: SharedFlow<EventoEditor> = _eventos.asSharedFlow()

    private val alcancePersistencia = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutexPersistencia = Mutex()
    private val mutexOperaciones = Mutex()

    private var tareaCargada: Tarea? = null
    private var revision = 0L
    private var revisionGuardada = 0L
    private var ultimaEscrituraEn = 0L
    private var trabajoDebounce: Job? = null
    private var eliminada = false

    init {
        observarCursos()
        if (tareaId != null) cargar(tareaId) else _estado.update { it.copy(cargando = false) }
    }

    // ---------------------------------------------------------------------
    // Carga
    // ---------------------------------------------------------------------

    private fun cargar(id: Long) {
        viewModelScope.launch {
            try {
                aplicarInstantanea(repositorioTareas.obtenerTareaConSubtareas(id))
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (_: Exception) {
                _estado.update { it.copy(cargando = false, errorCarga = true) }
            }
        }
    }

    private fun aplicarInstantanea(instantanea: TareaConSubtareas?) {
        if (instantanea == null) {
            _estado.update { it.copy(cargando = false, errorCarga = true) }
            return
        }
        tareaCargada = instantanea.tarea
        _estado.value = BorradorTarea(
            tareaId = instantanea.tarea.id,
            titulo = instantanea.tarea.titulo,
            notas = instantanea.tarea.notas,
            cursoId = instantanea.tarea.cursoId,
            entregaEn = instantanea.tarea.entregaEn,
            prioridad = instantanea.tarea.prioridad,
            estado = instantanea.tarea.estado,
            esGrupal = instantanea.tarea.esGrupal,
            entregada = instantanea.tarea.entregada,
            externalId = instantanea.tarea.externalId,
            subtareas = instantanea.subtareas.sortedBy { it.posicion }.map { it.aFila() },
            cargando = false,
        )
    }

    fun reintentarCarga() {
        val id = tareaId ?: return
        _estado.update { it.copy(cargando = true, errorCarga = false) }
        cargar(id)
    }

    private fun observarCursos() {
        viewModelScope.launch {
            repositorioCursos.observarCursos()
                .catch { }
                .collect { _cursos.value = it }
        }
    }

    // ---------------------------------------------------------------------
    // Cambios y autosave
    // ---------------------------------------------------------------------

    fun cambiarTitulo(valor: String) = actualizarBorrador { it.copy(titulo = valor) }

    fun cambiarNotas(valor: String) = actualizarBorrador { it.copy(notas = valor) }

    fun cambiarCurso(cursoId: Long?) = actualizarBorrador { it.copy(cursoId = cursoId) }

    fun cambiarEntrega(entregaEn: Long?) = actualizarBorrador { it.copy(entregaEn = entregaEn) }

    fun cambiarPrioridad(prioridad: Prioridad) =
        actualizarBorrador { it.copy(prioridad = prioridad) }

    fun cambiarEstado(estado: Estado) = actualizarBorrador { it.copy(estado = estado) }

    private fun actualizarBorrador(transformacion: (BorradorTarea) -> BorradorTarea) {
        _estado.update(transformacion)
        revision += 1
        programarGuardado()
    }

    private fun programarGuardado() {
        trabajoDebounce?.cancel()
        trabajoDebounce = viewModelScope.launch {
            delay(MS_DEBOUNCE)
            val transcurrido = reloj() - ultimaEscrituraEn
            if (transcurrido in 0 until MS_INTERVALO_MIN_ESCRITURA) {
                delay(MS_INTERVALO_MIN_ESCRITURA - transcurrido)
            }
            persistir()
        }
    }

    /** Flush silencioso al volver, al ir a segundo plano o al destruirse. */
    fun volcar() {
        trabajoDebounce?.cancel()
        alcancePersistencia.launch { persistir() }
    }

    /**
     * Drena la escritura pendiente y espera a que termine, con un máximo de
     * [timeoutMillis]. Se usa desde el back confirmado: el `ViewModelStore` se
     * destruye al volver, así que este es el último punto donde se puede
     * garantizar que el borrador llegue a Room. No bloquea el hilo de UI.
     */
    suspend fun volcarYEsperar(timeoutMillis: Long = MS_TIMEOUT_VOLCADO) {
        trabajoDebounce?.cancel()
        withTimeoutOrNull(timeoutMillis) {
            mutexPersistencia.withLock { persistirBloqueado() }
        }
    }

    fun reintentarGuardado() {
        _estado.update { it.copy(errorGuardado = false) }
        alcancePersistencia.launch { persistir() }
    }

    private suspend fun persistir() {
        mutexPersistencia.withLock { persistirBloqueado() }
    }

    private suspend fun persistirBloqueado() {
        if (eliminada) return
        val borrador = _estado.value
        if (borrador.cargando || borrador.errorCarga) return
        val revisionAlIniciar = revision
        try {
            if (borrador.tareaId == 0L) {
                if (!EditorRules.tieneContenido(
                        borrador.titulo,
                        borrador.notas,
                        borrador.subtareas.size,
                    )
                ) {
                    return
                }
                val creada = repositorioTareas.crearTarea(
                    Tarea(
                        titulo = EditorRules.tituloVisible(borrador.titulo, etiquetaSinTitulo),
                        notas = borrador.notas,
                        cursoId = borrador.cursoId,
                        entregaEn = borrador.entregaEn,
                        prioridad = borrador.prioridad,
                        estado = borrador.estado,
                    ),
                )
                tareaCargada = repositorioTareas.obtenerTarea(creada)
                _estado.update { it.copy(tareaId = creada, errorGuardado = false) }
            } else {
                if (revisionAlIniciar == revisionGuardada) return
                val base = tareaCargada
                    ?: repositorioTareas.obtenerTarea(borrador.tareaId)?.also {
                        tareaCargada = it
                    }
                    ?: return
                val actualizada = base.copy(
                    titulo = EditorRules.tituloVisible(borrador.titulo, etiquetaSinTitulo),
                    notas = borrador.notas,
                    cursoId = borrador.cursoId,
                    entregaEn = borrador.entregaEn,
                    prioridad = borrador.prioridad,
                    estado = borrador.estado,
                )
                repositorioTareas.actualizarTarea(actualizada)
                tareaCargada = actualizada
                _estado.update { it.copy(errorGuardado = false) }
            }
            revisionGuardada = revisionAlIniciar
            ultimaEscrituraEn = reloj()
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (_: Exception) {
            _estado.update { it.copy(errorGuardado = true) }
        }
    }

    /**
     * Garantiza que exista una fila en la base antes de tocar subtareas. Si el
     * borrador está vacío, el título se guarda como "Sin título" porque la
     * subtarea que se está agregando ya es contenido.
     */
    private suspend fun asegurarTareaId(): Long? = mutexPersistencia.withLock {
        val actual = _estado.value
        actual.tareaId.takeIf { it != 0L }?.let { return@withLock it }
        if (eliminada) return@withLock null
        val revisionAlIniciar = revision
        return@withLock try {
            val id = repositorioTareas.crearTarea(
                Tarea(
                    titulo = EditorRules.tituloVisible(actual.titulo, etiquetaSinTitulo),
                    notas = actual.notas,
                    cursoId = actual.cursoId,
                    entregaEn = actual.entregaEn,
                    prioridad = actual.prioridad,
                    estado = actual.estado,
                ),
            )
            tareaCargada = repositorioTareas.obtenerTarea(id)
            _estado.update { it.copy(tareaId = id) }
            revisionGuardada = revisionAlIniciar
            ultimaEscrituraEn = reloj()
            id
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (_: Exception) {
            _estado.update { it.copy(errorGuardado = true) }
            null
        }
    }

    // ---------------------------------------------------------------------
    // Subtareas
    // ---------------------------------------------------------------------

    /**
     * Agrega una subtarea y solo entonces confirma con [alAgregar]. Si la
     * escritura falla, el campo conserva el texto y se muestra el error de
     * guardado; nunca se limpia antes de tener la fila persistida.
     */
    fun agregarSubtarea(titulo: String, alAgregar: () -> Unit = {}) {
        val recortado = titulo.trim()
        if (recortado.isEmpty()) return
        viewModelScope.launch {
            mutexOperaciones.withLock {
                val id = asegurarTareaId() ?: return@withLock
                try {
                    repositorioTareas.agregarSubtarea(id, recortado)
                    sincronizarSubtareas(id)
                    alAgregar()
                } catch (cancelada: CancellationException) {
                    throw cancelada
                } catch (_: Exception) {
                    _estado.update { it.copy(errorGuardado = true) }
                }
            }
        }
    }

    fun alternarSubtarea(fila: SubtareaEditor) {
        if (fila.id <= 0L) return
        viewModelScope.launch {
            mutexOperaciones.withLock {
                try {
                    repositorioTareas.alternarSubtarea(fila.id)
                    sincronizarSubtareas(_estado.value.tareaId)
                } catch (cancelada: CancellationException) {
                    throw cancelada
                } catch (_: Exception) {
                    _estado.update { it.copy(errorGuardado = true) }
                }
            }
        }
    }

    fun eliminarSubtarea(fila: SubtareaEditor) {
        if (fila.id <= 0L) return
        viewModelScope.launch {
            mutexOperaciones.withLock {
                try {
                    repositorioTareas.eliminarSubtarea(fila.id)
                    sincronizarSubtareas(_estado.value.tareaId)
                    _eventos.emit(EventoEditor.SubtareaEliminada(fila.titulo, fila.hecha))
                } catch (cancelada: CancellationException) {
                    throw cancelada
                } catch (_: Exception) {
                    _estado.update { it.copy(errorGuardado = true) }
                }
            }
        }
    }

    fun deshacerEliminarSubtarea(evento: EventoEditor.SubtareaEliminada) {
        viewModelScope.launch {
            mutexOperaciones.withLock {
                val id = asegurarTareaId() ?: return@withLock
                try {
                    val nuevoId = repositorioTareas.agregarSubtarea(id, evento.titulo)
                    if (evento.estabaHecha) {
                        repositorioTareas.cambiarSubtareaHecha(nuevoId, true)
                    }
                    sincronizarSubtareas(id)
                } catch (cancelada: CancellationException) {
                    throw cancelada
                } catch (_: Exception) {
                    _estado.update { it.copy(errorGuardado = true) }
                }
            }
        }
    }

    private suspend fun sincronizarSubtareas(tareaId: Long) {
        if (tareaId == 0L) return
        val filas = repositorioTareas.obtenerTareaConSubtareas(tareaId)
            ?.subtareas
            ?.sortedBy { it.posicion }
            ?.map { it.aFila() }
            .orEmpty()
        _estado.update { it.copy(subtareas = filas) }
    }

    // ---------------------------------------------------------------------
    // Cursos y eliminación
    // ---------------------------------------------------------------------

    fun limpiarErrorNuevoCurso() {
        _errorNuevoCurso.value = null
    }

    /** Crea un curso y la selecciona. [alCrear] se ejecuta en el hilo principal. */
    fun crearCursoYSeleccionar(
        nombre: String,
        alCrear: () -> Unit,
    ) {
        val recortado = nombre.trim()
        if (recortado.isEmpty()) return
        viewModelScope.launch {
            when (val resultado = repositorioCursos.crearCurso(recortado)) {
                is ResultadoCurso.Creado -> {
                    _errorNuevoCurso.value = null
                    actualizarBorrador { it.copy(cursoId = resultado.id) }
                    alCrear()
                }

                else -> _errorNuevoCurso.value = resultado
            }
        }
    }

    fun eliminarTarea(alEliminar: () -> Unit) {
        trabajoDebounce?.cancel()
        viewModelScope.launch {
            try {
                mutexPersistencia.withLock {
                    eliminada = true
                    val id = _estado.value.tareaId
                    if (id != 0L) repositorioTareas.eliminarTarea(id)
                }
                alEliminar()
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (_: Exception) {
                eliminada = false
                _estado.update { it.copy(errorGuardado = true) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        alcancePersistencia.launch { persistir() }
    }

    companion object {
        private const val MS_DEBOUNCE = 500L
        private const val MS_INTERVALO_MIN_ESCRITURA = 1_000L
        private const val MS_TIMEOUT_VOLCADO = 800L

        fun factory(context: Context, tareaId: Long?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val contenedor = context.contenedorNotally
                    EditorViewModel(
                        tareaId = tareaId,
                        repositorioTareas = contenedor.repositorioTareas,
                        repositorioCursos = contenedor.repositorioCursos,
                        etiquetaSinTitulo = context.getString(R.string.editor_untitled),
                    )
                }
            }
    }
}

private fun Subtarea.aFila(): SubtareaEditor =
    SubtareaEditor(id = id, titulo = titulo, hecha = hecha)
