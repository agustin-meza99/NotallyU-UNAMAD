package com.example.notally.ui.subjects

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.notally.contenedorNotally
import com.example.notally.data.model.Ajustes
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Materia
import com.example.notally.data.model.MateriaConConteo
import com.example.notally.data.model.Tarea
import com.example.notally.data.repository.AjustesRepository
import com.example.notally.data.repository.MateriasRepository
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

/** Una materia con los conteos reales que muestra la tarjeta (§7.4). */
data class TarjetaMateriaUi(
    val materia: Materia,
    val totalTareas: Int,
    val tareasHechas: Int,
) {
    val tareasPendientes: Int get() = (totalTareas - tareasHechas).coerceAtLeast(0)
}

data class EstadoUiMaterias(
    val cargando: Boolean = true,
    val error: Boolean = false,
    val tieneDatosEnCache: Boolean = false,
    val materias: List<TarjetaMateriaUi> = emptyList(),
    val totalPendientes: Int = 0,
    val entregasSemanales: Int = 0,
)

/** Error de la hoja de materia que se muestra dentro del formulario. */
enum class ErrorFormularioMateria {
    DUPLICADO,
    GENERICO,
}

/** Resultado explícito de guardar una materia desde la hoja. */
enum class ResultadoGuardarMateria {
    GUARDADA,
    DUPLICADA,
    GENERICA,
}

sealed interface EventoMaterias {
    data class Fallo(val reintentar: () -> Unit) : EventoMaterias
}

/**
 * ViewModel de Materias (§7.4).
 *
 * Los conteos de cada tarjeta salen de `observarTareasActivas()` (no archivadas
 * e incluye completadas) y el total de pendientes del encabezado usa el conteo
 * de `MateriaDao`, que excluye completadas y archivadas. "Entregas esta
 * semana" se calcula con [WeekDeliveries] y respeta el ajuste de inicio de
 * semana.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SubjectsViewModel(
    private val repositorioMaterias: MateriasRepository,
    private val repositorioTareas: TareasRepository,
    private val repositorioAjustes: AjustesRepository,
    private val reloj: () -> Long = System::currentTimeMillis,
    private val zona: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private data class DatosCrudos(
        val materias: List<MateriaConConteo>,
        val tareas: List<Tarea>,
        val ajustes: Ajustes,
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
    private val _errorFormulario = MutableStateFlow<ErrorFormularioMateria?>(null)
    val errorFormulario: StateFlow<ErrorFormularioMateria?> = _errorFormulario

    private val _eventos = MutableSharedFlow<EventoMaterias>(extraBufferCapacity = 4)
    val eventos: SharedFlow<EventoMaterias> = _eventos.asSharedFlow()

    private val flujoReloj: Flow<Long> = flow {
        while (true) {
            val ahora = reloj()
            emit(ahora)
            delay(60_000L - (ahora % 60_000L))
        }
    }

    private val estadosCarga: Flow<EstadoCarga> = disparadorReintento.flatMapLatest {
        val listo: Flow<EstadoCarga> = combine(
            repositorioMaterias.observarMateriasConConteo(),
            repositorioTareas.observarTareasActivas(),
            repositorioAjustes.ajustes,
        ) { materias, tareas, ajustes ->
            EstadoCarga.Listo(DatosCrudos(materias, tareas, ajustes))
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

    val estadoUi: StateFlow<EstadoUiMaterias> = combine(
        agregado,
        flujoReloj,
    ) { estado, ahora ->
        construirEstado(estado, ahora)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EstadoUiMaterias(),
    )

    fun reintentar() {
        disparadorReintento.update { it + 1 }
    }

    fun limpiarErrorFormulario() {
        _errorFormulario.value = null
    }

    /**
     * Crea o renombra una materia. Devuelve el resultado para que la hoja
     * distinga éxito de error y pueda reactivar "Guardar" en `finally`.
     */
    suspend fun guardarMateria(
        existente: Materia?,
        nombre: String,
        color: Long,
    ): ResultadoGuardarMateria {
        val recortado = nombre.trim()
        return try {
            if (existente == null) {
                repositorioMaterias.crearMateria(nombre = recortado, color = color)
            } else {
                repositorioMaterias.actualizarMateria(
                    existente.copy(nombre = recortado, color = color),
                )
            }
            _errorFormulario.value = null
            ResultadoGuardarMateria.GUARDADA
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (_: SQLiteConstraintException) {
            _errorFormulario.value = ErrorFormularioMateria.DUPLICADO
            ResultadoGuardarMateria.DUPLICADA
        } catch (_: Exception) {
            _errorFormulario.value = ErrorFormularioMateria.GENERICO
            ResultadoGuardarMateria.GENERICA
        }
    }

    fun eliminarMateria(materia: Materia, alEliminar: () -> Unit) {
        viewModelScope.launch {
            try {
                repositorioMaterias.eliminarMateria(materia.id)
                alEliminar()
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (_: Exception) {
                _eventos.emit(EventoMaterias.Fallo { eliminarMateria(materia, alEliminar) })
            }
        }
    }

    /** Color por defecto en alta: siguiente swatch en orden (§2.5). */
    fun colorPorDefecto(cantidadMaterias: Int): Long {
        val muestras = com.example.notally.ui.theme.MuestrasMaterias
        return muestras[cantidadMaterias % muestras.size].argb
    }

    private fun construirEstado(agregado: Agregado, ahora: Long): EstadoUiMaterias {
        val datos = agregado.datos ?: return EstadoUiMaterias(
            cargando = agregado.cargando,
            error = agregado.error,
        )
        val tareasPorMateria = datos.tareas.groupBy { it.materiaId }
        val tarjetas = datos.materias
            .sortedBy { it.materia.creadoEn }
            .map { entrada ->
                val tareas = tareasPorMateria[entrada.materia.id].orEmpty()
                TarjetaMateriaUi(
                    materia = entrada.materia,
                    totalTareas = tareas.size,
                    tareasHechas = tareas.count { it.estado == Estado.COMPLETADA },
                )
            }
        return EstadoUiMaterias(
            cargando = false,
            error = agregado.error,
            tieneDatosEnCache = true,
            materias = tarjetas,
            totalPendientes = datos.materias.sumOf { it.tareasPendientes },
            entregasSemanales = WeekDeliveries.contar(
                fechasEntrega = datos.tareas.map { it.entregaEn },
                ahora = ahora,
                semanaEmpiezaLunes = datos.ajustes.semanaEmpiezaLunes,
                zona = zona,
            ),
        )
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val contenedor = context.contenedorNotally
                SubjectsViewModel(
                    repositorioMaterias = contenedor.repositorioMaterias,
                    repositorioTareas = contenedor.repositorioTareas,
                    repositorioAjustes = contenedor.repositorioAjustes,
                )
            }
        }
    }
}
