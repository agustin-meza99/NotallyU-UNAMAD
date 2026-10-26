package com.example.notally

import android.app.Application
import android.content.Context
import com.example.notally.core.AppContainer

class NotallyApp : Application() {

    lateinit var contenedor: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = AppContainer(this)
    }
}

/** Acceso al contenedor desde cualquier `Context` (activities, fragments, etc.). */
val Context.contenedorNotally: AppContainer
    get() = (applicationContext as NotallyApp).contenedor
