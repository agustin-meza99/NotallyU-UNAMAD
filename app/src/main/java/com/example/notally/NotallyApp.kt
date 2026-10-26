package com.example.notally

import android.app.Application
import android.content.Context
import com.example.notally.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class NotallyApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // El seed de demo se completa antes de que MainActivity componga Hoy:
        // así la primera emisión de Room ya trae datos y no se pinta un vacío
        // falso (§7.13). En release `seedIfNeeded` retorna sin tocar la BD.
        runBlocking(Dispatchers.IO) {
            container.seedIfNeeded()
        }
    }
}

/** Acceso al contenedor desde cualquier `Context` (activities, fragments, etc.). */
val Context.notallyContainer: AppContainer
    get() = (applicationContext as NotallyApp).container
