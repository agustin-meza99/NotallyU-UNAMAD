package com.example.notally

import android.app.Application
import android.content.Context
import com.example.notally.core.ContenedorApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class NotallyApp : Application() {

    lateinit var contenedor: ContenedorApp
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = ContenedorApp(this)
        // El seed de demo se completa antes de que MainActivity componga Hoy:
        // así la primera emisión de Room ya trae datos y no se pinta un vacío
        // falso (§7.13). En release `sembrarSiHaceFalta` retorna sin tocar la BD.
        runBlocking(Dispatchers.IO) {
            contenedor.sembrarSiHaceFalta()
        }
    }
}

/** Acceso al contenedor desde cualquier `Context` (activities, fragments, etc.). */
val Context.contenedorNotally: ContenedorApp
    get() = (applicationContext as NotallyApp).contenedor
