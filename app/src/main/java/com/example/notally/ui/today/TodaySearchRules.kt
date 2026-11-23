package com.example.notally.ui.today

import com.example.notally.data.model.Tarea
import java.time.Instant
import java.time.ZoneId

/** Franja del día para saludo contextual en el encabezado UNAMAD. */
enum class SaludoDia {
    MANANA,
    TARDE,
    NOCHE,
}

/**
 * Reglas puras de búsqueda, saludo contextual y métricas académicas en Hoy.
 * Sin dependencias de Android para pruebas unitarias rápidas en JVM
 * (ISO/IEC 25010 — Mantenibilidad y Adecuación Funcional).
 */
internal fun saludoSegunHora(
    ahoraMillis: Long,
    zona: ZoneId = ZoneId.systemDefault(),
): SaludoDia {
    val hora = Instant.ofEpochMilli(ahoraMillis).atZone(zona).hour
    return when (hora) {
        in 5..11 -> SaludoDia.MANANA
        in 12..18 -> SaludoDia.TARDE
        else -> SaludoDia.NOCHE
    }
}

/**
 * Filtra una lista de elementos de Hoy por texto libre sobre título, nombre
 * del curso, unidad o notas (insensible a mayúsculas y espacios laterales).
 */
internal fun filtrarElementosPorConsulta(
    elementos: List<ElementoHoy>,
    consulta: String,
): List<ElementoHoy> {
    val q = consulta.trim().lowercase()
    if (q.isEmpty()) return elementos
    return elementos.filter { elem ->
        elem.tarea.titulo.lowercase().contains(q) ||
            (elem.curso?.nombre?.lowercase()?.contains(q) == true) ||
            (elem.tarea.unidad?.lowercase()?.contains(q) == true) ||
            elem.tarea.notas.lowercase().contains(q)
    }
}

/**
 * Filtra las secciones de Hoy conservando solo las secciones con coincidencias
 * y expandiéndolas automáticamente cuando hay una búsqueda activa.
 */
internal fun filtrarSeccionesPorConsulta(
    secciones: List<SeccionHoyUi>,
    consulta: String,
): List<SeccionHoyUi> {
    val q = consulta.trim()
    if (q.isEmpty()) return secciones
    return secciones.mapNotNull { seccion ->
        val filtrados = filtrarElementosPorConsulta(seccion.elementos, q)
        if (filtrados.isEmpty()) {
            null
        } else {
            seccion.copy(
                elementos = filtrados,
                cantidad = filtrados.size,
                expandida = true,
            )
        }
    }
}

/**
 * Calcula el promedio de calificaciones válidas (0.0..20.0) importadas del Aula
 * Virtual UNAMAD. Devuelve `null` si aún no existen tareas calificadas.
 */
internal fun calcularPromedioNotas(tareas: List<Tarea>): Double? {
    val notas = tareas.mapNotNull { it.calificacion }.filter { it in 0.0..20.0 }
    if (notas.isEmpty()) return null
    return notas.average()
}
