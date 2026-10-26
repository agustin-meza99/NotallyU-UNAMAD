package com.example.notally.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Subtarea de una [Tarea]. Se elimina en cascada al borrar la tarea.
 *
 * Las columnas físicas conservan sus nombres en inglés.
 */
@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = Tarea::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["taskId"])],
)
data class Subtarea(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "taskId")
    val tareaId: Long,
    @ColumnInfo(name = "title")
    val titulo: String,
    @ColumnInfo(name = "isDone")
    val hecha: Boolean = false,
    @ColumnInfo(name = "position")
    val posicion: Int = 0,
)
