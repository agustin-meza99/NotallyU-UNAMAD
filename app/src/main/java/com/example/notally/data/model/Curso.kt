package com.example.notally.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Curso o asignatura universitaria.
 *
 * La tabla y las columnas físicas conservan sus nombres en inglés.
 *
 * @property color color de acento en formato ARGB (por ejemplo `0xFF3A5A9E`).
 */
@Entity(
    tableName = "subjects",
    indices = [Index(value = ["name"], unique = true)],
)
data class Curso(
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

/** Curso con el número de tareas no completadas y no archivadas. */
data class CursoConConteo(
    @Embedded
    val curso: Curso,
    @ColumnInfo(name = "pendingTaskCount")
    val tareasPendientes: Int,
)
