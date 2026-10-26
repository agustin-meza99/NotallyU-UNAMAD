package com.example.notally

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.example.notally.ui.NotallyRoot
import com.example.notally.ui.theme.TemaNotally

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tema claro único: iconos de barra oscuros sobre fondo transparente.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        // El cluster flota sobre el contenido y consume el inset de navegación
        // con `navigationBarsPadding()`; sin el scrim del sistema la píldora no
        // queda apoyada sobre una banda opaca.
        window.isNavigationBarContrastEnforced = false
        setContent {
            RaizAplicacion()
        }
    }
}

@Composable
private fun RaizAplicacion() {
    TemaNotally {
        NotallyRoot()
    }
}
