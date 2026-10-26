package com.example.notally.core

import android.content.Context
import com.example.notally.data.NotallyDatabase
import com.example.notally.data.repository.AjustesRepository
import com.example.notally.data.repository.MateriasRepository
import com.example.notally.data.repository.TareasRepository
import com.example.notally.data.repository.dataStoreAjustesNotally

/**
 * Contenedor de dependencias manual (sin framework de inyección).
 *
 * La UI debe obtenerlo con `(context.applicationContext as NotallyApp).contenedor`
 * o con el accesor `Context.contenedorNotally`.
 */
class AppContainer(context: Context) {

    private val contextoApp = context.applicationContext

    val baseDatos: NotallyDatabase = NotallyDatabase.construir(contextoApp)

    val repositorioTareas = TareasRepository(
        tareaDao = baseDatos.tareaDao(),
        subtareaDao = baseDatos.subtareaDao(),
    )

    val repositorioMaterias = MateriasRepository(baseDatos.materiaDao())

    val repositorioAjustes = AjustesRepository(contextoApp.dataStoreAjustesNotally)
}
