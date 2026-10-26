package com.example.notally.ui.cursos

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.notally.contenedorNotally
import com.example.notally.data.model.Settings
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Curso
import com.example.notally.data.model.CursoConConteo
import com.example.notally.data.model.Tarea
import com.example.notally.data.repository.SettingsRepository
import com.example.notally.data.repository.CursosRepository
import com.example.notally.data.repository.TareasRepository
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

/** Un curso con los conteos reales que muestra la tarjeta (§7.4). */
data class TarjetaCursoUi(
    val curso: Curso,
    val totalTareas: Int,
    val tareasHechas: Int,
) {
    val tareasPendientes: Int get() = (totalTareas - tareasHechas).coerceAtLeast(0)
}

data class EstadoUiCursos(
    val cargando: Boolean = true,
    val error: Boolean = false,
    val tieneDatosEnCache: Boolean = false,
    val cursos: List<TarjetaCursoUi> = emptyList(),
    val totalPendientes: Int = 0,
    val entregasSemanales: Int = 0,
)

/** Error de la hoja de curso que se muestra dentro del formulario. */
enum class ErrorFormularioCurso {
    DUPLICADO,
    GENERICO,
}

/** Resultado explícito de guardar un curso desde la hoja. */
enum class ResultadoGuardarCurso {
    GUARDADO,
    DUPLICADO,
    GENERICO,
}

sealed interface EventoCursos {
    data class Fallo(val reintentar: () -> Unit) : EventoCursos
}

/**
 * ViewModel de Cursos (§7.4).
 *
 * Los conteos de cada tarjeta salen de `observarTareasActivas()` (no archivadas
 * e incluye completadas) y el total de pendientes del encabezado usa el conteo
 * de `CursoDao`, que excluye completadas y archivadas. "Entregas esta
 * semana" se calcula con [WeekDeliveries] y respeta el ajuste de inicio de
 * semana.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CursosViewModel(
    private val repositorioCursos: CursosRepository,
    private val repositorioTareas: TareasRepository,
    private val repositorioSettings: SettingsRepository,
    private val reloj: () -> Long = System::currentTimeMillis,
    private val zona: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private data class DatosCrudos(
        val cursos: List<CursoConConteo>,
        val tareas: List<Tarea>,
        val settings: Settings,
    )

    private sealed interface EstadoCarga {
        data object Cargando : EstadoCarga
        data class Listo(val datos: DatosCrudos) : EstadoCarga
        data object Error : EstadoCarga
    }

    private data class Agregado(
        val datos: DatosCrudos? = null,
        val cargando: Boolean = true,
        val error: Boolean = false,
    )

    private val disparadorReintento = MutableStateFlow(0)
    private val _errorFormulario = MutableStateFlow<ErrorFormularioCurso?>(null)
    val errorFormulario: StateFlow<ErrorFormularioCurso?> = _errorFormulario

    private val _eventos = MutableSharedFlow<EventoCursos>(extraBufferCapacity = 4)
    val eventos: SharedFlow<EventoCursos> = _eventos.asSharedFlow()

    private val flujoReloj: Flow<Long> = flow {
        while (true) {
            val ahora = reloj()
            emit(ahora)
            delay(60_000L - (ahora % 60_000L))
        }
    }

    private val estadosCarga: Flow<EstadoCarga> = disparadorReintento.flatMapLatest {
        val listo: Flow<EstadoCarga> = combine(
            repositorioCursos.observarCursosConConteo(),
            repositorioTareas.observarTareasActivas(),
            repositorioSettings.settings,
        ) { cursos, tareas, settings ->
            EstadoCarga.Listo(DatosCrudos(cursos, tareas, settings))
        }
        listo
            .onStart { emit(EstadoCarga.Cargando) }
            .catch { emit(EstadoCarga.Error) }
    }

    private val agregado: Flow<Agregado> =
        estadosCarga.scan(Agregado()) { acumulado, estado ->
            when (estado) {
                EstadoCarga.Cargando ->
                    acumulado.copy(cargando = true, error = false)

                is EstadoCarga.Listo ->
                    Agregado(datos = estado.datos, cargando = false, error = false)

                EstadoCarga.Error ->
                    acumulado.copy(cargando = false, error = true)
            }
        }

    val estadoUi: StateFlow<EstadoUiCursos> = combine(
        agregado,
        flujoReloj,
    ) { estado, ahora ->
        construirEstado(estado, ahora)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EstadoUiCursos(),
    )

    fun reintentar() {
        disparadorReintento.update { it + 1 }
    }

    fun limpiarErrorFormulario() {
        _errorFormulario.value = null
    }

    /**
     * Crea o renombra un curso. Devuelve el resultado para que la hoja
     * distinga éxito de error y pueda reactivar "Guardar" en `finally`.
     */
    suspend fun guardarCurso(
        existente: Curso?,
        nombre: String,
        color: Long,
    ): ResultadoGuardarCurso {
        val recortado = nombre.trim()
        return try {
            if (existente == null) {
                repositorioCursos.crearCurso(nombre = recortado, color = color)
            } else {
                repositorioCursos.actualizarCurso(
                    existente.copy(nombre = recortado, color = color),
                )
            }
            _errorFormulario.value = null
            ResultadoGuardarCurso.GUARDADO
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (_: SQLiteConstraintException) {
            _errorFormulario.value = ErrorFormularioCurso.DUPLICADO
            ResultadoGuardarCurso.DUPLICADO
        } catch (_: Exception) {
            _errorFormulario.value = ErrorFormularioCurso.GENERICO
            ResultadoGuardarCurso.GENERICO
        }
    }

    fun eliminarCurso(curso: Curso, alEliminar: () -> Unit) {
        viewModelScope.launch {
            try {
                repositorioCursos.eliminarCurso(curso.id)
                alEliminar()
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (_: Exception) {
                _eventos.emit(EventoCursos.Fallo { eliminarCurso(curso, alEliminar) })
            }
        }
    }

    /** Color por defecto en alta: siguiente swatch en orden (§2.5). */
    fun colorPorDefecto(cantidadCursos: Int): Long {
        val muestras = com.example.notally.ui.theme.MuestrasCursos
        return muestras[cantidadCursos % muestras.size].argb
    }

    private fun construirEstado(agregado: Agregado, ahora: Long): EstadoUiCursos {
        val datos = agregado.datos ?: return EstadoUiCursos(
            cargando = agregado.cargando,
            error = agregado.error,
        )
        val tareasPorCurso = datos.tareas.groupBy { it.cursoId }
        val tarjetas = datos.cursos
            .sortedBy { it.curso.creadoEn }
            .map { entrada ->
                val tareas = tareasPorCurso[entrada.curso.id].orEmpty()
                TarjetaCursoUi(
                    curso = entrada.curso,
                    totalTareas = tareas.size,
                    tareasHechas = tareas.count { it.estado == Estado.COMPLETADA },
                )
            }
        return EstadoUiCursos(
            cargando = false,
            error = agregado.error,
            tieneDatosEnCache = true,
            cursos = tarjetas,
            totalPendientes = datos.cursos.sumOf { it.tareasPendientes },
            entregasSemanales = WeekDeliveries.contar(
                fechasEntrega = datos.tareas.map { it.entregaEn },
                ahora = ahora,
                semanaEmpiezaLunes = datos.settings.semanaEmpiezaLunes,
                zona = zona,
            ),
        )
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val contenedor = context.contenedorNotally
                CursosViewModel(
                    repositorioCursos = contenedor.repositorioCursos,
                    repositorioTareas = contenedor.repositorioTareas,
                    repositorioSettings = contenedor.repositorioSettings,
                )
            }
        }
    }
}
