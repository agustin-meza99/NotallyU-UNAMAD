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
 */
@Entity(
    tableName = "subjects",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["externalId"], unique = true),
    ],
)
data class Curso(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "name")
    val nombre: String,
    @ColumnInfo(name = "professor")
    val profesor: String? = null,
    @ColumnInfo(name = "credits")
    val creditos: Int? = null,
    @ColumnInfo(name = "createdAt")
    val creadoEn: Long = System.currentTimeMillis(),
    /**
     * `sectionId` del Aula Virtual. `null` para cursos creados a mano; la
     * importación lo usa para actualizar en vez de duplicar.
     */
    @ColumnInfo(name = "externalId")
    val externalId: String? = null,
    /** `codeCourse` del Aula Virtual. Opcional y solo para el detalle. */
    @ColumnInfo(name = "codigo")
    val codigo: String? = null,
    /** `codeSection` del Aula Virtual. Opcional. */
    @ColumnInfo(name = "seccion")
    val seccion: String? = null,
    /** Periodo académico seleccionado (p. ej. "2026-2"). Opcional. */
    @ColumnInfo(name = "periodo")
    val periodo: String? = null,
    /** `type` del Aula Virtual (obligatorio/electivo). Opcional. */
    @ColumnInfo(name = "tipo")
    val tipo: String? = null,
)

/** Curso con el número de tareas no completadas y no archivadas. */
data class CursoConConteo(
    @Embedded
    val curso: Curso,
    @ColumnInfo(name = "pendingTaskCount")
    val tareasPendientes: Int,
)
