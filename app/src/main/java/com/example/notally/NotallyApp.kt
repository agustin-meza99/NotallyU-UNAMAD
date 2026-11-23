package com.example.notally

import android.app.Application
import android.content.Context
import com.example.notally.core.AppContainer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class NotallyApp : Application() {

    lateinit var contenedor: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // ISO/IEC 25010 (Fiabilidad / Disponibilidad): garantiza persistencia
        // offline explícita en disco para trabajar sin conexión a internet.
        runCatching {
            val ajustesFirestore = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = ajustesFirestore
        }
        contenedor = AppContainer(this)
    }
}

/** Acceso al contenedor desde cualquier `Context` (activities, fragments, etc.). */
val Context.contenedorNotally: AppContainer
    get() = (applicationContext as NotallyApp).contenedor
