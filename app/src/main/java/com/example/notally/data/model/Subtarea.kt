package com.example.notally.data.model

/**
 * Subtarea de una [Tarea].
 */
data class Subtarea(
    val id: Long = 0L,
    val tareaId: Long,
    val titulo: String,
    val hecha: Boolean = false,
    val posicion: Int = 0,
)
