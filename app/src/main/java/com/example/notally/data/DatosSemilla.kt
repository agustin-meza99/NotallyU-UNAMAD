package com.example.notally.data

import androidx.room.withTransaction
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Materia
import com.example.notally.data.model.Prioridad
import com.example.notally.data.model.Subtarea
import com.example.notally.data.model.Tarea
import java.time.Instant
import java.time.ZoneId

/**
 * Datos de ejemplo en español para builds de **debug/demo**.
 *
 * Excepción consciente a la regla del DESIGN.md que prohíbe datos de ejemplo en
 * producción: aquí sirven para revisar la UI sin trabajo manual y **nunca** se
 * siembran en release (`ContenedorApp.semillaActiva` usa el flag debug de la
 * app). El contenedor consulta el flag persistido `seed_done` antes de llamar a
 * [poblarSiVacio], de modo que vaciar las tablas no vuelve a llenarlas.
 */
object DatosSemilla {

    private const val MILLIS_DIA = 86_400_000L

    private data class MateriaSemilla(
        val nombre: String,
        val color: Long,
        val profesor: String,
        val creditos: Int,
    )

    private data class TareaSemilla(
        val titulo: String,
        val notas: String = "",
        val indiceMateria: Int? = null,
        val diasHastaEntrega: Int? = null,
        val horaEntrega: Int = 23,
        val minutoEntrega: Int = 59,
        val prioridad: Prioridad = Prioridad.MEDIA,
        val estado: Estado = Estado.PENDIENTE,
        val creadaHaceDias: Int = 1,
        val titulosSubtareas: List<String> = emptyList(),
    )

    private val MATERIAS = listOf(
        MateriaSemilla("Cálculo III", ColoresMateria.INDIGO, "Dra. Mariana Quispe", 4),
        MateriaSemilla("Estructuras de Datos", ColoresMateria.FOREST, "Ing. Carlos Mendoza", 5),
        MateriaSemilla("Bases de Datos", ColoresMateria.TERRACOTTA, "Mg. Lucía Fernández", 4),
        MateriaSemilla("Ética Profesional", ColoresMateria.LIME, "Dr. Jorge Salazar", 2),
    )

    private val TAREAS = listOf(
        TareaSemilla(
            titulo = "Proyecto 1: analizador léxico",
            notas = "Entregar el repositorio con el lexer y el informe de pruebas.",
            indiceMateria = 1,
            diasHastaEntrega = 3,
            prioridad = Prioridad.ALTA,
            creadaHaceDias = 4,
            titulosSubtareas = listOf(
                "Definir la gramática del lenguaje",
                "Implementar el analizador léxico",
                "Escribir pruebas unitarias",
            ),
        ),
        TareaSemilla(
            titulo = "Taller de normalización de bases de datos",
            notas = "Normalizar el esquema hasta 3FN y justificar cada decisión.",
            indiceMateria = 2,
            diasHastaEntrega = 1,
            horaEntrega = 10,
            minutoEntrega = 0,
            prioridad = Prioridad.MEDIA,
        ),
        TareaSemilla(
            titulo = "Parcial de Cálculo III",
            notas = "Capítulos 4 y 5: integrales múltiples y campos vectoriales.",
            indiceMateria = 0,
            diasHastaEntrega = 5,
            horaEntrega = 8,
            minutoEntrega = 0,
            prioridad = Prioridad.ALTA,
            creadaHaceDias = 7,
        ),
        TareaSemilla(
            titulo = "Ensayo sobre consentimiento informado",
            notas = "Mínimo 1500 palabras con al menos tres fuentes académicas.",
            indiceMateria = 3,
            diasHastaEntrega = 7,
            prioridad = Prioridad.MEDIA,
            creadaHaceDias = 2,
        ),
        TareaSemilla(
            titulo = "Laboratorio de árboles AVL",
            notas = "Implementar inserción, eliminación y rotaciones.",
            indiceMateria = 1,
            diasHastaEntrega = 0,
            horaEntrega = 18,
            minutoEntrega = 0,
            prioridad = Prioridad.ALTA,
            estado = Estado.EN_CURSO,
            creadaHaceDias = 3,
            titulosSubtareas = listOf("Rotaciones simples", "Rotaciones dobles"),
        ),
        TareaSemilla(
            titulo = "Consultas SQL avanzadas",
            notas = "Resolver los diez ejercicios del cuadernillo 3.",
            indiceMateria = 2,
            diasHastaEntrega = 2,
            prioridad = Prioridad.MEDIA,
            titulosSubtareas = listOf("JOINs", "Subconsultas", "Funciones de ventana"),
        ),
        TareaSemilla(
            titulo = "Ejercicios de series de Fourier",
            notas = "Página 210, ejercicios impares.",
            indiceMateria = 0,
            diasHastaEntrega = 1,
            horaEntrega = 7,
            minutoEntrega = 0,
            prioridad = Prioridad.BAJA,
            creadaHaceDias = 2,
        ),
        TareaSemilla(
            titulo = "Exposición: sesgos algorítmicos",
            notas = "Preparar diapositivas y un caso práctico para el debate.",
            indiceMateria = 3,
            diasHastaEntrega = 9,
            horaEntrega = 14,
            minutoEntrega = 0,
            prioridad = Prioridad.MEDIA,
            creadaHaceDias = 1,
        ),
        TareaSemilla(
            titulo = "Diseño del modelo entidad-relación",
            notas = "Diagrama entregado y validado con el docente.",
            indiceMateria = 2,
            diasHastaEntrega = -2,
            prioridad = Prioridad.BAJA,
            estado = Estado.COMPLETADA,
            creadaHaceDias = 6,
        ),
        TareaSemilla(
            titulo = "Quiz de estructuras de datos",
            notas = "Temas: pilas, colas y listas enlazadas.",
            indiceMateria = 1,
            diasHastaEntrega = 4,
            horaEntrega = 10,
            minutoEntrega = 0,
            prioridad = Prioridad.MEDIA,
        ),
        TareaSemilla(
            titulo = "Renovar el carné universitario",
            notas = "Llevar fotografía y comprobante de matrícula.",
            indiceMateria = null,
            diasHastaEntrega = 2,
            horaEntrega = 12,
            minutoEntrega = 0,
            prioridad = Prioridad.BAJA,
        ),
    )

    /**
     * Inserta materias, tareas y subtareas si no existe ningún registro.
     * Devuelve `true` cuando se sembraron datos.
     */
    suspend fun poblarSiVacio(
        baseDatos: BaseDatosNotally,
        ahora: Long = System.currentTimeMillis(),
    ): Boolean {
        if (baseDatos.materiaDao().contar() > 0 || baseDatos.tareaDao().contar() > 0) {
            return false
        }
        val zona = ZoneId.systemDefault()

        baseDatos.withTransaction {
            val idsMaterias = MATERIAS.map { semilla ->
                baseDatos.materiaDao().insertar(
                    Materia(
                        nombre = semilla.nombre,
                        color = semilla.color,
                        profesor = semilla.profesor,
                        creditos = semilla.creditos,
                        creadoEn = ahora - 30 * MILLIS_DIA,
                    ),
                )
            }

            TAREAS.forEach { semilla ->
                val creadaEn = ahora - semilla.creadaHaceDias * MILLIS_DIA
                val completadaEn = if (semilla.estado == Estado.COMPLETADA) {
                    ahora - MILLIS_DIA
                } else {
                    null
                }
                val tareaId = baseDatos.tareaDao().insertar(
                    Tarea(
                        titulo = semilla.titulo,
                        notas = semilla.notas,
                        materiaId = semilla.indiceMateria?.let(idsMaterias::get),
                        entregaEn = semilla.diasHastaEntrega?.let { dias ->
                            fechaEntrega(
                                ahora = ahora,
                                dias = dias,
                                hora = semilla.horaEntrega,
                                minuto = semilla.minutoEntrega,
                                zona = zona,
                            )
                        },
                        prioridad = semilla.prioridad,
                        estado = semilla.estado,
                        creadoEn = creadaEn,
                        actualizadoEn = completadaEn ?: creadaEn,
                        completadoEn = completadaEn,
                    ),
                )
                if (semilla.titulosSubtareas.isNotEmpty()) {
                    baseDatos.subtareaDao().insertarTodas(
                        semilla.titulosSubtareas.mapIndexed { indice, titulo ->
                            Subtarea(tareaId = tareaId, titulo = titulo, posicion = indice)
                        },
                    )
                }
            }
        }
        return true
    }

    private fun fechaEntrega(ahora: Long, dias: Int, hora: Int, minuto: Int, zona: ZoneId): Long =
        Instant.ofEpochMilli(ahora)
            .atZone(zona)
            .toLocalDate()
            .plusDays(dias.toLong())
            .atTime(hora, minuto)
            .atZone(zona)
            .toInstant()
            .toEpochMilli()
}
