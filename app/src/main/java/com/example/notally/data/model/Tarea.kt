package com.example.notally.data.model

/**
 * Tarea o entrega académica.
 *
 * [cursoId] nulo representa una tarea sin curso asignado.
 * [entregaEn] nulo representa una tarea sin fecha límite.
 */
data class Tarea(
    val id: Long = 0L,
    val titulo: String,
    val notas: String = "",
    val cursoId: Long? = null,
    val entregaEn: Long? = null,
    val prioridad: Prioridad = Prioridad.MEDIA,
    val estado: Estado = Estado.PENDIENTE,
    val creadoEn: Long = System.currentTimeMillis(),
    val actualizadoEn: Long = System.currentTimeMillis(),
    val completadoEn: Long? = null,
    val archivada: Boolean = false,
    /**
     * `id` de la tarea del Aula Virtual. `null` para tareas creadas a mano;
     * la importación lo usa para actualizar en vez de duplicar.
     */
    val externalId: String? = null,
    /** Unidad del Aula Virtual. Opcional y solo informativa. */
    val unidad: String? = null,
    /** `dateBegin` del Aula Virtual en epoch (America/Lima). Opcional. */
    val inicioEn: Long? = null,
    /** Intentos permitidos por el Aula Virtual. Opcional. */
    val intentos: Int? = null,
    /** `isGroup` del Aula Virtual. La UI solo muestra la etiqueta "Grupal". */
    val esGrupal: Boolean = false,
    /**
     * `homeworkstds > 0` del Aula Virtual: el alumno ya tiene entregas. Es
     * informativo y no altera el estado, la prioridad ni la sincronización.
     */
    val entregada: Boolean = false,
    val calificacion: Double? = null,
    val retroalimentacion: String? = null,
)

enum class Prioridad {
    BAJA,
    MEDIA,
    ALTA,
}

enum class Estado {
    PENDIENTE,
    EN_CURSO,
    COMPLETADA,
}

/** Tarea con sus subtareas, usada para el detalle. */
data class TareaConSubtareas(
    val tarea: Tarea,
    val subtareas: List<Subtarea>,
)
