package com.example.notally.data

import androidx.room.withTransaction
import com.example.notally.data.model.Priority
import com.example.notally.data.model.Status
import com.example.notally.data.model.Subject
import com.example.notally.data.model.Subtask
import com.example.notally.data.model.Task
import java.time.Instant
import java.time.ZoneId

/**
 * Datos de ejemplo en español para builds de **debug/demo**.
 *
 * Excepción consciente a la regla del DESIGN.md que prohíbe datos de ejemplo en
 * producción: aquí sirven para revisar la UI sin trabajo manual y **nunca** se
 * siembran en release (`AppContainer.seedEnabled` usa el flag debug de la app).
 * El contenedor consulta el flag persistido `seed_done` antes de llamar a
 * [populateIfEmpty], de modo que vaciar las tablas no vuelve a llenarlas.
 */
object SeedData {

    private const val DAY_MILLIS = 86_400_000L

    private data class SeedSubject(
        val name: String,
        val colorArgb: Long,
        val professor: String,
        val credits: Int,
    )

    private data class SeedTask(
        val title: String,
        val notes: String = "",
        val subjectIndex: Int? = null,
        val dueInDays: Int? = null,
        val dueHour: Int = 23,
        val dueMinute: Int = 59,
        val priority: Priority = Priority.MEDIUM,
        val status: Status = Status.PENDING,
        val createdDaysAgo: Int = 1,
        val subtaskTitles: List<String> = emptyList(),
    )

    private val SUBJECTS = listOf(
        SeedSubject("Cálculo III", SubjectColors.INDIGO, "Dra. Mariana Quispe", 4),
        SeedSubject("Estructuras de Datos", SubjectColors.FOREST, "Ing. Carlos Mendoza", 5),
        SeedSubject("Bases de Datos", SubjectColors.TERRACOTTA, "Mg. Lucía Fernández", 4),
        SeedSubject("Ética Profesional", SubjectColors.OCHRE, "Dr. Jorge Salazar", 2),
    )

    private val TASKS = listOf(
        SeedTask(
            title = "Proyecto 1: analizador léxico",
            notes = "Entregar el repositorio con el lexer y el informe de pruebas.",
            subjectIndex = 1,
            dueInDays = 3,
            priority = Priority.HIGH,
            createdDaysAgo = 4,
            subtaskTitles = listOf(
                "Definir la gramática del lenguaje",
                "Implementar el analizador léxico",
                "Escribir pruebas unitarias",
            ),
        ),
        SeedTask(
            title = "Taller de normalización de bases de datos",
            notes = "Normalizar el esquema hasta 3FN y justificar cada decisión.",
            subjectIndex = 2,
            dueInDays = 1,
            dueHour = 10,
            dueMinute = 0,
            priority = Priority.MEDIUM,
        ),
        SeedTask(
            title = "Parcial de Cálculo III",
            notes = "Capítulos 4 y 5: integrales múltiples y campos vectoriales.",
            subjectIndex = 0,
            dueInDays = 5,
            dueHour = 8,
            dueMinute = 0,
            priority = Priority.HIGH,
            createdDaysAgo = 7,
        ),
        SeedTask(
            title = "Ensayo sobre consentimiento informado",
            notes = "Mínimo 1500 palabras con al menos tres fuentes académicas.",
            subjectIndex = 3,
            dueInDays = 7,
            priority = Priority.MEDIUM,
            createdDaysAgo = 2,
        ),
        SeedTask(
            title = "Laboratorio de árboles AVL",
            notes = "Implementar inserción, eliminación y rotaciones.",
            subjectIndex = 1,
            dueInDays = 0,
            dueHour = 18,
            dueMinute = 0,
            priority = Priority.HIGH,
            status = Status.IN_PROGRESS,
            createdDaysAgo = 3,
            subtaskTitles = listOf("Rotaciones simples", "Rotaciones dobles"),
        ),
        SeedTask(
            title = "Consultas SQL avanzadas",
            notes = "Resolver los diez ejercicios del cuadernillo 3.",
            subjectIndex = 2,
            dueInDays = 2,
            priority = Priority.MEDIUM,
            subtaskTitles = listOf("JOINs", "Subconsultas", "Funciones de ventana"),
        ),
        SeedTask(
            title = "Ejercicios de series de Fourier",
            notes = "Página 210, ejercicios impares.",
            subjectIndex = 0,
            dueInDays = 1,
            dueHour = 7,
            dueMinute = 0,
            priority = Priority.LOW,
            createdDaysAgo = 2,
        ),
        SeedTask(
            title = "Exposición: sesgos algorítmicos",
            notes = "Preparar diapositivas y un caso práctico para el debate.",
            subjectIndex = 3,
            dueInDays = 9,
            dueHour = 14,
            dueMinute = 0,
            priority = Priority.MEDIUM,
            createdDaysAgo = 1,
        ),
        SeedTask(
            title = "Diseño del modelo entidad-relación",
            notes = "Diagrama entregado y validado con el docente.",
            subjectIndex = 2,
            dueInDays = -2,
            priority = Priority.LOW,
            status = Status.DONE,
            createdDaysAgo = 6,
        ),
        SeedTask(
            title = "Quiz de estructuras de datos",
            notes = "Temas: pilas, colas y listas enlazadas.",
            subjectIndex = 1,
            dueInDays = 4,
            dueHour = 10,
            dueMinute = 0,
            priority = Priority.MEDIUM,
        ),
        SeedTask(
            title = "Renovar el carné universitario",
            notes = "Llevar fotografía y comprobante de matrícula.",
            subjectIndex = null,
            dueInDays = 2,
            dueHour = 12,
            dueMinute = 0,
            priority = Priority.LOW,
        ),
    )

    /**
     * Inserta materias, tareas y subtareas si no existe ningún registro.
     * Devuelve `true` cuando se sembraron datos.
     */
    suspend fun populateIfEmpty(
        database: NotallyDatabase,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        if (database.subjectDao().count() > 0 || database.taskDao().count() > 0) {
            return false
        }
        val zone = ZoneId.systemDefault()

        database.withTransaction {
            val subjectIds = SUBJECTS.map { seed ->
                database.subjectDao().insert(
                    Subject(
                        name = seed.name,
                        colorArgb = seed.colorArgb,
                        professor = seed.professor,
                        credits = seed.credits,
                        createdAt = now - 30 * DAY_MILLIS,
                    ),
                )
            }

            TASKS.forEach { seed ->
                val createdAt = now - seed.createdDaysAgo * DAY_MILLIS
                val completedAt = if (seed.status == Status.DONE) {
                    now - DAY_MILLIS
                } else {
                    null
                }
                val taskId = database.taskDao().insert(
                    Task(
                        title = seed.title,
                        notes = seed.notes,
                        subjectId = seed.subjectIndex?.let(subjectIds::get),
                        dueAt = seed.dueInDays?.let { days ->
                            dueAt(
                                now = now,
                                days = days,
                                hour = seed.dueHour,
                                minute = seed.dueMinute,
                                zone = zone,
                            )
                        },
                        priority = seed.priority,
                        status = seed.status,
                        createdAt = createdAt,
                        updatedAt = completedAt ?: createdAt,
                        completedAt = completedAt,
                    ),
                )
                if (seed.subtaskTitles.isNotEmpty()) {
                    database.subtaskDao().insertAll(
                        seed.subtaskTitles.mapIndexed { index, title ->
                            Subtask(taskId = taskId, title = title, position = index)
                        },
                    )
                }
            }
        }
        return true
    }

    private fun dueAt(now: Long, days: Int, hour: Int, minute: Int, zone: ZoneId): Long =
        Instant.ofEpochMilli(now)
            .atZone(zone)
            .toLocalDate()
            .plusDays(days.toLong())
            .atTime(hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
}
