package com.example.notally

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notally.data.model.Settings
import com.example.notally.data.model.TemaApp
import com.example.notally.ui.NotallyRoot
import com.example.notally.ui.theme.TemaNotally

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.isNavigationBarContrastEnforced = false
        setContent {
            val settings by contenedorNotally.repositorioSettings.settings
                .collectAsStateWithLifecycle(initialValue = Settings())
            val sistemaOscuro = isSystemInDarkTheme()
            val oscuro = when (settings.tema) {
                TemaApp.CLARO -> false
                TemaApp.OSCURO -> true
                TemaApp.SISTEMA -> sistemaOscuro
            }

            LaunchedEffect(oscuro) {
                val estilo = if (oscuro) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    )
                }
                enableEdgeToEdge(
                    statusBarStyle = estilo,
                    navigationBarStyle = estilo,
                )
            }

            RaizAplicacion(temaApp = settings.tema)
        }
    }
}

@Composable
private fun RaizAplicacion(temaApp: TemaApp) {
    TemaNotally(temaApp = temaApp) {
        NotallyRoot()
    }
}
