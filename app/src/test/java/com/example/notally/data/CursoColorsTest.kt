package com.example.notally.data

import androidx.compose.ui.graphics.Color
import com.example.notally.R
import com.example.notally.ui.theme.SwatchesCursos
import com.example.notally.ui.theme.PaletteCursos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CursoColorsTest {

    @Test
    fun `hay seis acentos opacos y distintos`() {
        val colors = ColorsCurso.all

        assertEquals(6, colors.size)
        assertEquals(6, colors.distinct().size)
        assertTrue(colors.all { (it ushr 24) == 0xFFL })
    }

    @Test
    fun `los valores respetan la tabla del documento`() {
        assertEquals(
            listOf(0xFF3A5A9EL, 0xFF2F6A4EL, 0xFF99482EL, 0xFF5F7300L, 0xFF7A5F87L, 0xFF1D5F6AL),
            ColorsCurso.all,
        )
    }

    @Test
    fun `las muestras conservan orden nombres y variantes del documento`() {
        val swatches = SwatchesCursos

        assertEquals(ColorsCurso.all, swatches.map { it.argb })
        assertEquals(
            listOf(
                R.string.course_color_indigo,
                R.string.course_color_forest,
                R.string.course_color_terracotta,
                R.string.course_color_lime,
                R.string.course_color_lavender,
                R.string.course_color_petrol,
            ),
            swatches.map { it.nameRes },
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
            swatches.map { it.light },
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
            swatches.map { it.lightContainer },
        )
        swatches.forEach { swatch ->
            assertEquals(swatch.light, PaletteCursos.Single.colorCurso(swatch.argb))
            assertEquals(swatch.lightContainer, PaletteCursos.Single.contenedorCurso(swatch.argb))
        }
    }
}
