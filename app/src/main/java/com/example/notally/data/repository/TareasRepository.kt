package com.example.notally.data.repository

import com.example.notally.data.local.SubtareaDao
import com.example.notally.data.local.TareaDao
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Subtarea
import com.example.notally.data.model.Tarea
import com.example.notally.data.model.TareaConSubtareas
import kotlinx.coroutines.flow.Flow

/**
 * Acceso de lectura/escritura a tareas y subtareas.
 *
 * Las consultas por día reciben el rango ya calculado (ver
 * [com.example.notally.data.DateRanges]) para no acoplar el repositorio a la
 * zona horaria ni al reloj del sistema.
 */
class TareasRepository(
    private val tareaDao: TareaDao,
    private val subtareaDao: SubtareaDao,
) {

    fun observarTareasActivas(): Flow<List<Tarea>> = tareaDao.observarTareasActivas()

    fun observarTareasDelDia(inicioMillis: Long, finMillis: Long): Flow<List<Tarea>> =
        tareaDao.observarTareasDelDia(inicioMillis, finMillis)

    /**
     * Tareas no archivadas con `dueAt` posterior a [despuesDeMillis], sin tope
     * superior. "Próximas" la usa para no perder tareas lejanas.
     */
    fun observarTareasFuturas(despuesDeMillis: Long): Flow<List<Tarea>> =
        tareaDao.observarTareasFuturas(despuesDeMillis)

    suspend fun obtenerTareaConSubtareas(tareaId: Long): TareaConSubtareas? =
        tareaDao.obtenerTareaConSubtareas(tareaId)

    suspend fun obtenerTarea(tareaId: Long): Tarea? = tareaDao.obtenerTarea(tareaId)

    /**
     * Inserta [tarea] (ignorando su id) y, opcionalmente, sus subtareas en el
     * orden recibido. Devuelve el id de la tarea creada.
     */
    suspend fun crearTarea(
        tarea: Tarea,
        titulosSubtareas: List<String> = emptyList(),
    ): Long {
        val ahora = System.currentTimeMillis()
        val tareaId = tareaDao.insertar(
            tarea.copy(
                id = 0L,
                creadoEn = ahora,
                actualizadoEn = ahora,
                completadoEn = if (tarea.estado == Estado.COMPLETADA) ahora else null,
            ),
        )
        if (titulosSubtareas.isNotEmpty()) {
            subtareaDao.insertarTodas(
                titulosSubtareas.mapIndexed { indice, titulo ->
                    Subtarea(tareaId = tareaId, titulo = titulo, posicion = indice)
                },
            )
        }
        return tareaId
    }

    suspend fun actualizarTarea(tarea: Tarea) {
        val ahora = System.currentTimeMillis()
        val completadoEn = when (tarea.estado) {
            Estado.COMPLETADA -> tarea.completadoEn ?: ahora
            else -> null
        }
        tareaDao.actualizar(tarea.copy(actualizadoEn = ahora, completadoEn = completadoEn))
    }

    suspend fun eliminarTarea(tareaId: Long) = tareaDao.eliminarPorId(tareaId)

    suspend fun alternarCompletada(tareaId: Long, ahora: Long = System.currentTimeMillis()) =
        tareaDao.alternarCompletada(tareaId, ahora)

    suspend fun cambiarEstado(
        tareaId: Long,
        estado: Estado,
        ahora: Long = System.currentTimeMillis(),
    ) {
        val completadoEn = if (estado == Estado.COMPLETADA) ahora else null
        tareaDao.cambiarEstado(tareaId, estado, completadoEn, ahora)
    }

    suspend fun agregarSubtarea(tareaId: Long, titulo: String): Long {
        val posicion = subtareaDao.siguientePosicion(tareaId)
        val id = subtareaDao.insertar(
            Subtarea(tareaId = tareaId, titulo = titulo, posicion = posicion),
        )
        tareaDao.tocar(tareaId, System.currentTimeMillis())
        return id
    }

    suspend fun cambiarSubtareaHecha(subtareaId: Long, hecha: Boolean) =
        subtareaDao.cambiarHecha(subtareaId, hecha)

    suspend fun alternarSubtarea(subtareaId: Long) = subtareaDao.alternarHecha(subtareaId)

    suspend fun eliminarSubtarea(subtareaId: Long) = subtareaDao.eliminarPorId(subtareaId)
}
