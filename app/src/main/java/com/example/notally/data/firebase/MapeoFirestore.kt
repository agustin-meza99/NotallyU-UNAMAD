package com.example.notally.data.firebase

import com.example.notally.data.model.Curso
import com.example.notally.data.model.Subtarea
import com.example.notally.data.model.Tarea
import com.example.notally.data.remote.firestore.toRemote

/**
 * Marca de tiempo de un documento que todavía no conoce Firestore.
 *
 * Mantiene el mapeo puro y testeable en JVM: [Servidor] se traduce a
 * `FieldValue.serverTimestamp()` en el borde y [Existente] al `Timestamp` que ya
 * tiene el documento remoto, tal como exigen las reglas en un update.
 */
sealed interface MarcaTiempo {
    data object Servidor : MarcaTiempo
    data class Existente(val epochMillis: Long) : MarcaTiempo
}

/** Documento listo para escribir: el id va en el path, no en los campos. */
data class DocumentoSync(
    val id: String,
    val campos: Map<String, Any?>,
)

/**
 * `createdAt` remoto que debe llevar un documento.
 *
 * En create no existe todavía: se usa [MarcaTiempo.Servidor]. En update se
 * reenvía el valor leído del documento porque las reglas exigen
 * `request.resource.data.createdAt == resource.data.createdAt`.
 */
fun marcaCreadoEn(creadoEnRemoto: Long?): MarcaTiempo =
    creadoEnRemoto?.let(MarcaTiempo::Existente) ?: MarcaTiempo.Servidor

/**
 * Mapea un curso local a los campos de `users/{uid}/courses/{courseId}`.
 *
 * Devuelve `null` si el curso no tiene `externalId`: los cursos manuales no se
 * suben en V1 porque no tienen un docId estable entre dispositivos.
 *
 * @param creadoEnRemoto `createdAt` del documento existente, o `null` si es
 *   nuevo.
 * @throws IllegalArgumentException si el curso rompe los límites de las reglas.
 */
fun construirDocumentoCurso(
    curso: Curso,
    creadoEnRemoto: Long?,
): DocumentoSync? {
    val docId = curso.externalId?.takeIf { it.isNotBlank() } ?: return null
    val remoto = curso.toRemote()
    return DocumentoSync(
        id = docId,
        campos = mapOf(
            "name" to remoto.name,
            "professor" to remoto.professor,
            "createdAt" to marcaCreadoEn(creadoEnRemoto),
            "updatedAt" to MarcaTiempo.Servidor,
            "deletedAt" to null,
        ),
    )
}

/**
 * Mapea una tarea local a los campos de `users/{uid}/tasks/{taskId}`.
 *
 * Devuelve `null` para tareas sin `externalId` (no vienen del Aula y no tienen
 * docId estable) y para tareas archivadas: las reglas no tienen campo
 * `isArchived` y V1 no las sube (ver `docs/ARCHITECTURE.md` 5.3).
 *
 * @param cursoExternalId `externalId` del curso local; `null` si la tarea no
 *   tiene curso o el curso es manual y no se subió.
 * @param creadoEnRemoto `createdAt` del documento existente, o `null` si es
 *   nuevo.
 * @throws IllegalArgumentException si la tarea rompe los límites de las reglas.
 */
fun construirDocumentoTarea(
    tarea: Tarea,
    subtareas: List<Subtarea> = emptyList(),
    cursoExternalId: String? = null,
    creadoEnRemoto: Long? = null,
): DocumentoSync? {
    val docId = tarea.externalId?.takeIf { it.isNotBlank() } ?: return null
    if (tarea.archivada) return null
    val remoto = tarea.toRemote(
        subtasks = subtareas,
        courseId = cursoExternalId,
    )
    return DocumentoSync(
        id = docId,
        campos = mapOf(
            "title" to remoto.title,
            "notes" to remoto.notes,
            "courseId" to remoto.courseId,
            "dueAt" to remoto.dueAt,
            "priority" to remoto.priority,
            "status" to remoto.status,
            "subtasks" to remoto.subtasks.map { subtarea ->
                mapOf(
                    "id" to subtarea.id,
                    "title" to subtarea.title,
                    "isDone" to subtarea.isDone,
                    "position" to subtarea.position,
                )
            },
            "createdAt" to marcaCreadoEn(creadoEnRemoto),
            "updatedAt" to MarcaTiempo.Servidor,
            "completedAt" to remoto.completedAt,
            "deletedAt" to null,
        ),
    )
}
