package com.example.notally.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Tema de Notally.
 *
 * La paleta es fija: [EsquemaColoresClaro] o [EsquemaColoresOscuro] según el
 * modo elegido. No existe variante dinámica ni ajuste que altere los roles M3.
 */
@Composable
fun TemaNotally(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) EsquemaColoresOscuro else EsquemaColoresClaro
    val paletaCursos = if (darkTheme) PaletaCursos.Oscuro else PaletaCursos.Claro

    CompositionLocalProvider(LocalPaletaCursos provides paletaCursos) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Tipografia,
            shapes = FormasNotally,
            content = content,
        )
    }
}
