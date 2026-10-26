package com.example.notally

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notally.data.model.Ajustes
import com.example.notally.data.model.ModoTema
import com.example.notally.ui.RaizNotally
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
        setContent {
            RaizAplicacion()
        }
    }
}

@Composable
private fun RaizAplicacion() {
    val contexto = LocalContext.current
    val ajustes by contexto.contenedorNotally.repositorioAjustes.ajustes
        .collectAsStateWithLifecycle(initialValue = Ajustes())

    val sistemaOscuro = isSystemInDarkTheme()
    val temaOscuro = when (ajustes.modoTema) {
        ModoTema.SISTEMA -> sistemaOscuro
        ModoTema.CLARO -> false
        ModoTema.OSCURO -> true
    }

    // Iconos de barra del sistema coherentes con el tema elegido en la app,
    // no con el del dispositivo.
    val vista = LocalView.current
    SideEffect {
        val ventana = (vista.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(ventana, vista).apply {
            isAppearanceLightStatusBars = !temaOscuro
            isAppearanceLightNavigationBars = !temaOscuro
        }
    }

    TemaNotally(
        darkTheme = temaOscuro,
        dynamicColor = ajustes.usarColorDinamico,
    ) {
        RaizNotally()
    }
}
