package com.example.notally.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation

/** Tarea con sus subtareas, usada para el detalle. */
data class TaskWithSubtasks(
    @Embedded
    val task: Task,
    @Relation(parentColumn = "id", entityColumn = "taskId")
    val subtasks: List<Subtask>,
)

/** Materia con el número de tareas no completadas y no archivadas. */
data class SubjectWithTaskCount(
    @Embedded
    val subject: Subject,
    @ColumnInfo(name = "pendingTaskCount")
    val pendingTaskCount: Int,
)
