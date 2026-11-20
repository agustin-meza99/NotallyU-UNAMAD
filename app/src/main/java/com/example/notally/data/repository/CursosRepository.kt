package com.example.notally.data.repository

import com.example.notally.data.firebase.AuthRepository
import com.example.notally.data.firebase.esperar
import com.example.notally.data.model.Curso
import com.example.notally.data.model.CursoConConteo
import com.example.notally.data.model.Estado
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Resultado explícito de escribir un curso, sin excepciones hacia la UI. */
sealed interface ResultadoCurso {
    /** El curso se insertó con este id. */
    data class Creado(val id: Long) : ResultadoCurso

    /** El curso existente se actualizó. */
    data class Actualizado(val id: Long) : ResultadoCurso

    /** Nombre repetido. */
    data object Duplicado : ResultadoCurso

    /** Cualquier otro fallo de escritura. */
    data object Error : ResultadoCurso

    val esExito: Boolean
        get() = this is Creado || this is Actualizado
}

/** Resultado de un `upsert` por `externalId` desde el Aula Virtual. */
sealed interface ResultadoUpsert {
    data class Creado(val id: Long) : ResultadoUpsert
    data class Actualizado(val id: Long) : ResultadoUpsert
    data object Error : ResultadoUpsert
}

@OptIn(ExperimentalCoroutinesApi::class)
class CursosRepository(
    private val authRepository: AuthRepository,
    private val tareasRepository: TareasRepository,
    private val proveedorFirestore: () -> FirebaseFirestore = {
        FirebaseFirestore.getInstance()
    },
) {

    private fun coleccionCursos(): CollectionReference? =
        authRepository.coleccionUsuario(proveedorFirestore, "courses")

    fun observarCursos(): Flow<List<Curso>> =
        authRepository.observarColeccion(proveedorFirestore, "courses") { it.aCurso() }
            .map { lista -> lista.sortedBy { it.nombre.lowercase() } }

    /** Todos los cursos. */
    suspend fun obtenerCursos(): List<Curso> {
        val ref = coleccionCursos() ?: return emptyList()
        return ref.get().esperar().documents.mapNotNull { it.aCurso() }
    }

    fun observarCursosConConteo(): Flow<List<CursoConConteo>> =
        combine(
            observarCursos(),
            tareasRepository.observarTareasActivas(),
        ) { cursos, tareas ->
            cursos.map { curso ->
                val delCurso = tareas.filter { it.cursoId == curso.id }
                CursoConConteo(
                    curso = curso,
                    tareasPendientes = delCurso.count { it.estado != Estado.COMPLETADA },
                    tareasEntregadas = delCurso.count { it.entregada },
                )
            }
        }

    suspend fun crearCurso(
        nombre: String,
        profesor: String? = null,
        creditos: Int? = null,
    ): ResultadoCurso = escribir {
        val ref = coleccionCursos() ?: return@escribir ResultadoCurso.Error
        val existentes = ref.whereEqualTo("name", nombre).limit(1).get().esperar()
        if (!existentes.isEmpty) return@escribir ResultadoCurso.Duplicado
        val id = nuevoIdFirestore()
        val curso = Curso(
            id = id,
            nombre = nombre,
            profesor = profesor,
            creditos = creditos,
        )
        ref.document(id.toString()).set(curso.aMapaFirestore()).esperar()
        ResultadoCurso.Creado(id)
    }

    suspend fun actualizarCurso(curso: Curso): ResultadoCurso = escribir {
        val ref = coleccionCursos() ?: return@escribir ResultadoCurso.Error
        val docId = curso.externalId ?: curso.id.toString()
        ref.document(docId).set(curso.aMapaFirestore()).esperar()
        ResultadoCurso.Actualizado(curso.id)
    }

    suspend fun obtenerCursoPorExternalId(externalId: String): Curso? {
        val ref = coleccionCursos() ?: return null
        return ref.document(externalId).get().esperar().aCurso()
    }

    /**
     * Inserta o actualiza un curso importado del Aula identificado por
     * `externalId`. Nunca duplica: si ya existe, conserva su id y su fecha de
     * creación.
     */
    suspend fun upsertCursoExterno(curso: Curso): ResultadoUpsert {
        val externalId = curso.externalId ?: return ResultadoUpsert.Error
        val ref = coleccionCursos() ?: return ResultadoUpsert.Error
        return try {
            val docRef = ref.document(externalId)
            val existente = docRef.get().esperar().aCurso()
            if (existente == null) {
                val id = nuevoIdFirestore()
                docRef.set(curso.copy(id = id).aMapaFirestore()).esperar()
                ResultadoUpsert.Creado(id)
            } else {
                val actualizado = curso.copy(id = existente.id, creadoEn = existente.creadoEn)
                docRef.set(actualizado.aMapaFirestore()).esperar()
                ResultadoUpsert.Actualizado(existente.id)
            }
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (_: Exception) {
            ResultadoUpsert.Error
        }
    }

    private suspend fun escribir(bloque: suspend () -> ResultadoCurso): ResultadoCurso =
        try {
            bloque()
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (_: Exception) {
            ResultadoCurso.Error
        }

    suspend fun eliminarCurso(cursoId: Long) {
        val ref = coleccionCursos() ?: return
        val snap = ref.whereEqualTo("id", cursoId).limit(1).get().esperar()
        snap.documents.firstOrNull()?.reference?.delete()?.esperar()
    }
}

private fun Curso.aMapaFirestore(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to nombre,
    "professor" to profesor,
    "credits" to creditos,
    "createdAt" to creadoEn,
    "externalId" to externalId,
    "codigo" to codigo,
    "seccion" to seccion,
    "periodo" to periodo,
    "tipo" to tipo,
)

private fun DocumentSnapshot.aCurso(): Curso? {
    if (!exists()) return null
    val nombre = getString("name") ?: return null
    return Curso(
        id = getLong("id") ?: 0L,
        nombre = nombre,
        profesor = getString("professor"),
        creditos = getLong("credits")?.toInt(),
        creadoEn = getLong("createdAt") ?: System.currentTimeMillis(),
        externalId = getString("externalId"),
        codigo = getString("codigo"),
        seccion = getString("seccion"),
        periodo = getString("periodo"),
        tipo = getString("tipo"),
    )
}
