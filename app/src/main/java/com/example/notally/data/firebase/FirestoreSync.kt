package com.example.notally.data.firebase

import com.example.notally.data.repository.CursosRepository
import com.example.notally.data.repository.TareasRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import java.io.IOException
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Contadores de una subida. `actualizados` es el subconjunto ya existente. */
data class ResultadoSync(
    val cursosSubidos: Int = 0,
    val tareasSubidas: Int = 0,
    val actualizados: Int = 0,
    val errores: Int = 0,
)

/**
 * Subida unidireccional de cursos y tareas locales a Firestore.
 *
 * - Solo sube entidades con `externalId` (importadas del Aula). Los cursos y
 *   tareas manuales se quedan en Room: no tienen docId estable entre
 *   dispositivos.
 * - Es idempotente: el docId es `externalId`, así que reimportar o volver a
 *   sincronizar actualiza el mismo documento en lugar de duplicarlo.
 * - Antes de escribir lee `createdAt` de los documentos existentes. En update
 *   las reglas exigen conservarlo; en create se usa
 *   `FieldValue.serverTimestamp()`.
 * - No borra nada, ni remoto ni local. No toca Room.
 * - `Firestore` se obtiene de forma perezosa, después de comprobar la sesión:
 *   sin usuario no se inicializa ni se hace ninguna lectura anónima.
 * - Reintenta solo errores de red (máximo [MAX_REINTENTOS]); un
 *   `PERMISSION_DENIED` falla de inmediato.
 */
class FirestoreSync(
    private val authRepository: AuthRepository,
    private val repositorioCursos: CursosRepository,
    private val repositorioTareas: TareasRepository,
    private val proveedorFirestore: () -> FirebaseFirestore = {
        FirebaseFirestore.getInstance()
    },
    private val espera: suspend (Long) -> Unit = { delay(it) },
) {

    /**
     * Sube cursos y tareas del usuario autenticado.
     *
     * Si no hay sesión devuelve un resultado vacío y no toca Firestore. Los
     * errores de red o de permisos se cuentan en [ResultadoSync.errores] sin
     * lanzar; una cancelación de la corrutina sí se propaga.
     */
    suspend fun subirTodo(): ResultadoSync {
        val usuario = authRepository.usuario.value ?: return ResultadoSync()
        return try {
            subir(usuario.uid)
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (_: Exception) {
            // Fallo de lectura previa: no se escribe nada para no romper las
            // reglas de createdAt.
            ResultadoSync(errores = 1)
        }
    }

    private suspend fun subir(uid: String): ResultadoSync {
        val cursos = repositorioCursos.obtenerCursos()
        val tareas = repositorioTareas.obtenerTareasActivasConSubtareas()

        // Nada que subir: no se inicializa Firestore (evita lecturas vacías).
        if (cursos.none { it.externalId != null } &&
            tareas.none { it.tarea.externalId != null }
        ) {
            return ResultadoSync()
        }

        val firestore = proveedorFirestore()
        val cursosRef = firestore.collection("users").document(uid).collection("courses")
        val tareasRef = firestore.collection("users").document(uid).collection("tasks")

        // Las dos lecturas ocurren antes de cualquier escritura.
        val creadosCurso = conReintentosRed { leerCreados(cursosRef) }
        val creadosTarea = conReintentosRed { leerCreados(tareasRef) }

        var errores = 0
        val documentosCurso = mutableListOf<DocumentoSync>()
        cursos.forEach { curso ->
            try {
                construirDocumentoCurso(curso, creadosCurso[curso.externalId])?.let {
                    documentosCurso += it
                }
            } catch (_: IllegalArgumentException) {
                // Rompe los límites de firestore.rules: no se sube, se cuenta.
                errores++
            }
        }

        val cursosPorId = cursos.associateBy { it.id }
        val documentosTarea = mutableListOf<DocumentoSync>()
        tareas.forEach { tareaLocal ->
            val tarea = tareaLocal.tarea
            val externalId = tarea.externalId ?: return@forEach
            val cursoExternalId = tarea.cursoId
                ?.let { cursosPorId[it]?.externalId }
                ?.takeIf { it.isNotBlank() }
            try {
                construirDocumentoTarea(
                    tarea = tarea,
                    subtareas = tareaLocal.subtareas,
                    cursoExternalId = cursoExternalId,
                    creadoEnRemoto = creadosTarea[externalId],
                )?.let { documentosTarea += it }
            } catch (_: IllegalArgumentException) {
                errores++
            }
        }

        val cursosExistentes = creadosCurso.keys
        val tareasExistentes = creadosTarea.keys
        var cursosSubidos = 0
        var tareasSubidas = 0
        var actualizados = 0

        documentosCurso.chunked(MAX_LOTE).forEach { lote ->
            val exito = escribirLote(firestore, cursosRef, lote)
            if (exito) {
                cursosSubidos += lote.size
                actualizados += lote.count { it.id in cursosExistentes }
            } else {
                errores += lote.size
            }
        }

        documentosTarea.chunked(MAX_LOTE).forEach { lote ->
            val exito = escribirLote(firestore, tareasRef, lote)
            if (exito) {
                tareasSubidas += lote.size
                actualizados += lote.count { it.id in tareasExistentes }
            } else {
                errores += lote.size
            }
        }

        return ResultadoSync(
            cursosSubidos = cursosSubidos,
            tareasSubidas = tareasSubidas,
            actualizados = actualizados,
            errores = errores,
        )
    }

    /** Escribe [lote] en un batch atómico; reintenta el lote completo si es red. */
    private suspend fun escribirLote(
        firestore: FirebaseFirestore,
        coleccion: CollectionReference,
        lote: List<DocumentoSync>,
    ): Boolean = try {
        conReintentosRed {
            val batch = firestore.batch()
            lote.forEach { documento ->
                batch.set(
                    coleccion.document(documento.id),
                    documento.campos.aCamposFirestore(),
                )
            }
            batch.commit().esperar()
        }
        true
    } catch (cancelada: CancellationException) {
        throw cancelada
    } catch (_: Exception) {
        false
    }

    private suspend fun leerCreados(
        coleccion: CollectionReference,
    ): Map<String, Long?> {
        val snapshot = coleccion.get(Source.SERVER).esperar()
        return snapshot.documents.associate { documento ->
            documento.id to documento.getTimestamp("createdAt")?.toDate()?.time
        }
    }

    /** Reintenta solo fallos de red, hasta [MAX_REINTENTOS] veces. */
    private suspend fun <T> conReintentosRed(bloque: suspend () -> T): T {
        var intento = 0
        while (true) {
            try {
                return bloque()
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (error: Exception) {
                if (!error.esErrorRed() || intento >= MAX_REINTENTOS) throw error
                espera(ESPERA_BASE_MS * (intento + 1))
                intento++
            }
        }
    }

    private fun Throwable.esErrorRed(): Boolean = when (this) {
        is FirebaseNetworkException -> true
        is IOException -> true
        is FirebaseFirestoreException -> code in CODIGOS_RED
        else -> false
    }

    private companion object {
        const val MAX_LOTE = 450
        const val MAX_REINTENTOS = 2
        const val ESPERA_BASE_MS = 500L

        val CODIGOS_RED = setOf(
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.ABORTED,
            FirebaseFirestoreException.Code.INTERNAL,
        )
    }
}

/** Traduce las marcas puras del mapper a valores que entiende Firestore. */
internal fun Map<String, Any?>.aCamposFirestore(): Map<String, Any?> =
    entries.associate { (clave, valor) -> clave to valor.aValorFirestore() }

private fun Any?.aValorFirestore(): Any? = when (this) {
    MarcaTiempo.Servidor -> FieldValue.serverTimestamp()
    is MarcaTiempo.Existente -> Timestamp(Date(epochMillis))
    else -> this
}
