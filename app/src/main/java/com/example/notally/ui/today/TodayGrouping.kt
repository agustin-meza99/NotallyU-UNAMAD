package com.example.notally.ui.today

import com.example.notally.data.DateRanges
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Curso
import com.example.notally.data.model.Prioridad
import com.example.notally.data.model.Tarea
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Filtro de Hoy (§7.7). No se persiste entre sesiones. */
enum class FiltroHoy(val id: String) {
    TODAS("all"),
    PENDIENTES("pending"),
    COMPLETADAS("done");

    companion object {
        fun desdeId(id: String): FiltroHoy =
            entries.firstOrNull { it.id == id } ?: TODAS
    }
}

/**
 * Secciones de Hoy, en el orden del documento (§7.3). "No entregadas" va antes
 * que "Atrasadas" porque el Aula Virtual no maneja el concepto de atraso.
 */
enum class TipoSeccionHoy {
    NO_ENTREGADAS,
    ATRASADAS,
    HOY,
    SIN_HORA,
    ENTREGADAS,
    PROXIMAS,
    COMPLETADAS,
}

/** Tarea con su curso resuelta, lista para pintar una fila. */
data class ElementoHoy(
    val tarea: Tarea,
    val curso: Curso?,
)

/** Sección ya resuelta para la UI. */
data class SeccionHoyUi(
    val tipo: TipoSeccionHoy,
    val elementos: List<ElementoHoy>,
    val expandida: Boolean,
    val contraible: Boolean,
    val cantidad: Int?,
)

/** Día de la tira semanal (§7.1.1) con su número de pendientes. */
data class DiaSemanaHoy(
    val fecha: LocalDate,
    val pendientes: Int,
)

/** Tarea destacada (§7.3.1): id estable, instantánea vigente y su grupo. */
data class TareaFocalHoy(
    val tareaId: Long,
    val elemento: ElementoHoy,
    val grupo: TipoSeccionHoy,
)

/** Conteos de los chips de filtro (§7.7), calculados antes de extraer la focal. */
data class ConteosFiltroHoy(
    val todas: Int = 0,
    val pendientes: Int = 0,
    val completadas: Int = 0,
)

/**
 * Resultado puro de agrupar las tareas de Hoy.
 *
 * - No entregadas: tareas del Aula (`externalId != null`) sin entregar y vencidas.
 * - Atrasadas: solo tareas manuales vencidas; el Aula no maneja "atrasada".
 * - Entregadas: tareas del Aula ya entregadas y vencidas. Es un estado distinto
 *   del completado, así que no entra en el resumen "X de Y completadas".
 * - Para hoy / Sin hora / Próximas: solo pendientes y ya sin la tarea focal,
 *   para no duplicar su fila.
 * - Completadas: tareas hechas de la agenda de hoy (con o sin hora).
 * - historialCompletadas: todo el historial, para el filtro "Completadas".
 * - resumen: conteo del día (No entregadas + Atrasadas + Para hoy + Sin hora);
 *   Próximas fuera.
 * - conteosFiltro: lo que cuenta cada chip; se mide sobre las listas completas,
 *   así que la focal entra una sola vez. `todas` sigue siendo el conteo de las
 *   secciones de la vista por secciones (§7.7), no el de "Completadas".
 * - estaVacio: no hay nada que mostrar en ningún filtro (ni secciones ni
 *   historial); `tieneContenidoSecciones` responde solo por las secciones.
 */
data class GruposHoy(
    val noEntregadas: List<ElementoHoy> = emptyList(),
    val atrasadas: List<ElementoHoy> = emptyList(),
    val hoy: List<ElementoHoy> = emptyList(),
    val sinHora: List<ElementoHoy> = emptyList(),
    val entregadas: List<ElementoHoy> = emptyList(),
    val proximas: List<ElementoHoy> = emptyList(),
    val completadas: List<ElementoHoy> = emptyList(),
    val historialCompletadas: List<ElementoHoy> = emptyList(),
    val diasSemana: List<DiaSemanaHoy> = emptyList(),
    val focal: TareaFocalHoy? = null,
    val conteosFiltro: ConteosFiltroHoy = ConteosFiltroHoy(),
    val resumenCompletadas: Int = 0,
    val resumenTotal: Int = 0,
    private val agendaPendientes: Int = 0,
    private val proximasPendientes: Int = 0,
) {
    /** Nada que mostrar en ningún filtro: ni secciones ni historial. */
    val estaVacio: Boolean
        get() = conteosFiltro.todas == 0 && historialCompletadas.isEmpty()

    /** Hay al menos una fila (o la focal) en las secciones de Hoy. */
    val tieneContenidoSecciones: Boolean
        get() = conteosFiltro.todas > 0

    val tienePendientes: Boolean
        get() = conteosFiltro.pendientes > 0

    val agendaPendienteVacia: Boolean
        get() = agendaPendientes == 0

    val tieneProximasPendientes: Boolean
        get() = proximasPendientes > 0
}

/**
 * Agrupa por fecha local y estado. Función pura y testeable, sin Android.
 *
 * @param tareasDelDia tareas con `dueAt` dentro de hoy (incluye completadas).
 * @param tareasProximas todo el futuro desde mañana (sin tope superior; incluye
 *   completadas). La consulta no recorta a siete días.
 * @param tareasActivas tareas no archivadas; se usan para "Sin hora", atrasadas,
 *   historial de completadas y el conteo de la tira semanal.
 * @param semanaEmpiezaLunes ajuste "La semana inicia en lunes" (§7.12).
 */
fun agruparHoy(
    ahora: Long,
    zona: ZoneId,
    tareasDelDia: List<Tarea>,
    tareasProximas: List<Tarea>,
    tareasActivas: List<Tarea>,
    cursosPorId: Map<Long, Curso>,
    semanaEmpiezaLunes: Boolean = true,
): GruposHoy {
    val hoy = DateRanges.fechaLocal(ahora, zona)
    val inicioHoy = DateRanges.rangoDia(hoy, zona).first
    val inicioManana = DateRanges.rangoDia(hoy.plusDays(1), zona).first

    fun elemento(tarea: Tarea) = ElementoHoy(
        tarea = tarea,
        curso = tarea.cursoId?.let(cursosPorId::get),
    )

    fun estaPendiente(tarea: Tarea) = tarea.estado != Estado.COMPLETADA

    val activas = tareasActivas.filter { !it.archivada }
    val sinFecha = activas.filter { it.entregaEn == null }

    val elementosNoEntregadas = activas
        .filter {
            estaPendiente(it) && it.externalId != null && !it.entregada &&
                it.entregaEn != null && it.entregaEn < inicioHoy
        }
        .sortedWith(comparadorEntrega)
        .map(::elemento)

    val elementosAtrasadas = activas
        .filter {
            estaPendiente(it) && it.externalId == null &&
                it.entregaEn != null && it.entregaEn < inicioHoy
        }
        .sortedWith(comparadorEntrega)
        .map(::elemento)

    val elementosHoy = tareasDelDia
        .filter { estaPendiente(it) && it.entregaEn != null }
        .sortedWith(comparadorEntrega)
        .map(::elemento)

    val elementosSinHora = sinFecha
        .filter(::estaPendiente)
        .sortedWith(comparadorSinHora)
        .map(::elemento)

    val elementosEntregadas = activas
        .filter {
            it.externalId != null && it.entregada &&
                it.entregaEn != null && it.entregaEn < inicioHoy
        }
        .sortedWith(comparadorEntregadas)
        .map(::elemento)

    val elementosProximas = tareasProximas
        .filter { estaPendiente(it) && it.entregaEn != null && it.entregaEn >= inicioManana }
        .sortedWith(comparadorEntrega)
        .map(::elemento)

    val sinFechaCompletadasHoy = sinFecha.filter { tarea ->
        !estaPendiente(tarea) && tarea.completadoEn?.let {
            DateRanges.fechaLocal(it, zona) == hoy
        } == true
    }

    val elementosCompletadas =
        (tareasDelDia.filter { !estaPendiente(it) } + sinFechaCompletadasHoy)
            .distinctBy { it.id }
            .sortedWith(comparadorCompletadas)
            .map(::elemento)

    val elementosHistorial = activas
        .filter { !estaPendiente(it) }
        .sortedWith(comparadorCompletadas)
        .map(::elemento)

    val sinFechaEnResumen = sinFecha.count { tarea ->
        estaPendiente(tarea) ||
            tarea.completadoEn?.let { DateRanges.fechaLocal(it, zona) == hoy } == true
    }

    // Conteos de los chips (§7.7): sobre las listas completas, sin extraer la
    // focal, de modo que cada número coincide con lo que verá el usuario.
    val cantidadPendientes =
        (elementosNoEntregadas + elementosAtrasadas + elementosHoy + elementosSinHora +
            elementosEntregadas + elementosProximas)
            .distinctBy { it.tarea.id }
            .size
    val cantidadTodas =
        (elementosNoEntregadas + elementosAtrasadas + elementosHoy + elementosSinHora +
            elementosEntregadas + elementosProximas + elementosCompletadas)
            .distinctBy { it.tarea.id }
            .size
    val cantidadCompletadas = elementosHistorial.distinctBy { it.tarea.id }.size

    val focal = seleccionarFocal(
        noEntregadas = elementosNoEntregadas,
        atrasadas = elementosAtrasadas,
        hoy = elementosHoy,
        sinHora = elementosSinHora,
        proximas = elementosProximas,
    )
    val idFiltrado = focal?.tareaId

    return GruposHoy(
        noEntregadas = elementosNoEntregadas.filterNot { it.tarea.id == idFiltrado },
        atrasadas = elementosAtrasadas.filterNot { it.tarea.id == idFiltrado },
        hoy = elementosHoy.filterNot { it.tarea.id == idFiltrado },
        sinHora = elementosSinHora.filterNot { it.tarea.id == idFiltrado },
        entregadas = elementosEntregadas,
        proximas = elementosProximas.filterNot { it.tarea.id == idFiltrado },
        completadas = elementosCompletadas,
        historialCompletadas = elementosHistorial,
        diasSemana = construirDiasSemana(hoy, zona, semanaEmpiezaLunes, activas),
        focal = focal,
        conteosFiltro = ConteosFiltroHoy(
            todas = cantidadTodas,
            pendientes = cantidadPendientes,
            completadas = cantidadCompletadas,
        ),
        resumenCompletadas = elementosCompletadas.size,
        resumenTotal = elementosNoEntregadas.size + elementosAtrasadas.size +
            tareasDelDia.size + sinFechaEnResumen,
        agendaPendientes = elementosNoEntregadas.size + elementosAtrasadas.size +
            elementosHoy.size + elementosSinHora.size,
        proximasPendientes = elementosProximas.size,
    )
}

/**
 * Elige la tarea focal de forma determinista (§7.3.1): grupo No entregadas >
 * Atrasadas > Para hoy > Sin hora > Próximas; dentro, prioridad, fecha, creación
 * e id.
 */
private fun seleccionarFocal(
    noEntregadas: List<ElementoHoy>,
    atrasadas: List<ElementoHoy>,
    hoy: List<ElementoHoy>,
    sinHora: List<ElementoHoy>,
    proximas: List<ElementoHoy>,
): TareaFocalHoy? {
    val grupos = listOf(
        TipoSeccionHoy.NO_ENTREGADAS to noEntregadas,
        TipoSeccionHoy.ATRASADAS to atrasadas,
        TipoSeccionHoy.HOY to hoy,
        TipoSeccionHoy.SIN_HORA to sinHora,
        TipoSeccionHoy.PROXIMAS to proximas,
    )
    grupos.forEach { (tipo, elementos) ->
        val mejor = elementos.minWithOrNull(comparadorFocal) ?: return@forEach
        return TareaFocalHoy(tareaId = mejor.tarea.id, elemento = mejor, grupo = tipo)
    }
    return null
}

private val comparadorFocal: Comparator<ElementoHoy> =
    compareBy<ElementoHoy> { rangoPrioridad(it.tarea) }
        .thenBy { it.tarea.entregaEn ?: Long.MAX_VALUE }
        .thenBy { it.tarea.creadoEn }
        .thenBy { it.tarea.id }

/** Siete días de la semana en curso, con sus pendientes con fecha. */
private fun construirDiasSemana(
    hoy: LocalDate,
    zona: ZoneId,
    semanaEmpiezaLunes: Boolean,
    activas: List<Tarea>,
): List<DiaSemanaHoy> {
    val inicioSemana = DateRanges.inicioSemana(hoy, semanaEmpiezaLunes)
    val pendientesConFecha =
        activas.filter { it.estado != Estado.COMPLETADA && it.entregaEn != null }
    return (0L..6L).map { desplazamiento ->
        val fecha = inicioSemana.plusDays(desplazamiento)
        val rango = DateRanges.rangoDia(fecha, zona)
        DiaSemanaHoy(
            fecha = fecha,
            pendientes = pendientesConFecha.count { tarea ->
                tarea.entregaEn?.let { it in rango } == true
            },
        )
    }
}

/**
 * Construye las secciones visibles según el filtro y el estado de expansión.
 *
 * Por defecto "Próximas" se abre si no hay nada en la agenda de hoy; y
 * "Completadas" se abre si, además, no hay próximas (para no dejar la pantalla
 * vacía, §7.3). Las preferencias manuales del usuario mandan sobre el default.
 */
fun construirSeccionesHoy(
    grupos: GruposHoy,
    filtro: FiltroHoy,
    expansionManual: Map<TipoSeccionHoy, Boolean> = emptyMap(),
): List<SeccionHoyUi> {
    if (filtro == FiltroHoy.COMPLETADAS) return emptyList()

    val proximasPorDefecto = grupos.agendaPendienteVacia && grupos.tieneProximasPendientes
    val completadasPorDefecto = grupos.agendaPendienteVacia && !grupos.tieneProximasPendientes

    fun seccion(
        tipo: TipoSeccionHoy,
        elementos: List<ElementoHoy>,
        contraible: Boolean,
        expandida: Boolean,
        cantidad: Int? = null,
    ) = SeccionHoyUi(
        tipo = tipo,
        elementos = elementos,
        expandida = if (contraible) expansionManual[tipo] ?: expandida else true,
        contraible = contraible,
        cantidad = cantidad,
    )

    return buildList {
        if (grupos.noEntregadas.isNotEmpty()) {
            add(seccion(TipoSeccionHoy.NO_ENTREGADAS, grupos.noEntregadas, false, true))
        }
        if (grupos.atrasadas.isNotEmpty()) {
            add(seccion(TipoSeccionHoy.ATRASADAS, grupos.atrasadas, false, true))
        }
        if (grupos.hoy.isNotEmpty()) {
            add(seccion(TipoSeccionHoy.HOY, grupos.hoy, false, true))
        }
        if (grupos.sinHora.isNotEmpty()) {
            add(seccion(TipoSeccionHoy.SIN_HORA, grupos.sinHora, false, true))
        }
        if (grupos.entregadas.isNotEmpty()) {
            add(
                seccion(
                    tipo = TipoSeccionHoy.ENTREGADAS,
                    elementos = grupos.entregadas,
                    contraible = true,
                    expandida = false,
                    cantidad = grupos.entregadas.size,
                ),
            )
        }
        if (grupos.proximas.isNotEmpty()) {
            add(
                seccion(
                    tipo = TipoSeccionHoy.PROXIMAS,
                    elementos = grupos.proximas,
                    contraible = true,
                    expandida = proximasPorDefecto,
                    cantidad = grupos.proximas.size,
                ),
            )
        }
        if (filtro != FiltroHoy.PENDIENTES && grupos.completadas.isNotEmpty()) {
            add(
                seccion(
                    tipo = TipoSeccionHoy.COMPLETADAS,
                    elementos = grupos.completadas,
                    contraible = true,
                    expandida = completadasPorDefecto,
                    cantidad = grupos.completadas.size,
                ),
            )
        }
    }
}

private val rangoPrioridad: (Tarea) -> Int = {
    when (it.prioridad) {
        Prioridad.ALTA -> 0
        Prioridad.MEDIA -> 1
        Prioridad.BAJA -> 2
    }
}

private fun claveTitulo(tarea: Tarea): String =
    tarea.titulo.lowercase(Locale.forLanguageTag("es"))

private val comparadorEntrega: Comparator<Tarea> =
    compareBy<Tarea> { it.entregaEn ?: Long.MAX_VALUE }
        .thenBy(rangoPrioridad)
        .thenBy(::claveTitulo)

private val comparadorSinHora: Comparator<Tarea> =
    compareBy<Tarea> { it.creadoEn }
        .thenBy(rangoPrioridad)
        .thenBy(::claveTitulo)

/** Entregadas: la más reciente primero, igual que las completadas. */
private val comparadorEntregadas: Comparator<Tarea> =
    compareByDescending<Tarea> { it.entregaEn ?: Long.MIN_VALUE }
        .thenBy(rangoPrioridad)
        .thenBy(::claveTitulo)

private val comparadorCompletadas: Comparator<Tarea> =
    compareByDescending<Tarea> { it.completadoEn ?: it.actualizadoEn }
        .thenBy(rangoPrioridad)
        .thenBy(::claveTitulo)
