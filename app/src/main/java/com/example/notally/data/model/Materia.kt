package com.example.notally.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Materia o asignatura universitaria.
 *
 * La tabla y las columnas físicas conservan sus nombres en inglés.
 *
 * @property color color de acento en formato ARGB (por ejemplo `0xFF6C5CE7`).
 */
@Entity(
    tableName = "subjects",
    indices = [Index(value = ["name"], unique = true)],
)
data class Materia(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "name")
    val nombre: String,
    @ColumnInfo(name = "colorArgb")
    val color: Long,
    @ColumnInfo(name = "professor")
    val profesor: String? = null,
    @ColumnInfo(name = "credits")
    val creditos: Int? = null,
    @ColumnInfo(name = "createdAt")
    val creadoEn: Long = System.currentTimeMillis(),
)

/** Materia con el número de tareas no completadas y no archivadas. */
data class MateriaConConteo(
    @Embedded
    val materia: Materia,
    @ColumnInfo(name = "pendingTaskCount")
    val tareasPendientes: Int,
)
