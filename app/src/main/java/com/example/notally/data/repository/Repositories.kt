package com.example.notally.data.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.notally.data.local.CursoDao
import com.example.notally.data.local.SubtareaDao
import com.example.notally.data.local.TareaDao
import com.example.notally.data.model.Curso
import com.example.notally.data.model.CursoConConteo
import com.example.notally.data.model.Estado
import com.example.notally.data.model.Settings
import com.example.notally.data.model.Subtarea
import com.example.notally.data.model.Tarea
import com.example.notally.data.model.TareaConSubtareas
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

/** Resultado explícito de escribir un curso, sin excepciones hacia la UI. */
sealed interface ResultadoCurso {
    /** El curso se insertó con este id. */
    data class Creado(val id: Long) : ResultadoCurso

    /** El curso existente se actualizó. */
    data class Actualizado(val id: Long) : ResultadoCurso

    /** Nombre repetido: la restricción única de la base lo rechazó. */
    data object Duplicado : ResultadoCurso

    /** Cualquier otro fallo de escritura. */
    data object Error : ResultadoCurso

    val esExito: Boolean
        get() = this is Creado || this is Actualizado
}

class CursosRepository(
    private val cursoDao: CursoDao,
) {

    fun observarCursos(): Flow<List<Curso>> = cursoDao.observarCursos()

    fun observarCursosConConteo(): Flow<List<CursoConConteo>> =
        cursoDao.observarCursosConConteo()

    suspend fun crearCurso(
        nombre: String,
        color: Long,
        profesor: String? = null,
        creditos: Int? = null,
    ): ResultadoCurso = escribir {
        ResultadoCurso.Creado(
            cursoDao.insertar(
                Curso(
                    nombre = nombre,
                    color = color,
                    profesor = profesor,
                    creditos = creditos,
                ),
            ),
        )
    }

    suspend fun actualizarCurso(curso: Curso): ResultadoCurso = escribir {
        cursoDao.actualizar(curso)
        ResultadoCurso.Actualizado(curso.id)
    }

    private suspend fun escribir(bloque: suspend () -> ResultadoCurso): ResultadoCurso =
        try {
            bloque()
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (_: SQLiteConstraintException) {
            ResultadoCurso.Duplicado
        } catch (_: Exception) {
            ResultadoCurso.Error
        }

    suspend fun eliminarCurso(cursoId: Long) = cursoDao.eliminarPorId(cursoId)
}

private const val NOMBRE_ARCHIVO_SETTINGS = "notally_settings"

/** DataStore de preferencias de Notally, ligado al contexto de la aplicación. */
val Context.dataStoreSettingsNotally: DataStore<Preferences> by preferencesDataStore(
    name = NOMBRE_ARCHIVO_SETTINGS,
)

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {

    val settings: Flow<Settings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferencias ->
            Settings(
                semanaEmpiezaLunes = preferencias[Claves.SEMANA_EMPIEZA_LUNES] ?: true,
            )
        }

    suspend fun cambiarSemanaEmpiezaLunes(activo: Boolean) {
        dataStore.edit { it[Claves.SEMANA_EMPIEZA_LUNES] = activo }
    }

    private object Claves {
        val SEMANA_EMPIEZA_LUNES = booleanPreferencesKey("week_starts_monday")
    }
}
