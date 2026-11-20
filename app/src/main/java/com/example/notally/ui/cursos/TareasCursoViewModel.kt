package com.example.notally.ui.cursos

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.notally.contenedorNotally
import com.example.notally.data.model.Curso
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Tarea
import com.example.notally.data.repository.CursosRepository
import com.example.notally.data.repository.TareasRepository
import com.example.notally.ui.utils.clockFlow
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Tareas de un curso, ya ordenadas para pintar la lista. */
data class EstadoUiTareasCurso(
    val cargando: Boolean = true,
    val error: Boolean = false,
    val curso: Curso? = null,
    val tareas: List<Tarea> = emptyList(),
    val ahora: Long = System.currentTimeMillis(),
)

/** Fallo de escritura al alternar el estado de una tarea. */
sealed interface EventoTareasCurso {
    data object FalloAlternar : EventoTareasCurso
}

/**
 * ViewModel de la lista de tareas de un curso.
 *
 * No hay consulta por curso en `TareaDao`: se reutiliza
 * `observarTareasActivas()` (no archivadas, incluye completadas) y se filtra
 * por `cursoId` en memoria. Así no se toca Firestore ni el esquema.
 */
class TareasCursoViewModel(
    private val cursoId: Long,
    private val repositorioTareas: TareasRepository,
    private val repositorioCursos: CursosRepository,
    private val reloj: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val reintento = MutableStateFlow(0)

    private val _eventos = MutableSharedFlow<EventoTareasCurso>(extraBufferCapacity = 4)
    val eventos: SharedFlow<EventoTareasCurso> = _eventos.asSharedFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val estadoUi: StateFlow<EstadoUiTareasCurso> = reintento
        .flatMapLatest {
            combine(
                repositorioCursos.observarCursos(),
                repositorioTareas.observarTareasActivas(),
                clockFlow(reloj),
            ) { cursos, tareas, ahora ->
                EstadoUiTareasCurso(
                    cargando = false,
                    curso = cursos.firstOrNull { it.id == cursoId },
                    tareas = ordenarTareas(tareas.filter { it.cursoId == cursoId }),
                    ahora = ahora,
                )
            }
        }
        .catch { emit(EstadoUiTareasCurso(cargando = false, error = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EstadoUiTareasCurso(),
        )

    fun reintentar() {
        reintento.update { it + 1 }
    }

    fun alternarCompletada(tareaId: Long) {
        viewModelScope.launch {
            try {
                repositorioTareas.alternarCompletada(tareaId)
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (_: Exception) {
                _eventos.emit(EventoTareasCurso.FalloAlternar)
            }
        }
    }

    companion object {
        fun factory(context: Context, cursoId: Long): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val contenedor = context.contenedorNotally
                    TareasCursoViewModel(
                        cursoId = cursoId,
                        repositorioTareas = contenedor.repositorioTareas,
                        repositorioCursos = contenedor.repositorioCursos,
                    )
                }
            }
    }
}

/**
 * Pendientes primero por fecha de entrega (sin fecha al final) y, después,
 * las completadas en el mismo orden. Misma regla de fecha que Hoy, sin
 * duplicar la prioridad: aquí el curso ya es el contexto.
 */
private fun ordenarTareas(tareas: List<Tarea>): List<Tarea> {
    val (completadas, pendientes) = tareas.partition { it.estado == Estado.COMPLETADA }
    return pendientes.sortedWith(comparadorEntrega) + completadas.sortedWith(comparadorEntrega)
}

private val comparadorEntrega: Comparator<Tarea> =
    compareBy<Tarea> { it.entregaEn ?: Long.MAX_VALUE }
        .thenBy { it.titulo.lowercase(Locale.forLanguageTag("es")) }
