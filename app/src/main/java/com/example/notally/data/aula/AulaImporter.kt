package com.example.notally.data.aula

import com.example.notally.data.model.Curso
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Tarea
import com.example.notally.data.repository.CursosRepository
import com.example.notally.data.repository.ResultadoUpsert
import com.example.notally.data.repository.TareasRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Progreso de la importación, curso a curso. */
data class ProgresoAula(
    val cursosTotales: Int,
    val cursoActual: Int,
    val cursoNombre: String,
    val tareasNuevas: Int,
    val tareasActualizadas: Int,
)

/** Resultado final de una importación. */
sealed interface ResultadoImportacion {
    data class Exito(
        val cursos: Int,
        val tareasNuevas: Int,
        val tareasActualizadas: Int,
        val errores: Int,
        val ahora: Long,
    ) : ResultadoImportacion

    data class Fallo(
        val tipo: ErrorAula,
        val mensajeServidor: String? = null,
    ) : ResultadoImportacion
}

/**
 * Importa cursos y tareas del Aula Virtual a Room.
 *
 * Es idempotente: cada curso se identifica por `sectionId` y cada tarea por su
 * `id`, así que reimportar actualiza las filas existentes en vez de duplicar.
 * Las tareas ya completadas por el usuario conservan su estado, prioridad y
 * archivo; solo se refrescan título, notas, curso y fecha de entrega.
 *
 * Respeta un intervalo de [PAUSA_ENTRE_PETICIONES_MS] para no golpear el
 * servidor. Un solo intento de login por llamada: nunca reintenta.
 */
class AulaImporter(
    private val repositorioCursos: CursosRepository,
    private val repositorioTareas: TareasRepository,
    private val reloj: () -> Long = System::currentTimeMillis,
    private val espera: suspend (Long) -> Unit = { delay(it) },
) {

    suspend fun importar(
        cliente: AulaApi,
        usuario: String,
        clave: String,
        alProgreso: (ProgresoAula) -> Unit = {},
    ): ResultadoImportacion {
        try {
            when (val login = cliente.iniciarSesion(usuario, clave)) {
                ResultadoLogin.Exito -> Unit
                is ResultadoLogin.Fallo -> return ResultadoImportacion.Fallo(
                    login.tipo,
                    login.mensajeServidor,
                )
            }

            espera(PAUSA_ENTRE_PETICIONES_MS)
            // El periodo es informativo: si falla, la importación continúa.
            val periodo = try {
                cliente.periodoAcademico()
            } catch (_: AulaException) {
                null
            }

            espera(PAUSA_ENTRE_PETICIONES_MS)
            val cursos = try {
                cliente.cursos()
            } catch (error: AulaException) {
                return ResultadoImportacion.Fallo(error.tipo, error.mensajeServidor)
            }
            if (cursos.isEmpty()) {
                return ResultadoImportacion.Fallo(
                    ErrorAula.SIN_CURSOS,
                    "El Aula Virtual no devolvió cursos para esta cuenta.",
                )
            }

            val ahora = reloj()
            var tareasNuevas = 0
            var tareasActualizadas = 0
            var errores = 0
            var cursosImportados = 0

            cursos.forEachIndexed { indice, cursoAula ->
                val cursoLocal = Curso(
                    nombre = cursoAula.nombre,
                    externalId = cursoAula.sectionId,
                    creadoEn = ahora,
                    codigo = cursoAula.codigoCurso.takeUnless { it.isBlank() },
                    seccion = cursoAula.codigoSeccion.takeUnless { it.isBlank() },
                    periodo = periodo,
                    tipo = cursoAula.tipo,
                )
                val resultadoCurso = repositorioCursos.upsertCursoExterno(cursoLocal)
                when (resultadoCurso) {
                    is ResultadoUpsert.Creado,
                    is ResultadoUpsert.Actualizado,
                    -> Unit

                    ResultadoUpsert.Error -> {
                        errores++
                        alProgreso(
                            ProgresoAula(
                                cursosTotales = cursos.size,
                                cursoActual = indice + 1,
                                cursoNombre = cursoAula.nombre,
                                tareasNuevas = tareasNuevas,
                                tareasActualizadas = tareasActualizadas,
                            ),
                        )
                        return@forEachIndexed
                    }
                }

                val cursoId = when (resultadoCurso) {
                    is ResultadoUpsert.Creado -> resultadoCurso.id
                    is ResultadoUpsert.Actualizado -> resultadoCurso.id
                    ResultadoUpsert.Error -> return@forEachIndexed
                }
                cursosImportados++

                espera(PAUSA_ENTRE_PETICIONES_MS)
                val tareas = try {
                    cliente.tareas(cursoAula.sectionId)
                } catch (error: AulaException) {
                    if (error.tipo == ErrorAula.SESION_CADUCADA || error.tipo == ErrorAula.BLOQUEO) {
                        return ResultadoImportacion.Fallo(error.tipo, error.mensajeServidor)
                    }
                    errores++
                    alProgreso(
                        ProgresoAula(
                            cursosTotales = cursos.size,
                            cursoActual = indice + 1,
                            cursoNombre = cursoAula.nombre,
                            tareasNuevas = tareasNuevas,
                            tareasActualizadas = tareasActualizadas,
                        ),
                    )
                    return@forEachIndexed
                }

                tareas.forEach { tareaAula ->
                    val existente = repositorioTareas
                        .obtenerTareaPorExternalId(tareaAula.id)
                    val nota = tareaAula.descripcion
                        .takeUnless { it.isBlank() }
                        ?: tareaAula.unidad.orEmpty()
                    val base = Tarea(
                        titulo = tareaAula.titulo,
                        notas = nota,
                        cursoId = cursoId,
                        entregaEn = tareaAula.entregaEn,
                        estado = Estado.PENDIENTE,
                        creadoEn = existente?.creadoEn ?: ahora,
                        actualizadoEn = ahora,
                        externalId = tareaAula.id,
                        unidad = tareaAula.unidad,
                        inicioEn = tareaAula.inicioEn,
                        intentos = tareaAula.intentos,
                        esGrupal = tareaAula.grupal,
                        entregada = tareaAula.entregada,
                    )
                    val aGuardar = if (existente == null) {
                        base
                    } else {
                        base.copy(
                            prioridad = existente.prioridad,
                            estado = existente.estado,
                            completadoEn = existente.completadoEn,
                            archivada = existente.archivada,
                        )
                    }
                    when (repositorioTareas.upsertTareaExterna(aGuardar)) {
                        is ResultadoUpsert.Creado -> tareasNuevas++
                        is ResultadoUpsert.Actualizado -> tareasActualizadas++
                        ResultadoUpsert.Error -> errores++
                    }
                }

                alProgreso(
                    ProgresoAula(
                        cursosTotales = cursos.size,
                        cursoActual = indice + 1,
                        cursoNombre = cursoAula.nombre,
                        tareasNuevas = tareasNuevas,
                        tareasActualizadas = tareasActualizadas,
                    ),
                )
            }

            return ResultadoImportacion.Exito(
                cursos = cursosImportados,
                tareasNuevas = tareasNuevas,
                tareasActualizadas = tareasActualizadas,
                errores = errores,
                ahora = ahora,
            )
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (error: Exception) {
            return ResultadoImportacion.Fallo(
                ErrorAula.SERVIDOR,
                "No se pudo completar la importación.",
            )
        } finally {
            cliente.disconnect()
        }
    }

    private companion object {
        const val PAUSA_ENTRE_PETICIONES_MS = 300L
    }
}
