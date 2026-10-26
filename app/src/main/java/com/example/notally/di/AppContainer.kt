package com.example.notally.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.notally.data.NotallyDatabase
import com.example.notally.data.SeedData
import com.example.notally.data.repository.SettingsRepository
import com.example.notally.data.repository.SubjectRepository
import com.example.notally.data.repository.TaskRepository
import com.example.notally.data.repository.notallySettingsDataStore

/**
 * Contenedor de dependencias manual (sin framework de inyección).
 *
 * La UI debe obtenerlo con `(context.applicationContext as NotallyApp).container`
 * o con el accesor `Context.notallyContainer`.
 */
class AppContainer(context: Context) {

    private val applicationContext = context.applicationContext

    /**
     * Equivalente a `BuildConfig.DEBUG` sin tocar Gradle (el módulo no genera
     * `BuildConfig`): un APK debuggable es exactamente el build de debug/demo.
     */
    val seedEnabled: Boolean =
        (applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    val database: NotallyDatabase = NotallyDatabase.build(applicationContext)

    val taskRepository = TaskRepository(
        taskDao = database.taskDao(),
        subtaskDao = database.subtaskDao(),
    )

    val subjectRepository = SubjectRepository(database.subjectDao())

    val settingsRepository = SettingsRepository(applicationContext.notallySettingsDataStore)

    /**
     * Siembra los datos de demo **antes** de que se pinte la UI (§7.13: nunca
     * mostrar un vacío falso mientras Room entrega su primera emisión).
     *
     * Solo en debug y solo una vez por instalación: el flag `seed_done` evita
     * que vaciar las tablas vuelva a llenarlas. En release no hace nada y la
     * app arranca vacía. Son 15 filas en una transacción; la llamada se hace en
     * `NotallyApp.onCreate` con `runBlocking(Dispatchers.IO)` para no bloquear
     * el hilo principal durante el trabajo de disco.
     */
    suspend fun seedIfNeeded(): Boolean {
        if (!seedEnabled) return false
        if (settingsRepository.isSeedDone()) return false
        val seeded = SeedData.populateIfEmpty(database)
        settingsRepository.markSeedDone()
        return seeded
    }
}
