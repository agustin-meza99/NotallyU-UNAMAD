package com.example.notally.data

import androidx.compose.ui.graphics.Color
import com.example.notally.R
import com.example.notally.ui.theme.MuestrasMaterias
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColoresMateriaTest {

    @Test
    fun `hay seis acentos opacos y distintos`() {
        val colores = ColoresMateria.todos

        assertEquals(6, colores.size)
        assertEquals(6, colores.distinct().size)
        assertTrue(colores.all { (it ushr 24) == 0xFFL })
    }

    @Test
    fun `los valores respetan la tabla del documento`() {
        assertEquals(
            listOf(0xFF3A5A9EL, 0xFF2F6A4EL, 0xFF99482EL, 0xFF5F7300L, 0xFF7A5F87L, 0xFF1D5F6AL),
            ColoresMateria.todos,
        )
    }

    @Test
    fun `las muestras conservan orden nombres y variantes del documento`() {
        val muestras = MuestrasMaterias

        assertEquals(ColoresMateria.todos, muestras.map { it.argb })
        assertEquals(
            listOf(
                R.string.subject_color_indigo,
                R.string.subject_color_forest,
                R.string.subject_color_terracotta,
                R.string.subject_color_lime,
                R.string.subject_color_lavender,
                R.string.subject_color_petrol,
            ),
            muestras.map { it.recursoNombre },
        )
        assertEquals(
            listOf(
                Color(0xFF3A5A9E),
                Color(0xFF2F6A4E),
                Color(0xFF99482E),
                Color(0xFF5F7300),
                Color(0xFF7A5F87),
                Color(0xFF1D5F6A),
            ),
            muestras.map { it.claro },
        )
        assertEquals(
            listOf(
                Color(0xFFAFC3EE),
                Color(0xFF9FC9B2),
                Color(0xFFE7AE97),
                Color(0xFFD8E5A0),
                Color(0xFFCAA8B8),
                Color(0xFF96CDD6),
            ),
            muestras.map { it.oscuro },
        )
        assertEquals(
            listOf(
                Color(0xFFE9ECF5),
                Color(0xFFE6EFE8),
                Color(0xFFF4E8E2),
                Color(0xFFF3F5E0),
                Color(0xFFF3EDF5),
                Color(0xFFE3EEEE),
            ),
            muestras.map { it.contenedorClaro },
        )
        assertEquals(
            listOf(
                Color(0xFF222B3F),
                Color(0xFF1D2C24),
                Color(0xFF33231C),
                Color(0xFF2A2B16),
                Color(0xFF2B2233),
                Color(0xFF182B2E),
            ),
            muestras.map { it.contenedorOscuro },
        )
        muestras.forEach { muestra ->
            assertEquals(muestra.claro, muestra.acento(esOscuro = false))
            assertEquals(muestra.oscuro, muestra.acento(esOscuro = true))
            assertEquals(muestra.contenedorClaro, muestra.contenedor(esOscuro = false))
            assertEquals(muestra.contenedorOscuro, muestra.contenedor(esOscuro = true))
            assertEquals(muestra.claro, muestra.sobreContenedor(esOscuro = false))
            assertEquals(muestra.oscuro, muestra.sobreContenedor(esOscuro = true))
        }
    }
}
