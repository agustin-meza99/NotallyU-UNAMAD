package com.example.notally.data.aula

/**
 * Curso tal como lo entrega el Aula Virtual.
 *
 * [sectionId] es el identificador estable de la sección y se usa como
 * `externalId` del curso local.
 */
data class AulaCurso(
    val id: String,
    val codigoCurso: String,
    val sectionId: String,
    val codigoSeccion: String,
    val nombre: String,
    val tipo: String? = null,
    val area: String? = null,
    val departamento: String? = null,
    val facultad: String? = null,
)

/**
 * Tarea/entrega del Aula Virtual ya normalizada por [AulaParser]:
 * la descripción llega sin HTML y las fechas como epoch en milisegundos.
 *
 * [id] es el identificador estable y se usa como `externalId` de la tarea
 * local.
 */
data class AulaTarea(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val unidad: String? = null,
    val inicioEn: Long? = null,
    val entregaEn: Long? = null,
    val finalizada: Boolean = false,
    val intentos: Int = 0,
    val grupal: Boolean = false,
    /** `homeworkstds > 0`: el alumno ya registra entregas en el Aula. */
    val entregada: Boolean = false,
)
