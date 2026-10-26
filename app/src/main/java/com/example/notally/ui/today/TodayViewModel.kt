package com.example.notally.ui.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.notally.contenedorNotally
import com.example.notally.data.DateRanges
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Curso
import com.example.notally.data.model.Tarea
import com.example.notally.data.repository.SettingsRepository
import com.example.notally.data.repository.CursosRepository
import com.example.notally.data.repository.TareasRepository
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
data class ResumenHoy(
    val completadas: Int,
    val total: Int,
)

/** Estado vacío que corresponde pintar. */
sealed interface VacioHoy {
    data object Total : VacioHoy
    data object FiltroPendientes : VacioHoy
    data object FiltroCompletadas : VacioHoy
}

data class EstadoUiHoy(
    val cargando: Boolean = true,
    val error: Boolean = false,
    val tieneDatosEnCache: Boolean = false,
    val filtro: FiltroHoy = FiltroHoy.TODAS,
    val antetitulo: String = "",
    val ahora: Long = System.currentTimeMillis(),
    val secciones: List<SeccionHoyUi> = emptyList(),
    val completadasPlanas: List<ElementoHoy> = emptyList(),
    val diasSemana: List<DiaSemanaHoy> = emptyList(),
    val focal: TareaFocalHoy? = null,
    val conteosFiltro: ConteosFiltroHoy = ConteosFiltroHoy(),
    val resumen: ResumenHoy? = null,
    val vacio: VacioHoy? = null,
    val estaVacioTotal: Boolean = true,
)

/** Acción reversible que el snackbar puede deshacer. */
sealed interface DeshacerHoy {
    data class Completada(val tarea: Tarea, val estadoPrevio: Estado) : DeshacerHoy
    data class Eliminacion(val tarea: Tarea, val titulosSubtareas: List<String>) : DeshacerHoy
}

/** Mensajes de una sola vez para la pantalla. */
sealed interface EventoHoy {
    data class Completada(val deshacer: DeshacerHoy) : EventoHoy
    data class Eliminada(val deshacer: DeshacerHoy) : EventoHoy
    data class FalloGuardado(val reintentar: () -> Unit) : EventoHoy
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
    private val repositorioTareas: TareasRepository,
    private val repositorioCursos: CursosRepository,
    private val repositorioSettings: SettingsRepository,
    private val reloj: () -> Long = System::currentTimeMillis,
    private val zona: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private data class DatosCrudos(
        val dia: List<Tarea>,
        val proximas: List<Tarea>,
        val activas: List<Tarea>,
        val cursos: List<Curso>,
        val semanaEmpiezaLunes: Boolean,
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

    private val disparadorRefresco = MutableStateFlow(0)
    private val estadoFiltro = MutableStateFlow(FiltroHoy.TODAS)
    private val expansionManual =
        MutableStateFlow<Map<TipoSeccionHoy, Boolean>>(emptyMap())

    private val _eventos = MutableSharedFlow<EventoHoy>(extraBufferCapacity = 8)
    val eventos: SharedFlow<EventoHoy> = _eventos.asSharedFlow()

    private val flujoReloj: Flow<Long> = flow {
        while (true) {
            val ahora = reloj()
            emit(ahora)
            delay(60_000L - (ahora % 60_000L))
        }
    }

    private val flujoInicioHoy: Flow<Long> = flujoReloj
        .map { DateRanges.rangoDia(it, zona).first }
        .distinctUntilChanged()

    private val estadosCarga: Flow<EstadoCarga> = disparadorRefresco.flatMapLatest {
        flujoInicioHoy.flatMapLatest { inicio ->
            val hoy = Instant.ofEpochMilli(inicio).atZone(zona).toLocalDate()
            val finDia = DateRanges.rangoDia(hoy, zona).last
            // Último milisegundo de hoy: la consulta de futuro usa `>` y así
            // incluye todo lo que empieza mañana a las 00:00, sin tope superior.
            val inicioManana = DateRanges.rangoDia(hoy.plusDays(1), zona).first
            val listo: Flow<EstadoCarga> = combine(
                repositorioTareas.observarTareasDelDia(inicio, finDia),
                repositorioTareas.observarTareasFuturas(inicioManana - 1),
                repositorioTareas.observarTareasActivas(),
                repositorioCursos.observarCursos(),
                repositorioSettings.settings,
            ) { dia, proximas, activas, cursos, settings ->
                EstadoCarga.Listo(
                    DatosCrudos(
                        dia = dia,
                        proximas = proximas,
                        activas = activas,
                        cursos = cursos,
                        semanaEmpiezaLunes = settings.semanaEmpiezaLunes,
                    ),
                )
            }
            listo
                .onStart { emit(EstadoCarga.Cargando) }
                .catch { emit(EstadoCarga.Error) }
        }
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

    val estadoUi: StateFlow<EstadoUiHoy> = combine(
        agregado,
        flujoReloj,
        estadoFiltro,
        expansionManual,
    ) { agregado, ahora, filtro, expansion ->
        construirEstado(agregado, ahora, filtro, expansion)
    }
        .onEach { estado ->
            // Con cero tareas no tiene sentido conservar "Pendientes" o
            // "Completadas"; se vuelve a "Todas" sin tocar el estado durante
            // la carga inicial (donde el vacío es solo transitorio).
            if (estado.tieneDatosEnCache && estado.estaVacioTotal &&
                estadoFiltro.value != FiltroHoy.TODAS
            ) {
                estadoFiltro.value = FiltroHoy.TODAS
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EstadoUiHoy(),
        )

    fun cambiarFiltro(filtro: FiltroHoy) {
        estadoFiltro.value = filtro
    }

    fun alternarSeccion(tipo: TipoSeccionHoy) {
        val actual = estadoUi.value.secciones.firstOrNull { it.tipo == tipo }?.expandida
            ?: return
        expansionManual.update { it + (tipo to !actual) }
    }

    fun reintentar() {
        disparadorRefresco.update { it + 1 }
    }

    fun alternarCompletada(tarea: Tarea) {
        val previo = tarea.estado
        val completando = previo != Estado.COMPLETADA
        viewModelScope.launch {
            try {
                repositorioTareas.alternarCompletada(tarea.id, reloj())
                if (completando) {
                    _eventos.emit(
                        EventoHoy.Completada(DeshacerHoy.Completada(tarea, previo)),
                    )
                }
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (_: Exception) {
                _eventos.emit(EventoHoy.FalloGuardado { alternarCompletada(tarea) })
            }
        }
    }

    /** Swipe "Completar": no reabre una tarea ya completada. */
    fun completar(tarea: Tarea) {
        if (tarea.estado != Estado.COMPLETADA) alternarCompletada(tarea)
    }

    fun posponerParaManana(tarea: Tarea) {
        viewModelScope.launch {
            try {
                repositorioTareas.actualizarTarea(
                    tarea.copy(entregaEn = posponerEntrega(tarea.entregaEn, reloj(), zona)),
                )
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (_: Exception) {
                _eventos.emit(EventoHoy.FalloGuardado { posponerParaManana(tarea) })
            }
        }
    }

    fun eliminar(tarea: Tarea) {
        viewModelScope.launch {
            try {
                val instantanea = repositorioTareas.obtenerTareaConSubtareas(tarea.id)
                val titulos = instantanea?.subtareas
                    ?.sortedBy { it.posicion }
                    ?.map { it.titulo }
                    .orEmpty()
                repositorioTareas.eliminarTarea(tarea.id)
                _eventos.emit(EventoHoy.Eliminada(DeshacerHoy.Eliminacion(tarea, titulos)))
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (_: Exception) {
                _eventos.emit(EventoHoy.FalloGuardado { eliminar(tarea) })
            }
        }
    }

    fun deshacer(deshacer: DeshacerHoy) {
        viewModelScope.launch {
            try {
                when (deshacer) {
                    is DeshacerHoy.Completada ->
                        repositorioTareas.cambiarEstado(
                            deshacer.tarea.id,
                            deshacer.estadoPrevio,
                            reloj(),
                        )

                    is DeshacerHoy.Eliminacion ->
                        repositorioTareas.crearTarea(
                            deshacer.tarea.copy(id = 0L),
                            deshacer.titulosSubtareas,
                        )
                }
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (_: Exception) {
                _eventos.emit(EventoHoy.FalloGuardado { deshacer(deshacer) })
            }
        }
    }

    private fun construirEstado(
        agregado: Agregado,
        ahora: Long,
        filtro: FiltroHoy,
        expansion: Map<TipoSeccionHoy, Boolean>,
    ): EstadoUiHoy {
        val antetitulo = DateFormat.antetitulo(ahora, zona)
        val datos = agregado.datos
        if (datos == null) {
            return EstadoUiHoy(
                cargando = agregado.cargando,
                error = agregado.error,
                antetitulo = antetitulo,
                ahora = ahora,
                estaVacioTotal = true,
            )
        }

        val cursosPorId = datos.cursos.associateBy { it.id }
        val grupos = agruparHoy(
            ahora = ahora,
            zona = zona,
            tareasDelDia = datos.dia,
            tareasProximas = datos.proximas,
            tareasActivas = datos.activas,
            cursosPorId = cursosPorId,
            semanaEmpiezaLunes = datos.semanaEmpiezaLunes,
        )

        val secciones = if (filtro == FiltroHoy.COMPLETADAS) {
            emptyList()
        } else {
            construirSeccionesHoy(grupos, filtro, expansion)
        }

        val vacio = when {
            agregado.cargando -> null
            grupos.estaVacio -> VacioHoy.Total
            // Con historial pero sin secciones de hoy, "Todas" no tiene filas:
            // se ofrece el vacío total; los chips siguen visibles para llegar
            // a "Completadas" (§7.7).
            filtro == FiltroHoy.COMPLETADAS ->
                if (grupos.historialCompletadas.isEmpty()) VacioHoy.FiltroCompletadas else null

            filtro == FiltroHoy.PENDIENTES ->
                if (!grupos.tienePendientes) VacioHoy.FiltroPendientes else null

            !grupos.tieneContenidoSecciones -> VacioHoy.Total
            else -> null
        }

        return EstadoUiHoy(
            cargando = false,
            error = agregado.error,
            tieneDatosEnCache = true,
            filtro = filtro,
            antetitulo = antetitulo,
            ahora = ahora,
            secciones = secciones,
            completadasPlanas = if (filtro == FiltroHoy.COMPLETADAS) {
                grupos.historialCompletadas
            } else {
                emptyList()
            },
            diasSemana = grupos.diasSemana,
            focal = if (filtro == FiltroHoy.COMPLETADAS) null else grupos.focal,
            conteosFiltro = grupos.conteosFiltro,
            resumen = if (grupos.resumenTotal > 0) {
                ResumenHoy(
                    completadas = grupos.resumenCompletadas,
                    total = grupos.resumenTotal,
                )
            } else {
                null
            },
            vacio = vacio,
            estaVacioTotal = grupos.estaVacio,
        )
    }

    companion object {
        /** Factory sin Hilt: lee el contenedor manual de la aplicación. */
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val contenedor = context.contenedorNotally
                TodayViewModel(
                    repositorioTareas = contenedor.repositorioTareas,
                    repositorioCursos = contenedor.repositorioCursos,
                    repositorioSettings = contenedor.repositorioSettings,
                )
            }
        }
    }
}
