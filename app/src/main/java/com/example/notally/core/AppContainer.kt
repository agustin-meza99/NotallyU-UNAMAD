package com.example.notally.core

import android.content.Context
import com.example.notally.data.NotallyDatabase
import com.example.notally.data.aula.AulaCredencialesStore
import com.example.notally.data.aula.AulaImporter
import com.example.notally.data.repository.SettingsRepository
import com.example.notally.data.repository.CursosRepository
import com.example.notally.data.repository.TareasRepository
import com.example.notally.data.repository.dataStoreSettingsNotally

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

    val repositorioCursos = CursosRepository(baseDatos.cursoDao())

    val repositorioSettings = SettingsRepository(contextoApp.dataStoreSettingsNotally)

    val credencialesAula = AulaCredencialesStore(contextoApp)

    val importadorAula = AulaImporter(
        repositorioCursos = repositorioCursos,
        repositorioTareas = repositorioTareas,
    )
}
