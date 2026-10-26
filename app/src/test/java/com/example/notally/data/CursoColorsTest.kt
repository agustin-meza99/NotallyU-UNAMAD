package com.example.notally.data

import androidx.compose.ui.graphics.Color
import com.example.notally.R
import com.example.notally.ui.theme.MuestrasCursos
import com.example.notally.ui.theme.PaletaCursos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CursoColorsTest {

    @Test
    fun `hay seis acentos opacos y distintos`() {
        val colores = ColoresCurso.todos

        assertEquals(6, colores.size)
        assertEquals(6, colores.distinct().size)
        assertTrue(colores.all { (it ushr 24) == 0xFFL })
    }

    @Test
    fun `los valores respetan la tabla del documento`() {
        assertEquals(
            listOf(0xFF3A5A9EL, 0xFF2F6A4EL, 0xFF99482EL, 0xFF5F7300L, 0xFF7A5F87L, 0xFF1D5F6AL),
            ColoresCurso.todos,
        )
    }

    @Test
    fun `las muestras conservan orden nombres y variantes del documento`() {
        val muestras = MuestrasCursos

        assertEquals(ColoresCurso.todos, muestras.map { it.argb })
        assertEquals(
            listOf(
                R.string.course_color_indigo,
                R.string.course_color_forest,
                R.string.course_color_terracotta,
                R.string.course_color_lime,
                R.string.course_color_lavender,
                R.string.course_color_petrol,
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
                Color(0xFFE9ECF5),
                Color(0xFFE6EFE8),
                Color(0xFFF4E8E2),
                Color(0xFFF3F5E0),
                Color(0xFFF3EDF5),
                Color(0xFFE3EEEE),
            ),
            muestras.map { it.contenedorClaro },
        )
        muestras.forEach { muestra ->
            assertEquals(muestra.claro, PaletaCursos.Unica.colorCurso(muestra.argb))
            assertEquals(muestra.contenedorClaro, PaletaCursos.Unica.contenedorCurso(muestra.argb))
        }
    }
}
