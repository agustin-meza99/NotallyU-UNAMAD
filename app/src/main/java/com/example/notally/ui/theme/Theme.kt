package com.example.notally.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

/**
 * Tema de Notally.
 *
 * `dynamicColor` está desactivado por defecto: la identidad es la paleta del
 * documento. Si el usuario lo activa en API ≥ 31 se usa el esquema dinámico
 * completo del sistema; los acentos de materia no cambian.
 */
@Composable
fun NotallyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        darkTheme -> NotallyDarkColorScheme
        else -> NotallyLightColorScheme
    }
    val subjectPalette = if (darkTheme) SubjectPalette.Dark else SubjectPalette.Light

    CompositionLocalProvider(LocalSubjectPalette provides subjectPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = NotallyShapes,
            content = content,
        )
    }
}
