package com.example.notally.data.model

data class Curso(
    val id: Long = 0L,
    val nombre: String,
    val profesor: String? = null,
    val creditos: Int? = null,
    val creadoEn: Long = System.currentTimeMillis(),
    /**
     * `sectionId` del Aula Virtual. `null` para cursos creados a mano; la
     * importación lo usa para actualizar en vez de duplicar.
     */
    val externalId: String? = null,
    /** `codeCourse` del Aula Virtual. Opcional y solo para el detalle. */
    val codigo: String? = null,
    /** `codeSection` del Aula Virtual. Opcional. */
    val seccion: String? = null,
    /** Periodo académico seleccionado (p. ej. "2026-2"). Opcional. */
    val periodo: String? = null,
    /** `type` del Aula Virtual (obligatorio/electivo). Opcional. */
    val tipo: String? = null,
)

/** Curso con el número de tareas pendientes y de entregadas del Aula. */
data class CursoConConteo(
    val curso: Curso,
    val tareasPendientes: Int,
    val tareasEntregadas: Int = 0,
)
