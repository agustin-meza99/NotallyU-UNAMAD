package com.example.notally.data.repository

import com.example.notally.data.firebase.AuthRepository
import com.example.notally.data.firebase.esperar
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Prioridad
import com.example.notally.data.model.Subtarea
import com.example.notally.data.model.Tarea
import com.example.notally.data.model.TareaConSubtareas
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal fun nuevoIdFirestore(): Long = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE

internal fun AuthRepository.coleccionUsuario(
    proveedorFirestore: () -> FirebaseFirestore,
    coleccion: String,
): CollectionReference? {
    val uid = usuario.value?.uid ?: return null
    return proveedorFirestore().collection("users").document(uid).collection(coleccion)
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> AuthRepository.observarColeccion(
    proveedorFirestore: () -> FirebaseFirestore,
    coleccion: String,
    mapear: (DocumentSnapshot) -> T?,
): Flow<List<T>> = usuario.flatMapLatest { user ->
    if (user == null) {
        flowOf(emptyList())
    } else {
        callbackFlow {
            val ref = proveedorFirestore()
                .collection("users")
                .document(user.uid)
                .collection(coleccion)
            val registro = ref.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull(mapear).orEmpty())
            }
            awaitClose { registro.remove() }
        }
    }
}

/**
 * Acceso de lectura/escritura a tareas y subtareas directamente en Firestore.
 *
 * La persistencia offline nativa del SDK de Firestore mantiene la caché local
 * y sincroniza en segundo plano cuando vuelve la red.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TareasRepository(
    private val authRepository: AuthRepository,
    private val proveedorFirestore: () -> FirebaseFirestore = {
        FirebaseFirestore.getInstance()
    },
) {

    private fun coleccionTareas(): CollectionReference? =
        authRepository.coleccionUsuario(proveedorFirestore, "tasks")

    private fun observarTodasLasTareas(): Flow<List<TareaConSubtareas>> =
        authRepository.observarColeccion(proveedorFirestore, "tasks") { it.aTareaConSubtareas() }

    fun observarTareasActivas(): Flow<List<Tarea>> =
        observarTodasLasTareas().map { lista ->
            lista.map { it.tarea }.filter { !it.archivada }
        }

    fun observarTareasDelDia(inicioMillis: Long, finMillis: Long): Flow<List<Tarea>> =
        observarTareasActivas().map { lista ->
            lista.filter { t ->
                val entrega = t.entregaEn ?: return@filter false
                entrega in inicioMillis..finMillis
            }
        }

    /**
     * Tareas no archivadas con `dueAt` posterior a [despuesDeMillis], sin tope
     * superior. "Próximas" la usa para no perder tareas lejanas.
     */
    fun observarTareasFuturas(despuesDeMillis: Long): Flow<List<Tarea>> =
        observarTareasActivas().map { lista ->
            lista.filter { t ->
                val entrega = t.entregaEn ?: return@filter false
                entrega > despuesDeMillis
            }
        }

    suspend fun obtenerTareaConSubtareas(tareaId: Long): TareaConSubtareas? {
        val ref = coleccionTareas() ?: return null
        val snap = ref.whereEqualTo("id", tareaId).limit(1).get().esperar()
        return snap.documents.firstOrNull()?.aTareaConSubtareas()
    }

    /** Tareas no archivadas con sus subtareas. */
    suspend fun obtenerTareasActivasConSubtareas(): List<TareaConSubtareas> {
        val ref = coleccionTareas() ?: return emptyList()
        val snap = ref.whereEqualTo("isArchived", false).get().esperar()
        return snap.documents.mapNotNull { it.aTareaConSubtareas() }
    }

    suspend fun obtenerTarea(tareaId: Long): Tarea? =
        obtenerTareaConSubtareas(tareaId)?.tarea

    suspend fun obtenerTareaPorExternalId(externalId: String): Tarea? {
        val ref = coleccionTareas() ?: return null
        val doc = ref.document(externalId).get().esperar()
        return doc.aTareaConSubtareas()?.tarea
    }

    /**
     * Inserta o actualiza una tarea importada del Aula identificada por
     * `externalId`. Nunca duplica: si ya existe, actualiza esa fila.
     */
    suspend fun upsertTareaExterna(tarea: Tarea): ResultadoUpsert {
        val externalId = tarea.externalId ?: return ResultadoUpsert.Error
        val ref = coleccionTareas() ?: return ResultadoUpsert.Error
        return try {
            val docRef = ref.document(externalId)
            val existente = docRef.get().esperar().aTareaConSubtareas()
            if (existente == null) {
                val id = nuevoIdFirestore()
                val nueva = tarea.copy(id = id)
                docRef.set(nueva.aMapaFirestore(emptyList())).esperar()
                ResultadoUpsert.Creado(id)
            } else {
                val actualizada = tarea.copy(id = existente.tarea.id)
                docRef.set(actualizada.aMapaFirestore(existente.subtareas)).esperar()
                ResultadoUpsert.Actualizado(existente.tarea.id)
            }
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (_: Exception) {
            ResultadoUpsert.Error
        }
    }

    /**
     * Inserta [tarea] (ignorando su id) y, opcionalmente, sus subtareas en el
     * orden recibido. Devuelve el id de la tarea creada.
     */
    suspend fun crearTarea(
        tarea: Tarea,
        titulosSubtareas: List<String> = emptyList(),
    ): Long {
        val ref = coleccionTareas() ?: return 0L
        val ahora = System.currentTimeMillis()
        val tareaId = nuevoIdFirestore()
        val nueva = tarea.copy(
            id = tareaId,
            creadoEn = ahora,
            actualizadoEn = ahora,
            completadoEn = if (tarea.estado == Estado.COMPLETADA) ahora else null,
        )
        val subtareas = titulosSubtareas.mapIndexed { indice, titulo ->
            Subtarea(
                id = nuevoIdFirestore(),
                tareaId = tareaId,
                titulo = titulo,
                posicion = indice,
            )
        }
        val docId = nueva.externalId ?: tareaId.toString()
        ref.document(docId).set(nueva.aMapaFirestore(subtareas)).esperar()
        return tareaId
    }

    suspend fun actualizarTarea(tarea: Tarea) {
        val ref = coleccionTareas() ?: return
        val existente = obtenerDocumentoPorTareaId(ref, tarea.id)
        val subtareas = existente?.aTareaConSubtareas()?.subtareas.orEmpty()
        val ahora = System.currentTimeMillis()
        val completadoEn = when (tarea.estado) {
            Estado.COMPLETADA -> tarea.completadoEn ?: ahora
            else -> null
        }
        val actualizada = tarea.copy(actualizadoEn = ahora, completadoEn = completadoEn)
        val docRef = existente?.reference ?: ref.document(tarea.externalId ?: tarea.id.toString())
        docRef.set(actualizada.aMapaFirestore(subtareas)).esperar()
    }

    suspend fun eliminarTarea(tareaId: Long) {
        val ref = coleccionTareas() ?: return
        val doc = obtenerDocumentoPorTareaId(ref, tareaId) ?: return
        doc.reference.delete().esperar()
    }

    suspend fun alternarCompletada(tareaId: Long, ahora: Long = System.currentTimeMillis()) {
        val ref = coleccionTareas() ?: return
        val doc = obtenerDocumentoPorTareaId(ref, tareaId) ?: return
        val actual = doc.aTareaConSubtareas() ?: return
        val nuevoEstado = if (actual.tarea.estado == Estado.COMPLETADA) {
            Estado.PENDIENTE
        } else {
            Estado.COMPLETADA
        }
        val completadoEn = if (nuevoEstado == Estado.COMPLETADA) ahora else null
        val actualizada = actual.tarea.copy(
            estado = nuevoEstado,
            completadoEn = completadoEn,
            actualizadoEn = ahora,
        )
        doc.reference.set(actualizada.aMapaFirestore(actual.subtareas)).esperar()
    }

    suspend fun cambiarEstado(
        tareaId: Long,
        estado: Estado,
        ahora: Long = System.currentTimeMillis(),
    ) {
        val ref = coleccionTareas() ?: return
        val doc = obtenerDocumentoPorTareaId(ref, tareaId) ?: return
        val actual = doc.aTareaConSubtareas() ?: return
        val completadoEn = if (estado == Estado.COMPLETADA) ahora else null
        val actualizada = actual.tarea.copy(
            estado = estado,
            completadoEn = completadoEn,
            actualizadoEn = ahora,
        )
        doc.reference.set(actualizada.aMapaFirestore(actual.subtareas)).esperar()
    }

    suspend fun agregarSubtarea(tareaId: Long, titulo: String): Long {
        val ref = coleccionTareas() ?: return 0L
        val doc = obtenerDocumentoPorTareaId(ref, tareaId) ?: return 0L
        val actual = doc.aTareaConSubtareas() ?: return 0L
        val posicion = (actual.subtareas.maxOfOrNull { it.posicion } ?: -1) + 1
        val nuevaSub = Subtarea(
            id = nuevoIdFirestore(),
            tareaId = tareaId,
            titulo = titulo,
            posicion = posicion,
        )
        val actualizada = actual.tarea.copy(actualizadoEn = System.currentTimeMillis())
        doc.reference.set(actualizada.aMapaFirestore(actual.subtareas + nuevaSub)).esperar()
        return nuevaSub.id
    }

    suspend fun cambiarSubtareaHecha(subtareaId: Long, hecha: Boolean) {
        actualizarSubtarea(subtareaId) { it.copy(hecha = hecha) }
    }

    suspend fun alternarSubtarea(subtareaId: Long) {
        actualizarSubtarea(subtareaId) { it.copy(hecha = !it.hecha) }
    }

    suspend fun eliminarSubtarea(subtareaId: Long) {
        val ref = coleccionTareas() ?: return
        val snap = ref.get().esperar()
        for (doc in snap.documents) {
            val actual = doc.aTareaConSubtareas() ?: continue
            if (actual.subtareas.any { it.id == subtareaId }) {
                val filtradas = actual.subtareas.filterNot { it.id == subtareaId }
                doc.reference.set(actual.tarea.aMapaFirestore(filtradas)).esperar()
                return
            }
        }
    }

    private suspend fun actualizarSubtarea(
        subtareaId: Long,
        transformar: (Subtarea) -> Subtarea,
    ) {
        val ref = coleccionTareas() ?: return
        val snap = ref.get().esperar()
        for (doc in snap.documents) {
            val actual = doc.aTareaConSubtareas() ?: continue
            if (actual.subtareas.any { it.id == subtareaId }) {
                val nuevas = actual.subtareas.map {
                    if (it.id == subtareaId) transformar(it) else it
                }
                doc.reference.set(actual.tarea.aMapaFirestore(nuevas)).esperar()
                return
            }
        }
    }

    private suspend fun obtenerDocumentoPorTareaId(
        ref: CollectionReference,
        tareaId: Long,
    ): DocumentSnapshot? {
        val snap = ref.whereEqualTo("id", tareaId).limit(1).get().esperar()
        return snap.documents.firstOrNull()
    }
}

private fun Tarea.aMapaFirestore(subtareas: List<Subtarea>): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to titulo,
    "notes" to notas,
    "subjectId" to cursoId,
    "dueAt" to entregaEn,
    "priority" to prioridad.name,
    "status" to estado.name,
    "createdAt" to creadoEn,
    "updatedAt" to actualizadoEn,
    "completedAt" to completadoEn,
    "isArchived" to archivada,
    "externalId" to externalId,
    "unidad" to unidad,
    "inicioEn" to inicioEn,
    "intentos" to intentos,
    "esGrupal" to esGrupal,
    "entregada" to entregada,
    "calificacion" to calificacion,
    "retroalimentacion" to retroalimentacion,
    "subtasks" to subtareas.map { sub ->
        mapOf(
            "id" to sub.id,
            "taskId" to sub.tareaId,
            "title" to sub.titulo,
            "isDone" to sub.hecha,
            "position" to sub.posicion,
        )
    },
)

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.aTareaConSubtareas(): TareaConSubtareas? {
    if (!exists()) return null
    val titulo = getString("title") ?: return null
    val tareaId = getLong("id") ?: 0L
    val prioridad = getString("priority")?.let {
        runCatching { Prioridad.valueOf(it) }.getOrNull()
    } ?: Prioridad.MEDIA
    val estado = getString("status")?.let {
        runCatching { Estado.valueOf(it) }.getOrNull()
    } ?: Estado.PENDIENTE
    val tarea = Tarea(
        id = tareaId,
        titulo = titulo,
        notas = getString("notes").orEmpty(),
        cursoId = getLong("subjectId"),
        entregaEn = getLong("dueAt"),
        prioridad = prioridad,
        estado = estado,
        creadoEn = getLong("createdAt") ?: System.currentTimeMillis(),
        actualizadoEn = getLong("updatedAt") ?: System.currentTimeMillis(),
        completadoEn = getLong("completedAt"),
        archivada = getBoolean("isArchived") ?: false,
        externalId = getString("externalId"),
        unidad = getString("unidad"),
        inicioEn = getLong("inicioEn"),
        intentos = getLong("intentos")?.toInt(),
        esGrupal = getBoolean("esGrupal") ?: false,
        entregada = getBoolean("entregada") ?: false,
        calificacion = getDouble("calificacion"),
        retroalimentacion = getString("retroalimentacion"),
    )
    val rawSubtasks = get("subtasks") as? List<Map<String, Any?>> ?: emptyList()
    val subtareas = rawSubtasks.mapNotNull { m ->
        val subTitulo = m["title"] as? String ?: return@mapNotNull null
        Subtarea(
            id = (m["id"] as? Number)?.toLong() ?: 0L,
            tareaId = (m["taskId"] as? Number)?.toLong() ?: tareaId,
            titulo = subTitulo,
            hecha = m["isDone"] as? Boolean ?: false,
            posicion = (m["position"] as? Number)?.toInt() ?: 0,
        )
    }.sortedBy { it.posicion }
    return TareaConSubtareas(tarea = tarea, subtareas = subtareas)
}
