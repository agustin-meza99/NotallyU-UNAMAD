package com.example.notally.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Espaciado del DESIGN.md §4.1 (múltiplos de 4). */
object EspaciadoNotally {
    val x1 = 4.dp
    val x2 = 8.dp
    val x3 = 12.dp
    val x4 = 16.dp
    val x5 = 24.dp
    val x6 = 32.dp
    val x7 = 48.dp

    /** Padding inferior de listas con FAB. */
    val rellenoInferiorLista = 96.dp

    /** Banda del FAB de Hoy: 52 dp de alto + 16 dp de margen inferior. */
    val bandaFab = 68.dp

    /**
     * Padding inferior de una lista que comparte espacio con el FAB: base más
     * la banda del FAB, para que la última fila nunca quede debajo (§4.1).
     */
    val rellenoInferiorListaConFab = rellenoInferiorLista + bandaFab

    /** Ancho máximo de lectura. */
    val anchoMaximoContenido = 680.dp
}

/**
 * Margen lateral de pantalla: 20 dp; 24 dp en ventanas de 600 dp o más.
 * Se calcula con la configuración local para no fijar un valor único.
 */
val margenHorizontalPantalla: Dp
    @Composable
    @ReadOnlyComposable
    get() = if (LocalConfiguration.current.screenWidthDp >= 600) 24.dp else 20.dp
