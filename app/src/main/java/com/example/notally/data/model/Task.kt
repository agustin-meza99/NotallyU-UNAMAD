package com.example.notally.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Tarea o entrega académica.
 *
 * [subjectId] nulo representa una tarea sin materia asignada.
 * [dueAt] nulo representa una tarea sin fecha límite.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["dueAt"]),
        Index(value = ["status"]),
    ],
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val notes: String = "",
    val subjectId: Long? = null,
    val dueAt: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val status: Status = Status.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isArchived: Boolean = false,
)

enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
}

enum class Status {
    PENDING,
    IN_PROGRESS,
    DONE,
}

/** Tarea con sus subtareas, usada para el detalle. */
data class TaskWithSubtasks(
    @Embedded
    val task: Task,
    @Relation(parentColumn = "id", entityColumn = "taskId")
    val subtasks: List<Subtask>,
)
