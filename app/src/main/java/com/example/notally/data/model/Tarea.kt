package com.example.notally.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Tarea o entrega académica.
 *
 * Los nombres físicos de la tabla y de las columnas se mantienen en inglés
 * mediante anotaciones de Room: el esquema no cambia.
 *
 * [cursoId] nulo representa una tarea sin curso asignado.
 * [entregaEn] nulo representa una tarea sin fecha límite.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Curso::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["dueAt"]),
        Index(value = ["status"]),
        Index(value = ["externalId"], unique = true),
    ],
)
data class Tarea(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "title")
    val titulo: String,
    @ColumnInfo(name = "notes")
    val notas: String = "",
    @ColumnInfo(name = "subjectId")
    val cursoId: Long? = null,
    @ColumnInfo(name = "dueAt")
    val entregaEn: Long? = null,
    @ColumnInfo(name = "priority")
    val prioridad: Prioridad = Prioridad.MEDIA,
    @ColumnInfo(name = "status")
    val estado: Estado = Estado.PENDIENTE,
    @ColumnInfo(name = "createdAt")
    val creadoEn: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updatedAt")
    val actualizadoEn: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completedAt")
    val completadoEn: Long? = null,
    @ColumnInfo(name = "isArchived")
    val archivada: Boolean = false,
    /**
     * `id` de la tarea del Aula Virtual. `null` para tareas creadas a mano;
     * la importación lo usa para actualizar en vez de duplicar.
     */
    @ColumnInfo(name = "externalId")
    val externalId: String? = null,
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
    @Embedded
    val tarea: Tarea,
    @Relation(parentColumn = "id", entityColumn = "taskId")
    val subtareas: List<Subtarea>,
)
