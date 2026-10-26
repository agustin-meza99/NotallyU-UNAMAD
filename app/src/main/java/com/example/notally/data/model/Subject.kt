package com.example.notally.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Materia o asignatura universitaria.
 *
 * @property colorArgb color de acento en formato ARGB (por ejemplo `0xFF6C5CE7`).
 */
@Entity(
    tableName = "subjects",
    indices = [Index(value = ["name"], unique = true)],
)
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val colorArgb: Long,
    val professor: String? = null,
    val credits: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Materia con el número de tareas no completadas y no archivadas. */
data class SubjectWithTaskCount(
    @Embedded
    val subject: Subject,
    @ColumnInfo(name = "pendingTaskCount")
    val pendingTaskCount: Int,
)
