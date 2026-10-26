package com.example.notally.core

import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.notally.data.BaseDatosNotally
import com.example.notally.data.DatosSemilla
import com.example.notally.data.repository.RepositorioAjustes
import com.example.notally.data.repository.RepositorioMaterias
import com.example.notally.data.repository.RepositorioTareas
import com.example.notally.data.repository.dataStoreAjustesNotally

/**
 * Contenedor de dependencias manual (sin framework de inyección).
 *
 * La UI debe obtenerlo con `(context.applicationContext as NotallyApp).contenedor`
 * o con el accesor `Context.contenedorNotally`.
 */
class ContenedorApp(context: Context) {

    private val contextoApp = context.applicationContext

    /**
     * Equivalente a `BuildConfig.DEBUG` sin tocar Gradle (el módulo no genera
     * `BuildConfig`): un APK debuggable es exactamente el build de debug/demo.
     */
    val semillaActiva: Boolean =
        (contextoApp.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    val baseDatos: BaseDatosNotally = BaseDatosNotally.construir(contextoApp)

    val repositorioTareas = RepositorioTareas(
        tareaDao = baseDatos.tareaDao(),
        subtareaDao = baseDatos.subtareaDao(),
    )

    val repositorioMaterias = RepositorioMaterias(baseDatos.materiaDao())

    val repositorioAjustes = RepositorioAjustes(contextoApp.dataStoreAjustesNotally)

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
    suspend fun sembrarSiHaceFalta(): Boolean {
        if (!semillaActiva) return false
        if (repositorioAjustes.semillaCompletada()) return false
        val sembrado = DatosSemilla.poblarSiVacio(baseDatos)
        repositorioAjustes.marcarSemillaCompletada()
        return sembrado
    }
}
