package com.example.notally

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notally.data.model.Settings
import com.example.notally.data.model.TemaApp
import com.example.notally.ui.NotallyRoot
import com.example.notally.ui.theme.TemaNotally

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
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

            val view = LocalView.current
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !oscuro
                controller.isAppearanceLightNavigationBars = !oscuro
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
